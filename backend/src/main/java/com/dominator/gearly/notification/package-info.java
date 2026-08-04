/**
 * <b>Notification — generic subdomain.</b> The one way the system talks to a customer out
 * of band. Owns message templates and delivery; owns no business rule about <em>when</em>
 * a message is warranted — that decision belongs to the context that publishes the event.
 *
 * <p><b>Port:</b> {@code NotificationSender} in {@code notification.domain}.
 * <b>Adapter:</b> {@code SmtpNotificationSender} in {@code notification.infrastructure},
 * with the message bodies as templates rather than inline HTML, and every base URL taken
 * from configuration.
 *
 * <p><b>Relationships:</b> a downstream event consumer. Ordering's
 * {@code OrderPlaced}/{@code OrderStatusChanged} and Identity's {@code UserRegistered}
 * are handled {@code AFTER_COMMIT}, so a failed send can never roll back a committed
 * order or registration.
 *
 * <p>Filled in by <b>Sprint 13</b>.
 */
package com.dominator.gearly.notification;
