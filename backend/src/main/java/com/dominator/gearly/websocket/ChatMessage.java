package com.dominator.gearly.websocket;

public class ChatMessage {
    private String sessionId;
    private String sender;
    private String content;

    public ChatMessage() {}

    public ChatMessage(String sessionId, String sender, String content) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.content = content;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
