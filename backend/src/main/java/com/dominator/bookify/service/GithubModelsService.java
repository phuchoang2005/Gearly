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

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.TextContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubModelsService {

    private final ChatMemoryService chatMemoryService;

    @Value("${github.models.token}") // from application.properties
    private String githubToken;

    private static final String API_URL = "https://models.github.ai/inference/chat/completions";

    private static final String SYSTEM_PROMPT = """
            You are an expert AI assistant for a Computer & PC Component Store.

            Your goals:
            - Help users choose between CPUs, GPUs, Laptops, etc.
            - Use the provided database context to give accurate prices and specs.
            - Be concise and professional.
            """;

    public String getAIResponse(String sessionId, String userMessage) {
        try {
            chatMemoryService.addUserMessage(sessionId, userMessage);

            JSONObject payload = new JSONObject()
                    .put("model", "meta/meta-llama-3.1-8b-instruct")
                    .put("messages", buildMessages(sessionId))
                    .put("max_tokens", 256);

            String aiReply = parseReply(callModel(payload));
            if (aiReply == null) {
                return "(no response)";
            }

            chatMemoryService.addAiMessage(sessionId, aiReply);
            return aiReply;
        } catch (IOException | InterruptedException e) {
            log.error("GitHub Models request failed", e);
            return "(error: " + e.getMessage() + ")";
        }
    }

    /** Build the OpenAI-style messages array: system prompt followed by conversation history. */
    private JSONArray buildMessages(String sessionId) {
        JSONArray messagesArray = new JSONArray();
        messagesArray.put(new JSONObject()
                .put("role", "system")
                .put("content", SYSTEM_PROMPT));

        chatMemoryService.getMemory(sessionId).messages().forEach(msg -> {
            if (msg instanceof UserMessage user) {
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
        return messagesArray;
    }

    private String callModel(JSONObject payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    /** Extract the assistant reply, or null when the response carries no choice/message. */
    private String parseReply(String responseBody) {
        JSONObject json = new JSONObject(responseBody);
        JSONArray choices = json.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
            if (msg != null) {
                return msg.optString("content", "(no content)");
            }
        }
        return null;
    }
}
