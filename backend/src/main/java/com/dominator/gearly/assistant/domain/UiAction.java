package com.dominator.gearly.assistant.domain;

/**
 * Something the storefront should do as well as show the reply — today, navigate somewhere.
 *
 * <p>A record now rather than a two-field class with hand-written getters. Jackson serializes it
 * identically ({@code {"type":…,"path":…}}), which matters: this goes straight onto the STOMP
 * topic the React client subscribes to.
 */
public record UiAction(String type, String path) {

    /** The only action type the client understands. */
    public static UiAction navigateTo(String path) {
        return new UiAction("NAVIGATE", path);
    }
}
