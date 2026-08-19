package com.example.assistant.data.model;

public class User {
    public int id;          // 等同原本 uid

    public int getId() {
        return id;
    }
    public String email;
    public String displayName;
    public Integer age;   // 可為 null
    public String grade;
    public String token;       // 後端登入成功後的 JWT/Session
}
