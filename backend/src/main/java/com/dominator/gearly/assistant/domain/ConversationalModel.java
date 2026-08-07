package com.dominator.gearly.assistant.domain;

/**
 * A language model that remembers the conversation it is having.
 *
 * <p>The session id is the whole of the memory contract: the caller says which conversation this
 * turn belongs to, and the adapter is responsible for the history. That keeps the application
 * layer free of any notion of a message window, a token budget or where the history is kept —
 * which matters, because where it is kept is currently a scale-out problem. See
 * {@code assistant.infrastructure.ChatMemoryService}.
 */
public interface ConversationalModel {

    /** Answers within the given conversation, or a readable apology if the model cannot be reached. */
    String reply(String sessionId, String prompt);
}
