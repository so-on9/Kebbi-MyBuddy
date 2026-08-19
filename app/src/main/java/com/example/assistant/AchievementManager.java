package com.example.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.example.assistant.data.rest.RestClient;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AchievementManager {

    private static final String TAG = "AchievementManager";

    private static final String PREF = "achievements_v3";
    private static final String KEY_STAMP_COUNT_PREFIX = "stamp_count_";
    private static final String KEY_STAMP_ICON_PREFIX = "stamp_icon_";

    private static final int MAX_STAMPS = 48;

    private static final String URL_GET_STAMP = GlobalVariable.DB_URL + "/auth/get_stamp_count.php";
    private static final String URL_ADD_STAMP = GlobalVariable.DB_URL + "/auth/add_stamp.php";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 三選一，可重複
    private static final int[] STAMP_ICON_POOL = {
            R.drawable.icon_robot1,
            R.drawable.icon_robot2,
            R.drawable.icon_robot3
    };

    private final SharedPreferences sp;
    private final OkHttpClient http;
    private final Random random;

    public interface IntCallback {
        void onSuccess(int value);
        void onError(String message);
    }

    public AchievementManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        http = RestClient.http();
        random = new Random();
    }

    public int getMaxStamps() {
        return MAX_STAMPS;
    }

    public int getCachedStampCount(int userId) {
        return sp.getInt(keyCount(userId), 0);
    }

    private void setCachedStampCount(int userId, int count) {
        int oldCount = getCachedStampCount(userId);
        int newCount = Math.max(0, Math.min(count, MAX_STAMPS));

        // 如果成就數增加，替新解鎖的格子各自抽一張圖並固定保存
        if (newCount > oldCount) {
            for (int i = oldCount; i < newCount; i++) {
                ensureStampIcon(userId, i);
            }
        }

        sp.edit().putInt(keyCount(userId), newCount).apply();
    }

    private String keyCount(int userId) {
        return KEY_STAMP_COUNT_PREFIX + userId;
    }

    private String keyStampIcon(int userId, int slotIndex) {
        return KEY_STAMP_ICON_PREFIX + userId + "_" + slotIndex;
    }

    private int randomStampIcon() {
        return STAMP_ICON_POOL[random.nextInt(STAMP_ICON_POOL.length)];
    }

    private boolean isAllowedStampIcon(int iconRes) {
        for (int allowed : STAMP_ICON_POOL) {
            if (allowed == iconRes) {
                return true;
            }
        }
        return false;
    }

    // 確保某一格有固定圖示；若還沒有就抽一張存起來
    public int ensureStampIcon(int userId, int slotIndex) {
        if (userId <= 0 || slotIndex < 0 || slotIndex >= MAX_STAMPS) {
            return R.drawable.icon_robot1;
        }

        String key = keyStampIcon(userId, slotIndex);
        int saved = sp.getInt(key, 0);

        if (saved != 0 && isAllowedStampIcon(saved)) {
            return saved;
        }

        int icon = randomStampIcon();
        sp.edit().putInt(key, icon).apply();
        return icon;
    }

    // 讀某一格的圖示；若尚未建立就自動建立
    public int getStampIcon(int userId, int slotIndex) {
        return ensureStampIcon(userId, slotIndex);
    }

    public void fetchStampCount(int userId, @NonNull IntCallback cb) {
        if (userId <= 0) {
            cb.onError("user_id 不正確");
            return;
        }

        String json = "{\"user_id\":" + userId + "}";
        RequestBody body = RequestBody.create(json, JSON);

        Request req = new Request.Builder()
                .url(URL_GET_STAMP)
                .post(body)
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "fetchStampCount failed: " + e.getMessage());
                cb.onError("連線失敗");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "fetchStampCount http=" + response.code());
                    cb.onError("伺服器錯誤");
                    return;
                }

                try {
                    JsonObject obj = JsonParser.parseString(res).getAsJsonObject();
                    boolean ok = obj.has("success") && obj.get("success").getAsBoolean();

                    if (!ok) {
                        String err = obj.has("error") ? obj.get("error").getAsString() : "取得失敗";
                        Log.e(TAG, "fetchStampCount error: " + err);
                        cb.onError(err);
                        return;
                    }

                    int cnt = obj.has("stamp_count") ? obj.get("stamp_count").getAsInt() : 0;
                    cnt = Math.max(0, Math.min(cnt, MAX_STAMPS));
                    setCachedStampCount(userId, cnt);

                    cb.onSuccess(cnt);

                } catch (Throwable t) {
                    Log.e(TAG, "fetchStampCount parse error: " + t.getMessage());
                    cb.onError("回傳格式錯誤");
                }
            }
        });
    }

    public void addStamp(int userId, @NonNull IntCallback cb) {
        if (userId <= 0) {
            cb.onError("user_id 不正確");
            return;
        }

        String json = "{\"user_id\":" + userId + "}";
        RequestBody body = RequestBody.create(json, JSON);

        Request req = new Request.Builder()
                .url(URL_ADD_STAMP)
                .post(body)
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "addStamp failed: " + e.getMessage());
                cb.onError("連線失敗");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "addStamp http=" + response.code());
                    cb.onError("伺服器錯誤");
                    return;
                }

                try {
                    JsonObject obj = JsonParser.parseString(res).getAsJsonObject();
                    boolean ok = obj.has("success") && obj.get("success").getAsBoolean();

                    if (!ok) {
                        String err = obj.has("error") ? obj.get("error").getAsString() : "新增失敗";
                        Log.e(TAG, "addStamp error: " + err);
                        cb.onError(err);
                        return;
                    }

                    int cnt = obj.has("stamp_count") ? obj.get("stamp_count").getAsInt() : 0;
                    cnt = Math.max(0, Math.min(cnt, MAX_STAMPS));
                    setCachedStampCount(userId, cnt);

                    cb.onSuccess(cnt);

                } catch (Throwable t) {
                    Log.e(TAG, "addStamp parse error: " + t.getMessage());
                    cb.onError("回傳格式錯誤");
                }
            }
        });
    }

    public void clearCacheForUser(int userId) {
        if (userId <= 0) return;

        SharedPreferences.Editor editor = sp.edit();
        editor.remove(keyCount(userId));

        for (int i = 0; i < MAX_STAMPS; i++) {
            editor.remove(keyStampIcon(userId, i));
        }

        editor.apply();
    }

    public void clearAllCache() {
        sp.edit().clear().apply();
    }
}
