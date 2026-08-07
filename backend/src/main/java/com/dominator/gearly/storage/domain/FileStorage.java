package com.dominator.gearly.storage.domain;

/**
 * One way to put a file somewhere and get back the URL it is served at.
 *
 * <p>Before S13 there were two, and neither validated anything. {@code AvatarStorageService}
 * wrote {@code {userId}.jpg} whatever the file actually was, and {@code MediaController} did its
 * own {@code Files.createDirectories} and {@code Files.copy} inline in a request handler with no
 * service layer at all. Both wrote under {@code uploads/}, which is served statically at
 * {@code /uploads/**} — so between them, any authenticated customer could put a file of any type
 * on the application's own origin.
 *
 * <h2>Validation is the port's contract, not the caller's</h2>
 * Size and content type are checked here, once, for every caller. That is the whole reason this
 * is a port and not a utility class: a rule that each call site is asked to remember is a rule
 * that the next call site will not have.
 */
public interface FileStorage {

    /**
     * Stores {@code file} under a freshly generated name and returns its public URL path.
     *
     * <p>Use for content with no natural identity — product imagery, where each upload is a new
     * file and nothing replaces anything.
     *
     * @throws UnsupportedFileTypeException if the content type is not in the allow-list
     * @throws FileTooLargeException        if it exceeds the configured maximum
     * @throws FileStorageException         if it could not be written
     */
    String store(StorageArea area, UploadedFile file);

    /**
     * Stores {@code file} as the current content for {@code name}, replacing whatever was there.
     *
     * <p>Use for content with exactly one current version per owner — an avatar. The extension
     * still comes from the validated content type, so the stored name is
     * {@code name.<ext>}; any previously stored file for the same {@code name} under a different
     * extension is removed, which is what stops a customer who uploads a PNG and then a JPEG
     * from leaving a file behind that nothing references.
     */
    String storeAs(StorageArea area, String name, UploadedFile file);
}
