package com.dominator.bookify.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.dominator.bookify.ai.AiDecision;
import com.dominator.bookify.ai.AiRouter;
import com.dominator.bookify.ai.BackendResponse;
import com.dominator.bookify.ai.IntentClassifierService;
import com.dominator.bookify.service.GithubModelsService;

@Controller
public class ChatController {

    private final GithubModelsService githubModelsService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IntentClassifierService intentClassifierService;
    private final AiRouter aiRouter;


    public ChatController(
        GithubModelsService githubModelsService,
        IntentClassifierService intentClassifierService,
        AiRouter aiRouter,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.githubModelsService = githubModelsService;
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
