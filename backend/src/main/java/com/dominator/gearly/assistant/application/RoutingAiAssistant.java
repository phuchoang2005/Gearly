package com.dominator.gearly.assistant.application;

import com.dominator.gearly.assistant.domain.AiAssistant;
import com.dominator.gearly.assistant.domain.AiDecision;
import com.dominator.gearly.assistant.domain.AssistantReply;
import com.dominator.gearly.assistant.domain.IntentClassifier;
import com.dominator.gearly.assistant.domain.NavigationTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * The assistant pipeline: classify the message, then answer according to what it turned out to
 * be.
 *
 * <p>Was {@code ai.AiRouter}, which did only the second half — the websocket controller called
 * the classifier itself, then mutated the returned decision to attach the original message, then
 * called the router. Three steps of one pipeline split across the transport layer and a service,
 * with a mutation in the middle that nothing but that one caller performed. Both steps are here
 * now and {@code ChatController} calls {@link AiAssistant#respond} once.
 */
@Service
@RequiredArgsConstructor
public class RoutingAiAssistant implements AiAssistant {

    /**
     * Where each navigation target lives in the storefront.
     *
     * <p>A map rather than the two {@code switch} expressions this replaces. They had a
     * {@code default -> "/home"} each, which meant a target the classifier emitted but the
     * router had not been taught about silently sent the customer to the home page instead of
     * anywhere useful — and the two switches disagreed about which targets they handled, so
     * whether a mistake was silent depended on which intent came back.
     */
    private static final Map<NavigationTarget, String> PATHS = Map.ofEntries(
            Map.entry(NavigationTarget.HOME, "/home"),
            Map.entry(NavigationTarget.SHOP, "/shop"),
            Map.entry(NavigationTarget.LOGIN, "/login"),
            Map.entry(NavigationTarget.REGISTER, "/register"),
            Map.entry(NavigationTarget.PROFILE, "/me"),
            Map.entry(NavigationTarget.ORDERS, "/me/orders"),
            Map.entry(NavigationTarget.BLOG, "/blog"),
            Map.entry(NavigationTarget.ABOUT_US, "/about-us"),
            Map.entry(NavigationTarget.PRIVACY, "/privacy"),
            Map.entry(NavigationTarget.TERMS, "/terms"),
            Map.entry(NavigationTarget.RETURN_POLICY, "/return-policy"));

    private static final String FALLBACK_PATH = "/home";

    private final IntentClassifier intentClassifier;
    private final CustomerServiceHandler customerService;

    @Override
    public AssistantReply respond(String sessionId, String userMessage) {
        AiDecision decision = intentClassifier.classify(userMessage);

        return switch (decision.intent()) {
            case NAVIGATION, STATIC_PAGE ->
                    AssistantReply.withNavigation(decision.content(), pathFor(decision.target()));
            case CUSTOMER_SERVICE ->
                    customerService.handle(sessionId, decision);
            case UNRELATED ->
                    AssistantReply.text("Sorry, I currently can't help you with that.");
        };
    }

    /**
     * {@code NAVIGATION} and {@code STATIC_PAGE} share this, where they used to have a lookup
     * each. The split was never meaningful — a target belongs to exactly one of the two lists,
     * so a single table cannot be ambiguous — and keeping two meant a new target had to be added
     * to the right one or fall through to {@code /home}.
     */
    private static String pathFor(NavigationTarget target) {
        return target == null ? FALLBACK_PATH : PATHS.getOrDefault(target, FALLBACK_PATH);
    }
}
