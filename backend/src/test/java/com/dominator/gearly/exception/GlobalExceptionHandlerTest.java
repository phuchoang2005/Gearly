package com.dominator.gearly.exception;

import com.dominator.gearly.ordering.domain.IllegalOrderTransitionException;
import com.dominator.gearly.ordering.domain.OrderCannotBeCancelledException;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.reviews.domain.ReviewNotYoursException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises every branch of {@link GlobalExceptionHandler} in isolation via a
 * standalone MockMvc setup + a throwaway controller — no Spring context, no
 * MongoDB, no security. Complements the {@code @WebMvcTest} slices that assert
 * the same handler through the real security chain.
 */
class GlobalExceptionHandlerTest {

    @RestController
    static class BoomController {
        @GetMapping("/boom/not-found")
        void notFound() {
            throw new ResourceNotFoundException("thing not found");
        }

        @GetMapping("/boom/conflict")
        void conflict() {
            throw new ConflictException("email already registered");
        }

        @GetMapping("/boom/bad-request")
        void badRequest() {
            throw new BadRequestException("bad input");
        }

        @GetMapping("/boom/illegal-transition")
        void illegalTransition() {
            throw new IllegalOrderTransitionException(OrderStatus.PENDING, OrderStatus.REFUNDED);
        }

        @GetMapping("/boom/uncancellable")
        void uncancellable() {
            throw new OrderCannotBeCancelledException(OrderStatus.SHIPPED);
        }

        @GetMapping("/boom/stale")
        void stale() {
            throw new OptimisticLockingFailureException(
                    "Cannot save entity 42 with version 3 to collection products");
        }

        @GetMapping("/boom/not-yours")
        void notYours() {
            throw new ReviewNotYoursException();
        }

        @GetMapping("/boom/generic")
        void generic() {
            throw new IllegalStateException("leaky internal detail");
        }

        @PostMapping("/boom/body")
        void body(@RequestBody Payload payload) {
            // never reached with a malformed body
        }

        record Payload(String name) {}
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new BoomController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void resourceNotFound_maps404() throws Exception {
        mvc.perform(get("/boom/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("thing not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void conflict_maps409() throws Exception {
        mvc.perform(get("/boom/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("email already registered"));
    }

    @Test
    void badRequest_maps400() throws Exception {
        mvc.perform(get("/boom/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("bad input"));
    }

    @Test
    void malformedBody_maps400_withoutLeakingCause() throws Exception {
        mvc.perform(post("/boom/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    @Test
    void optimisticLockingFailure_maps409_withoutLeakingTheVersion() throws Exception {
        // A concurrent checkout losing the race on a @Versioned Product must read as a
        // retryable conflict, not as a server error — and must not disclose the internal
        // version counter or collection name.
        mvc.perform(get("/boom/stale"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("This item was modified by another request. Please refresh and try again."));
    }

    /**
     * The aggregate's own conflicts must come out as the 409 the services used to return by
     * throwing {@code ConflictException}. This is the assertion the S8 characterization suite
     * points at when it stopped expecting that type: an order refusing an illegal transition
     * or an uncancellable cancellation is the same HTTP response it always was, just decided
     * here rather than in the domain.
     */
    @Test
    void anIllegalOrderTransition_maps409() throws Exception {
        mvc.perform(get("/boom/illegal-transition"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("An order cannot go from PENDING to REFUNDED"));
    }

    @Test
    void cancellingAnOrderThatHasShipped_maps409() throws Exception {
        mvc.perform(get("/boom/uncancellable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("This order already has status that cannot be cancelled: SHIPPED"));
    }

    /**
     * The regression this was added for: {@code AccessDeniedDomainException} was in the shared
     * kernel and {@code ReviewNotYoursException} already extended it, but nothing here handled
     * it — so a correctly refused request fell through to {@link #uncaughtException_maps500_withOpaqueMessage}'s
     * catch-all and answered <b>500 "Internal server error"</b>. The status is the visible half;
     * the message is the other half, since the opaque 500 also swallowed the reason.
     */
    @Test
    void aDomainAccessDenial_maps403_withTheDomainsOwnMessage() throws Exception {
        mvc.perform(get("/boom/not-yours"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error")
                        .value("You are not allowed to review the items in this order"));
    }

    @Test
    void uncaughtException_maps500_withOpaqueMessage() throws Exception {
        mvc.perform(get("/boom/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal server error")) // no "leaky internal detail"
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
}
