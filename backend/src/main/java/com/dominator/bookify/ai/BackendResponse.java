package com.dominator.bookify.ai;

public class BackendResponse {

    private String content;
    private UiAction uiAction;

    public static BackendResponse text(String content) {
        BackendResponse r = new BackendResponse();
        r.content = content;
        return r;
    }

    public static BackendResponse withNavigation(String content, String path) {
        BackendResponse r = new BackendResponse();
        r.content = content;
        r.uiAction = new UiAction("NAVIGATE", path);
        return r;
    }

    public String getContent() {
        return content;
    }

    public UiAction getUiAction() {
        return uiAction;
    }
}
