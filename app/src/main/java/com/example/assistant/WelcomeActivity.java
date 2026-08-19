package com.example.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.assistant.data.model.User;
import com.example.assistant.data.rest.RestClient;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Log.d("WelcomeActivity", "onCreate called");

        User user = ((GlobalVariable) getApplication()).getCurrentUser();
        if (user == null) {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("哈囉 " + user.displayName + "!");

        View btnProfile = findViewById(R.id.btnProfile);
        View btnChatHistory = findViewById(R.id.btnChatHistory);
        View btnStartChat = findViewById(R.id.btnStartChat);
        View btnAchievementBoard = findViewById(R.id.btnAchievementBoard);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, ProfileActivity.class)));

        btnChatHistory.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, ChatHistoryActivity.class);
            intent.putExtra("USER_ID", user.id);
            startActivity(intent);
        });

        btnStartChat.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, SlideActivity.class)));

        btnAchievementBoard.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, AchievementBoardActivity.class)));

        btnLogout.setOnClickListener(v -> {
            RestClient.revokeSession();
            ((GlobalVariable) getApplication()).logout();
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            finish();
        });
    }
}
