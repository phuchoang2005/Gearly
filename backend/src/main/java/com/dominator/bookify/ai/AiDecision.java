package com.dominator.bookify.ai;

public class AiDecision {

    private String intent;
    private NavigationTarget target;
    private String content;
    private String originalUserMessage;
    private String searchQuery;

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public NavigationTarget getTarget() {
        return target;
    }

    public void setTarget(NavigationTarget target) {
        this.target = target;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOriginalUserMessage() {
        return originalUserMessage;
    }

    public void setOriginalUserMessage(String originalUserMessage) {
        this.originalUserMessage = originalUserMessage;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
}
