package com.example.assistant;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.assistant.data.model.User;
import com.example.assistant.data.rest.RestClient;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextAge, editTextGrade, editTextName;
    private TextView profileEmail, profileFaceID;
    private Button buttonSave, buttonBack;

    private int userId = -1;
    private String email;
    private String mName;
    private String mFaceID;

    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // UI
        editTextAge   = findViewById(R.id.editTextAge);
        editTextGrade = findViewById(R.id.editTextGrade);
        editTextName  = findViewById(R.id.editTextName);
        profileEmail  = findViewById(R.id.profileEmail);
        profileFaceID = findViewById(R.id.profileFaceID);
        buttonSave    = findViewById(R.id.buttonSave);
        buttonBack    = findViewById(R.id.buttonBack);

        // 取註冊/登入流程帶來的資料（優先 Intent，其次 GlobalVariable）
        userId  = getIntent().getIntExtra("user_id", -1);
        email   = getIntent().getStringExtra("email");
        mName   = getIntent().getStringExtra("mName");
        mFaceID = getIntent().getStringExtra("mFaceID");

        if (userId <= 0) {
            User u = ((GlobalVariable) getApplication()).getCurrentUser();
            if (u != null && u.id > 0) {
                userId = u.id;
                if (email == null) email = u.email;
                if (mName == null)  mName  = u.displayName;
            }
        }

        // 先畫上已知的（避免空白閃爍）
        profileEmail.setText("Email: " + nvl(email, "未知"));
        editTextName.setText(nvl(mName, ""));
        profileFaceID.setText("Face ID: " + nvl(mFaceID, "未知"));

        // ★ 一進頁就向後端抓 DB 資料，預填年齡/年級/姓名/face_id
        if (userId > 0) {
            fetchProfile(userId,
                    // onOk
                    prof -> runOnUiThread(() -> {
                        // 後端有就覆蓋到 UI
                        if (prof.displayName != null && !prof.displayName.isEmpty()) {
                            editTextName.setText(prof.displayName);
                        }
                        if (prof.age != null)   editTextAge.setText(String.valueOf(prof.age));
                        if (prof.grade != null) editTextGrade.setText(String.valueOf(prof.grade));
                        if (prof.faceId != null && !prof.faceId.isEmpty()) {
                            profileFaceID.setText("Face ID: " + prof.faceId);
                            mFaceID = prof.faceId; // 讓後續更新時能送出
                        } else {
                            profileFaceID.setText("Face ID: 未知");
                        }
                        if (prof.email != null && !prof.email.isEmpty()) {
                            profileEmail.setText("Email: " + prof.email);
                            email = prof.email;
                        }
                    }),
                    // onErr
                    err -> runOnUiThread(() ->
                            Toast.makeText(this, "讀取個資失敗: " + err, Toast.LENGTH_LONG).show())
            );
        }

        // 儲存 → update_profile.php
        buttonSave.setOnClickListener(v -> {
            if (userId <= 0) {
                Toast.makeText(this, "缺少 user_id，無法更新", Toast.LENGTH_SHORT).show();
                return;
            }

            String ageStr   = editTextAge.getText().toString().trim();
            String gradeStr = editTextGrade.getText().toString().trim();
            String nameStr  = editTextName.getText().toString().trim();

            if (ageStr.isEmpty() || gradeStr.isEmpty()) {
                Toast.makeText(this, "請填完整年齡與年級", Toast.LENGTH_SHORT).show();
                return;
            }

            int age, grade;
            try {
                age   = Integer.parseInt(ageStr);
                grade = Integer.parseInt(gradeStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "年齡與年級需為數字", Toast.LENGTH_SHORT).show();
                return;
            }

            // 只有 faceId 有值且不是「未知」才送
            String faceIdToSend = null;
            if (mFaceID != null && !mFaceID.isEmpty() && !"未知".equals(mFaceID)) {
                faceIdToSend = mFaceID;
            }

            buttonSave.setEnabled(false);
            postUpdateProfile(
                    userId, age, grade,
                    nameStr.isEmpty() ? null : nameStr,
                    faceIdToSend,
                    () -> runOnUiThread(() -> {
                        buttonSave.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "已更新", Toast.LENGTH_SHORT).show();
                    }),
                    err -> runOnUiThread(() -> {
                        buttonSave.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "更新失敗: " + err, Toast.LENGTH_LONG).show();
                    })
            );
        });

        buttonBack.setOnClickListener(v -> finish());
    }

    // ---------- 抓取個資（預填 UI） ----------
    private void fetchProfile(int userId, Consumer<ProfileDTO> onOk, Consumer<String> onErr) {
        String url = RestClient.url("auth/get_profile.php");

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);

        String json = gson.toJson(payload);
        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json"), json))
                .build();

        RestClient.http().newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                onErr.accept(e.getMessage());
            }
            @Override public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    String body = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        onErr.accept("HTTP " + res.code() + (body.isEmpty() ? "" : (": " + body)));
                        return;
                    }
                    // 兼容兩種回傳：
                    // A) {success:true, data:{email,display_name,face_id,age,grade}}
                    // B) {email,display_name,face_id,age,grade}
                    ProfileDTO dto = new ProfileDTO();
                    try {
                        JsonObject root = gson.fromJson(body, JsonObject.class);
                        JsonObject data = root.has("data") && root.get("data").isJsonObject()
                                ? root.getAsJsonObject("data") : root;

                        dto.email        = getAsString(data, "email");
                        dto.displayName  = getAsString(data, "display_name");
                        dto.faceId       = getAsString(data, "face_id");
                        dto.age          = getAsInteger(data, "age");
                        dto.grade        = getAsInteger(data, "grade");
                    } catch (Exception ex) {
                        onErr.accept("parse error: " + ex.getMessage() + " / body=" + body);
                        return;
                    }
                    onOk.accept(dto);
                }
            }
        });
    }

    // ---------- 更新個資 ----------
    private void postUpdateProfile(int userId, int age, int grade,
                                   @Nullable String displayName, @Nullable String faceId,
                                   Runnable onOk, Consumer<String> onErr) {
        String url = RestClient.url("auth/update_profile.php");

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("age", age);
        payload.put("grade", grade);
        if (displayName != null) payload.put("display_name", displayName);
        if (faceId != null)      payload.put("face_id", faceId);

        String json = gson.toJson(payload);

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json"), json))
                .build();

        RestClient.http().newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                onErr.accept(e.getMessage());
            }
            @Override public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    String body = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        onErr.accept("HTTP " + res.code() + (body.isEmpty() ? "" : (": " + body)));
                        return;
                    }
                    onOk.run();
                }
            }
        });
    }

    // ---------- JSON helpers ----------
    private static String getAsString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return null;
        JsonElement e = obj.get(key);
        return e.isJsonNull() ? null : e.getAsString();
    }
    private static Integer getAsInteger(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return null;
        JsonElement e = obj.get(key);
        if (e.isJsonNull()) return null;
        try { return e.getAsInt(); } catch (Exception ex) { return null; }
    }

    private static String nvl(String s, String alt) {
        return (s == null || s.isEmpty()) ? alt : s;
    }

    // ---------- DTO ----------
    private static class ProfileDTO {
        String email;
        String displayName;
        String faceId;
        Integer age;
        Integer grade;
    }
}
