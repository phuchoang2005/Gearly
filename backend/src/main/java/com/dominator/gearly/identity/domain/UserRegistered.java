package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.PersonName;

import java.time.Instant;

/**
 * Somebody created an account.
 *
 * <p>Raised so that the verification mail is no longer sent by the registration itself.
 * {@code AuthService.register} was {@code @Transactional} and called
 * {@code verificationTokenService.createAndSend(...)} as its last statement, which put an SMTP
 * conversation with an external server inside a database transaction: the transaction stayed
 * open for as long as the mail server took to answer, and a mail server that was slow or down
 * failed the registration outright — the account the customer had just created was rolled back
 * because a notification could not be delivered.
 *
 * <p>Consumed {@code AFTER_COMMIT} by {@code VerificationMailListener}, so the account exists
 * before anything tries to tell anyone about it. The trade this makes is deliberate and is the
 * right way round: a mail failure now leaves a real account with no verification mail, which
 * {@code POST /api/users/resend-verification} already exists to fix, instead of leaving the
 * customer with no account and a 500.
 *
 * <p>Carries only shared-kernel types, as every published event must — the name and the address
 * are what a notification needs, and neither is identity's private vocabulary.
 */
public record UserRegistered(EmailAddress email,
                             PersonName name,
                             Instant occurredOn) implements DomainEvent {
}
