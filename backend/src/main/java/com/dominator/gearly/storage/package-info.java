/**
 * <b>Storage — generic subdomain.</b> One way to put a file somewhere and get a URL back,
 * replacing the avatar service and the raw {@code Files.copy} calls currently sitting
 * inline in a controller.
 *
 * <p><b>Port:</b> {@code FileStorage} in {@code storage.domain}. <b>Adapter:</b>
 * {@code LocalFileStorage} in {@code storage.infrastructure}, writing under the
 * {@code /uploads} mount. Content-type and size validation are the port's contract, not
 * each caller's responsibility.
 *
 * <p><b>Relationships:</b> used by Identity (avatars) and Catalog (product imagery)
 * through the port. Storage depends on no context.
 *
 * <p>Filled in by <b>Sprint 13</b>.
 */
package com.dominator.gearly.storage;
