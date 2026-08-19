package com.example.assistant;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.assistant.data.AuthRepository;
import com.example.assistant.data.model.User;
import com.example.assistant.data.rest.RestAuthRepository;

public class LoginActivity extends AppCompatActivity {

    private AuthRepository auth;
    private EditText loginEmail, loginPassword;
    private TextView signupRedirectText;
    private Button loginButton, faceLoginButton;

    // 防止連點
    private boolean isLoggingIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = new RestAuthRepository();

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        faceLoginButton = findViewById(R.id.face_login_button);
        signupRedirectText = findViewById(R.id.signUpRedirectText);

        // 登入按鈕
        loginButton.setOnClickListener(view -> attemptLogin(
                loginEmail.getText().toString().trim(),
                loginPassword.getText().toString()
        ));

        // Face Login
        faceLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FaceRecognitionLoginActivity.class);
            startActivity(intent);
        });

        // 前往註冊頁
        signupRedirectText.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
    }

    private void attemptLogin(String email, String pass) {
        if (email.isEmpty() && pass.isEmpty()) {
            showErrorDialog("信箱與密碼不能為空");
            return;
        }
        if (email.isEmpty()) {
            showErrorDialog("信箱不能為空");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorDialog("信箱格式不正確\n請輸入有效的 Email");
            return;
        }
        if (pass.isEmpty()) {
            showErrorDialog("密碼不能為空");
            return;
        }

        if (isLoggingIn) return;
        isLoggingIn = true;
        loginButton.setEnabled(false);

        auth.signIn(email, pass, new AuthRepository.UserCallback() {
            @Override
            public void onSuccess(User u) {
                runOnUiThread(() -> {
                    isLoggingIn = false;
                    loginButton.setEnabled(true);

                    ((GlobalVariable) getApplication()).setCurrentUser(u);

                    startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    isLoggingIn = false;
                    loginButton.setEnabled(true);

                    String msg = mapLoginErrorToChinese(t);
                    showErrorDialog(msg);
                });
            }
        });
    }

    /**
     * 把後端/例外訊息轉成較友善的中文提示
     * 你後端如果回傳 error 字串（例如 "user_not_found" / "wrong_password"），這裡會更準。
     */
    private String mapLoginErrorToChinese(Throwable t) {
        if (t == null || t.getMessage() == null) {
            return "請稍後再試";
        }
        String raw = t.getMessage().toLowerCase();

        // 你可以依你 PHP 回傳的字串調整關鍵字
        if (raw.contains("user_not_found") || raw.contains("email not found") || raw.contains("no such user")) {
            return "信箱錯誤或尚未註冊";
        }
        if (raw.contains("wrong_password") || raw.contains("invalid password") || raw.contains("password incorrect")) {
            return "密碼錯誤\n請再試一次。";
        }
        if (raw.contains("empty email")) {
            return "信箱不能為空";
        }
        if (raw.contains("empty password")) {
            return "密碼不能為空";
        }
        if (raw.contains("timeout") || raw.contains("failed to connect") || raw.contains("unable to resolve host")) {
            return "連線失敗\n請確認網路或伺服器是否正常";
        }

        return "請確認信箱與密碼是否正確";
    }

    /**
     * 自訂錯誤視窗：不用 Toast、用對話框 + 彈出動畫
     * 需要你新增 dialog_error.xml + style（下面我給你）
     */
    private void showErrorDialog(String message) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_error);
        dialog.setCancelable(true);

        TextView tvMsg = dialog.findViewById(R.id.tvErrorMessage);
        Button btnOk = dialog.findViewById(R.id.btnErrorOk);

        tvMsg.setText(message);
        btnOk.setOnClickListener(v -> dialog.dismiss());

        // 套用彈出動畫（需 styles.xml 設定，下面有）
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogPopAnimation;
        }

        dialog.show();
    }
}
