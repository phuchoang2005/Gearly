package com.dominator.gearly.service;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dominator.gearly.ai.AiPrompts;
import com.dominator.gearly.ai.GithubModelsClient;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.TextContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubModelsService {

    private static final String MODEL = "meta/meta-llama-3.1-8b-instruct";

    private final ChatMemoryService chatMemoryService;
    private final GithubModelsClient githubModelsClient;
    private final AiPrompts prompts;

    @Value("${github.models.token}") // from application.properties
    private String githubToken;

    public String getAIResponse(String sessionId, String userMessage) {
        try {
            chatMemoryService.addUserMessage(sessionId, userMessage);

            JSONObject payload = new JSONObject()
                    .put("model", MODEL)
                    .put("messages", buildMessages(sessionId))
                    .put("max_tokens", 256);

            String aiReply = parseReply(githubModelsClient.postChatCompletion(githubToken, payload));
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
                .put("content", prompts.getSystemAssistant()));

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
