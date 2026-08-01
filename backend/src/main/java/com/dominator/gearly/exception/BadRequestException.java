package com.dominator.gearly.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the request is malformed or violates a business rule. Maps to HTTP 400. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
