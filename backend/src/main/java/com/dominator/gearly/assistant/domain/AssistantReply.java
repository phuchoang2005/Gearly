package com.dominator.gearly.assistant.domain;

/**
 * What the assistant sends back: something to say, and optionally something for the UI to do.
 *
 * <p>Was {@code ai.BackendResponse}. The name was about the transport rather than the meaning —
 * every response in the application is a backend response. The JSON is unchanged
 * ({@code {"content":…,"uiAction":…}}, with {@code uiAction} null for a plain answer), which it
 * has to be: the React client reads both fields off the STOMP message.
 */
public record AssistantReply(String content, UiAction uiAction) {

    public static AssistantReply text(String content) {
        return new AssistantReply(content, null);
    }

    public static AssistantReply withNavigation(String content, String path) {
        return new AssistantReply(content, UiAction.navigateTo(path));
    }
}
