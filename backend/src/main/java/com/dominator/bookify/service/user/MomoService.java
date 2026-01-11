package com.dominator.bookify.service.user;

import com.dominator.bookify.config.MomoConfig;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class MomoService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final MomoConfig config;
    private final FxService fxService;
    private final RestTemplate rest = new RestTemplate();
    private final DateTimeFormatter RESPONSE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MomoService(MomoConfig config, FxService fxService) {
        this.config = config;
        this.fxService = fxService;
    }

    public String createPaymentUrl(BigDecimal amountUsd, String orderId) {
        long amountVnd = fxService.getUsdToVndRate()
                .multiply(amountUsd)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        String momoOrderId = "Bookify-" + orderId;
        String amount = String.valueOf(amountVnd);
        String orderInfo = "Bookify Purchase - Order #" + orderId;

        String extraData = Base64.getEncoder()
                .encodeToString(
                        String.format("{\"orderId\":\"%s\"}", orderId)
                                .getBytes(StandardCharsets.UTF_8)
                );

        String rawSignature = String.join("&",
                "accessKey=" + config.getAccessKey(),
                "amount=" + amount,
                "extraData=" + extraData,
                "ipnUrl=" + config.getNotifyUrl(),
                "orderId=" + momoOrderId,
                "orderInfo=" + orderInfo,
                "partnerCode=" + config.getPartnerCode(),
                "redirectUrl=" + config.getReturnUrl(),
                "requestId=" + momoOrderId,
                "requestType=" + "payWithMethod"
        );

        String signature = hmacSha256(rawSignature, config.getSecretKey());

        Map<String, Object> payload = new HashMap<>();
        payload.put("partnerCode", config.getPartnerCode());
        payload.put("accessKey", config.getAccessKey());
        payload.put("requestId", momoOrderId);
        payload.put("amount", amount);
        payload.put("orderId", momoOrderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", config.getReturnUrl());
        payload.put("ipnUrl", config.getNotifyUrl());
        payload.put("extraData", extraData);
        payload.put("requestType", "payWithMethod");
        payload.put("signature", signature);
        payload.put("lang", "en");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = rest.postForEntity(
                "https://test-payment.momo.vn/v2/gateway/api/create",
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to create MoMo payment URL");
        }

        return (String) response.getBody().get("payUrl");
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * rawHmac.length);
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA256 failed", e);
        }
    }
}
