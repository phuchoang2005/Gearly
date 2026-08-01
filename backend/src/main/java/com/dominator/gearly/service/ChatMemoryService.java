package com.dominator.gearly.service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMemoryService {

    // One memory per session
    private final Map<String, ChatMemory> memoryStore = new ConcurrentHashMap<>();

    // Get or create memory for a session
    public ChatMemory getMemory(String sessionId) {
        return memoryStore.computeIfAbsent(
            sessionId,
            id -> MessageWindowChatMemory.withMaxMessages(10)
        );
    }

    public void addUserMessage(String sessionId, String content) {
        getMemory(sessionId).add(UserMessage.from(content));
    }

    public void addAiMessage(String sessionId, String content) {
        getMemory(sessionId).add(AiMessage.from(content));
    }
}
