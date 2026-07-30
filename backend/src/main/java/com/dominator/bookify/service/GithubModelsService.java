package com.dominator.bookify.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

//import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.TextContent;

@Service
@RequiredArgsConstructor
public class GithubModelsService {

    private final ChatMemoryService chatMemoryService;

    @Value("${github.models.token}") // from application.properties
    private String githubToken;

    private static final String API_URL = "https://models.github.ai/inference/chat/completions";

    public String getAIResponse(String sessionId, String userMessage) {
        try {
            // 1. Save user message
            chatMemoryService.addUserMessage(sessionId, userMessage);

            // 2. Build messages array from memory
            JSONArray messagesArray = new JSONArray();

            // System prompt (always first)
            messagesArray.put(new JSONObject()
                .put("role", "system")
                .put("content", """
                    You are an expert AI assistant for a Computer & PC Component Store.
                    
                    Your goals:
                    - Help users choose between CPUs, GPUs, Laptops, etc.
                    - Use the provided database context to give accurate prices and specs.
                    - Be concise and professional.
                    """));

            // Conversation history
            chatMemoryService.getMemory(sessionId).messages().forEach(msg -> {

                if (msg instanceof UserMessage user) {
                    // UserMessage can have multiple contents
                    user.contents().forEach(content -> {
                        if (content instanceof TextContent textContent) {
                            messagesArray.put(new JSONObject()
                                .put("role", "user")
                                .put("content", textContent.text()));
                        }
                    });


                } else if (msg instanceof AiMessage ai) {
                    messagesArray.put(new JSONObject()
                        .put("role", "assistant")
                        .put("content", ai.text()));
                }
            });


            // 3. Build payload (USE messagesArray DIRECTLY)
            JSONObject payload = new JSONObject()
                .put("model", "meta/meta-llama-3.1-8b-instruct")
                .put("messages", messagesArray)
                .put("max_tokens", 256);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. Parse AI reply
            JSONObject json = new JSONObject(response.body());
            JSONArray choices = json.optJSONArray("choices");

            if (choices != null && choices.length() > 0) {
                JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
                if (msg != null) {
                    String aiReply = msg.optString("content", "(no content)");

                    // 5. Save AI reply
                    chatMemoryService.addAiMessage(sessionId, aiReply);

                    // 6. Return
                    return aiReply;
                }
            }

            return "(no response)";

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "(error: " + e.getMessage() + ")";
        }
    }
}
