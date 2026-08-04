/**
 * The context's inbound HTTP edge: controllers, request/response DTOs and the mapping
 * between them and the application layer's commands. Controllers unwrap the
 * authenticated principal into a typed id before calling in — application services never
 * see a Spring Security type.
 *
 * <p><b>Layer contract:</b> must not depend on {@code infrastructure}.
 *
 * @see com.dominator.gearly.catalog
 */
package com.dominator.gearly.catalog.api;
