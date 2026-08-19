package com.example.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class startNuwaFaceRecognitionActivity extends AppCompatActivity {
    private static final String TAG = "startNuwaFaceRecActivity";

    private ImageButton closeBtn;
    private Button startRecognitionBtn;
    private EditText inputNameEdit;

    // 從註冊成功頁帶進來（在 onCreate 裡取值）
    private int userIdFromSignup = -1;
    private String emailFromSignup;

    private static final int ACTIVITY_FACE_RECOGNITION = 1;
    private static final int ACTIVITY_FACE_RECOGNITION_ERROR = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_nuwa_face_recognition);

        inputNameEdit = findViewById(R.id.input_name);
        closeBtn = findViewById(R.id.imgbtn_quit);
        startRecognitionBtn = findViewById(R.id.btn_start);

        // 取得註冊步驟傳來的憑證（一定要在 onCreate() 內）
        Intent from = getIntent();
        if (from != null) {
            userIdFromSignup = from.getIntExtra("user_id", -1);
            emailFromSignup  = from.getStringExtra("email");
        }

        closeBtn.setOnClickListener(view -> finish());

        startRecognitionBtn.setOnClickListener(v -> {
            String name = inputNameEdit.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                launchFaceRecogWithName(name);
            } else {
                launchFaceRecog();
            }
        });
    }

    private void launchFaceRecog() {
        Intent intent = new Intent("com.nuwarobotics.action.FACE_REC");
        intent.setPackage("com.nuwarobotics.app.facerecognition2");
        intent.putExtra("EXTRA_3RD_REC_ONCE", true);
        startActivityForResult(intent, ACTIVITY_FACE_RECOGNITION);
    }

    private void launchFaceRecogWithName(String name) {
        Intent intent = new Intent("com.nuwarobotics.action.FACE_REC");
        intent.setPackage("com.nuwarobotics.app.facerecognition2");
        intent.putExtra("EXTRA_3RD_REC_ONCE", true);
        intent.putExtra("EXTRA_3RD_CONFIG_NAME", name);
        startActivityForResult(intent, ACTIVITY_FACE_RECOGNITION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode > 0 && data != null) {
            switch (resultCode) {
                case ACTIVITY_FACE_RECOGNITION: {
                    long mFaceID = data.getLongExtra("EXTRA_RESULT_FACEID", 0L);
                    String mName = data.getStringExtra("EXTRA_RESULT_NAME");

                    // 若 Nuwa 沒回名字，就用輸入框的
                    if (TextUtils.isEmpty(mName)) {
                        mName = inputNameEdit.getText().toString().trim();
                    }

                    // 進入 UserProfileActivity，帶齊註冊憑證 + 掃臉資訊
                    Intent i = new Intent(this, UserProfileActivity.class);
                    i.putExtra("user_id", userIdFromSignup);
                    i.putExtra("email", emailFromSignup);
                    i.putExtra("mName", mName);
                    i.putExtra("mFaceID", String.valueOf(mFaceID));    // Face ID 轉字串傳遞
                    startActivity(i);
                    finish();
                    break;
                }
                case ACTIVITY_FACE_RECOGNITION_ERROR: {
                    String msg = data.getStringExtra("EXTRA_MSG");
                    Toast.makeText(this, msg == null ? "Face recognition error" : msg, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        } else {
            Log.w(TAG, "Face recognition canceled or data is null");
            Toast.makeText(this, "Face recognition canceled.", Toast.LENGTH_SHORT).show();
        }
    }
}
