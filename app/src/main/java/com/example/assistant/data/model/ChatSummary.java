package com.example.assistant.data.model;

public class ChatSummary {
    public String id;
    public String title;
    public String lastMessage; // 後端若沒給可留空不用
    public String updatedAt;   // ISO8601 / 或資料庫時間字串
}
