package com.dominator.gearly.exception;

import com.dominator.gearly.shared.domain.DomainConflictException;
import com.dominator.gearly.shared.domain.DomainNotFoundException;
import com.dominator.gearly.shared.domain.DomainRuleViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source of truth for HTTP error responses. Replaces the per-controller
 * try/catch blocks that used to return {@code Map.of("error", ...)}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Application exceptions carry their own status. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus().value(), ex.getMessage()));
    }

    /** Spring's own {@code ResponseStatusException}, still thrown by some services pending S3. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), message));
    }

    /** Bean-validation failures on {@code @Valid} request bodies → 400 with per-field messages. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Validation failed", fieldErrors));
    }

    /** Malformed/unreadable request body → 400 rather than a leaked 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Malformed request body"));
    }

    /**
     * An optimistic-locking clash → 409, not a leaked 500.
     *
     * <p>Raised when a {@code @Version}ed document (Product, Order, Cart) was changed by
     * someone else between this request reading it and writing it back — most importantly
     * two concurrent checkouts racing on the same product's stock, which is exactly the
     * oversell the version field exists to prevent. 409 tells the caller the request was
     * valid but is now stale: re-read and retry.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(),
                        "This item was modified by another request. Please refresh and try again."));
    }

    /**
     * A domain rule refused the requested state change → 409.
     *
     * <p>An illegal order-status transition, or cancelling an order that has already shipped.
     * The request was well-formed and the caller was entitled to make it; the aggregate is
     * simply not in a state where it makes sense.
     *
     * <p>One handler covers every context because {@link DomainConflictException} lives in the
     * shared kernel. An aggregate states the rule that broke and names no web type — it may
     * not, since {@code org.springframework.http} is banned from a domain package — so
     * choosing the status code is this class's job. That is what keeps the response identical
     * to the {@code ConflictException} these replaced.
     */
    @ExceptionHandler(DomainConflictException.class)
    public ResponseEntity<ErrorResponse> handleDomainConflict(DomainConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * The aggregate the request named does not exist → 404.
     *
     * <p>The sibling of the handler above, added when S11 gave the catalog a
     * {@code ProductNotFoundException}. {@code ProductService.getProductById} used to answer
     * {@code null} and leave each caller to decide what that meant; several forgot, and a
     * delisted product turned a checkout into a {@code NullPointerException} and an opaque
     * 500. The response is identical to {@link ResourceNotFoundException}'s — what changed is
     * that the domain can say "missing" without naming an HTTP type.
     */
    @ExceptionHandler(DomainNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDomainNotFound(DomainNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /**
     * A domain rule refused the request as it stands → 400.
     *
     * <p>Ordering ten units of something that has three is the case this was added for:
     * {@code InsufficientStockException} is the single rule S11 collapsed five copies of the
     * stock check into, and 400 is the status all five of them already returned through
     * {@link BadRequestException}. Moving where the rule lives and changing what it answers
     * are separate decisions; only the first has been made.
     */
    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleDomainRuleViolation(DomainRuleViolationException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    /** Missing static resource (e.g. an avatar/media file under /uploads/**) → 404, not 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "Resource not found"));
    }

    /** Catch-all: log the real cause server-side, return an opaque 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error"));
    }
}
