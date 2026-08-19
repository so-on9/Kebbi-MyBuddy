package com.example.assistant;

/**
 * Userdatabase 類：對應 MySQL 中 chat_messages 資料表
 */
public class Userdatabase {

    // === 對應 chat_messages 資料表欄位 ===
    private int id;               // 訊息主鍵
    private int user_id;          // 使用者 ID
    private String role;          // 角色（user 或 gpt）
    private String message;       // 訊息內容
    private String created_at;    // 建立時間（MySQL 自動生成）

    private String display_name;

    // === 建構子 ===
    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public Userdatabase(int id, int user_id, String role, String message, String created_at) {
        this.id = id;
        this.user_id = user_id;
        this.role = role;
        this.message = message;
        this.created_at = created_at;
    }

    // === Getter ===
    public int getId() {
        return id;
    }

    public int getUser_id() {
        return user_id;
    }

    public String getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }

    public String getCreated_at() {
        return created_at;
    }

    // === 為了相容舊 Adapter ===
    // 若 ChatHistoryAdapter 仍呼叫 getMessageText() 或 getDateTime()，
    // 可以保留以下兩個方法讓它正常運作：
    public String getMessageText() {
        return message;  // 與 MySQL message 欄位對應
    }

    public String getDateTime() {
        return created_at;  // 與 MySQL created_at 欄位對應
    }

    // === Setter（若後續需要修改時使用） ===
    public void setId(int id) {
        this.id = id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}
