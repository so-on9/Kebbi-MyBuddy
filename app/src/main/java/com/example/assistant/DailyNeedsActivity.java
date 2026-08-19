package com.example.assistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DailyNeedsActivity extends AppCompatActivity {

    private Button dn_back, dn_main;

    private View cardPhysio, cardMeal, cardCloth, cardBorrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_needs);

        dn_back = findViewById(R.id.daily_back);
        dn_main = findViewById(R.id.daily_main);

        // 綁定四個卡片
        cardPhysio = findViewById(R.id.card_physio);
        cardMeal   = findViewById(R.id.card_meal);
        cardCloth  = findViewById(R.id.card_cloth);
        cardBorrow = findViewById(R.id.card_borrow);

        // 卡片點擊後：設定主題 -> 直接進聊天頁
        cardPhysio.setOnClickListener(v -> goChatWithTopic("生理需求"));
        cardMeal.setOnClickListener(v   -> goChatWithTopic("三餐選擇"));
        cardCloth.setOnClickListener(v  -> goChatWithTopic("衣服選擇"));
        cardBorrow.setOnClickListener(v -> goChatWithTopic("借用物品"));

        // 上頁
        dn_back.setOnClickListener(v -> finish());

        // 首頁
        dn_main.setOnClickListener(v -> {
            startActivity(new Intent(DailyNeedsActivity.this, WelcomeActivity.class));
        });
    }

    private void goChatWithTopic(String topicName) {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setTopic_nam(topicName);
        // 這頁是由 SchoolActivity 分支出來（ast_num 已於 SchoolActivity 設為 "1"）
        // 如需保險可再寫一次：
        g.setAst_num("1");
        startActivity(new Intent(this, MainActivity2.class));
    }
}
