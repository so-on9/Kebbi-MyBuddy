package com.example.assistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LearningSupport extends AppCompatActivity {

    private Button l_back, l_main;
    private View cardRaiseHand, cardAskHelp, cardConflict, cardMeeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_support);

        // Top buttons
        l_back = findViewById(R.id.l_back);
        l_main = findViewById(R.id.l_main);

        // Cards
        cardRaiseHand = findViewById(R.id.card_raise_hand);
        cardAskHelp   = findViewById(R.id.card_ask_help);
        cardConflict  = findViewById(R.id.card_conflict);
        cardMeeting   = findViewById(R.id.card_meeting);

        // 點卡直接進入聊天，並設定 ast_num = "4"
        cardRaiseHand.setOnClickListener(v -> goWithTopic("上課發言"));
        cardAskHelp.setOnClickListener(v   -> goWithTopic("尋求幫助"));
        cardConflict.setOnClickListener(v  -> goWithTopic("衝突處理"));
        cardMeeting.setOnClickListener(v   -> goWithTopic("開會討論"));

        // 上頁 / 首頁
        l_back.setOnClickListener(v -> finish());
        l_main.setOnClickListener(v -> startActivity(new Intent(this, WelcomeActivity.class)));
    }

    private void goWithTopic(String topic) {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setAst_num("4");         // 學習支援類別
        g.setTopic_nam(topic);     // 具體主題
        startActivity(new Intent(this, MainActivity2.class));
    }
}
