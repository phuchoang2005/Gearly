package com.dominator.gearly.assistant.api;

import com.dominator.gearly.assistant.domain.AiAssistant;
import com.dominator.gearly.assistant.domain.AssistantReply;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * The STOMP edge of the chat assistant. Destinations and message shapes unchanged: clients still
 * send to {@code /app/sendMessage} and subscribe to {@code /topic/chat/{sessionId}}.
 *
 * <p>An inbound adapter and nothing else now. It used to hold both the classifier and the router,
 * call them in order, and mutate the decision in between —
 * {@code decision.setOriginalUserMessage(...)} — so two thirds of the assistant's pipeline lived
 * in the transport layer and one of its invariants was maintained by this class remembering to.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final AiAssistant assistant;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sendMessage")
    public void sendMessage(ChatMessage message) {
        AssistantReply reply = assistant.respond(message.getSessionId(), message.getContent());

        messagingTemplate.convertAndSend("/topic/chat/" + message.getSessionId(), reply);
    }
}
