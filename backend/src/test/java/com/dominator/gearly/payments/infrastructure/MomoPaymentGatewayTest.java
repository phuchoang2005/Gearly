package com.dominator.gearly.payments.infrastructure;

import com.dominator.gearly.payments.domain.ExchangeRateProvider;
import com.dominator.gearly.payments.domain.GatewaySettlement;
import com.dominator.gearly.payments.domain.InvalidGatewaySignatureException;
import com.dominator.gearly.payments.domain.MalformedGatewayNotificationException;
import com.dominator.gearly.shared.domain.Money;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The MoMo adapter, including the parts that had <b>no coverage at all</b> before S13.
 *
 * <p>{@code MomoService} built its HTTP client in a field initialiser
 * ({@code private final RestTemplate rest = new RestTemplate()}), so there was no way to reach
 * {@code createPaymentUrl} from a test without making a real call to MoMo's sandbox. That is the
 * concrete reason the plan asks for the client to be injected: not tidiness, but that an
 * untestable field made an entire money path untested. It is injected now and
 * {@code MockRestServiceServer} stands in for the gateway.
 *
 * <h2>The signature is recomputed here, not copied</h2>
 * These tests build the expected HMAC with their own {@link #hmac} rather than calling
 * {@link HmacSha256}. A test that signs with the same helper the code signs with asserts only
 * that the helper is deterministic — it would pass just as happily if both were wrong.
 */
class MomoPaymentGatewayTest {

    private static final String SECRET = "test-secret-key";

    private static final MomoProperties MOMO = new MomoProperties(
            "MOMO", "test-access-key", SECRET,
            "http://localhost:5173/momo-return",
            "http://localhost:8080/api/payments/momo/notify",
            "https://momo.test/create",
            Duration.ofSeconds(10));

    private final ObjectMapper json = new ObjectMapper();

    private MomoPaymentGateway gateway;
    private MockRestServiceServer server;
    private BigDecimal rate = new BigDecimal("25000");

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ExchangeRateProvider rates = () -> rate;
        gateway = new MomoPaymentGateway(MOMO, rates, json, builder.build());
    }

    // ---- outbound ----------------------------------------------------------

    @Nested
    @DisplayName("startCheckout")
    class StartCheckout {

        @Test
        @DisplayName("converts the USD total to whole VND at the provider's rate and returns the payUrl")
        void convertsAndReturnsPayUrl() {
            // $36.60 at 25,000 VND/USD = 915,000 VND exactly.
            server.expect(requestTo("https://momo.test/create"))
                    .andExpect(method(org.springframework.http.HttpMethod.POST))
                    .andExpect(jsonPath("$.amount").value("915000"))
                    .andExpect(jsonPath("$.orderId").value("Gearly-order-9"))
                    .andExpect(jsonPath("$.requestId").value("Gearly-order-9"))
                    .andExpect(jsonPath("$.orderInfo").value("Gearly Purchase - Order #order-9"))
                    .andRespond(withSuccess("{\"payUrl\":\"https://momo.test/pay/abc\"}",
                            MediaType.APPLICATION_JSON));

            String url = gateway.startCheckout(Money.of("36.60"), "order-9");

            assertThat(url).isEqualTo("https://momo.test/pay/abc");
            server.verify();
        }

        @Test
        @DisplayName("rounds a fractional VND amount half-up rather than truncating")
        void roundsHalfUp() {
            // $0.10 at 23,455 VND/USD = 2,345.5 VND, which must become 2,346 and not 2,345.
            rate = new BigDecimal("23455");
            server.expect(requestTo("https://momo.test/create"))
                    .andExpect(jsonPath("$.amount").value("2346"))
                    .andRespond(withSuccess("{\"payUrl\":\"u\"}", MediaType.APPLICATION_JSON));

            gateway.startCheckout(Money.of("0.10"), "order-1");

            server.verify();
        }

        @Test
        @DisplayName("signs the create request over the ten fields MoMo expects, in order")
        void signsTheCreateRequest() {
            String extraData = java.util.Base64.getEncoder().encodeToString(
                    "{\"orderId\":\"order-9\"}".getBytes(StandardCharsets.UTF_8));
            String expected = hmac(String.join("&",
                    "accessKey=test-access-key",
                    "amount=915000",
                    "extraData=" + extraData,
                    "ipnUrl=http://localhost:8080/api/payments/momo/notify",
                    "orderId=Gearly-order-9",
                    "orderInfo=Gearly Purchase - Order #order-9",
                    "partnerCode=MOMO",
                    "redirectUrl=http://localhost:5173/momo-return",
                    "requestId=Gearly-order-9",
                    "requestType=payWithMethod"), SECRET);

            server.expect(requestTo("https://momo.test/create"))
                    .andExpect(jsonPath("$.signature").value(expected))
                    .andRespond(withSuccess("{\"payUrl\":\"u\"}", MediaType.APPLICATION_JSON));

            gateway.startCheckout(Money.of("36.60"), "order-9");

            server.verify();
        }

        /**
         * MoMo answers HTTP 200 with a non-zero {@code resultCode} when it refuses a request, so
         * a body with no {@code payUrl} is the real failure signal. {@code MomoService} checked
         * only the status and the body's nullness, then cast {@code body.get("payUrl")} to a
         * {@code String} — which for a refusal is a null cast that succeeds, returning a null
         * checkout URL to the customer.
         */
        @Test
        @DisplayName("a 200 response carrying no payUrl is a failure, not a null URL")
        void missingPayUrlIsAFailure() {
            server.expect(requestTo("https://momo.test/create"))
                    .andRespond(withSuccess(
                            "{\"resultCode\":41,\"message\":\"Duplicated order\"}",
                            MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> gateway.startCheckout(Money.of("10.00"), "order-9"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("order-9");
        }
    }

    // ---- inbound -----------------------------------------------------------

    @Nested
    @DisplayName("verifyNotification")
    class VerifyNotification {

        @Test
        @DisplayName("a correctly signed success becomes a settlement with our own order id")
        void validSuccess() {
            GatewaySettlement settlement = gateway.verifyNotification(signedNotification(
                    notification("Gearly-order-9", "0", "2468013579")));

            assertThat(settlement.orderReference()).isEqualTo("order-9");
            assertThat(settlement.transactionId()).isEqualTo("2468013579");
            assertThat(settlement.successful()).isTrue();
        }

        @Test
        @DisplayName("resultCode 0 is the only success — 1006 is a failed settlement, not a rejected one")
        void nonZeroResultCodeIsAFailedSettlement() {
            GatewaySettlement settlement = gateway.verifyNotification(signedNotification(
                    notification("Gearly-order-9", "1006", "2468013579")));

            assertThat(settlement.successful()).isFalse();
            assertThat(settlement.orderReference()).isEqualTo("order-9");
        }

        /** Carried down from {@code OnlinePaymentServiceTest}, where it lived until S13. */
        @Test
        @DisplayName("only a leading Gearly- prefix is stripped from the gateway's order id")
        void stripsOnlyTheLeadingPrefix() {
            GatewaySettlement settlement = gateway.verifyNotification(signedNotification(
                    notification("Gearly-order-Gearly-1", "0", "tx")));

            assertThat(settlement.orderReference()).isEqualTo("order-Gearly-1");
        }

        @Test
        @DisplayName("the notification is stored exactly as received, not re-serialised")
        void keepsTheRawBodyVerbatim() {
            String raw = signedNotification(notification("Gearly-order-9", "0", "tx"));

            assertThat(gateway.verifyNotification(raw).rawNotification()).isEqualTo(raw);
        }

        @Test
        @DisplayName("a tampered amount invalidates the signature")
        void tamperedAmountIsRejected() {
            Map<String, String> forged = notification("Gearly-order-9", "0", "tx");
            String signed = signedNotification(forged);
            String tampered = signed.replace("\"amount\":\"915000\"", "\"amount\":\"1\"");

            assertThatThrownBy(() -> gateway.verifyNotification(tampered))
                    .isInstanceOf(InvalidGatewaySignatureException.class);
        }

        @Test
        @DisplayName("a notification with no signature at all is rejected, not NPE'd")
        void missingSignatureIsRejected() {
            String unsigned = write(notification("Gearly-order-9", "0", "tx"));

            assertThatThrownBy(() -> gateway.verifyNotification(unsigned))
                    .isInstanceOf(InvalidGatewaySignatureException.class);
        }

        /**
         * The guard the plan asks for. Before S13 this was
         * {@code Integer.parseInt(p.resultCode())} with nothing around it, so the response was a
         * 500 — which MoMo reads as a delivery failure and retries.
         */
        @Test
        @DisplayName("a signed notification whose resultCode is not a number is a rejection, not a 500")
        void nonNumericResultCodeIsRejected() {
            String signed = signedNotification(notification("Gearly-order-9", "OK", "tx"));

            assertThatThrownBy(() -> gateway.verifyNotification(signed))
                    .isInstanceOf(MalformedGatewayNotificationException.class)
                    .hasMessageContaining("resultCode");
        }

        @Test
        @DisplayName("a body that is not JSON is a rejection, not a 500")
        void garbageBodyIsRejected() {
            assertThatThrownBy(() -> gateway.verifyNotification("not json at all"))
                    .isInstanceOf(MalformedGatewayNotificationException.class);
        }

        /**
         * <b>The inherited quirk, pinned deliberately.</b> The old code concatenated record
         * fields into the signature string, so an absent field rendered as the literal
         * {@code "null"} — except {@code payType}, which was explicitly coalesced to {@code ""}.
         *
         * <p>This asymmetry is almost certainly not what MoMo does. It is reproduced exactly
         * because verifying a change to it needs the sandbox and real credentials, which this
         * refactor does not have, and getting a payment provider's canonicalisation wrong stops
         * live payments verifying. Pinning it makes it a described decision rather than an
         * accident nobody can find later.
         */
        @Test
        @DisplayName("an absent field still signs as the literal \"null\", and payType still as \"\"")
        void reproducesTheInheritedNullRendering() {
            Map<String, String> fields = notification("Gearly-order-9", "0", "tx");
            fields.remove("orderType");
            fields.remove("payType");

            // Built the way the pre-S13 code built it: "null" for the missing orderType,
            // "" for the missing payType.
            String canonical = String.join("&",
                    "accessKey=test-access-key",
                    "amount=915000",
                    "extraData=" + fields.get("extraData"),
                    "message=" + fields.get("message"),
                    "orderId=Gearly-order-9",
                    "orderInfo=" + fields.get("orderInfo"),
                    "orderType=null",
                    "partnerCode=MOMO",
                    "payType=",
                    "requestId=" + fields.get("requestId"),
                    "responseTime=" + fields.get("responseTime"),
                    "resultCode=0",
                    "transId=tx");
            fields.put("signature", hmac(canonical, SECRET));

            assertThat(gateway.verifyNotification(write(fields)).successful()).isTrue();
        }

        /**
         * MoMo sends {@code amount} and {@code transId} as JSON numbers. Binding to a record of
         * {@code String}s used to coerce them via Jackson's {@code StringDeserializer}; reading
         * the tree with {@code asText()} has to do the same or every real notification fails to
         * verify.
         */
        @Test
        @DisplayName("unquoted JSON numbers sign identically to their quoted form")
        void numericFieldsSignLikeStrings() {
            String quoted = signedNotification(notification("Gearly-order-9", "0", "2468013579"));
            String unquoted = quoted
                    .replace("\"amount\":\"915000\"", "\"amount\":915000")
                    .replace("\"transId\":\"2468013579\"", "\"transId\":2468013579");

            assertThat(gateway.verifyNotification(unquoted).transactionId()).isEqualTo("2468013579");
        }
    }

    // ---- acknowledgement ---------------------------------------------------

    @Nested
    @DisplayName("acknowledgementFor")
    class Acknowledgement {

        @Test
        @DisplayName("echoes the gateway's identifiers and signs the reply")
        void signsTheReply() throws Exception {
            String raw = signedNotification(notification("Gearly-order-9", "0", "tx"));

            JsonNode ack = json.readTree(gateway.acknowledgementFor(raw));

            assertThat(ack.get("partnerCode").asText()).isEqualTo("MOMO");
            assertThat(ack.get("orderId").asText()).isEqualTo("Gearly-order-9");
            assertThat(ack.get("resultCode").asInt()).isZero();
            assertThat(ack.get("message").asText()).isEqualTo("Thành công");

            String expected = hmac(String.join("&",
                    "accessKey=test-access-key",
                    "extraData=" + ack.get("extraData").asText(),
                    "message=Thành công",
                    "orderId=Gearly-order-9",
                    "partnerCode=MOMO",
                    "requestId=" + ack.get("requestId").asText(),
                    "responseTime=" + ack.get("responseTime").asText(),
                    "resultCode=0"), SECRET);
            assertThat(ack.get("signature").asText()).isEqualTo(expected);
        }

        @Test
        @DisplayName("a failed payment is acknowledged as Thất bại")
        void failureMessage() throws Exception {
            String raw = signedNotification(notification("Gearly-order-9", "1006", "tx"));

            JsonNode ack = json.readTree(gateway.acknowledgementFor(raw));

            assertThat(ack.get("resultCode").asInt()).isEqualTo(1006);
            assertThat(ack.get("message").asText()).isEqualTo("Thất bại");
        }
    }

    // ---- fixtures ----------------------------------------------------------

    /** A realistic MoMo IPN payload, unsigned. */
    private static Map<String, String> notification(String orderId, String resultCode, String transId) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("partnerCode", "MOMO");
        fields.put("orderId", orderId);
        fields.put("requestId", orderId);
        fields.put("amount", "915000");
        fields.put("orderInfo", "Gearly Purchase - Order #order-9");
        fields.put("orderType", "momo_wallet");
        fields.put("transId", transId);
        fields.put("resultCode", resultCode);
        fields.put("message", "Successful.");
        fields.put("payType", "qr");
        fields.put("responseTime", "1700000000000");
        fields.put("extraData", "eyJvcmRlcklkIjoib3JkZXItOSJ9");
        return fields;
    }

    /** The same payload with a signature MoMo would have produced. */
    private String signedNotification(Map<String, String> fields) {
        String canonical = String.join("&",
                "accessKey=test-access-key",
                "amount=" + fields.get("amount"),
                "extraData=" + fields.get("extraData"),
                "message=" + fields.get("message"),
                "orderId=" + fields.get("orderId"),
                "orderInfo=" + fields.get("orderInfo"),
                "orderType=" + fields.get("orderType"),
                "partnerCode=" + fields.get("partnerCode"),
                "payType=" + fields.get("payType"),
                "requestId=" + fields.get("requestId"),
                "responseTime=" + fields.get("responseTime"),
                "resultCode=" + fields.get("resultCode"),
                "transId=" + fields.get("transId"));
        Map<String, String> signed = new LinkedHashMap<>(fields);
        signed.put("signature", hmac(canonical, SECRET));
        return write(signed);
    }

    private String write(Map<String, String> fields) {
        try {
            return json.writeValueAsString(fields);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** An independent HMAC-SHA256, so these assertions do not lean on the class under test. */
    private static String hmac(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
