package com.dominator.bookify.ai;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IntentClassifierService {

    @Value("${github.models.secondToken}")
    private String token;

    private static final String API_URL =
        "https://models.github.ai/inference/chat/completions";

    public AiDecision classify(String userMessage) {
        try {
            JSONObject payload = new JSONObject()
                .put("model", "meta/meta-llama-3.1-8b-instruct")
                .put("temperature", 0)
                .put("max_tokens", 200)
                .put("messages", new JSONArray()
                    .put(new JSONObject()
                        .put("role", "system")
                        .put("content",
                            """
                            You are an intent classification engine for a PC Store.

                            INTENTS:
                            - NAVIGATION: For "Go to...", "Login", "Register".
                            - CUSTOMER_SERVICE: For product searches ("Ryzen CPU"), questions ("What is RAM?"), AND general greetings ("Hi", "How are you?").
                            - STATIC_PAGE: For "About us", "Terms".
                            - UNRELATED: Only for questions completely off-topic (e.g., "How to bake a cake?").

                            OUTPUT RULES:
                            1. If user greets or chats ("Hi", "Thanks"), set intent to CUSTOMER_SERVICE and leave "search_query" as null.
                            2. If user asks for a product, set intent to CUSTOMER_SERVICE and put the KEYWORD in "search_query".
                            3. Respond ONLY in JSON.

                            EXAMPLES:
                            User: "Hi, how are you?"
                            JSON: {"intent": "CUSTOMER_SERVICE", "target": "UNKNOWN", "content": "", "search_query": null}

                            User: "Show me Ryzen 5"
                            JSON: {"intent": "CUSTOMER_SERVICE", "target": "UNKNOWN", "content": "", "search_query": "Ryzen 5"}

                            User: "Go to login"
                            JSON: {"intent": "NAVIGATION", "target": "LOGIN", "content": "", "search_query": null}
                            """
                        ))
                    .put(new JSONObject()
                        .put("role", "user")
                        .put("content", userMessage))
                );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response =
                HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body())
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message");

            JSONObject parsed = new JSONObject(json.getString("content"));

            AiDecision decision = new AiDecision();
            decision.setIntent(parsed.getString("intent"));
            decision.setContent(parsed.optString("content", ""));
            decision.setSearchQuery(parsed.optString("search_query", null)); // Capture the query

            try {
                decision.setTarget(
                    NavigationTarget.valueOf(parsed.optString("target", "UNKNOWN"))
                );
            } catch (Exception e) {
                decision.setTarget(NavigationTarget.UNKNOWN);
            }

            return decision;

        } catch (Exception e) {
            AiDecision fallback = new AiDecision();
            fallback.setIntent("UNRELATED");
            fallback.setTarget(NavigationTarget.UNKNOWN);
            fallback.setContent("Sorry, I can only help with this store.");
            return fallback;
        }
    }
}
