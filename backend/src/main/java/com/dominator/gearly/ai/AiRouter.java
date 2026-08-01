package com.dominator.gearly.ai;

import org.springframework.stereotype.Service;

@Service
public class AiRouter {

    private final CustomerServiceHandler customerServiceHandler;

    public AiRouter(CustomerServiceHandler customerServiceHandler) {
        this.customerServiceHandler = customerServiceHandler;
    }

    public BackendResponse route(AiDecision decision, String sessionId) {

        return switch (decision.getIntent()) {

            case NAVIGATION ->
                BackendResponse.withNavigation(
                    decision.getContent(),
                    mapNavigation(decision.getTarget())
                );

            case STATIC_PAGE ->
                BackendResponse.withNavigation(
                    decision.getContent(),
                    mapStatic(decision.getTarget())
                );

            case CUSTOMER_SERVICE ->
                customerServiceHandler.handle(sessionId, decision);

            case UNRELATED ->
                BackendResponse.text(
                    "Sorry, I currently can't help you with that."
                );
        };
    }

    private String mapNavigation(NavigationTarget t) {
        return switch (t) {
            case HOME -> "/home";
            case SHOP -> "/shop";
            case LOGIN -> "/login";
            case REGISTER -> "/register";
            case PROFILE -> "/me";
            case ORDERS -> "/me/orders";
            case BLOG -> "/blog";
            default -> "/home";
        };
    }

    private String mapStatic(NavigationTarget t) {
        return switch (t) {
            case ABOUT_US -> "/about-us";
            case PRIVACY -> "/privacy";
            case TERMS -> "/terms";
            case RETURN_POLICY -> "/return-policy";
            default -> "/home";
        };
    }
}
