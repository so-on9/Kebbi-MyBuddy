package com.example.assistant.data.rest;

import androidx.annotation.Nullable;

import com.example.assistant.data.AuthRepository;
import com.example.assistant.data.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestAuthRepository implements AuthRepository {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Gson gson = new Gson();
    private User cached;

    // -----------------------------
    // Sign In
    // -----------------------------
    @Override
    public void signIn(String email, String password, UserCallback cb) {
        String url = RestClient.base() + "/auth/login.php";

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);

        String body = gson.toJson(payload);
        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, body))
                .build();

        RestClient.http().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.onError(e);
            }

            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    String resp = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        cb.onError(new RuntimeException("HTTP " + res.code()));
                        return;
                    }
                    User u = parseUserFromResponse(resp);
                    if (u == null || u.token == null || u.token.trim().isEmpty() || u.id <= 0) {
                        cb.onError(new RuntimeException("登入失敗：回傳內容不完整"));
                        return;
                    }
                    cached = u;
                    RestClient.setAuthToken(u.token);
                    cb.onSuccess(cached);
                }
            }
        });
    }

    // -----------------------------
    // Sign Up (原簽名) — 只送 email/password/displayName
    // -----------------------------
    @Override
    public void signUp(String email, String password, String displayName, UserCallback cb) {
        signUp(email, password, displayName, null, null, cb);
    }

    // -----------------------------
    // Sign Up (延伸版) — 可一起送 age / grade
    // ※ 若要從介面呼叫，請把 AuthRepository 也加上這個簽名
    // -----------------------------
    public void signUp(String email, String password, String displayName,
                       @Nullable Integer age, @Nullable String grade,
                       UserCallback cb) {
        String url = RestClient.base() + "/auth/signup.php";

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("displayName", displayName);
        if (age != null)   payload.put("age", age);
        if (grade != null && !grade.trim().isEmpty()) payload.put("grade", grade.trim());

        String body = gson.toJson(payload);
        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, body))
                .build();

        RestClient.http().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.onError(e);
            }

            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    String resp = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        cb.onError(new RuntimeException("HTTP " + res.code()));
                        return;
                    }
                    User u = parseUserFromResponse(resp);
                    if (u == null || u.token == null || u.token.trim().isEmpty() || u.id <= 0) {
                        cb.onError(new RuntimeException("註冊失敗：回傳內容不完整"));
                        return;
                    }
                    cached = u;
                    RestClient.setAuthToken(u.token);
                    cb.onSuccess(cached);
                }
            }
        });
    }

    // -----------------------------
    // Face Login
    // -----------------------------
    @Override
    public void signInWithFace(String faceId, UserCallback cb) {
        String url = RestClient.base() + "/auth/face_login.php";

        Map<String, Object> payload = new HashMap<>();
        payload.put("face_id", faceId);

        String body = gson.toJson(payload);
        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, body))
                .build();

        RestClient.http().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.onError(e);
            }

            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    String resp = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        cb.onError(new RuntimeException("HTTP " + res.code()));
                        return;
                    }
                    User u = parseUserFromResponse(resp);
                    if (u == null || u.token == null || u.token.trim().isEmpty() || u.id <= 0) {
                        cb.onError(new RuntimeException("人臉登入失敗：回傳內容不完整"));
                        return;
                    }
                    cached = u;
                    RestClient.setAuthToken(u.token);
                    cb.onSuccess(cached);
                }
            }
        });
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private User parseUserFromResponse(String resp) {
        try {
            // 先嘗試解析 { success: true, user: {...} }
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (root.has("success")) {
                boolean ok = root.get("success").getAsBoolean();
                if (!ok) return null;
                if (root.has("user") && root.get("user").isJsonObject()) {
                    return gson.fromJson(root.get("user"), User.class);
                }
                // 有些後端直接平鋪在根節點（退而求其次）
                return gson.fromJson(root, User.class);
            }
        } catch (Exception ignore) {
            // 不是 JSON 物件或結構不同，改用直接反序列化
        }
        // 直接嘗試把整個字串當 User
        try {
            return gson.fromJson(resp, User.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override public void signOut() {
        cached = null;
        RestClient.revokeSession();
    }
    @Override public boolean isLoggedIn() { return cached != null && cached.token != null; }
    @Override public String getToken() { return cached == null ? null : cached.token; }
    @Override public User getCachedUser() { return cached; }
}
