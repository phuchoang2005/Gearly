package com.dominator.gearly.storage.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * An upload larger than the configured maximum.
 *
 * <p>Spring's {@code spring.servlet.multipart.max-file-size} already refuses oversized requests,
 * and this does not replace it — that limit protects the container, this one is the application
 * saying what it is willing to keep. They are set independently on purpose: a deployment can
 * accept a 10MB request while storing at most 2MB of avatar.
 */
public class FileTooLargeException extends DomainRuleViolationException {

    public FileTooLargeException(long sizeInBytes, long maxInBytes) {
        super("The file is " + sizeInBytes + " bytes; the maximum is " + maxInBytes + " bytes");
    }
}
