package com.dominator.bookify.controller.user;

import com.dominator.bookify.config.MomoConfig;
import com.dominator.bookify.service.user.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final OrderService orderService;
    private final MomoConfig momoConfig;
    private final DateTimeFormatter RESPONSE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PaymentController(OrderService orderService, MomoConfig momoConfig) {
        this.orderService = orderService;
        this.momoConfig = momoConfig;
    }

    public record MomoNotifyRequest(
            String partnerCode,
            String requestId,
            String orderId,
            String amount,
            String orderInfo,
            String orderType,
            String transId,
            String resultCode,
            String payType,
            String message,
            String responseTime,
            String extraData,
            String signature
    ) { }

    @PostMapping("/momo/notify")
    public ResponseEntity<Map<String, Object>> momoNotify(@RequestBody MomoNotifyRequest p) {
        String rawSignature = String.join("&",
                "accessKey="   + momoConfig.getAccessKey(),
                "amount="      + p.amount(),
                "extraData="   + p.extraData(),
                "message="     + p.message(),
                "orderId="     + p.orderId(),
                "orderInfo="   + p.orderInfo(),
                "orderType="   + p.orderType(),
                "partnerCode=" + p.partnerCode(),
                "payType="     + (p.payType() != null ? p.payType() : ""),
                "requestId="   + p.requestId(),
                "responseTime="+ p.responseTime(),
                "resultCode="  + p.resultCode(),
                "transId="     + p.transId()
        );

        if (!verifySignature(rawSignature, p.signature(), momoConfig.getSecretKey())) {
            return ResponseEntity.badRequest().build();
        }

        int resultCode = Integer.parseInt(p.resultCode());
        orderService.updateOrderStatusFromMomo(
                p.orderId(), p.transId(), resultCode, p.toString()
        );

        String now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(RESPONSE_TIME_FORMAT);
        String message = (resultCode == 0) ? "Thành công" : "Thất bại";

        String rawRespSig = String.join("&",
                "accessKey="   + momoConfig.getAccessKey(),
                "extraData="   + p.extraData(),
                "message="     + message,
                "orderId="     + p.orderId(),
                "partnerCode=" + p.partnerCode(),
                "requestId="   + p.requestId(),
                "responseTime="+ now,
                "resultCode="  + resultCode
        );

        String respSignature = hmacSha256(rawRespSig, momoConfig.getSecretKey());

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", p.partnerCode());
        body.put("requestId",   p.requestId());
        body.put("orderId",     p.orderId());
        body.put("resultCode",  resultCode);
        body.put("message",     message);
        body.put("responseTime",now);
        body.put("extraData",   p.extraData());
        body.put("signature",   respSignature);

        return ResponseEntity.ok(body);
    }

    private boolean verifySignature(String data, String received, String key) {
        return hmacSha256(data, key).equals(received);
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * raw.length);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA256 failed", e);
        }
    }
}
