package com.dominator.gearly.storage.infrastructure;

import com.dominator.gearly.storage.domain.FileStorage;
import com.dominator.gearly.storage.domain.FileStorageException;
import com.dominator.gearly.storage.domain.FileTooLargeException;
import com.dominator.gearly.storage.domain.StorageArea;
import com.dominator.gearly.storage.domain.UnsupportedFileTypeException;
import com.dominator.gearly.storage.domain.UploadedFile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Writes uploads to the local filesystem under the configured area directories.
 *
 * <p>The one implementation of {@link FileStorage}, replacing {@code AvatarStorageService} and
 * the {@code Files.createDirectories} / {@code Files.copy} pair that lived inline in
 * {@code MediaController}'s request handler.
 *
 * <h2>The stored name never contains anything the client sent</h2>
 * It is a generated UUID (or a caller-chosen name that is itself derived from a server-side id),
 * plus an extension looked up from the <em>validated</em> content type. The old code took the
 * extension from {@code MultipartFile.getOriginalFilename()} by cutting at the last dot, which
 * is attacker-controlled text appended to a path — and it need not look like an extension: an
 * original filename of {@code "a./../../evil"} produces {@code "./../../evil"}, and
 * {@code Path.resolve} interprets it as a path, not as a suffix. The write is also confined to
 * the area directory afterwards, so the guarantee does not rest on the naming alone.
 */
@Component
public class LocalFileStorage implements FileStorage {

    /** The extension each accepted type is stored with. Keyed by the allow-listed MIME types. */
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "image/avif", ".avif");

    private final StorageProperties properties;

    public LocalFileStorage(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String store(StorageArea area, UploadedFile file) {
        return write(area, UUID.randomUUID().toString(), file, false);
    }

    @Override
    public String storeAs(StorageArea area, String name, UploadedFile file) {
        return write(area, name, file, true);
    }

    private String write(StorageArea area, String baseName, UploadedFile file, boolean replacing) {
        String extension = validate(file);

        StorageProperties.Area configured = properties.areaFor(area);
        Path directory = Path.of(configured.directory()).toAbsolutePath().normalize();
        String filename = baseName + extension;
        Path target = directory.resolve(filename).normalize();

        // Belt and braces: baseName is server-generated and the extension comes from a fixed
        // table, so this cannot currently fail. It is here so that a future caller passing
        // something less trustworthy to storeAs finds a wall rather than a filesystem.
        if (!target.startsWith(directory)) {
            throw new FileStorageException("Refusing to write outside " + directory, null);
        }

        try {
            Files.createDirectories(directory);
            if (replacing) {
                removeOtherExtensions(directory, baseName, filename);
            }
            try (InputStream content = file.content().open()) {
                Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new FileStorageException("Could not store " + filename + " in " + directory, e);
        }

        return configured.publicBase() + "/" + filename;
    }

    /**
     * The extension for this file's type, having checked it is one we accept.
     *
     * <p>Order matters: type before size. Both are refusals, but the type check needs no
     * knowledge of the content and gives the more useful message.
     */
    private String validate(UploadedFile file) {
        if (!properties.allowedContentTypes().contains(file.contentType())) {
            throw new UnsupportedFileTypeException(file.contentType(), properties.allowedContentTypes());
        }
        long max = properties.maxFileSize().toBytes();
        if (file.sizeInBytes() > max) {
            throw new FileTooLargeException(file.sizeInBytes(), max);
        }
        String extension = EXTENSIONS.get(file.contentType());
        if (extension == null) {
            // Configured as allowed but with no known extension: a configuration mistake, not a
            // bad request, and storing it extensionless would break the static mapping.
            throw new IllegalStateException(
                    "gearly.storage.allowed-content-types permits '" + file.contentType()
                            + "' but no file extension is known for it");
        }
        return extension;
    }

    /**
     * Deletes previous versions of {@code baseName} stored under a different extension.
     *
     * <p>Avatars are one-per-account, and the extension now follows the uploaded type rather than
     * always being {@code .jpg}. Without this, a customer who uploads a PNG and then a JPEG
     * leaves {@code {userId}.png} on disk with nothing referencing it — the account's
     * {@code avatarUrl} points at the JPEG.
     */
    private static void removeOtherExtensions(Path directory, String baseName, String keep)
            throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> siblings = Files.list(directory)) {
            for (Path sibling : siblings.toList()) {
                String name = sibling.getFileName().toString();
                if (!name.equals(keep) && EXTENSIONS.containsValue(extensionOf(name))
                        && name.substring(0, name.length() - extensionOf(name).length()).equals(baseName)) {
                    Files.deleteIfExists(sibling);
                }
            }
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot);
    }
}
