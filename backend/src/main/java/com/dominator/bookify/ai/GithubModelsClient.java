package com.dominator.bookify.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * Thin HTTP client for the GitHub Models chat/completions endpoint. Centralizes
 * the request wiring that {@link IntentClassifierService} and
 * {@code GithubModelsService} previously duplicated; the caller supplies the
 * bearer token (they use different ones) and the request payload.
 */
@Component
public class GithubModelsClient {

    private static final String API_URL = "https://models.github.ai/inference/chat/completions";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /** POST a chat/completions payload and return the raw JSON response body. */
    public String postChatCompletion(String bearerToken, JSONObject payload)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
