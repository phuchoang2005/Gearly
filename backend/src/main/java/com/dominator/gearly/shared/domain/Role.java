package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

/**
 * What a user is allowed to be.
 *
 * <p>Replaces {@code User.role}, a {@code String} defaulted to {@code "CUSTOMER"} in a
 * field initializer and concatenated straight into a Spring Security authority as
 * {@code "ROLE_" + user.getRole()}. Any typo in that string produced a valid-looking
 * authority that simply matched no rule — a silent authorization failure rather than a
 * loud one.
 *
 * <p>The constant names are the stored and transmitted values, so Spring Data's built-in
 * enum handling and Jackson's default enum serialization both already write exactly what
 * the documents contain. {@link #authority()} is the one derived form.
 *
 * <p><b>Placement:</b> the shared kernel rather than {@code identity.domain}, because
 * {@code shared} may not depend on a context and {@code DomainTypeConverters} lives here.
 * S12 revisits this when {@code User} becomes a real aggregate.
 */
public enum Role {

    CUSTOMER,
    ADMIN;

    /** The default for a newly registered account, matching the old field initializer. */
    public static final Role DEFAULT = CUSTOMER;

    /** The Spring Security authority name, i.e. the constant prefixed with {@code ROLE_}. */
    public String authority() {
        return "ROLE_" + name();
    }

    /**
     * Parses a stored or submitted role, case-insensitively.
     *
     * @throws IllegalArgumentException on an unknown value — deliberately louder than the
     *         string concatenation it replaces
     */
    @JsonCreator
    public static Role fromValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown role: " + value));
    }
}
