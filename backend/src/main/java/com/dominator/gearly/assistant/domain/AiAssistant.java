package com.dominator.gearly.assistant.domain;

/**
 * The store's chat assistant, as the inbound edge sees it.
 *
 * <p>One method, because there is one thing anyone wants: given who is talking and what they
 * said, produce something to send back. Everything else — classifying the intent, searching the
 * catalog, asking a language model to phrase a recommendation, remembering the conversation — is
 * behind this and none of it is the websocket controller's business.
 *
 * <p>Before S13 that controller held an {@code IntentClassifierService} and an {@code AiRouter},
 * called them in the right order, and mutated the decision in between. Two of the three steps of
 * the pipeline were in the transport layer.
 */
public interface AiAssistant {

    AssistantReply respond(String sessionId, String userMessage);
}
