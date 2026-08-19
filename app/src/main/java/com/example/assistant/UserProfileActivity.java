package com.example.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.assistant.data.model.User;
import com.example.assistant.data.rest.RestClient;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserProfileActivity extends AppCompatActivity {

    private EditText editTextAge, editTextGrade, editTextName;
    private TextView profileEmail, profileFaceID;
    private Button buttonSave, buttonBack;

    private int userId = -1;
    private String email;
    private String mName;
    private String mFaceID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editTextAge = findViewById(R.id.editTextAge);
        editTextGrade = findViewById(R.id.editTextGrade);
        editTextName = findViewById(R.id.editTextName);
        profileEmail = findViewById(R.id.profileEmail);
        profileFaceID = findViewById(R.id.profileFaceID);
        buttonSave = findViewById(R.id.buttonSave);
        buttonBack = findViewById(R.id.buttonBack);

        Intent from = getIntent();
        if (from != null) {
            userId = from.getIntExtra("user_id", -1);
            email = from.getStringExtra("email");
            mName = from.getStringExtra("mName");
            mFaceID = from.getStringExtra("mFaceID");
        }
        User currentUser = ((GlobalVariable) getApplication()).getCurrentUser();
        if (userId <= 0 && currentUser != null && currentUser.id > 0) {
            userId = currentUser.id;
            if (email == null) email = currentUser.email;
            if (mName == null) mName = currentUser.displayName;
        }

        profileEmail.setText("Email: " + valueOrUnknown(email));
        editTextName.setText(mName == null ? "" : mName);
        profileFaceID.setText("Face ID: " + valueOrUnknown(mFaceID));

        buttonSave.setOnClickListener(v -> saveProfile());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void saveProfile() {
        if (userId <= 0 || RestClient.getAuthToken() == null) {
            Toast.makeText(this, "登入資訊已失效，請重新登入", Toast.LENGTH_LONG).show();
            return;
        }

        String ageValue = editTextAge.getText().toString().trim();
        String gradeValue = editTextGrade.getText().toString().trim();
        String displayName = editTextName.getText().toString().trim();
        if (ageValue.isEmpty() || gradeValue.isEmpty()) {
            Toast.makeText(this, "請填寫年齡和年級", Toast.LENGTH_SHORT).show();
            return;
        }

        final int age;
        final int grade;
        try {
            age = Integer.parseInt(ageValue);
            grade = Integer.parseInt(gradeValue);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "年齡和年級必須是數字", Toast.LENGTH_SHORT).show();
            return;
        }

        String faceId = (mFaceID == null || mFaceID.isEmpty() || "未知".equals(mFaceID)) ? null : mFaceID;
        buttonSave.setEnabled(false);
        postUpdateProfile(userId, age, grade, displayName.isEmpty() ? null : displayName, faceId,
                () -> runOnUiThread(() -> {
                    User currentUser = ((GlobalVariable) getApplication()).getCurrentUser();
                    if (currentUser != null) {
                        currentUser.age = age;
                        currentUser.grade = String.valueOf(grade);
                        if (!displayName.isEmpty()) currentUser.displayName = displayName;
                    }
                    Toast.makeText(this, "註冊完成", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, WelcomeActivity.class));
                    finish();
                }),
                error -> runOnUiThread(() -> {
                    buttonSave.setEnabled(true);
                    Toast.makeText(this, "儲存失敗: " + error, Toast.LENGTH_LONG).show();
                }));
    }

    private void postUpdateProfile(int userId, int age, int grade, @Nullable String displayName,
                                   @Nullable String faceId, Runnable onSuccess, Consumer<String> onError) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("age", age);
        payload.put("grade", grade);
        if (displayName != null) payload.put("display_name", displayName);
        if (faceId != null) payload.put("face_id", faceId);

        Request request = RestClient.authenticatedRequestBuilder()
                .url(RestClient.url("auth/update_profile.php"))
                .post(RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(payload)))
                .build();
        RestClient.http().newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                onError.accept(e.getMessage());
            }

            @Override public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    if (!res.isSuccessful()) {
                        onError.accept("HTTP " + res.code());
                        return;
                    }
                    onSuccess.run();
                }
            }
        });
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isEmpty() ? "未知" : value;
    }
}
