package com.dominator.gearly.exception;

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

        @GetMapping("/boom/stale")
        void stale() {
            throw new OptimisticLockingFailureException(
                    "Cannot save entity 42 with version 3 to collection products");
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

    @Test
    void uncaughtException_maps500_withOpaqueMessage() throws Exception {
        mvc.perform(get("/boom/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal server error")) // no "leaky internal detail"
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
}
