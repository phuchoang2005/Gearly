/**
 * Adapters that implement this context's domain ports: Spring Data repositories, HTTP
 * clients, converters and anything else that knows about a specific technology.
 *
 * <p><b>Layer contract:</b> depends on {@code domain} (never the reverse) and is
 * referenced by nothing except the Spring container that wires it.
 *
 * @see com.dominator.gearly.content
 */
package com.dominator.gearly.content.infrastructure;
