package com.dominator.gearly.assistant.domain;

/**
 * High-level intent categories produced by {@link IntentClassifier} and
 * consumed by the router. Replaces the free-form intent strings the model
 * used to emit.
 */
public enum Intent {
    NAVIGATION,
    CUSTOMER_SERVICE,
    STATIC_PAGE,
    UNRELATED;

    /** Parse a model-provided intent string, falling back to {@link #UNRELATED} when unrecognized. */
    public static Intent from(String raw) {
        if (raw == null) {
            return UNRELATED;
        }
        try {
            return Intent.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNRELATED;
        }
    }
}
