package com.dominator.gearly.storage.domain;

import java.io.IOException;
import java.io.InputStream;

/**
 * Opens the bytes of an upload.
 *
 * <p>A {@code Supplier<InputStream>} in all but name, declared here so that it may throw
 * {@link IOException} and — more importantly — so the port does not have to name Spring's
 * {@code MultipartFile}. That type is in {@code org.springframework.web}, which
 * {@code domain_is_free_of_framework_types} bans outright, and rightly: a storage port that can
 * only be called with a live HTTP multipart request is a port that cannot be used from a batch
 * import, a test, or anything else.
 *
 * <p>Deliberately a factory rather than a single stream. Nothing here needs to read the content
 * twice today, but handing out a one-shot stream makes that impossible to add later without
 * changing the interface.
 */
@FunctionalInterface
public interface ContentSource {

    InputStream open() throws IOException;
}
