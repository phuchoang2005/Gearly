package com.dominator.gearly.assistant.application;

import com.dominator.gearly.assistant.domain.AiDecision;
import com.dominator.gearly.assistant.domain.AssistantReply;
import com.dominator.gearly.assistant.domain.ConversationalModel;
import com.dominator.gearly.assistant.infrastructure.AiPrompts;
import com.dominator.gearly.catalog.domain.ProductBrief;
import com.dominator.gearly.catalog.domain.ProductSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Answers a product question: search the catalog, then ask the model to say something useful
 * about what came back.
 *
 * <h2>What changed at the context boundary</h2>
 * It held {@code catalog.application.ProductQueryService} and formatted
 * {@code catalog.api.ProductSummaryDTO}s — another context's application service and its
 * <em>response DTO</em>. Beyond being the coupling
 * {@code contexts_touch_each_other_only_through_published_types} refuses, it meant a change to
 * the storefront's product-list JSON could break the chatbot. It uses
 * {@link ProductSearchPort} and {@link ProductBrief} now, which are catalog's published
 * language and change only deliberately.
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceHandler {

    private final ProductSearchPort catalog;
    private final ConversationalModel model;
    private final AiPrompts prompts;

    public AssistantReply handle(String sessionId, AiDecision decision) {
        List<ProductBrief> products = catalog.searchByTitle(decision.effectiveSearchTerm());

        if (products.isEmpty()) {
            // Nothing matched, so there is nothing to recommend — just answer the question.
            return AssistantReply.text(model.reply(sessionId, decision.originalUserMessage()));
        }

        String prompt = prompts.getProductRecommendation()
                .formatted(decision.originalUserMessage(), describe(products));
        String explanation = model.reply(sessionId, prompt);

        // One clear match is worth taking the customer to; several is a list to read first.
        if (products.size() == 1) {
            return AssistantReply.withNavigation(
                    explanation, "/product/" + products.getFirst().productId().value());
        }
        return AssistantReply.text(explanation);
    }

    private static String describe(List<ProductBrief> products) {
        return products.stream()
                .map(product -> String.format("- %s (ID: %s): Price %s, Rating %s",
                        product.title(),
                        product.productId().value(),
                        product.price().toDouble(),
                        product.averageRating()))
                .collect(Collectors.joining("\n"));
    }
}
