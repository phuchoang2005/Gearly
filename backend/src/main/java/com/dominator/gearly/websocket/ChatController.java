package com.dominator.gearly.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.dominator.gearly.ai.AiDecision;
import com.dominator.gearly.ai.AiRouter;
import com.dominator.gearly.ai.BackendResponse;
import com.dominator.gearly.ai.IntentClassifierService;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final IntentClassifierService intentClassifierService;
    private final AiRouter aiRouter;

    public ChatController(
        IntentClassifierService intentClassifierService,
        AiRouter aiRouter,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.intentClassifierService = intentClassifierService;
        this.aiRouter = aiRouter;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/sendMessage")
    public void sendMessage(ChatMessage message) {

        AiDecision decision =
            intentClassifierService.classify(message.getContent());

        decision.setOriginalUserMessage(message.getContent());

        BackendResponse response =
            aiRouter.route(decision, message.getSessionId());

        messagingTemplate.convertAndSend(
            "/topic/chat/" + message.getSessionId(),
            response
        );
    }


}
