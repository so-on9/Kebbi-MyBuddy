package com.example.assistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SchoolActivity extends AppCompatActivity {

    private Button sc_back;
    private Button sc_main;

    private View cardNeeds, cardEmotion, cardInteraction, cardLearning, cardChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school);

        sc_back = findViewById(R.id.school_back);
        sc_main = findViewById(R.id.school_main);

        // 綁定卡片
        cardNeeds       = findViewById(R.id.card_needs);
        cardEmotion     = findViewById(R.id.card_emotion);
        cardInteraction = findViewById(R.id.card_interaction);
        cardLearning    = findViewById(R.id.card_learning);
        cardChat        = findViewById(R.id.card_chat);

        // 方塊：直接跳頁
        cardNeeds.setOnClickListener(v -> gotoDailyNeeds());
        cardEmotion.setOnClickListener(v -> gotoEmotion());
        cardInteraction.setOnClickListener(v -> gotoInteraction());
        cardLearning.setOnClickListener(v -> gotoLearning());
        cardChat.setOnClickListener(v -> gotoChat());

        // 上頁
        sc_back.setOnClickListener(view -> finish());

        // 首頁（可選：回首頁並清掉返回堆疊）
        sc_main.setOnClickListener(view -> {
            Intent intent = new Intent(SchoolActivity.this, WelcomeActivity.class);
            // 如果你希望回首頁後，按返回不會回到這頁，可加入以下旗標：
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            // 可選：finish(); // 回首頁後關閉本頁
        });
    }

    private void gotoDailyNeeds() {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setAst_num("1");
        startActivity(new Intent(this, DailyNeedsActivity.class));
    }

    private void gotoEmotion() {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setAst_num("2");
        startActivity(new Intent(this, EmotionalExpression.class));
    }

    private void gotoInteraction() {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setAst_num("3");
        startActivity(new Intent(this, Interaction.class));
    }

    private void gotoLearning() {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setAst_num("4");
        startActivity(new Intent(this, LearningSupport.class));
    }

    private void gotoChat() {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setAst_num("5");
        g.setTopic_nam("閒聊");
        startActivity(new Intent(this, MainActivity2.class));
    }
}
