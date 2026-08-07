package com.dominator.gearly.assistant.domain;

/**
 * What the classifier made of a message: what the customer seems to want, where they might be
 * trying to go, and what to search for if they are asking about a product.
 *
 * <p>Was a mutable five-field bag with a setter each, built field by field in the classifier and
 * then <em>modified again</em> by the websocket controller — {@code decision.setOriginalUserMessage(…)}
 * after {@code classify()} had already returned it. That is why {@link #originalUserMessage} was
 * reliably null anywhere the controller was not the caller. It is a constructor argument now, so
 * a decision cannot exist without the message it was made about.
 */
public record AiDecision(
        Intent intent,
        NavigationTarget target,
        String content,
        String originalUserMessage,
        String searchQuery) {

    /** The answer when the model could not be reached or could not be understood. */
    public static AiDecision unrelated(String userMessage, String content) {
        return new AiDecision(Intent.UNRELATED, NavigationTarget.UNKNOWN, content, userMessage, null);
    }

    /** The phrase to search the catalog with: the extracted query, or the whole message. */
    public String effectiveSearchTerm() {
        return (searchQuery != null && !searchQuery.isBlank()) ? searchQuery : originalUserMessage;
    }
}
