package com.dominator.gearly.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntentClassifierService {

    private static final String MODEL = "meta/meta-llama-3.1-8b-instruct";

    private final GithubModelsClient githubModelsClient;
    private final AiPrompts prompts;

    @Value("${github.models.secondToken}")
    private String token;

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

            AiDecision decision = new AiDecision();
            decision.setIntent(Intent.from(parsed.optString("intent", null)));
            decision.setContent(parsed.optString("content", ""));
            decision.setSearchQuery(parsed.optString("search_query", null));
            decision.setTarget(parseTarget(parsed.optString("target", "UNKNOWN")));
            return decision;

        } catch (Exception e) {
            AiDecision fallback = new AiDecision();
            fallback.setIntent(Intent.UNRELATED);
            fallback.setTarget(NavigationTarget.UNKNOWN);
            fallback.setContent("Sorry, I can only help with this store.");
            return fallback;
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
