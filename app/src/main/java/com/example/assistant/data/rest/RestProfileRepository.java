package com.example.assistant.data.rest;

import com.example.assistant.data.ProfileRepository;
import com.example.assistant.data.AuthRepository;
import com.example.assistant.data.model.User;
import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestProfileRepository implements ProfileRepository {
    private final AuthRepository auth;
    private final Gson gson = new Gson();

    public RestProfileRepository(AuthRepository auth) { this.auth = auth; }

    @Override
    public void getProfile(String userId, ProfileCb cb) {
        Request req = new Request.Builder()
                .url(RestClient.base() + "/users/" + userId)
                .addHeader("Authorization", "Bearer " + auth.getToken())
                .get()
                .build();

        RestClient.http().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { cb.onError(e); }
            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    if (!res.isSuccessful()) { cb.onError(new RuntimeException("HTTP " + res.code())); return; }
                    cb.onSuccess(gson.fromJson(res.body().string(), User.class));
                }
            }
        });
    }

    @Override
    public void updateProfile(User u, ProfileCb cb) {
        String json = gson.toJson(u);
        Request req = new Request.Builder()
                .url(RestClient.base() + "/users/" + u.id)
                .addHeader("Authorization", "Bearer " + auth.getToken())
                .put(RequestBody.create(MediaType.parse("application/json"), json))
                .build();

        RestClient.http().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { cb.onError(e); }
            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    if (!res.isSuccessful()) { cb.onError(new RuntimeException("HTTP " + res.code())); return; }
                    cb.onSuccess(gson.fromJson(res.body().string(), User.class));
                }
            }
        });
    }
}
