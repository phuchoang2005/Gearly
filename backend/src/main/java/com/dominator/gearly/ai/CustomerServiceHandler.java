package com.dominator.gearly.ai;

import com.dominator.gearly.model.Book;
import com.dominator.gearly.service.GithubModelsService;
import com.dominator.gearly.service.user.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceHandler {

    private final BookService bookService;
    private final GithubModelsService mainBot;
    private final AiPrompts prompts;

    public BackendResponse handle(String sessionId, AiDecision decision) {

        // Use the extracted query if available, otherwise fall back to the full message
        String searchTerm = (decision.getSearchQuery() != null && !decision.getSearchQuery().isBlank())
            ? decision.getSearchQuery()
            : decision.getOriginalUserMessage();

        // 1. Search the catalog for matching products
        List<Book> products = bookService.getBooksByTitle(searchTerm);

        if (!products.isEmpty()) {
            // 2. Build the product context for the main bot
            String productContext = products.stream()
                .map(p -> String.format("- %s (ID: %s): Price %s, Rating %s",
                    p.getTitle(), p.getId(), p.getPrice(), p.getAverageRating()))
                .collect(Collectors.joining("\n"));

            String prompt = prompts.getProductRecommendation()
                .formatted(decision.getOriginalUserMessage(), productContext);

            // 3. Ask the AI to explain/recommend
            String explanation = mainBot.getAIResponse(sessionId, prompt);

            // 4. Navigate on a single clear match, otherwise just return text
            if (products.size() == 1) {
                return BackendResponse.withNavigation(
                    explanation,
                    "/book/" + products.get(0).getId() // becomes /product/<id> in the frontend if needed
                );
            }

            return BackendResponse.text(explanation);
        }

        // Fallback: no products found, just chat
        String reply = mainBot.getAIResponse(sessionId, decision.getOriginalUserMessage());
        return BackendResponse.text(reply);
    }
}
