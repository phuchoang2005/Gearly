package com.dominator.gearly.storage.domain;

import java.util.Objects;

/**
 * A file offered for storage, described in terms the domain can check.
 *
 * <p>Note what is <em>not</em> here: the client's filename. It used to decide the stored
 * extension — {@code MediaController} took everything after the last dot of
 * {@code getOriginalFilename()} and appended it to a generated name. That is attacker-controlled
 * text going into a path, and it does not have to look like an extension: a filename of
 * {@code "a./../../evil"} yields {@code "./../../evil"}, which {@code Path.resolve} is happy to
 * interpret. The stored extension is derived from the declared content type instead, which is
 * validated against a fixed list, so the vector does not exist rather than being filtered.
 *
 * @param contentType the declared MIME type; checked against the configured allow-list
 * @param sizeInBytes how big the caller says it is; checked before anything is read
 * @param content     opens the bytes
 */
public record UploadedFile(String contentType, long sizeInBytes, ContentSource content) {

    public UploadedFile {
        Objects.requireNonNull(content, "content must not be null");
        if (sizeInBytes < 0) {
            throw new IllegalArgumentException("sizeInBytes must not be negative");
        }
        contentType = contentType == null ? "" : contentType.trim().toLowerCase();
    }

    public boolean isEmpty() {
        return sizeInBytes == 0;
    }
}
