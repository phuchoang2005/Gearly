package com.dominator.gearly.assistant.infrastructure;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-session conversation history, held in this JVM.
 *
 * <h2>Known limitation, logged rather than fixed</h2>
 * The store is a {@link ConcurrentHashMap} in process memory, which has two consequences the
 * refactoring plan asks be written down rather than addressed here:
 *
 * <ul>
 *   <li><b>It does not survive a restart, and it does not scale out.</b> A second instance
 *       behind a load balancer has its own map, so a customer whose next message lands on the
 *       other node is talking to an assistant with amnesia. Fixing it means a shared store
 *       (Redis, or the database) and is a deployment decision, not a refactoring one.</li>
 *   <li><b>Nothing evicts a session.</b> Each conversation caps at ten messages, so a single
 *       session is bounded — but the number of <em>sessions</em> is not, and an entry is never
 *       removed. A long-running instance leaks one small object per distinct session id, which
 *       is client-supplied.</li>
 * </ul>
 *
 * <p>Both were true before S13; putting the memory behind {@code ConversationalModel} is what
 * makes them replaceable without touching the assistant's logic.
 */
@Service
public class ChatMemoryService {

    /** One memory per session. See the class note on why this is a follow-up, not a design. */
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
