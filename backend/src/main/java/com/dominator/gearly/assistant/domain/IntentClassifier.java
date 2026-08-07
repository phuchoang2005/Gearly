package com.dominator.gearly.assistant.domain;

/**
 * Works out what a message is asking for.
 *
 * <p>A port because the implementation is a call to a hosted language model — the thing most
 * worth being able to replace with a stub. Classification is also the step whose failure must
 * not be an error: the adapter answers {@link AiDecision#unrelated} rather than throwing, so a
 * model outage degrades the chat to "I can't help with that" instead of breaking the socket.
 */
public interface IntentClassifier {

    AiDecision classify(String userMessage);
}
