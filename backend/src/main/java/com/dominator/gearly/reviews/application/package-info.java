/**
 * Use-case orchestration for this context: application services that take command
 * records plus a typed id, load an aggregate through a port, invoke behavior on it, and
 * persist the result. One aggregate per transaction; anything crossing an aggregate
 * boundary goes out as a domain event.
 *
 * <p><b>Layer contract:</b> no HTTP or security types, and no direct dependency on
 * another context's domain — cross-context work goes through {@code shared}, a published
 * event, or a named port.
 *
 * @see com.dominator.gearly.reviews
 */
package com.dominator.gearly.reviews.application;
