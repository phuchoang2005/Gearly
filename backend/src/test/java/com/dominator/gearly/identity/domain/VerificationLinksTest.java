package com.dominator.gearly.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The verification link, which used to be two copies of a string literal beginning
 * {@code "http://localhost:8080/api/users/verify?token="}.
 */
class VerificationLinksTest {

    private final VerificationLinks links = new VerificationLinks("https://gearly.example.com");

    @Test
    @DisplayName("builds the same route the hard-coded links did")
    void buildsTheRoute() {
        assertThat(links.forToken(VerificationToken.TokenType.EMAIL_VERIFICATION, "abc-123"))
                .isEqualTo("https://gearly.example.com/api/users/verify"
                        + "?token=abc-123&tokenType=EMAIL_VERIFICATION");
    }

    @Test
    @DisplayName("the token type is part of the link, so the two mails differ")
    void tokenTypeSelectsTheFlow() {
        assertThat(links.forToken(VerificationToken.TokenType.PASSWORD_RESET, "abc-123"))
                .endsWith("&tokenType=PASSWORD_RESET");
    }

    @Test
    @DisplayName("a trailing slash on the configured base does not produce a double slash")
    void trailingSlashIsTrimmed() {
        assertThat(new VerificationLinks("https://gearly.example.com/")
                .forToken(VerificationToken.TokenType.EMAIL_VERIFICATION, "t"))
                .contains("gearly.example.com/api/users/verify")
                .doesNotContain("//api");
    }

    /**
     * Tokens are UUIDs today, so this changes nothing about what is sent. It is here because the
     * value is interpolated into a query string, and "the current generator happens to produce
     * safe characters" is a property of a different class.
     */
    @Test
    @DisplayName("a token is URL-encoded rather than concatenated raw")
    void encodesTheToken() {
        assertThat(links.forToken(VerificationToken.TokenType.EMAIL_VERIFICATION, "a b&tokenType=X"))
                .contains("token=a+b%26tokenType%3DX")
                .endsWith("&tokenType=EMAIL_VERIFICATION");
    }
}
