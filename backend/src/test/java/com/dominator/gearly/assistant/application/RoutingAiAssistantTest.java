package com.dominator.gearly.assistant.application;

import com.dominator.gearly.assistant.domain.AiDecision;
import com.dominator.gearly.assistant.domain.AssistantReply;
import com.dominator.gearly.assistant.domain.Intent;
import com.dominator.gearly.assistant.domain.IntentClassifier;
import com.dominator.gearly.assistant.domain.NavigationTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The assistant pipeline, which had no test at all — every piece of it was reachable only
 * through a live STOMP session and two calls to a hosted language model.
 *
 * <p>{@link IntentClassifier} is a port now, so the model is a stub and the routing is an
 * ordinary unit test.
 */
@ExtendWith(MockitoExtension.class)
class RoutingAiAssistantTest {

    @Mock private IntentClassifier classifier;
    @Mock private CustomerServiceHandler customerService;

    private RoutingAiAssistant assistant;

    @BeforeEach
    void setUp() {
        assistant = new RoutingAiAssistant(classifier, customerService);
    }

    private void classifierReturns(Intent intent, NavigationTarget target, String content) {
        when(classifier.classify("hi")).thenReturn(
                new AiDecision(intent, target, content, "hi", null));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "HOME,/home", "SHOP,/shop", "LOGIN,/login", "REGISTER,/register",
            "PROFILE,/me", "ORDERS,/me/orders", "BLOG,/blog"})
    @DisplayName("a navigation intent answers with the storefront path for its target")
    void navigates(NavigationTarget target, String path) {
        classifierReturns(Intent.NAVIGATION, target, "Taking you there");

        AssistantReply reply = assistant.respond("s1", "hi");

        assertThat(reply.content()).isEqualTo("Taking you there");
        assertThat(reply.uiAction().type()).isEqualTo("NAVIGATE");
        assertThat(reply.uiAction().path()).isEqualTo(path);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "ABOUT_US,/about-us", "PRIVACY,/privacy",
            "TERMS,/terms", "RETURN_POLICY,/return-policy"})
    @DisplayName("a static-page intent resolves from the same table")
    void staticPages(NavigationTarget target, String path) {
        classifierReturns(Intent.STATIC_PAGE, target, "Here you go");

        assertThat(assistant.respond("s1", "hi").uiAction().path()).isEqualTo(path);
    }

    /**
     * The two intents used to have a lookup each, with a {@code default -> "/home"} apiece, so
     * whether a target resolved depended on which intent it arrived with — {@code SHOP} under
     * {@code STATIC_PAGE} went to {@code /home}, and {@code TERMS} under {@code NAVIGATION} did
     * too. One table cannot disagree with itself.
     */
    @ParameterizedTest
    @EnumSource(value = NavigationTarget.class, names = "UNKNOWN", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("every target resolves the same way under either navigating intent")
    void bothIntentsAgree(NavigationTarget target) {
        classifierReturns(Intent.NAVIGATION, target, "x");
        String viaNavigation = assistant.respond("s1", "hi").uiAction().path();

        classifierReturns(Intent.STATIC_PAGE, target, "x");
        String viaStaticPage = assistant.respond("s1", "hi").uiAction().path();

        assertThat(viaNavigation).isEqualTo(viaStaticPage);
        if (target != NavigationTarget.HOME) {
            // Not merely equal because both fell through to the shared fallback — which is what
            // half of them used to do. HOME is excluded because /home is its real answer.
            assertThat(viaNavigation).isNotEqualTo("/home");
        }
    }

    @Test
    @DisplayName("an unknown target falls back to the home page rather than a null path")
    void unknownTargetFallsBack() {
        classifierReturns(Intent.NAVIGATION, NavigationTarget.UNKNOWN, "x");

        assertThat(assistant.respond("s1", "hi").uiAction().path()).isEqualTo("/home");
    }

    @Test
    @DisplayName("a customer-service intent is handed to the product handler with the session")
    void delegatesCustomerService() {
        classifierReturns(Intent.CUSTOMER_SERVICE, NavigationTarget.UNKNOWN, "x");
        when(customerService.handle(eq("s1"), any())).thenReturn(AssistantReply.text("about the GPU"));

        assertThat(assistant.respond("s1", "hi").content()).isEqualTo("about the GPU");
        verify(customerService).handle(eq("s1"), any(AiDecision.class));
    }

    @Test
    @DisplayName("an unrelated question is refused politely and never reaches the catalog")
    void unrelated() {
        classifierReturns(Intent.UNRELATED, NavigationTarget.UNKNOWN, "ignored");

        AssistantReply reply = assistant.respond("s1", "hi");

        assertThat(reply.content()).isEqualTo("Sorry, I currently can't help you with that.");
        assertThat(reply.uiAction()).isNull();
        verifyNoInteractions(customerService);
    }

    /**
     * The message travels with the decision now. It used to be attached by {@code ChatController}
     * <em>after</em> the classifier returned, which is why nothing else could call the pipeline.
     */
    @Test
    @DisplayName("the handler receives the message the customer actually sent")
    void carriesTheOriginalMessage() {
        when(classifier.classify("show me a 4090")).thenReturn(new AiDecision(
                Intent.CUSTOMER_SERVICE, NavigationTarget.UNKNOWN, "", "show me a 4090", "4090"));
        when(customerService.handle(any(), any())).thenReturn(AssistantReply.text("ok"));

        assistant.respond("s1", "show me a 4090");

        var decision = org.mockito.ArgumentCaptor.forClass(AiDecision.class);
        verify(customerService).handle(eq("s1"), decision.capture());
        assertThat(decision.getValue().originalUserMessage()).isEqualTo("show me a 4090");
        assertThat(decision.getValue().effectiveSearchTerm()).isEqualTo("4090");
    }

    @Test
    @DisplayName("with no extracted query, the whole message is the search term")
    void fallsBackToTheWholeMessage() {
        AiDecision decision = new AiDecision(
                Intent.CUSTOMER_SERVICE, NavigationTarget.UNKNOWN, "", "show me a 4090", "  ");

        assertThat(decision.effectiveSearchTerm()).isEqualTo("show me a 4090");
    }
}
