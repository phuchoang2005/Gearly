package com.dominator.gearly.storage.infrastructure;

import com.dominator.gearly.storage.domain.FileTooLargeException;
import com.dominator.gearly.storage.domain.StorageArea;
import com.dominator.gearly.storage.domain.UnsupportedFileTypeException;
import com.dominator.gearly.storage.domain.UploadedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The local-disk adapter — mostly a test of the validation that did not exist before S13.
 *
 * <p>Runs against a real temporary directory rather than a mocked filesystem, because the
 * claims worth making here are about what ends up on disk: the name, the extension, and that
 * nothing lands outside the area.
 */
class LocalFileStorageTest {

    @TempDir
    Path root;

    private LocalFileStorage storage;
    private Path products;
    private Path avatars;

    @BeforeEach
    void setUp() {
        products = root.resolve("uploads");
        avatars = root.resolve("uploads/avatars");
        storage = new LocalFileStorage(new StorageProperties(
                DataSize.ofKilobytes(10),
                Set.of("image/jpeg", "image/png", "image/webp"),
                Map.of(
                        StorageArea.PRODUCT_IMAGES,
                        new StorageProperties.Area(products.toString(), "/uploads"),
                        StorageArea.AVATARS,
                        new StorageProperties.Area(avatars.toString(), "/uploads/avatars"))));
    }

    private static UploadedFile image(String contentType, int bytes) {
        byte[] content = new byte[bytes];
        return new UploadedFile(contentType, bytes, () -> new ByteArrayInputStream(content));
    }

    private static UploadedFile png() {
        return image("image/png", 64);
    }

    // ---- storing ------------------------------------------------------------

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("writes the bytes and returns a URL under the area's public base")
        void writesAndReturnsUrl() throws Exception {
            byte[] content = "the image".getBytes(StandardCharsets.UTF_8);
            String url = storage.store(StorageArea.PRODUCT_IMAGES, new UploadedFile(
                    "image/png", content.length, () -> new ByteArrayInputStream(content)));

            assertThat(url).startsWith("/uploads/").endsWith(".png");
            Path written = products.resolve(url.substring("/uploads/".length()));
            assertThat(Files.readAllBytes(written)).isEqualTo(content);
        }

        @Test
        @DisplayName("creates the area directory if it does not exist")
        void createsTheDirectory() {
            assertThat(Files.exists(products)).isFalse();

            storage.store(StorageArea.PRODUCT_IMAGES, png());

            assertThat(Files.isDirectory(products)).isTrue();
        }

        @Test
        @DisplayName("each upload gets its own name, so one never replaces another")
        void namesAreUnique() {
            String first = storage.store(StorageArea.PRODUCT_IMAGES, png());
            String second = storage.store(StorageArea.PRODUCT_IMAGES, png());

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("the extension follows the content type, not anything the client sent")
        void extensionFollowsContentType() {
            assertThat(storage.store(StorageArea.PRODUCT_IMAGES, image("image/jpeg", 10)))
                    .endsWith(".jpg");
            assertThat(storage.store(StorageArea.PRODUCT_IMAGES, image("image/webp", 10)))
                    .endsWith(".webp");
        }
    }

    // ---- validation ---------------------------------------------------------

    @Nested
    @DisplayName("validation")
    class Validation {

        /**
         * The reason this check matters: {@code uploads/} is served statically at
         * {@code /uploads/**}, so before S13 an authenticated customer could upload markup as
         * their avatar and get a same-origin URL that serves it.
         */
        @Test
        @DisplayName("a non-image is refused and nothing is written")
        void refusesNonImages() {
            assertThatThrownBy(() -> storage.store(StorageArea.PRODUCT_IMAGES,
                    image("text/html", 10)))
                    .isInstanceOf(UnsupportedFileTypeException.class)
                    .hasMessageContaining("text/html");

            assertThat(Files.exists(products)).isFalse();
        }

        @Test
        @DisplayName("an SVG is refused — it is an image that can carry script")
        void refusesSvg() {
            assertThatThrownBy(() -> storage.store(StorageArea.PRODUCT_IMAGES,
                    image("image/svg+xml", 10)))
                    .isInstanceOf(UnsupportedFileTypeException.class);
        }

        @Test
        @DisplayName("a file with no declared content type is refused rather than guessed at")
        void refusesUnknownType() {
            assertThatThrownBy(() -> storage.store(StorageArea.PRODUCT_IMAGES,
                    new UploadedFile(null, 10, () -> new ByteArrayInputStream(new byte[10]))))
                    .isInstanceOf(UnsupportedFileTypeException.class)
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("content type is matched case-insensitively, as clients send it")
        void contentTypeIsCaseInsensitive() {
            assertThat(storage.store(StorageArea.PRODUCT_IMAGES, image("IMAGE/PNG", 10)))
                    .endsWith(".png");
        }

        @Test
        @DisplayName("an oversized file is refused and nothing is written")
        void refusesOversized() {
            assertThatThrownBy(() -> storage.store(StorageArea.PRODUCT_IMAGES,
                    image("image/png", (int) DataSize.ofKilobytes(11).toBytes())))
                    .isInstanceOf(FileTooLargeException.class);

            assertThat(Files.exists(products)).isFalse();
        }

        @Test
        @DisplayName("a file exactly at the limit is accepted")
        void acceptsTheLimitExactly() {
            assertThat(storage.store(StorageArea.PRODUCT_IMAGES,
                    image("image/png", (int) DataSize.ofKilobytes(10).toBytes()))).isNotNull();
        }
    }

    // ---- replacing ----------------------------------------------------------

    @Nested
    @DisplayName("storeAs")
    class StoreAs {

        @Test
        @DisplayName("stores under the caller's name, so an account has one avatar")
        void storesUnderTheGivenName() {
            String url = storage.storeAs(StorageArea.AVATARS, "u1", png());

            assertThat(url).isEqualTo("/uploads/avatars/u1.png");
            assertThat(Files.exists(avatars.resolve("u1.png"))).isTrue();
        }

        @Test
        @DisplayName("re-uploading the same type overwrites in place")
        void overwritesSameType() throws Exception {
            storage.storeAs(StorageArea.AVATARS, "u1", png());
            byte[] replacement = "newer".getBytes(StandardCharsets.UTF_8);
            storage.storeAs(StorageArea.AVATARS, "u1", new UploadedFile(
                    "image/png", replacement.length, () -> new ByteArrayInputStream(replacement)));

            assertThat(Files.readAllBytes(avatars.resolve("u1.png"))).isEqualTo(replacement);
            assertThat(filesIn(avatars)).containsExactly("u1.png");
        }

        /**
         * {@code AvatarStorageService} wrote {@code {userId}.jpg} whatever the upload was, so this
         * situation could not arise — and the stored file lied about its type. Now the extension
         * follows the content, which means the previous one has to go or it is an orphan nothing
         * references.
         */
        @Test
        @DisplayName("uploading a different type removes the previous avatar rather than orphaning it")
        void replacesAcrossExtensions() {
            storage.storeAs(StorageArea.AVATARS, "u1", image("image/png", 10));
            String second = storage.storeAs(StorageArea.AVATARS, "u1", image("image/jpeg", 10));

            assertThat(second).isEqualTo("/uploads/avatars/u1.jpg");
            assertThat(filesIn(avatars)).containsExactly("u1.jpg");
        }

        @Test
        @DisplayName("another account's avatar is left alone")
        void doesNotTouchOtherNames() {
            storage.storeAs(StorageArea.AVATARS, "u1", image("image/png", 10));
            storage.storeAs(StorageArea.AVATARS, "u2", image("image/jpeg", 10));

            assertThat(filesIn(avatars)).containsExactlyInAnyOrder("u1.png", "u2.jpg");
        }

        /**
         * A prefix match would have deleted {@code u1.png} when {@code u10} was stored, since
         * {@code "u10.jpg"} starts with {@code "u1"}. The comparison is on the whole base name.
         */
        @Test
        @DisplayName("a name that is a prefix of another is not collateral damage")
        void prefixNamesAreDistinct() {
            storage.storeAs(StorageArea.AVATARS, "u1", image("image/png", 10));
            storage.storeAs(StorageArea.AVATARS, "u10", image("image/jpeg", 10));

            assertThat(filesIn(avatars)).containsExactlyInAnyOrder("u1.png", "u10.jpg");
        }

        @Test
        @DisplayName("the two areas are separate directories")
        void areasAreSeparate() {
            storage.storeAs(StorageArea.AVATARS, "u1", png());
            storage.store(StorageArea.PRODUCT_IMAGES, png());

            assertThat(filesIn(avatars)).containsExactly("u1.png");
            assertThat(filesIn(products)).hasSize(1).doesNotContain("u1.png");
        }
    }

    private static List<String> filesIn(Path directory) {
        try (var entries = Files.list(directory)) {
            return entries.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
