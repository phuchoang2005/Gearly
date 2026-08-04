package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The identity of a catalog category.
 *
 * <p><b>This is the type that absorbs the project's id asymmetry.</b> Category ids are
 * stored as BSON {@code ObjectId} — {@code Product.categoryIds} is a
 * {@code List<ObjectId>} — while every other id in the system is stored as a string.
 * That mismatch is currently patched by hand at each boundary: {@code ProductMapper}
 * converts hex strings to {@code ObjectId} on the update path, the create path takes
 * {@code ObjectId} straight off the request body, and {@code ProductService} converts back
 * to strings to resolve names.
 *
 * <p>Here the conversion happens once, in the Mongo converter pair registered by
 * {@code DomainTypeConverters}: the field is a {@code CategoryId} in Java, an
 * {@code ObjectId} in the document, and a hex string on the wire — all three exactly as
 * they are today. The constructor therefore enforces the 24-hex-character form, because a
 * value that cannot become an {@code ObjectId} would fail at write time rather than at
 * construction time.
 */
public record CategoryId(String value) implements DomainId {

    public CategoryId {
        value = DomainId.requireObjectId(value, "category id");
    }

    @JsonCreator
    public static CategoryId of(String value) {
        return new CategoryId(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
