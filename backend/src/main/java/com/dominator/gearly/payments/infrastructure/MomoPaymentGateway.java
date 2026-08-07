package com.dominator.gearly.payments.infrastructure;

import com.dominator.gearly.payments.domain.ExchangeRateProvider;
import com.dominator.gearly.payments.domain.GatewaySettlement;
import com.dominator.gearly.payments.domain.InvalidGatewaySignatureException;
import com.dominator.gearly.payments.domain.MalformedGatewayNotificationException;
import com.dominator.gearly.payments.domain.PaymentGateway;
import com.dominator.gearly.shared.domain.Money;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The MoMo adapter — every fact about MoMo, and nothing else in the codebase knowing any of
 * them.
 *
 * <p>Before S13 those facts were spread across three files in two packages: the outbound
 * signature in {@code MomoService}, the callback signature in {@code PaymentController}, the
 * {@code Gearly-} order-id prefix written in the first and undone in
 * {@code OnlinePaymentService}, and {@code resultCode == 0} meaning success known to all three.
 * A provider change touched all of them, and the HMAC helper had already been copied between
 * two of them.
 *
 * <h2>The signature strings are reproduced exactly, deliberately</h2>
 * The canonical strings below are byte-for-byte what the two old copies built, including one
 * oddity: an absent field concatenates as the literal {@code "null"} (Java string concatenation
 * of a null reference), while {@code payType} alone is coalesced to {@code ""}. That asymmetry
 * is almost certainly not what MoMo does on its side.
 *
 * <p>It is preserved anyway. Changing it can only be validated against the MoMo sandbox with
 * real credentials, which this refactor does not have, and the failure mode of getting it wrong
 * is that live payments stop verifying. A refactor does not get to guess at a payment
 * provider's canonicalisation. {@code MomoPaymentGatewayTest} pins both renderings so the
 * behaviour is described rather than merely inherited, and fixing it is a one-line change with
 * a sandbox run behind it.
 */
public class MomoPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MomoPaymentGateway.class);

    /** MoMo namespaces our order ids; {@link #verifyNotification} is where it comes back off. */
    private static final String ORDER_ID_PREFIX = "Gearly-";

    private static final String REQUEST_TYPE = "payWithMethod";

    /** MoMo's own success code. Known here and nowhere else. */
    private static final int SUCCESS = 0;

    private static final DateTimeFormatter RESPONSE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId GATEWAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MomoProperties momo;
    private final ExchangeRateProvider exchangeRates;
    private final ObjectMapper json;
    private final RestClient http;

    /**
     * Takes a built {@link RestClient} rather than a builder so a test can bind
     * {@code MockRestServiceServer} to it — the point of the plan's "inject {@code RestClient}
     * as a bean instead of {@code new RestTemplate()}" item. A {@code new}'d client is a field
     * no test can reach, which is why {@code MomoService.createPaymentUrl} had no coverage at
     * all.
     */
    public MomoPaymentGateway(MomoProperties momo,
                              ExchangeRateProvider exchangeRates,
                              ObjectMapper json,
                              RestClient http) {
        this.momo = momo;
        this.exchangeRates = exchangeRates;
        this.json = json;
        this.http = http;
    }

    // ---- outbound: sending the customer to pay -----------------------------

    @Override
    public String startCheckout(Money amount, String orderReference) {
        String amountVnd = String.valueOf(exchangeRates.usdToVnd()
                .multiply(amount.amount())
                .setScale(0, RoundingMode.HALF_UP)
                .longValue());

        String momoOrderId = ORDER_ID_PREFIX + orderReference;
        String orderInfo = "Gearly Purchase - Order #" + orderReference;
        String extraData = Base64.getEncoder().encodeToString(
                String.format("{\"orderId\":\"%s\"}", orderReference).getBytes(StandardCharsets.UTF_8));

        String rawSignature = String.join("&",
                "accessKey=" + momo.accessKey(),
                "amount=" + amountVnd,
                "extraData=" + extraData,
                "ipnUrl=" + momo.notifyUrl(),
                "orderId=" + momoOrderId,
                "orderInfo=" + orderInfo,
                "partnerCode=" + momo.partnerCode(),
                "redirectUrl=" + momo.returnUrl(),
                "requestId=" + momoOrderId,
                "requestType=" + REQUEST_TYPE);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", momo.partnerCode());
        payload.put("accessKey", momo.accessKey());
        payload.put("requestId", momoOrderId);
        payload.put("amount", amountVnd);
        payload.put("orderId", momoOrderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", momo.returnUrl());
        payload.put("ipnUrl", momo.notifyUrl());
        payload.put("extraData", extraData);
        payload.put("requestType", REQUEST_TYPE);
        payload.put("signature", HmacSha256.hexDigest(rawSignature, momo.secretKey()));
        payload.put("lang", "en");

        JsonNode response = http.post()
                .uri(momo.createUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.hasNonNull("payUrl")) {
            // The gateway answers 200 with a non-zero resultCode for a rejected request, so a
            // missing payUrl is the actual signal — checking the HTTP status alone (which is
            // what MomoService did) reads "your signature is wrong" as success.
            log.error("MoMo declined the checkout for order {}: {}", orderReference, response);
            throw new IllegalStateException("MoMo returned no payment URL for order " + orderReference);
        }
        return response.get("payUrl").asText();
    }

    // ---- inbound: the IPN callback -----------------------------------------

    @Override
    public GatewaySettlement verifyNotification(String rawNotification) {
        JsonNode payload = parse(rawNotification);

        if (!HmacSha256.matches(notificationSignatureText(payload),
                momo.secretKey(),
                text(payload, "signature"))) {
            log.warn("Rejected a MoMo notification for {}: signature did not verify",
                    text(payload, "orderId"));
            throw new InvalidGatewaySignatureException();
        }

        String gatewayOrderId = text(payload, "orderId");
        if (gatewayOrderId == null) {
            throw new MalformedGatewayNotificationException("the notification carries no orderId");
        }

        return new GatewaySettlement(
                gatewayOrderId.replaceFirst("^" + ORDER_ID_PREFIX, ""),
                text(payload, "transId"),
                resultCodeOf(payload) == SUCCESS,
                rawNotification);
    }

    @Override
    public String acknowledgementFor(String rawNotification) {
        JsonNode payload = parse(rawNotification);
        int resultCode = resultCodeOf(payload);
        String respondedAt = LocalDateTime.now(GATEWAY_ZONE).format(RESPONSE_TIME_FORMAT);
        String message = (resultCode == SUCCESS) ? "Thành công" : "Thất bại";

        String rawSignature = String.join("&",
                "accessKey=" + momo.accessKey(),
                "extraData=" + text(payload, "extraData"),
                "message=" + message,
                "orderId=" + text(payload, "orderId"),
                "partnerCode=" + text(payload, "partnerCode"),
                "requestId=" + text(payload, "requestId"),
                "responseTime=" + respondedAt,
                "resultCode=" + resultCode);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", text(payload, "partnerCode"));
        body.put("requestId", text(payload, "requestId"));
        body.put("orderId", text(payload, "orderId"));
        body.put("resultCode", resultCode);
        body.put("message", message);
        body.put("responseTime", respondedAt);
        body.put("extraData", text(payload, "extraData"));
        body.put("signature", HmacSha256.hexDigest(rawSignature, momo.secretKey()));

        try {
            return json.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("could not render the MoMo acknowledgement", e);
        }
    }

    /**
     * The string MoMo signs a notification with: thirteen named fields, in this order,
     * {@code &}-joined. See the class note on why {@code null} renders as it does.
     */
    private String notificationSignatureText(JsonNode p) {
        String payType = text(p, "payType");
        return String.join("&",
                "accessKey=" + momo.accessKey(),
                "amount=" + text(p, "amount"),
                "extraData=" + text(p, "extraData"),
                "message=" + text(p, "message"),
                "orderId=" + text(p, "orderId"),
                "orderInfo=" + text(p, "orderInfo"),
                "orderType=" + text(p, "orderType"),
                "partnerCode=" + text(p, "partnerCode"),
                "payType=" + (payType != null ? payType : ""),
                "requestId=" + text(p, "requestId"),
                "responseTime=" + text(p, "responseTime"),
                "resultCode=" + text(p, "resultCode"),
                "transId=" + text(p, "transId"));
    }

    /**
     * {@code resultCode} as an int.
     *
     * <p>This is the guard the plan calls for. The old code ran {@code Integer.parseInt} on the
     * bare value, so a non-numeric result code was an unhandled {@code NumberFormatException}
     * and a 500 — see {@link MalformedGatewayNotificationException} for why answering 500 to a
     * gateway is worse than it looks.
     */
    private static int resultCodeOf(JsonNode payload) {
        String value = text(payload, "resultCode");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new MalformedGatewayNotificationException(
                    "resultCode is not a number: " + value);
        }
    }

    private JsonNode parse(String rawNotification) {
        try {
            JsonNode parsed = json.readTree(rawNotification);
            if (!parsed.isObject()) {
                throw new MalformedGatewayNotificationException("the notification is not a JSON object");
            }
            return parsed;
        } catch (MalformedGatewayNotificationException rethrow) {
            throw rethrow;
        } catch (Exception e) {
            throw new MalformedGatewayNotificationException("the notification is not valid JSON");
        }
    }

    /**
     * A field as text, or {@code null} when absent — which then concatenates as {@code "null"},
     * reproducing what binding the payload to a record of {@code String}s used to do.
     *
     * <p>Numbers are read through {@code asText()} so that MoMo sending {@code "amount": 50000}
     * unquoted signs identically to {@code "amount": "50000"}, which is what Jackson's
     * {@code StringDeserializer} did for the old record.
     */
    private static String text(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }
}
