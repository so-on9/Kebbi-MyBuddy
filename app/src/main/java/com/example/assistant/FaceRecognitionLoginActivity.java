package com.example.assistant;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.assistant.data.model.User;
import com.example.assistant.data.rest.RestClient;
import com.google.gson.Gson;
import com.nuwarobotics.service.camera.common.Constants;
import com.nuwarobotics.service.camera.common.CsDebug;
import com.nuwarobotics.service.camera.sdk.CameraSDK;
import com.nuwarobotics.service.camera.sdk.OutputData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FaceRecognitionLoginActivity extends AppCompatActivity {

    private static final String TAG = "FaceRecognitionLogin";

    private ProgressBar progressBar;
    private ImageView ivFaceRecognition;
    private Button btnCancelFaceRecognition;
    private TextView tvFaceRecognitionPrompt;

    private CameraSDK cameraSDK;
    private final Gson gson = new Gson();

    private boolean isLoggingIn = false; // 避免重複觸發登入

    // 和官方 demo 一樣的資料結構
    private static class FRData {
        public int idx;
        public String conf;
        public String mask;
        public String name;
        public Rect rect;
        public String age;
        public String gender;
        public long faceid;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition_login);

        progressBar = findViewById(R.id.progressBar);
        ivFaceRecognition = findViewById(R.id.ivFaceRecognition);
        btnCancelFaceRecognition = findViewById(R.id.btnCancelFaceRecognition);
        tvFaceRecognitionPrompt = findViewById(R.id.tvFaceRecognitionPrompt);

        tvFaceRecognitionPrompt.setText("請讓小助手看清楚你的臉，\n辨識成功後會自動登入。");
        progressBar.setIndeterminate(true);

        // 啟動 Nuwa Camera service
        Intent intent = new Intent();
        intent.setClassName(Constants.SERVICE_PACKAGENAME, Constants.SERVICE_CLASSNAME);
        startService(intent);

        cameraSDK = new CameraSDK(this);

        btnCancelFaceRecognition.setOnClickListener(v -> {
            Toast.makeText(this, "已取消臉部登入", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 註冊回呼：只需要 FACE_RECOGNITION
        cameraSDK.register(
                mCameraSDKCallback,
                Constants.FACE_RECOGNITION,
                getPackageName()
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraSDK.unregister(getPackageName());

        // ✅ 避免回到畫面後卡住不再登入
        isLoggingIn = false;
    }

    private final CameraSDK.CameraSDKCallback mCameraSDKCallback = new CameraSDK.CameraSDKCallback() {
        @Override
        public void onConnected(boolean isConnected) {
            CsDebug.logD(TAG, "onConnected: " + isConnected);
        }

        @Override
        public void onOutput(Map<Integer, OutputData> resultMap) {
            OutputData outputData = resultMap.get(Constants.FACE_RECOGNITION);
            if (outputData == null || outputData.data == null || "null".equals(outputData.data)) {
                return;
            }

            try {
                FRData face = gson.fromJson(outputData.data, FRData.class);
                if (face == null) return;

                // 陌生人可能 name 為 "@#$" 或 "null"
                if (face.faceid <= 0 || face.name == null ||
                        "@#$".equals(face.name) || "null".equals(face.name)) {
                    return;
                }

                if (isLoggingIn) return;
                isLoggingIn = true;

                long faceId = face.faceid;
                runOnUiThread(() ->
                        tvFaceRecognitionPrompt.setText("已辨識到：「" + face.name + "」，登入中…")
                );

                authenticateWithMySQL(faceId);

            } catch (Exception e) {
                Log.e(TAG, "parse FRData error", e);
            }
        }

        @Override
        public void onPictureTaken(String path) {
            // 這裡用不到
        }
    };

    /** 呼叫 face_login.php，用 face_id 找出使用者並登入 */
    private void authenticateWithMySQL(long faceId) {
        String url = RestClient.base() + "/auth/face_login.php";

        Map<String, Object> payload = new HashMap<>();
        payload.put("face_id", String.valueOf(faceId)); // 傳字串就好

        String json = new Gson().toJson(payload);

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json"), json))
                .build();

        RestClient.http().newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                isLoggingIn = false; // ✅ 失敗可重試
                runOnUiThread(() -> {
                    Toast.makeText(FaceRecognitionLoginActivity.this,
                            "連線失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    isLoggingIn = false; // ✅ 失敗可重試
                    runOnUiThread(() -> {
                        Toast.makeText(FaceRecognitionLoginActivity.this,
                                "登入失敗: HTTP " + response.code(), Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                try {
                    JSONObject obj = new JSONObject(body);
                    if (obj.optBoolean("success")) {
                        JSONObject u = obj.getJSONObject("user");

                        User user = new User();
                        user.id = u.getInt("id");
                        user.email = u.getString("email");
                        user.displayName = u.optString("display_name", "");
                        user.token = u.optString("token", "");
                        if (user.token.isEmpty()) {
                            throw new JSONException("Missing session token");
                        }

                        RestClient.setAuthToken(user.token);

                        ((GlobalVariable) getApplication()).setCurrentUser(user);

                        runOnUiThread(() -> {
                            Toast.makeText(FaceRecognitionLoginActivity.this,
                                    "Face Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(FaceRecognitionLoginActivity.this,
                                    WelcomeActivity.class));
                            finish();
                        });
                    } else {
                        isLoggingIn = false; // ✅ 失敗可重試
                        String errorMsg = obj.optString("error", "Face ID not recognized.");
                        runOnUiThread(() -> {
                            Toast.makeText(FaceRecognitionLoginActivity.this,
                                    errorMsg, Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                } catch (JSONException e) {
                    isLoggingIn = false; // ✅ 失敗可重試
                    final String err = e.getMessage();
                    Log.e(TAG, "JSON parse error: " + err);
                    runOnUiThread(() -> {
                        Toast.makeText(FaceRecognitionLoginActivity.this,
                                "回傳格式錯誤: " + err, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            }
        });
    }
}
