package com.example.assistant;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.data.rest.RestClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ChatHistoryActivity";

    private RecyclerView recyclerViewChatHistory;
    private ChatHistoryAdapter chatHistoryAdapter;
    private List<Userdatabase> chatHistoryList;
    private ProgressBar progressBar;
    private TextView tvEmptyView;
    private ImageButton btnBack;



    // 修改這裡：你的伺服器 IP
    private static final String SERVER_URL = GlobalVariable.DB_URL + "/auth/get_messages.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        recyclerViewChatHistory = findViewById(R.id.recyclerViewChatHistory);
        recyclerViewChatHistory.setLayoutManager(new LinearLayoutManager(this));

        chatHistoryList = new ArrayList<>();
        chatHistoryAdapter = new ChatHistoryAdapter(
                chatHistoryList,
                message -> Toast.makeText(
                        ChatHistoryActivity.this,
                        message.getMessage(),
                        Toast.LENGTH_SHORT
                ).show()
        );
        recyclerViewChatHistory.setAdapter(chatHistoryAdapter);

        progressBar = findViewById(R.id.progressBar);
        tvEmptyView = findViewById(R.id.tvEmptyView);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        //  從 GlobalVariable 取得登入用戶
        GlobalVariable globalVariable = (GlobalVariable) getApplication();
        int userId = globalVariable.getCurrentUser().getId();

        loadChatHistory(userId);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);          // 視口從底部開始
        lm.setReverseLayout(false);        // 依舊是舊→新（不顛倒資料）
        recyclerViewChatHistory.setLayoutManager(lm);




    }

    private void loadChatHistory(int userId) {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewChatHistory.setVisibility(View.GONE);
        tvEmptyView.setVisibility(View.GONE);

        OkHttpClient client = RestClient.http();
        String url = SERVER_URL + "?user_id=" + userId;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ChatHistoryActivity.this, "載入失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "HTTP 錯誤: ", e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    try {
                        Gson gson = new Gson();
                        Type responseType = new TypeToken<MySQLResponse>() {}.getType();
                        MySQLResponse mysqlResponse = gson.fromJson(json, responseType);

                        if (mysqlResponse != null && mysqlResponse.success && mysqlResponse.messages != null) {
                            runOnUiThread(() -> {
                                chatHistoryList.clear();
                                chatHistoryList.addAll(mysqlResponse.messages);
                                chatHistoryAdapter.notifyDataSetChanged();

                                //自動捲到底
                                recyclerViewChatHistory.post(() ->
                                        recyclerViewChatHistory.scrollToPosition(chatHistoryAdapter.getItemCount() - 1)
                                );

                                if (chatHistoryList.isEmpty()) {
                                    recyclerViewChatHistory.setVisibility(View.GONE);
                                    tvEmptyView.setVisibility(View.VISIBLE);
                                } else {
                                    recyclerViewChatHistory.setVisibility(View.VISIBLE);
                                    tvEmptyView.setVisibility(View.GONE);
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                tvEmptyView.setVisibility(View.VISIBLE);
                                recyclerViewChatHistory.setVisibility(View.GONE);
                            });
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "JSON解析錯誤", e);
                    }
                } else {
                    Log.e(TAG, "HTTP 回應錯誤: " + response.code());
                }
            }
        });
    }

    // 對應 PHP JSON 結構
    public static class MySQLResponse {
        boolean success;
        List<Userdatabase> messages;
    }
}
