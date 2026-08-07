package com.dominator.gearly.catalog.api;

import com.dominator.gearly.storage.domain.FileStorage;
import com.dominator.gearly.storage.domain.StorageArea;
import com.dominator.gearly.storage.domain.UploadedFile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Image upload for the admin console's product forms. Writes into {@code uploads/}, which
 * {@code UploadsSecurityTest} pins as publicly readable and admin-only writable.
 *
 * <p><b>Defence in depth.</b> {@code @PreAuthorize("hasRole('ADMIN')")} repeats the
 * {@code /api/admin/**} URL rule from {@code SecurityConfig} at the class level rather than
 * replacing it, for the reason {@code AdminUserController} spells out: the URL rule is a prefix
 * match on a string, and every endpoint that has ever escaped one did so by being mounted
 * somewhere the pattern did not reach. The annotation travels with the code.
 *
 * <h2>S13: why it moved here, and what is left of it</h2>
 * It was {@code controller.admin.MediaController} and did its own {@code Files.createDirectories}
 * and {@code Files.copy} in the handler — the plan's "no service layer at all". Writing files is
 * {@link FileStorage}'s now, including the validation that never existed.
 *
 * <p>It lives in {@code catalog.api} because product imagery is what it uploads;
 * {@code storage/} is a generic subdomain with a domain and an infrastructure package and no
 * inbound HTTP edge of its own, exactly as the plan's target architecture lays it out. The URL
 * is unchanged.
 */
@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasRole('ADMIN')")
public class MediaController {

    private final FileStorage fileStorage;

    public MediaController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    /**
     * Stores the upload and returns the URL the admin console puts on a product.
     *
     * <p>The response shape — {@code {"url": "/uploads/…"}} — is unchanged. What changed is that
     * a non-image, or anything over the configured maximum, is now a 400 rather than a file on
     * the application's own origin.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = fileStorage.store(StorageArea.PRODUCT_IMAGES, uploadOf(file));
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Adapts Spring's multipart type to the port's.
     *
     * <p>This translation is the api layer's job precisely because {@code MultipartFile} is a
     * web type: the port would be unusable from anywhere but a controller if it named one.
     */
    static UploadedFile uploadOf(MultipartFile file) {
        return new UploadedFile(file.getContentType(), file.getSize(), file::getInputStream);
    }
}
