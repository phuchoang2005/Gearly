package com.dominator.gearly.storage.infrastructure;

import com.dominator.gearly.storage.domain.StorageArea;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Where each storage area lives and what it will accept, bound from {@code gearly.storage.*}.
 *
 * <p>Replaces {@code app.avatar.upload-dir} / {@code app.avatar.public-base} and the
 * {@code private static final String UPLOAD_DIR = "uploads/"} that {@code MediaController}
 * carried. Both areas are configured the same way now, which is the point: the product-image
 * directory was not configurable at all, so the two halves of the same {@code uploads/} tree
 * were set in two different ways and one of them needed a rebuild.
 *
 * @param maxFileSize          the largest file this application will keep, per area-independent rule
 * @param allowedContentTypes  the MIME types it will accept at all
 * @param areas                directory and public URL prefix per area
 */
@ConfigurationProperties(prefix = "gearly.storage")
public record StorageProperties(
        DataSize maxFileSize,
        Set<String> allowedContentTypes,
        Map<StorageArea, Area> areas) {

    /**
     * @param directory  where files are written, relative to the working directory or absolute
     * @param publicBase the URL prefix they are served under; must match the static mapping in
     *                   {@code application.properties}
     */
    public record Area(String directory, String publicBase) {
    }

    public StorageProperties {
        if (maxFileSize == null) {
            maxFileSize = DataSize.ofMegabytes(10);
        }
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
        }
        allowedContentTypes = normalized(allowedContentTypes);

        Map<StorageArea, Area> configured = new LinkedHashMap<>(areas == null ? Map.of() : areas);
        configured.putIfAbsent(StorageArea.PRODUCT_IMAGES, new Area("uploads", "/uploads"));
        configured.putIfAbsent(StorageArea.AVATARS, new Area("uploads/avatars", "/uploads/avatars"));
        areas = Map.copyOf(configured);
    }

    public Area areaFor(StorageArea area) {
        Area configured = areas.get(area);
        if (configured == null) {
            // Unreachable given the compact constructor, but a missing area would otherwise
            // surface as an NPE deep inside a write.
            throw new IllegalStateException("No gearly.storage.areas entry for " + area);
        }
        return configured;
    }

    private static Set<String> normalized(Set<String> types) {
        Set<String> lower = new LinkedHashSet<>();
        types.forEach(type -> lower.add(type.trim().toLowerCase()));
        return Set.copyOf(lower);
    }
}
