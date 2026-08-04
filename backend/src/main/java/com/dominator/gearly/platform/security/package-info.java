/**
 * The authentication and authorization edge: the filter chain, JWT issuing and
 * validation, and the {@code UserDetails} adapter.
 *
 * <p><b>Layer contract:</b> Spring Security types stop here. A controller unwraps the
 * principal into a {@code UserId} before calling an application service; no service
 * signature mentions a security type, and ownership checks are asked of the aggregate
 * ({@code order.isOwnedBy(userId)}), not performed in a filter.
 *
 * @see com.dominator.gearly.platform
 */
package com.dominator.gearly.platform.security;
