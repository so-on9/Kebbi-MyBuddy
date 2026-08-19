package com.example.assistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.assistant.data.AuthRepository;
import com.example.assistant.data.model.User;
import com.example.assistant.data.rest.RestAuthRepository;
import com.example.assistant.data.rest.RestClient;

public class SignUpActivity extends AppCompatActivity {

    private AuthRepository auth;
    private EditText signupEmail, signupPassword;
    private Button signupButton;
    private TextView loginRedirectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // 若需要改成你的後端網址（真機測試用區網 IP），可打開這行：
        auth = new RestAuthRepository();

        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        signupButton.setOnClickListener(view -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Email and Password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            signupButton.setEnabled(false);

            // 從 email 推一個簡單的 displayName（後端可儲存顯示用名稱）
            String displayName = displayNameFromEmail(email);

            auth.signUp(email, password, displayName, new AuthRepository.UserCallback() {
                @Override
                public void onSuccess(User u) {
                    runOnUiThread(() -> {
                        Toast.makeText(SignUpActivity.this, "註冊成功，接下來請進行人臉辨識", Toast.LENGTH_SHORT).show();

                        // 把新註冊的使用者放進 GlobalVariable（後面頁面若直接取 Global 就有 user_id）
                        ((GlobalVariable) getApplication()).setCurrentUser(u);
                        ((GlobalVariable) getApplication()).setUser_name(u.displayName); // 若你有用到

                        // 把 user_id/email/name 一併往後傳，避免中途 Global 被清掉
                        Intent intent = new Intent(SignUpActivity.this, startNuwaFaceRecognitionActivity.class);
                        intent.putExtra("user_id", u.id);          // 後端回來的 user id
                        intent.putExtra("email", email);           // 剛註冊用的 email
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> {
                        signupButton.setEnabled(true);
                        Toast.makeText(SignUpActivity.this, "Signup Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        loginRedirectText.setOnClickListener(view ->
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class)));
    }

    private static String displayNameFromEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
