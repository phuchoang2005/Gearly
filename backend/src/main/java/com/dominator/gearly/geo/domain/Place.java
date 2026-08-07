package com.dominator.gearly.geo.domain;

/**
 * A selectable place — the numeric id the dataset keys on, and the name a person reads.
 *
 * <p>Geo's published value. A caller filling a dropdown or resolving an address needs exactly
 * these two fields; it does not need {@code iso2}, {@code stateCode}, or the fact that the
 * dataset stores a separate Mongo {@code _id} alongside its own numeric key.
 *
 * @param id   the dataset's numeric key, which is what an {@code Address} stores
 * @param name the display name
 */
public record Place(int id, String name) {
}
