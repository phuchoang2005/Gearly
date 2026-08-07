package com.dominator.gearly.storage.domain;

/**
 * A file that passed validation but could not be written — a full disk, a permission problem, a
 * stream that failed mid-copy.
 *
 * <p>Not a {@code DomainRuleViolationException}: the caller did nothing wrong and has nothing to
 * correct, so this must not become a 400.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
