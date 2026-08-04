/**
 * The exception→HTTP-status translation table and the single {@code @RestControllerAdvice}
 * that applies it.
 *
 * <p><b>Layer contract:</b> domain code throws domain exceptions with no HTTP status
 * attached ({@code InsufficientStockException}, {@code IllegalOrderTransitionException},
 * {@code AccessDeniedDomainException}); mapping them to a status code is this package's
 * job alone.
 *
 * @see com.dominator.gearly.platform
 */
package com.dominator.gearly.platform.exception;
