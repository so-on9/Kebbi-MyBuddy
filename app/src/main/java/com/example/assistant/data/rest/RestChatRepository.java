package com.example.assistant.data.rest;

import com.example.assistant.data.ChatRepository;
import com.example.assistant.data.AuthRepository;
import com.example.assistant.data.model.ChatMessage;
import com.example.assistant.data.model.ChatSummary;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestChatRepository implements ChatRepository {
    private final AuthRepository auth;
    private final Gson gson = new Gson();

    public RestChatRepository(AuthRepository auth) { this.auth = auth; }

    @Override
    public void saveMessage(ChatMessage msg, VoidCb cb) {
        String json = gson.toJson(msg);
        Request req = new Request.Builder()
                .url(RestClient.base() + "/users/" + msg.userId + "/chats/" + msg.chatId + "/messages")
                .addHeader("Authorization", "Bearer " + auth.getToken())
                .post(RequestBody.create(MediaType.parse("application/json"), json))
                .build();

        RestClient.http().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { cb.onError(e); }
            @Override public void onResponse(Call call, Response response) {
                try (Response res = response) {
                    if (res.isSuccessful()) { cb.onSuccess(); }
                    else { cb.onError(new RuntimeException("HTTP " + res.code())); }
                }
            }
        });
    }

    @Override
    public void listMessages(String userId, String chatId, int limit, MsgListCb cb) {
        Request req = new Request.Builder()
                .url(RestClient.base() + "/users/" + userId + "/chats/" + chatId + "/messages?limit=" + limit)
                .addHeader("Authorization", "Bearer " + auth.getToken())
                .get()
                .build();

        RestClient.http().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { cb.onError(e); }
            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    if (!res.isSuccessful()) { cb.onError(new RuntimeException("HTTP " + res.code())); return; }
                    java.lang.reflect.Type t = new TypeToken<List<ChatMessage>>(){}.getType();
                    cb.onSuccess(gson.fromJson(res.body().string(), t));
                }
            }
        });
    }

    @Override
    public void listChats(String userId, int limit, ChatListCb cb) {
        Request req = new Request.Builder()
                .url(RestClient.base() + "/users/" + userId + "/chats?limit=" + limit)
                .addHeader("Authorization", "Bearer " + auth.getToken())
                .get()
                .build();

        RestClient.http().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { cb.onError(e); }
            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    if (!res.isSuccessful()) { cb.onError(new RuntimeException("HTTP " + res.code())); return; }
                    java.lang.reflect.Type t = new TypeToken<List<ChatSummary>>(){}.getType();
                    cb.onSuccess(gson.fromJson(res.body().string(), t));
                }
            }
        });
    }
}
