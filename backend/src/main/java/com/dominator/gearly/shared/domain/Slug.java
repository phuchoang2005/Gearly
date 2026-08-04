package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A URL-safe identifier: lower-case ASCII words joined by single hyphens.
 *
 * <p>Used for category and content paths. {@link #from(String)} is the lossy direction —
 * it strips diacritics (so {@code "Bàn phím"} becomes {@code "ban-phim"}, which matters
 * for the Vietnamese-language content in this catalog) and collapses everything else to
 * hyphens. The constructor is the strict direction and rejects anything that is not
 * already a slug, so a slug read back from storage is validated rather than silently
 * re-slugged.
 */
public record Slug(String value) {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    public Slug {
        Objects.requireNonNull(value, "slug must not be null");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("not a valid slug: " + value);
        }
    }

    @JsonCreator
    public static Slug of(String value) {
        return new Slug(value);
    }

    /** Derives a slug from free text. Throws if nothing sluggable is left. */
    public static Slug from(String text) {
        Objects.requireNonNull(text, "text must not be null");
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace("Đ", "D")
                .replace("đ", "d");
        ascii = DIACRITICS.matcher(ascii).replaceAll("");
        String slug = NON_SLUG.matcher(ascii.toLowerCase(Locale.ROOT))
                .replaceAll("-")
                .replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("cannot derive a slug from: " + text);
        }
        return new Slug(slug);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
