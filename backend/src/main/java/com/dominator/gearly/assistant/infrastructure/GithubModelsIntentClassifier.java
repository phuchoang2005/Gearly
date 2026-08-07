package com.dominator.gearly.assistant.infrastructure;

import com.dominator.gearly.assistant.domain.AiDecision;
import com.dominator.gearly.assistant.domain.Intent;
import com.dominator.gearly.assistant.domain.IntentClassifier;
import com.dominator.gearly.assistant.domain.NavigationTarget;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubModelsIntentClassifier implements IntentClassifier {

    private static final String MODEL = "meta/meta-llama-3.1-8b-instruct";

    private final GithubModelsClient githubModelsClient;
    private final AiPrompts prompts;

    @Value("${github.models.secondToken}")
    private String token;

    @Override
    public AiDecision classify(String userMessage) {
        try {
            JSONObject payload = new JSONObject()
                .put("model", MODEL)
                .put("temperature", 0)
                .put("max_tokens", 200)
                .put("messages", new JSONArray()
                    .put(new JSONObject()
                        .put("role", "system")
                        .put("content", prompts.getIntentClassifier()))
                    .put(new JSONObject()
                        .put("role", "user")
                        .put("content", userMessage)));

            String responseBody = githubModelsClient.postChatCompletion(token, payload);

            JSONObject message = new JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message");

            JSONObject parsed = new JSONObject(message.getString("content"));

            // The message is carried on the decision from the start. It used to be assigned
            // by ChatController after this method returned, which left it null for any other
            // caller — including the fallback below, whose handler needs it to search.
            return new AiDecision(
                    Intent.from(parsed.optString("intent", null)),
                    parseTarget(parsed.optString("target", "UNKNOWN")),
                    parsed.optString("content", ""),
                    userMessage,
                    parsed.optString("search_query", null));

        } catch (Exception e) {
            // Deliberately not rethrown: a model outage should degrade the chat, not break the
            // socket. Logged, which it was not before — the classifier failing and the customer
            // genuinely asking something off-topic produced the identical reply and no trace.
            log.warn("Intent classification failed; answering as UNRELATED", e);
            return AiDecision.unrelated(userMessage, "Sorry, I can only help with this store.");
        }
    }

    private NavigationTarget parseTarget(String raw) {
        try {
            return NavigationTarget.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return NavigationTarget.UNKNOWN;
        }
    }
}
