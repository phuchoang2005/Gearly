package com.dominator.gearly.exception;

import org.springframework.http.HttpStatus;

/** Thrown when authentication fails or credentials are invalid. Maps to HTTP 401. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
