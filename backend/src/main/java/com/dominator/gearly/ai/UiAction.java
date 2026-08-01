package com.dominator.gearly.ai;

public class UiAction {

    private String type;
    private String path;

    public UiAction() {}

    public UiAction(String type, String path) {
        this.type = type;
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }
}
