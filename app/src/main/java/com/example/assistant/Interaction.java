package com.example.assistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Interaction extends AppCompatActivity {

    private Button it_back, it_main;
    private View cardGreeting, cardMakeFriends, cardSharing, cardParticipation,
            cardInvite, cardPraise, cardAcceptPraise, cardInterest, cardHelp, cardRespondNeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interaction);

        it_back = findViewById(R.id.i_back);
        it_main = findViewById(R.id.i_main);

        cardGreeting = findViewById(R.id.card_greeting);
        cardMakeFriends = findViewById(R.id.card_makefriends);
        cardSharing = findViewById(R.id.card_sharing);
        cardParticipation = findViewById(R.id.card_participation);
        cardInvite = findViewById(R.id.card_invite);
        cardPraise = findViewById(R.id.card_praise);
        cardAcceptPraise = findViewById(R.id.card_accept_praise);
        cardInterest = findViewById(R.id.card_interest);
        cardHelp = findViewById(R.id.card_help);
        cardRespondNeed = findViewById(R.id.card_respond_need);

        cardGreeting.setOnClickListener(v -> goWithTopic("打招呼"));
        cardMakeFriends.setOnClickListener(v -> goWithTopic("認識同學"));
        cardSharing.setOnClickListener(v -> goWithTopic("分享物品"));
        cardParticipation.setOnClickListener(v -> goWithTopic("參與活動"));
        cardInvite.setOnClickListener(v -> goWithTopic("邀請別人"));
        cardPraise.setOnClickListener(v -> goWithTopic("讚美別人"));
        cardAcceptPraise.setOnClickListener(v -> goWithTopic("接受讚美"));
        cardInterest.setOnClickListener(v -> goWithTopic("討論興趣"));
        cardHelp.setOnClickListener(v -> goWithTopic("給予幫忙"));
        cardRespondNeed.setOnClickListener(v -> goWithTopic("回應他人需求"));

        it_back.setOnClickListener(v -> finish());
        it_main.setOnClickListener(v -> startActivity(new Intent(this, WelcomeActivity.class)));
    }

    private void goWithTopic(String topic) {
        GlobalVariable g = (GlobalVariable) getApplicationContext();
        g.setTopic_nam(topic);
        g.setAst_num("3");
        startActivity(new Intent(this, MainActivity2.class));
    }
}
