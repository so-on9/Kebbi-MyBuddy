package com.example.assistant.data.model;

public class ChatMessage {
    public String id;
    public String chatId;
    public String userId;
    public String role;      // "user" 或 "assistant"
    public String text;
    public String dateTime;  // 建議 ISO8601，例如 2025-09-02T14:05:00+08:00
}
