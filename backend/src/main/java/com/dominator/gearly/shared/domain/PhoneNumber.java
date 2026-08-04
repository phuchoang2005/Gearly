package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A contact phone number, kept exactly as the customer typed it.
 *
 * <p>Validation is intentionally shallow: 6–20 characters drawn from digits and the usual
 * separators, with an optional leading {@code +}. The stored data is a mix of Vietnamese
 * local format ({@code 0912 345 678}) and E.164, and this codebase has no libphonenumber
 * dependency — a stricter rule here would reject existing rows on read, which is exactly
 * the failure mode S9 must not introduce. What this buys is that a phone field can no
 * longer hold an arbitrary string.
 */
public record PhoneNumber(String value) {

    private static final Pattern PATTERN = Pattern.compile("^\\+?[0-9 ()\\-.]{6,20}$");

    public PhoneNumber {
        Objects.requireNonNull(value, "phone number must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("not a valid phone number: " + value);
        }
    }

    @JsonCreator
    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    /** Digits only (and a leading {@code +}), for comparison rather than for storage. */
    public String digitsOnly() {
        String digits = value.replaceAll("[^0-9]", "");
        return value.startsWith("+") ? "+" + digits : digits;
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
