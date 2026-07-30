package com.dominator.bookify.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the request conflicts with current state (e.g. duplicate email). Maps to HTTP 409. */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
