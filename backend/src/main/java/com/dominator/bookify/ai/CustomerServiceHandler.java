//
package com.dominator.bookify.ai;

import com.dominator.bookify.model.Book;
import com.dominator.bookify.service.GithubModelsService;
import com.dominator.bookify.service.user.BookService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceHandler {

    private final BookService bookService; 
    private final GithubModelsService mainBot;

    public CustomerServiceHandler(BookService bookService, GithubModelsService mainBot) {
        this.bookService = bookService;
        this.mainBot = mainBot;
    }

    public BackendResponse handle(String sessionId, AiDecision decision) {

        // Use extracted query if available, otherwise fallback to full message
        String searchTerm = (decision.getSearchQuery() != null && !decision.getSearchQuery().isBlank())
            ? decision.getSearchQuery()
            : decision.getOriginalUserMessage();

        // 1. Search DB for products (Books)
        List<Book> products = bookService.getBooksByTitle(searchTerm);

        if (!products.isEmpty()) {
            // 2. Prepare context for the Main Bot
            String productContext = products.stream()
                .map(p -> String.format("- %s (ID: %s): Price %s, Rating %s", 
                    p.getTitle(), p.getId(), p.getPrice(), p.getAverageRating()))
                .collect(Collectors.joining("\n"));

            String prompt = """
                User asked: "%s"
                
                I found these products in the database:
                %s
                
                Task:
                1. Summarize these options for the user.
                2. Recommend the best one based on rating or relevance.
                3. Tell the user you are taking them to the product page if there is a clear best match.
                """.formatted(decision.getOriginalUserMessage(), productContext);

            // 3. Get AI Explanation
            String explanation = mainBot.getAIResponse(sessionId, prompt);

            // 4. Decision: Navigate or just Text?
            // If we found exactly one, or a very clear top result, we navigate.
            if (products.size() == 1) {
                return BackendResponse.withNavigation(
                    explanation,
                    "/book/" + products.get(0).getId() // Changes to /product/id in frontend if needed
                );
            }
            
            // If multiple, just show text (user can ask to narrow down)
            return BackendResponse.text(explanation);
        }

        // Fallback: No products found, just chat
        String reply = mainBot.getAIResponse(sessionId, decision.getOriginalUserMessage());
        return BackendResponse.text(reply);
    }
}