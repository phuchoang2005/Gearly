package com.dominator.gearly.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for all application-thrown exceptions that map to a specific HTTP status.
 * Prefer the dedicated subclasses ({@link ResourceNotFoundException}, {@link BadRequestException},
 * {@link UnauthorizedException}, {@link ConflictException}); use this directly only for one-off statuses.
 */
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
