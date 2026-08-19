package com.example.assistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class EmotionalExpression extends AppCompatActivity {

    private Button ee_back, ee_main;
    private View cardFeelings, cardManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotional_expression);

        ee_back = findViewById(R.id.emo_back);
        ee_main = findViewById(R.id.emo_main);

        cardFeelings = findViewById(R.id.card_feelings);
        cardManage   = findViewById(R.id.card_manage);

        // 上頁/首頁
        ee_back.setOnClickListener(v -> finish());
        ee_main.setOnClickListener(v ->
                startActivity(new Intent(EmotionalExpression.this, WelcomeActivity.class))
        );

        // 直接點卡片跳聊天
        cardFeelings.setOnClickListener(v -> goChat("情緒表達"));
        cardManage.setOnClickListener(v -> goChat("情緒管理"));
    }

    private void goChat(String topic) {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setAst_num("2");            // 情緒表達分類固定 2（沿用你的分類）
        g.setTopic_nam(topic);        // 寫入主題
        startActivity(new Intent(this, MainActivity2.class));
    }
}
