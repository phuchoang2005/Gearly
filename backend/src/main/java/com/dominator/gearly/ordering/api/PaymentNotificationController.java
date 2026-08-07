package com.dominator.gearly.ordering.api;

import com.dominator.gearly.ordering.application.OnlinePaymentService;
import com.dominator.gearly.payments.domain.GatewayNotificationRejectedException;
import com.dominator.gearly.payments.domain.GatewaySettlement;
import com.dominator.gearly.payments.domain.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The payment gateway's server-to-server callback.
 *
 * <h2>Why this lives in {@code ordering.api} and not in {@code payments}</h2>
 * Because of what the endpoint is <em>for</em>. Its effect is to move an order to
 * {@code PROCESSING} and append a row to that order's payment ledger — an ordering use case,
 * reached over HTTP. Payments contributes the part that is genuinely about the provider:
 * authenticating the callback and translating it. The target architecture in the plan gives
 * {@code payments/} a {@code domain} and an {@code infrastructure} package and no {@code api}
 * for exactly this reason.
 *
 * <p>It also keeps the context rule satisfiable. A controller in {@code payments.api} would
 * have to call {@code ordering.application.OnlinePaymentService}, and an application service is
 * not another context's published language — {@code contexts_touch_each_other_only_through_
 * published_types} would fail it. This direction works because {@link PaymentGateway} is an
 * interface in a {@code domain} package: a port, which is exactly what a context is allowed to
 * reach across for.
 *
 * <p>The handler is deliberately three lines of protocol and no business logic. It does not
 * know a single MoMo field name; the body goes to the adapter as received.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentNotificationController {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationController.class);

    private final PaymentGateway paymentGateway;
    private final OnlinePaymentService onlinePaymentService;

    public PaymentNotificationController(PaymentGateway paymentGateway,
                                         OnlinePaymentService onlinePaymentService) {
        this.paymentGateway = paymentGateway;
        this.onlinePaymentService = onlinePaymentService;
    }

    /**
     * Verify, record, acknowledge — in that order, because recording an unverified notification
     * is how an attacker marks their own order paid.
     *
     * <p>The body is bound as text rather than to a DTO so that the exact bytes the gateway
     * signed are what gets verified and what gets stored on the payment ledger. Re-serialising
     * a parsed object would be a different document.
     *
     * <p><b>The URL, the 400 and the acknowledgement body are unchanged</b> from the endpoint
     * this replaces. A rejected notification still answers {@code 400} with no body — the
     * gateway keys on the status, and an error document would tell an unauthenticated caller
     * more than it should. What is new is that the rejection is logged.
     */
    @PostMapping(value = "/momo/notify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> momoNotify(@RequestBody String rawNotification) {
        GatewaySettlement settlement;
        try {
            settlement = paymentGateway.verifyNotification(rawNotification);
        } catch (GatewayNotificationRejectedException rejected) {
            log.warn("Refused a payment notification: {}", rejected.getMessage());
            return ResponseEntity.badRequest().build();
        }

        onlinePaymentService.recordSettlement(settlement);

        return ResponseEntity.ok(paymentGateway.acknowledgementFor(rawNotification));
    }
}
