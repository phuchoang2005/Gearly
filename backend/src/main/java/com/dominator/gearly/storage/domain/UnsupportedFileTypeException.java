package com.dominator.gearly.storage.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

import java.util.Collection;

/**
 * An upload whose content type is not one this application will store.
 *
 * <p>The check matters more than it looks. Everything written by either uploader lands under
 * {@code uploads/}, which is served statically from the application's own origin — so before
 * S13, an authenticated customer could upload an {@code .html} or an {@code .svg} as their
 * avatar and get back a same-origin URL serving markup they wrote. That is stored XSS with a
 * convenient hosting service attached. An allow-list of image types removes it.
 */
public class UnsupportedFileTypeException extends DomainRuleViolationException {

    public UnsupportedFileTypeException(String contentType, Collection<String> allowed) {
        super("Files of type '" + (contentType == null || contentType.isBlank() ? "unknown" : contentType)
                + "' cannot be uploaded. Allowed types: " + String.join(", ", allowed));
    }
}
