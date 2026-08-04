package com.dominator.gearly.ordering.application;

/** A customer's intent to cancel one of their own orders, and why. */
public record CancelOrderCommand(String orderId, String reason) {
}
