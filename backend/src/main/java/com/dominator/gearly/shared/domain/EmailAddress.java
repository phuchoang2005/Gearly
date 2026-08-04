package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A syntactically valid email address.
 *
 * <p><b>The value is preserved verbatim, not normalized.</b> Lower-casing here would be
 * the obvious move, but {@code User.email} carries a unique index and is the login
 * identifier: rewriting the case of stored addresses is a data migration with a duplicate-key
 * failure mode, not a type change. S9's contract is that the document is byte-identical
 * afterwards, so this type validates and nothing more. {@link #normalized()} exists for
 * callers that want a comparison key today, and case normalization becomes a real
 * migration in S12 when {@code User} becomes an aggregate.
 */
public record EmailAddress(String value) {

    /**
     * Deliberately permissive — one {@code @}, no whitespace, a dot-bearing domain.
     * Matching RFC 5322 in a regex is a well-known dead end, and the authoritative check
     * is the verification email the system already sends.
     */
    private static final Pattern PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");

    public EmailAddress {
        Objects.requireNonNull(value, "email must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("not a valid email address: " + value);
        }
    }

    @JsonCreator
    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    /** The lower-cased form, for comparison. Not what gets stored — see the class note. */
    public String normalized() {
        return value.toLowerCase(java.util.Locale.ROOT);
    }

    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
