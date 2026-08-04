/**
 * <b>Identity &amp; Access — supporting.</b> Owns who a person is and what they may do:
 * registration, email verification, password changes, profile and avatar, role, wishlist,
 * and the issuing of authentication tokens.
 *
 * <p><b>Aggregate:</b> {@code User} (root). {@code Role} is an enum, not a string;
 * the email is an {@code EmailAddress} and the display name a {@code PersonName} that
 * <em>derives</em> {@code fullName} rather than trusting a client-supplied copy.
 * The wishlist stays inside {@code User.favorites} as a deliberate aggregate choice —
 * splitting it out is logged as a follow-up, not done here.
 *
 * <p><b>Relationships:</b>
 * <ul>
 *   <li><b>Identity → everything</b> — other contexts hold a {@code UserId} and nothing
 *       else. No context receives a Spring Security {@code UserDetails}; controllers
 *       unwrap the principal at the edge.</li>
 *   <li><b>Identity → Notification</b> — {@code UserRegistered} triggers the verification
 *       mail from an {@code AFTER_COMMIT} listener, so an SMTP call is never inside a
 *       transaction.</li>
 *   <li><b>Identity → Storage</b> — avatars go through the {@code FileStorage} port.</li>
 * </ul>
 *
 * <p><b>Published events:</b> {@code UserRegistered}.
 *
 * <p>Filled in by <b>Sprint 12</b>.
 */
package com.dominator.gearly.identity;
