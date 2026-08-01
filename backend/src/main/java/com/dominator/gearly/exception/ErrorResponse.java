package com.dominator.gearly.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body returned by {@link GlobalExceptionHandler}.
 * <p>
 * The {@code error} field carries the human-readable message; both frontends read it as
 * {@code response.data.error}. {@code fieldErrors} is populated only for validation failures
 * and omitted from the JSON otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error) {
        return new ErrorResponse(Instant.now(), status, error, null);
    }

    public static ErrorResponse of(int status, String error, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, fieldErrors);
    }
}
