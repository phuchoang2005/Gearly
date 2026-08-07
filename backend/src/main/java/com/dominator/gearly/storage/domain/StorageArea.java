package com.dominator.gearly.storage.domain;

/**
 * Where a file belongs. Each area maps to a directory and a public URL prefix, both configured
 * in {@code gearly.storage.areas.*}.
 *
 * <p>An enum rather than a caller-supplied path, because a caller-supplied path is a directory
 * traversal waiting to be written. The set of places this application will write to is fixed at
 * compile time and adding one is a visible edit.
 */
public enum StorageArea {

    /** Product imagery, uploaded from the admin console's product forms. */
    PRODUCT_IMAGES,

    /** Customer avatars — one current file per account. */
    AVATARS
}
