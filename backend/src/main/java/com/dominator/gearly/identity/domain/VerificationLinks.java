package com.dominator.gearly.identity.domain;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Builds the links that verification and password-reset mails point at.
 *
 * <p>The route was written out twice in {@code VerificationTokenService}, each copy beginning
 * {@code "http://localhost:8080/api/users/verify?token="}. That is two problems in one line: a
 * deployment URL compiled into a service — so the mail a staging or production customer received
 * pointed at the developer's own machine — and the query-string contract for a public endpoint
 * duplicated, so the two could drift apart.
 *
 * <p>The host arrives from {@code gearly.identity.public-base-url}. The path stays here, beside
 * the tokens it describes, because it is this context's route and nobody else's — in particular
 * not the notification context's, which is why {@code Notification} takes a finished URL.
 */
public record VerificationLinks(String baseUrl) {

    public VerificationLinks {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        // A trailing slash would produce "…8080//api/users/verify", which most servers tolerate
        // and some proxies do not.
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** Where the button in a {@code type} mail should lead for this token. */
    public String forToken(VerificationToken.TokenType type, String token) {
        return baseUrl + "/api/users/verify"
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&tokenType=" + type.name();
    }
}
