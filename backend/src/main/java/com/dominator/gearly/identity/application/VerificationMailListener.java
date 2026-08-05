package com.dominator.gearly.identity.application;

import com.dominator.gearly.identity.domain.UserRegistered;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.identity.domain.VerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the verification mail once the account it belongs to actually exists.
 *
 * <h2>Why {@code AFTER_COMMIT}, where the catalog's stock listener is {@code BEFORE_COMMIT}</h2>
 * The two phases answer opposite questions, and the difference is the whole reason the choice
 * is explicit in both places.
 *
 * <p>{@code CatalogStockListener} runs {@code BEFORE_COMMIT} because the stock decrement
 * <em>must</em> be atomic with the order: an order that commits without its stock coming down
 * oversells the next customer, so a failure there has to take the order with it.
 *
 * <p>Here the opposite is true. Sending mail is not part of the registration and cannot be
 * rolled back if it half-succeeds — an SMTP server that accepts a message and then fails has
 * already delivered it. Running before the commit is what the old code effectively did, by
 * calling {@code createAndSend} as the last statement of a {@code @Transactional} method, and it
 * had both failure modes at once: the transaction stayed open for the length of an external
 * network round trip, and a mail server that was slow or down failed the registration outright.
 * A customer lost their new account because a notification could not be delivered.
 *
 * <p>{@code AFTER_COMMIT} inverts that trade deliberately. The account is durable before
 * anything tries to announce it, and a mail failure leaves a real account with no verification
 * mail — which {@code POST /api/users/resend-verification} already exists to fix, and which the
 * customer can trigger themselves. It is logged rather than rethrown for the same reason:
 * there is no transaction left to fail and nobody left to tell, so the only useful thing an
 * exception could do here is escape into the servlet container after the response has been
 * decided.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailListener {

    private final UserRepository users;
    private final VerificationTokenService verificationTokenService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegistered event) {
        try {
            users.findByEmail(event.email()).ifPresentOrElse(
                    user -> verificationTokenService.createAndSend(
                            user, VerificationToken.TokenType.EMAIL_VERIFICATION),
                    () -> log.warn("Registered user {} vanished before the verification mail "
                            + "could be sent", event.email()));
        } catch (RuntimeException failed) {
            log.error("Could not send the verification mail for {}; the account exists and the "
                    + "customer can request a new link", event.email(), failed);
        }
    }
}
