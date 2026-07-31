package com.dominator.bookify.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * Loads the AI prompt templates from {@code classpath:prompts/*} once at startup,
 * keeping the (long) prompt text out of the Java sources.
 */
@Getter
@Component
public class AiPrompts {

    /** System prompt for the main customer-facing assistant. */
    private final String systemAssistant;

    /** Instruction prompt for the intent-classification model. */
    private final String intentClassifier;

    /** Product-recommendation template; format args: (userMessage, productContext). */
    private final String productRecommendation;

    public AiPrompts(
            @Value("classpath:prompts/system-assistant.txt") Resource systemAssistant,
            @Value("classpath:prompts/intent-classifier.txt") Resource intentClassifier,
            @Value("classpath:prompts/product-recommendation.txt") Resource productRecommendation)
            throws IOException {
        this.systemAssistant = read(systemAssistant);
        this.intentClassifier = read(intentClassifier);
        this.productRecommendation = read(productRecommendation);
    }

    private static String read(Resource resource) throws IOException {
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
