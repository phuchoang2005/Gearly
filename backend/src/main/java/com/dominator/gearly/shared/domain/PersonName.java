package com.dominator.gearly.shared.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * A person's name, with {@code fullName} <b>derived</b> rather than stored beside the
 * parts it is made of.
 *
 * <p>This resolves a genuine split-brain in the current code: {@code UserService} computes
 * {@code fullName} from the first and last name on profile update, while
 * {@code AuthService.register} takes whatever {@code fullName} the registration request
 * body carried. Register as {@code "Jane Doe"} with a {@code fullName} of {@code "Bob"}
 * and the two disagree permanently — nothing ever reconciles them, and the reviews list
 * and the admin console read the stale one.
 *
 * <p>{@link #fullName()} is the single answer to that question. {@code User} keeps its
 * three stored fields for now (the document shape is frozen for S9) but they are written
 * from one place.
 */
public record PersonName(String firstName, String lastName) {

    public PersonName {
        firstName = requireText(firstName, "first name");
        lastName = requireText(lastName, "last name");
    }

    public static PersonName of(String firstName, String lastName) {
        return new PersonName(firstName, lastName);
    }

    /**
     * Splits a single display name into parts on the last space: everything before it is
     * the first name, the remainder is the last name.
     *
     * <p>For the registration path, which historically accepted a free-text
     * {@code fullName}. A name with no space at all has no last name to speak of and is
     * rejected rather than guessed at.
     */
    public static PersonName parse(String fullName) {
        String trimmed = requireText(fullName, "full name");
        int split = trimmed.lastIndexOf(' ');
        if (split < 0) {
            throw new IllegalArgumentException("full name must contain a first and last name: " + fullName);
        }
        return new PersonName(trimmed.substring(0, split), trimmed.substring(split + 1));
    }

    /**
     * {@link #parse(String)} for callers who cannot control the input — the Google sign-in
     * flow, where the {@code name} claim is a single free-text field and a one-word display
     * name is perfectly legal.
     */
    public static Optional<PersonName> tryParse(String fullName) {
        try {
            return Optional.of(parse(fullName));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** The one source of truth for the display name. */
    public String fullName() {
        return firstName + " " + lastName;
    }

    public String initials() {
        return "" + Character.toUpperCase(firstName.charAt(0)) + Character.toUpperCase(lastName.charAt(0));
    }

    @Override
    public String toString() {
        return fullName();
    }

    private static String requireText(String value, String what) {
        Objects.requireNonNull(value, what + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        return trimmed;
    }
}
