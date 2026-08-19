package com.example.assistant;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.os.CountDownTimer;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;



import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import com.google.gson.Gson;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;
import com.nuwarobotics.service.facecontrol.UnityFaceCallback;
import com.nuwarobotics.service.facecontrol.utils.ServiceConnectListener;

// Firebase imports are intentionally removed in this build.
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// User model import.
import com.example.assistant.data.model.User;
import com.example.assistant.data.rest.RestClient;

public class MainActivity2 extends AppCompatActivity {

    private static final class ResponseAssistantProfile {
        final String model;
        final String instructions;

        ResponseAssistantProfile(String model, String instructions) {
            this.model = model;
            this.instructions = instructions;
        }
    }

    private NuwaRobotAPI mRobotAPI;
    private IClientId mClientId;
    Context mContext;
    boolean mSDKinit = false;
    public static String ThreadID = "";
    public static String RunID = null;
    public static String previousResponseId = null;
    public static String Status = "1";
    public static String Topic_nam = "";
    public static String User_name = "";

    public static String assistant_id = null;
    private static ResponseAssistantProfile assistantProfile = null;
    private static final String RESPONSES_MODEL = "gpt-5.4-mini";
    private static final String KEYWORD_CATEGORY_PHYSIO = "physio";
    private static final String KEYWORD_CATEGORY_CLOTH = "cloth";
    private static final String KEYWORD_CATEGORY_MEAL = "meal";
    private static final String KEYWORD_CATEGORY_COMMON = "common";
    private static final String DOC_INSTRUCTION_ASSET = "instructions_master.txt";

    private View keywordOverlay;
    private ImageView ivKeywordImage;
    private ProgressBar pbKeywordTimer;
    private TextView tvKeywordTimer;
    private CountDownTimer keywordTimer;
    private AchievementManager achievementManager;
    private CountDownTimer topicStayTimer;
    private boolean topicStampAwarded = false;
    private static final long TOPIC_STAY_MS = 10_000;

    // Keyword image libraries.
    private final LinkedHashMap<String, Integer> keywordToDrawable = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> keywordToCategory = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> docInstructionSections = new LinkedHashMap<>();

    // Keyword overlay display duration.
    private static final int KEYWORD_OVERLAY_DURATION_MS = 5000;


    // The Android app calls the server proxy. The OpenAI key remains on Ubuntu.
    private static final OkHttpClient RESPONSES_PROXY = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request request = chain.request();
                String token = RestClient.getAuthToken();
                if (token != null && request.header("Authorization") == null) {
                    request = request.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build();
                }
                return chain.proceed(request);
            })
            .build();

    // Log non-2xx OpenAI responses and tell the caller to stop.
    private boolean handleNon2xxLog(Response response, String bodyForLog, String stageTag) {
        if (response.isSuccessful()) return false;
        Log.e(TAG, stageTag + " non-2xx code=" + response.code());
        return true;
    }

    private void handleResponseFailure(String stage, String detail) {
        Log.e(TAG, "Response flow failed at " + stage + ": " + detail);
        runOnUiThread(() -> mouthOn("\u6211\u73fe\u5728\u6c92\u6709\u6536\u5230\u56de\u8986\uff0c\u8acb\u518d\u8a66\u4e00\u6b21\u3002", FACE_MOUTH_SPEED));
    }

    private Button StartButton;
    private Button ma_back;
    private Button ma_main;
    private Button btnKeywordToggle;

    private volatile boolean overlayShowing = false;
    private volatile boolean pendingStartStt = false;
    private boolean keywordImageEnabled = true;
    private boolean isRobotSpeaking = false;
    private boolean isSttActive = false;
    private boolean isManualListeningPaused = false;
    private boolean ignoreNextSttCompletion = false;
    private long lastBellyToggleAt = 0L;


    private static final long FACE_MOUTH_SPEED = 200;
    private static final long UNITY_RETURN_DELAY_MS = 800;
    private static final long OVERLAY_FADE_MS = 220;
    private static final long BELLY_TOGGLE_COOLDOWN_MS = 800L;
    private static final String TTS_SPEED = "70";
    private static final String STT_TIMEOUT_TOKEN = "_VOICETIMEOUT_";
    private static final String WAITING_GESTURE_MOTION = "666_BA_RzArmS90";
    private static final String STT_TIMEOUT_FALLBACK = "\u6211\u6c92\u807d\u6e05\u695a\uff0c\u8acb\u518d\u8aaa\u4e00\u6b21\u3002";
    private boolean nativeWakeupSuppressed = false;

    // PHP REST API (MySQL)
    private static final String PHP_INSERT_URL = GlobalVariable.DB_URL + "/auth/insert_message.php";
    private static final String PHP_RESPONSES_URL = GlobalVariable.DB_URL + "/auth/respond.php";

    // Resolved user id for saving chat and stamps to PHP/MySQL.
    private String resolvedUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initKeywordImageLibraryByTopic();

        achievementManager = new AchievementManager(this);
        startTopicStayTimer();

        resolvedUserId = resolveUserId();
        String resolvedDisplayName = resolveDisplayName();
        mContext   = this;
        StartButton = findViewById(R.id.start_button);
        ma_back     = findViewById(R.id.ma2_back);
        ma_main     = findViewById(R.id.ma2_main);
        btnKeywordToggle = findViewById(R.id.btnKeywordToggle);

        keywordOverlay = findViewById(R.id.keywordOverlay);
        ivKeywordImage = findViewById(R.id.ivKeywordImage);
        pbKeywordTimer = findViewById(R.id.pbKeywordTimer);
        tvKeywordTimer = findViewById(R.id.tvKeywordTimer);


        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);

        Log.d(TAG, "register EventListener ");
        mRobotAPI.registerRobotEventListener(robotEventListener);
        mRobotAPI.registerVoiceEventListener(voiceEventListener);

        GlobalVariable globalVariable = ((GlobalVariable) getApplicationContext());
        DecideAst(globalVariable.getAst_num());
        Topic_nam = globalVariable.getTopic_nam();
        User_name = resolvedDisplayName != null ? resolvedDisplayName : globalVariable.getUser_name();
        previousResponseId = null;

        // 3) UI
        ma_back.setOnClickListener(view -> finish());
        ma_main.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity2.this, WelcomeActivity.class);
            startActivity(intent);
        });

        /*StartButton.setOnClickListener(view -> {
            CreateThreadPost();
            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

            showface();

            // Build the opening prompt for the current topic.

            // Save the opening user message to MySQL when user id exists.
            if (resolvedUserId != null) {
                saveMessageToMySQL(resolvedUserId, "user", question);
            } else {
                Log.w(TAG, "resolvedUserId is null; skip saving this user message to MySQL");
            }

            // Legacy thread flow kept only for fallback compatibility.
            CreateMessagePost(question);
        });   */
        StartButton.setOnClickListener(view -> {
            showface();

            String question = buildInitialQuestion();

            // Save the opening user message to MySQL when user id exists.
            if (resolvedUserId != null) {
                saveMessageToMySQL(resolvedUserId, "user", question);
            } else {
                Log.w(TAG, "resolvedUserId is null; skip saving this user message to MySQL");
            }

            // Send the opening message directly to the Responses API.
            requestResponse(question);
        });

        btnKeywordToggle.setOnClickListener(v -> {
            keywordImageEnabled = !keywordImageEnabled;
            updateKeywordToggleButton();
            if (!keywordImageEnabled) {

                boolean wasOverlayShowing = overlayShowing;

                if (keywordTimer != null) {
                    keywordTimer.cancel();
                    keywordTimer = null;
                }

                if (keywordOverlay != null) {
                    keywordOverlay.clearAnimation();
                    keywordOverlay.setVisibility(View.GONE);
                    keywordOverlay.setAlpha(1f);
                }

                overlayShowing = false;
                pendingStartStt = false;

                if (wasOverlayShowing) {
                    safeShowUnity();
                }
            }
        });
        updateKeywordToggleButton();



    }

    private void updateKeywordToggleButton() {
        if (btnKeywordToggle == null) return;
        btnKeywordToggle.setText("\u95dc\u9375\u5b57\u6587\u641c\u5716\uff1a" + (keywordImageEnabled ? "\u958b" : "\u95dc"));
    }

    private void initKeywordImageLibrary() {
        keywordToDrawable.clear();
        keywordToDrawable.put("drink", R.drawable.kw_drink);
        keywordToDrawable.put("school", R.drawable.kw_school);
        keywordToDrawable.put("run", R.drawable.kw_run);
    }

    private String buildInitialQuestion() {
        String displayName = User_name != null ? User_name : "\u540c\u5b78";
        String topic = Topic_nam != null ? Topic_nam.trim() : "";
        return "\u6211\u662f" + displayName + "\uff0c\u6211\u4eca\u5929\u60f3\u7df4\u7fd2" + topic + "\u3002\u8acb\u5148\u548c\u6211\u7c21\u55ae\u6696\u8eab\uff0c\u518d\u958b\u59cb\u4e3b\u984c\u5c0d\u8a71\u3002";
    }

    private void initKeywordImageLibraryByTopic() {
        keywordToDrawable.clear();
        keywordToCategory.clear();

        addKeyword("\u53e3\u6e34", R.drawable.kw_thirsty, KEYWORD_CATEGORY_PHYSIO);
        addKeyword("\u559d\u6c34", R.drawable.kw_drink, KEYWORD_CATEGORY_PHYSIO);
        addKeyword("\u98ef\u5f8c\u60f3\u4e0a\u5ec1\u6240", R.drawable.kw_aftereatingpoo, KEYWORD_CATEGORY_PHYSIO);
        addKeyword("\u5916\u9762\u60f3\u4e0a\u5ec1\u6240", R.drawable.kw_outdoorpoop, KEYWORD_CATEGORY_PHYSIO);
        addKeyword("\u809a\u5b50\u602a\u602a\u7684", R.drawable.kw_stomach, KEYWORD_CATEGORY_PHYSIO);
        addKeyword("\u6211\u8981\u4e0a\u5ec1\u6240", R.drawable.kw_wantstogototoilet, KEYWORD_CATEGORY_PHYSIO);
        addKeyword("\u4e0a\u8ab2\u60f3\u4e0a\u5ec1\u6240", R.drawable.kw_wantstogototoiletinclass, KEYWORD_CATEGORY_PHYSIO);
        addKeyword("\u60f3\u4e0a\u5ec1\u6240", R.drawable.kw_wantstopoo, KEYWORD_CATEGORY_PHYSIO);
        addKeyword("\u516c\u5712\u73a9", R.drawable.kw_park, KEYWORD_CATEGORY_PHYSIO);

        addKeyword("\u85cd\u8272\u8863\u670d", R.drawable.kw_bluecoat, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u5916\u5957", R.drawable.kw_pickcoat, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u7a7f\u9019\u4ef6", R.drawable.kw_iwantthiscoat, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u6211\u8981\u812b\u5916\u5957", R.drawable.kw_iwanttotakeoffcoat, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u7a7f\u54ea\u4ef6", R.drawable.kw_pickcoat, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u51b7", R.drawable.kw_sayhotorcold, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u71b1", R.drawable.kw_sayhotorcold, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u812b\u5916\u5957", R.drawable.kw_takeoffcoat, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u8981\u7a7f\u8863\u670d", R.drawable.kw_iwantowearcoat, KEYWORD_CATEGORY_CLOTH);
        addKeyword("\u8981\u812b\u8863\u670d", R.drawable.kw_takeorputoncoat, KEYWORD_CATEGORY_CLOTH);

        addKeyword("\u4e09\u660e\u6cbb", R.drawable.kw_lunch, KEYWORD_CATEGORY_MEAL);
        addKeyword("\u65e9\u9910", R.drawable.kw_breakfast, KEYWORD_CATEGORY_MEAL);
        addKeyword("\u5348\u9910", R.drawable.kw_lunch, KEYWORD_CATEGORY_MEAL);
        addKeyword("\u665a\u9910", R.drawable.kw_dinner, KEYWORD_CATEGORY_MEAL);
        addKeyword("\u98ef\u9084\u662f\u9eb5", R.drawable.kw_ricenoodle, KEYWORD_CATEGORY_MEAL);

        addKeyword("\u6700\u68d2", R.drawable.kw_best, KEYWORD_CATEGORY_COMMON);
        addKeyword("\u5f88\u597d", R.drawable.kw_good, KEYWORD_CATEGORY_COMMON);
        addKeyword("\u53ef\u4ee5\u600e\u9ebc\u8aaa", R.drawable.kw_howtosay, KEYWORD_CATEGORY_COMMON);
        addKeyword("\u8dd1\u6b65", R.drawable.kw_run, KEYWORD_CATEGORY_COMMON);
        addKeyword("\u8ddf\u8001\u5e2b\u8aaa", R.drawable.kw_talktoteacher, KEYWORD_CATEGORY_COMMON);
        addKeyword("\u5b78\u6821", R.drawable.kw_school, KEYWORD_CATEGORY_COMMON);
    }

    private void addKeyword(String keyword, int drawableId, String category) {
        keywordToDrawable.put(keyword, drawableId);
        keywordToCategory.put(keyword, category);
    }


    private void startTopicStayTimer() {
        if (topicStayTimer != null) topicStayTimer.cancel();

        topicStampAwarded = false;

        final String topicId = (Topic_nam != null && !Topic_nam.trim().isEmpty())
                ? Topic_nam.trim()
                : "unknown_topic";

        topicStayTimer = new CountDownTimer(TOPIC_STAY_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Award the topic stamp on the UI callback after the stay timer finishes.
            }

            @Override
            public void onFinish() {
                if (topicStampAwarded) return;
                topicStampAwarded = true;

                if (resolvedUserId == null || resolvedUserId.trim().isEmpty()) {
                    Log.e(TAG, "Stamp not awarded: resolvedUserId is null");
                    return;
                }

                int userIdInt;
                try {
                    userIdInt = Integer.parseInt(resolvedUserId.trim());
                } catch (Exception e) {
                    Log.e(TAG, "Stamp not awarded: invalid userId=" + resolvedUserId);
                    return;
                }

                achievementManager.addStamp(userIdInt, new AchievementManager.IntCallback() {
                    @Override
                    public void onSuccess(int newCount) {
                        Log.d(TAG, "Stamp awarded. stamp_count=" + newCount);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "addStamp failed: " + message);
                    }
                });
            }
        };
        topicStayTimer.start();
    }

    private boolean tryShowKeywordOverlayForAnswer(String answer) {
        if (!keywordImageEnabled) {
            Log.d(TAG, "Keyword overlay skipped: keywordImageEnabled=false, topic=" + Topic_nam);
            return false;
        }

        if (answer == null) return false;

        String lower = answer.toLowerCase(Locale.ROOT);
        Log.d(TAG, "Keyword overlay checking. topic=" + Topic_nam);

        for (Map.Entry<String, Integer> entry : keywordToDrawable.entrySet()) {
            String key = entry.getKey();
            int drawableId = entry.getValue();

            boolean hit;
            if (isAsciiWord(key)) {
                hit = lower.contains(key.toLowerCase(Locale.ROOT));
            } else {
                hit = answer.contains(key);
            }

            if (hit) {
                boolean allowed = isKeywordAllowedForCurrentTopic(key);
                Log.d(TAG, "Keyword overlay hit key=" + key + ", allowed=" + allowed + ", drawableId=" + drawableId);
                if (allowed) {
                    showKeywordImageOverlay(drawableId, KEYWORD_OVERLAY_DURATION_MS);
                    return true;
                }
            }
        }
        Log.d(TAG, "Keyword overlay no eligible match. topic=" + Topic_nam + ", answer=" + answer);
        return false;
    }

    private boolean isKeywordAllowedForCurrentTopic(String keyword) {
        String category = keywordToCategory.get(keyword);
        if (category == null) {
            Log.d(TAG, "Keyword category missing for key=" + keyword);
            return false;
        }
        if (KEYWORD_CATEGORY_COMMON.equals(category)) {
            Log.d(TAG, "Keyword allowed by common category. key=" + keyword);
            return true;
        }

        String topic = Topic_nam != null ? Topic_nam.trim() : "";
        Set<String> allowedCategories = getAllowedKeywordCategoriesForTopic(topic);
        boolean allowed = allowedCategories.contains(category);
        Log.d(TAG, "Keyword category check. key=" + keyword + ", category=" + category + ", topic=" + topic + ", allowedCategories=" + allowedCategories + ", allowed=" + allowed);
        return allowed;
    }

    private Set<String> getAllowedKeywordCategoriesForTopic(String topic) {
        Set<String> allowedCategories = new LinkedHashSet<>();
        allowedCategories.add(KEYWORD_CATEGORY_COMMON);

        if (topic == null) {
            return allowedCategories;
        }

        if ("\u751f\u7406\u9700\u6c42".equals(topic)) {
            allowedCategories.add(KEYWORD_CATEGORY_PHYSIO);
        } else if ("\u8863\u8457\u9700\u6c42".equals(topic) || "\u8863\u670d\u9078\u64c7".equals(topic)) {
            allowedCategories.add(KEYWORD_CATEGORY_CLOTH);
        } else if ("\u7528\u9910\u9700\u6c42".equals(topic) || "\u4e09\u9910\u9078\u64c7".equals(topic)) {
            allowedCategories.add(KEYWORD_CATEGORY_MEAL);
        }

        return allowedCategories;
    }

    private boolean isAsciiWord(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127) return false;
        }
        return true;
    }



    private void showKeywordImageOverlay(int drawableResId, int durationMs) {
        runOnUiThread(() -> {
            if (keywordOverlay == null) {
                Log.e(TAG, "keywordOverlay is null");
                return;
            }

            overlayShowing = true;
            pendingStartStt = false;

            keywordOverlay.bringToFront();
            keywordOverlay.setAlpha(1f);
            keywordOverlay.setVisibility(View.VISIBLE);
            keywordOverlay.requestLayout();
            keywordOverlay.invalidate();

            if (mRobotAPI != null) {
                mRobotAPI.stopTTS();
                safeHideUnity();
            }

            if (keywordTimer != null) keywordTimer.cancel();

            ivKeywordImage.setImageResource(drawableResId);
            pbKeywordTimer.setMax(durationMs);
            pbKeywordTimer.setProgress(durationMs);

            keywordTimer = new CountDownTimer(durationMs, 50) {
                @Override
                public void onTick(long millisUntilFinished) {
                    pbKeywordTimer.setProgress((int) millisUntilFinished);
                    int sec = (int) Math.ceil(millisUntilFinished / 1000.0);
                    tvKeywordTimer.setText("\u5269\u9918 " + sec + " \u79d2");
                }

                @Override
                public void onFinish() {
                    if (mRobotAPI != null) {
                        safeShowUnity();
                    }

                    keywordOverlay.postDelayed(() -> {
                        keywordOverlay.animate()
                                .alpha(0f)
                                .setDuration(OVERLAY_FADE_MS)
                                .withEndAction(() -> {
                                    keywordOverlay.setVisibility(View.GONE);
                                    keywordOverlay.setAlpha(1f);
                                    overlayShowing = false;

                                    if (pendingStartStt && mRobotAPI != null) {
                                        pendingStartStt = false;
                                        startAppSpeechToText();
                                    }
                                })
                                .start();
                    }, UNITY_RETURN_DELAY_MS);
                }
            }.start();
        });
    }




    // ------------------------------------------------------------------
    // Resolve user id and display name from GlobalVariable / current user state.
    // ------------------------------------------------------------------
    private String resolveUserId() {
        try {
            GlobalVariable g = (GlobalVariable) getApplication();
            // Preferred path: current user already set through GlobalVariable.setCurrentUser(User).
            User u = g.getCurrentUser();
            if (u != null && u.id > 0) {
                return String.valueOf(u.id); // users.id (int)
            }
        } catch (Throwable t) {
            Log.w(TAG, "resolveUserId() failed: " + t.getMessage());
        }
        return null;
    }

    private String resolveDisplayName() {
        try {
            GlobalVariable g = (GlobalVariable) getApplication();
            User u = g.getCurrentUser();
            if (u != null && u.displayName != null) return u.displayName;

            // Fallback to global user_name when the User object is not available.
            if (g.getUser_name() != null) return g.getUser_name();
        } catch (Throwable t) {
            Log.w(TAG, "resolveDisplayName() failed: " + t.getMessage());
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Save chat messages to MySQL through the PHP endpoint.
    private void saveMessageToMySQL(String userId, String role, String message) {
        OkHttpClient client = RestClient.http();

        RequestBody formBody = new FormBody.Builder()
                .add("user_id", userId) // users.id as string
                .add("role", role)      // "user" or "gpt"
                .add("message", message)
                .build();

        Request request = new Request.Builder()
                .url(PHP_INSERT_URL)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "MySQL insert failed: " + e.getMessage());
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "MySQL insert completed with HTTP " + response.code());
            }
        });
    }

    // ------------------------------------------------------------------
    // Assistant profile selection for the current topic group.
    // ------------------------------------------------------------------
    public void DecideAst (String ast) {
        assistantProfile = buildAssistantProfile(ast);
    }

    private ResponseAssistantProfile buildAssistantProfile(String ast) {
        switch (ast) {
            case "1":
                return new ResponseAssistantProfile(RESPONSES_MODEL, buildDailyNeedsInstructions());
            case "2":
                return new ResponseAssistantProfile(RESPONSES_MODEL, buildEmotionInstructions());
            case "3":
                return new ResponseAssistantProfile(RESPONSES_MODEL, buildInteractionInstructions());
            case "4":
                return new ResponseAssistantProfile(RESPONSES_MODEL, buildLearningSupportInstructions());
            case "5":
            default:
                return new ResponseAssistantProfile(RESPONSES_MODEL, buildCasualChatInstructions());
        }
    }

    private String loadInstructionSectionForCurrentTopic(String topicGroup) {
        ensureDocInstructionSectionsLoaded();

        if (docInstructionSections.isEmpty()) {
            return null;
        }

        String topic = Topic_nam != null ? Topic_nam.trim() : "";
        if (topic.isEmpty()) {
            return null;
        }

        String sectionKey = null;
        switch (topicGroup) {
            case "\u65e5\u5e38\u9700\u6c42":
                if ("\u751f\u7406\u9700\u6c42".equals(topic)) {
                    sectionKey = "\u65e5\u5e38\u9700\u6c42-\u751f\u7406\u9700\u6c42";
                } else if ("\u7528\u9910\u9700\u6c42".equals(topic) || "\u4e09\u9910\u9078\u64c7".equals(topic)) {
                    sectionKey = "\u65e5\u5e38\u9700\u6c42-\u4e09\u9910\u9078\u64c7";
                } else if ("\u8863\u8457\u9700\u6c42".equals(topic) || "\u8863\u670d\u9078\u64c7".equals(topic)) {
                    sectionKey = "\u65e5\u5e38\u9700\u6c42-\u8863\u670d\u9078\u64c7";
                } else if ("\u501f\u7269\u9700\u6c42".equals(topic) || "\u501f\u7528\u7269\u54c1".equals(topic)) {
                    sectionKey = "\u65e5\u5e38\u9700\u6c42-\u501f\u7528\u7269\u54c1";
                }
                break;
            case "\u4e92\u52d5\u4ea4\u6d41":
                if ("\u6253\u62db\u547c".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u6253\u62db\u547c";
                } else if ("\u4ea4\u670b\u53cb".equals(topic) || "\u8a8d\u8b58\u540c\u5b78".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u8a8d\u8b58\u540c\u5b78";
                } else if ("\u5206\u4eab".equals(topic) || "\u5206\u4eab\u7269\u54c1".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u5206\u4eab\u7269\u54c1";
                } else if ("\u53c3\u8207\u6d3b\u52d5".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u53c3\u8207\u6d3b\u52d5";
                } else if ("\u9080\u8acb\u5225\u4eba".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u9080\u8acb\u5225\u4eba";
                } else if ("\u8b9a\u7f8e\u5225\u4eba".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u8b9a\u7f8e\u5225\u4eba";
                } else if ("\u63a5\u53d7\u8b9a\u7f8e".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u63a5\u53d7\u8b9a\u7f8e";
                } else if ("\u8a0e\u8ad6\u8208\u8da3".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u8a0e\u8ad6\u8208\u8da3";
                } else if ("\u7d66\u4e88\u5e6b\u52a9".equals(topic) || "\u7d66\u4e88\u5e6b\u5fd9".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u7d66\u4e88\u5e6b\u5fd9";
                } else if ("\u56de\u61c9\u4ed6\u4eba\u9700\u6c42".equals(topic) || "\u56de\u61c9\u9700\u6c42".equals(topic)) {
                    sectionKey = "\u4e92\u52d5\u4ea4\u6d41-\u56de\u61c9\u9700\u6c42";
                }
                break;
            case "\u60c5\u611f\u8868\u9054":
                if ("\u60c5\u7dd2\u8868\u9054".equals(topic)) {
                    sectionKey = "\u60c5\u611f\u8868\u9054-\u60c5\u7dd2\u8868\u9054";
                } else if ("\u60c5\u7dd2\u7ba1\u7406".equals(topic)) {
                    sectionKey = "\u60c5\u611f\u8868\u9054-\u60c5\u7dd2\u7ba1\u7406";
                }
                break;
            case "\u5b78\u7fd2\u652f\u63f4":
                if ("\u4e0a\u8ab2\u767c\u8a00".equals(topic)) {
                    sectionKey = "\u5b78\u7fd2\u652f\u63f4-\u4e0a\u8ab2\u767c\u8a00";
                } else if ("\u5c0b\u6c42\u5e6b\u52a9".equals(topic)) {
                    sectionKey = "\u5b78\u7fd2\u652f\u63f4-\u5c0b\u6c42\u5e6b\u52a9";
                } else if ("\u885d\u7a81\u8655\u7406".equals(topic)) {
                    sectionKey = "\u5b78\u7fd2\u652f\u63f4-\u885d\u7a81\u8655\u7406";
                } else if ("\u53c3\u8207\u6703\u8b70".equals(topic) || "\u958b\u6703\u8a0e\u8ad6".equals(topic)) {
                    sectionKey = "\u5b78\u7fd2\u652f\u63f4-\u958b\u6703\u8a0e\u8ad6";
                }
                break;
            default:
                if ("\u9592\u804a".equals(topicGroup) && docInstructionSections.containsKey("\u9592\u804a")) {
                    sectionKey = "\u9592\u804a";
                } else {
                    String exactKey = topicGroup + "-" + topic;
                    if (docInstructionSections.containsKey(exactKey)) {
                        sectionKey = exactKey;
                    }
                }
                break;
        }

        return sectionKey != null ? docInstructionSections.get(sectionKey) : null;
    }

    private void ensureDocInstructionSectionsLoaded() {
        if (!docInstructionSections.isEmpty()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(DOC_INSTRUCTION_ASSET)))) {
            String line;
            String currentSection = null;
            StringBuilder buffer = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (isInstructionSectionHeader(trimmed)) {
                    if (currentSection != null && buffer.length() > 0) {
                        docInstructionSections.put(currentSection, buffer.toString().trim());
                    }
                    currentSection = trimmed;
                    buffer.setLength(0);
                    continue;
                }

                if (currentSection != null) {
                    if (buffer.length() > 0) {
                        buffer.append('\n');
                    }
                    buffer.append(line);
                }
            }

            if (currentSection != null && buffer.length() > 0) {
                docInstructionSections.put(currentSection, buffer.toString().trim());
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load instructions_master.txt: " + e.getMessage());
        }
    }

    private boolean isInstructionSectionHeader(String line) {
        return line.startsWith("\u65e5\u5e38\u9700\u6c42-")
                || line.startsWith("\u60c5\u611f\u8868\u9054-")
                || line.startsWith("\u4e92\u52d5\u4ea4\u6d41-")
                || line.startsWith("\u5b78\u7fd2\u652f\u63f4-")
                || "\u9592\u804a".equals(line)
                || line.startsWith("\u9592\u804a-");
    }

    private String buildDailyNeedsInstructions() {
        String docSection = loadInstructionSectionForCurrentTopic("\u65e5\u5e38\u9700\u6c42");
        if (docSection != null && !docSection.isEmpty()) {
            return docSection;
        }
        return String.join("\n",
                "\u4f60\u662f Kebbi\uff0c\u8acb\u4f9d\u7167\u76ee\u524d\u7684\u65e5\u5e38\u9700\u6c42\u4e3b\u984c\u548c\u5b78\u751f\u9032\u884c\u7df4\u7fd2\u3002",
                "\u5148\u7528\u7c21\u55ae\u6696\u8eab\u958b\u5834\uff0c\u518d\u81ea\u7136\u5f15\u5c0e\u5230\u4e3b\u984c\u60c5\u5883\u3002",
                "\u4e00\u6b21\u53ea\u554f\u4e00\u500b\u554f\u984c\uff0c\u512a\u5148\u5f15\u5c0e\u5b78\u751f\u7528\u5b8c\u6574\u53e5\u56de\u7b54\u3002",
                "\u82e5\u5b78\u751f\u4e0d\u6703\u56de\u7b54\uff0c\u8acb\u4f9d\u5e8f\u7528\u91cd\u554f\u3001\u4e8c\u9078\u4e00\u3001\u793a\u7bc4\u53e5\u4f86\u5354\u52a9\u3002"
        );
    }

    private String buildEmotionInstructions() {
        String docSection = loadInstructionSectionForCurrentTopic("\u60c5\u611f\u8868\u9054");
        if (docSection != null && !docSection.isEmpty()) {
            return docSection;
        }
        return String.join("\n",
                "\u4f60\u662f Kebbi\uff0c\u8acb\u7528\u6eab\u548c\u3001\u5177\u9ad4\u7684\u65b9\u5f0f\u548c\u5b78\u751f\u7df4\u7fd2\u60c5\u611f\u8868\u9054\u3002",
                "\u5148\u5f9e\u751f\u6d3b\u60c5\u5883\u6696\u8eab\uff0c\u518d\u5f15\u5c0e\u5b78\u751f\u63cf\u8ff0\u611f\u53d7\u548c\u539f\u56e0\u3002",
                "\u82e5\u5b78\u751f\u56de\u7b54\u7c21\u77ed\uff0c\u8acb\u8ffd\u554f\u4e26\u5354\u52a9\u4ed6\u628a\u53e5\u5b50\u8aaa\u5b8c\u6574\u3002",
                "\u7d50\u5c3e\u8acb\u7d66\u5b78\u751f\u660e\u78ba\u7684\u6b63\u5411\u9f13\u52f5\u3002"
        );
    }

    private String buildInteractionInstructions() {
        String docSection = loadInstructionSectionForCurrentTopic("\u4e92\u52d5\u4ea4\u6d41");
        if (docSection != null && !docSection.isEmpty()) {
            return docSection;
        }
        return String.join("\n",
                "\u4f60\u662f Kebbi\uff0c\u8acb\u548c\u5b78\u751f\u9032\u884c\u4e92\u52d5\u4ea4\u6d41\u4e3b\u984c\u7684\u53e3\u8a9e\u7df4\u7fd2\u3002",
                "\u5148\u505a 1 \u5230 2 \u5206\u9418\u7684\u6696\u8eab\uff0c\u518d\u9032\u5165\u76ee\u6a19\u60c5\u5883\u3002",
                "\u4e00\u6b21\u53ea\u554f\u4e00\u500b\u554f\u984c\uff0c\u4e26\u9f13\u52f5\u5b78\u751f\u7528\u5b8c\u6574\u53e5\u56de\u61c9\u3002",
                "\u5982\u679c\u5b78\u751f\u5361\u4f4f\uff0c\u8acb\u63d0\u4f9b\u5177\u9ad4\u751f\u6d3b\u4f8b\u5b50\u6216\u4e8c\u9078\u4e00\u63d0\u793a\u3002"
        );
    }

    private String buildLearningSupportInstructions() {
        String docSection = loadInstructionSectionForCurrentTopic("\u5b78\u7fd2\u652f\u63f4");
        if (docSection != null && !docSection.isEmpty()) {
            return docSection;
        }
        return String.join("\n",
                "\u4f60\u662f Kebbi\uff0c\u8acb\u5354\u52a9\u5b78\u751f\u7df4\u7fd2\u5b78\u7fd2\u652f\u63f4\u76f8\u95dc\u7684\u8868\u9054\u3002",
                "\u8acb\u4f7f\u7528\u4e0a\u8ab2\u3001\u8a0e\u8ad6\u3001\u6c42\u52a9\u7b49\u5177\u9ad4\u6821\u5712\u60c5\u5883\u3002",
                "\u4e00\u6b21\u53ea\u554f\u4e00\u500b\u554f\u984c\uff0c\u4e26\u5faa\u5e8f\u5354\u52a9\u5b78\u751f\u8aaa\u51fa\u5b8c\u6574\u56de\u61c9\u3002",
                "\u5c0d\u8a71\u6700\u5f8c\u8acb\u7c21\u77ed\u80af\u5b9a\u5b78\u751f\u7684\u8868\u73fe\u3002"
        );
    }

    private String buildCasualChatInstructions() {
        String docSection = loadInstructionSectionForCurrentTopic("\u9592\u804a");
        if (docSection != null && !docSection.isEmpty()) {
            return docSection;
        }
        return String.join("\n",
                "\u4f60\u662f Kebbi\uff0c\u8acb\u548c\u5b78\u751f\u81ea\u7136\u9592\u804a\u3002",
                "\u8acb\u4f7f\u7528\u5b78\u751f\u719f\u6089\u7684\u751f\u6d3b\u8a71\u984c\uff0c\u50cf\u662f\u5b78\u6821\u3001\u8208\u8da3\u3001\u5929\u6c23\u6216\u4eca\u5929\u505a\u4e86\u4ec0\u9ebc\u3002",
                "\u4e00\u6b21\u53ea\u554f\u4e00\u500b\u554f\u984c\uff0c\u4fdd\u6301\u53e3\u543b\u7c21\u55ae\u3001\u53cb\u5584\u3002",
                "\u82e5\u5b78\u751f\u6709\u56de\u61c9\uff0c\u8acb\u9806\u8457\u5167\u5bb9\u81ea\u7136\u8ffd\u554f\u3002"
        );
    }

    private void requestResponse(String question) {
        isSttActive = false;
        ResponseAssistantProfile profile = assistantProfile != null
                ? assistantProfile
                : buildAssistantProfile("5");

        JsonObject payload = new JsonObject();
        payload.addProperty("instructions", profile.instructions);
        payload.addProperty("input", question);

        if (previousResponseId != null && !previousResponseId.isEmpty()) {
            payload.addProperty("previous_response_id", previousResponseId);
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                payload.toString()
        );

        Request request = new Request.Builder()
                .url(PHP_RESPONSES_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        RESPONSES_PROXY.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Responses API request failed: " + e.getMessage());
                handleResponseFailure("request", e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "";
                if (handleNon2xxLog(response, res, "ResponsesCreate")) {
                    handleResponseFailure("non_2xx", "code=" + response.code());
                    return;
                }

                String answer;
                try {
                    JsonObject obj = JsonParser.parseString(res).getAsJsonObject();
                    previousResponseId = obj.has("id") ? obj.get("id").getAsString() : previousResponseId;
                    answer = extractResponseText(obj);
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to parse Responses API payload: " + t.getMessage());
                    handleResponseFailure("parse", t.getMessage());
                    return;
                }

                if (answer == null || answer.trim().isEmpty()) {
                    Log.w(TAG, "Responses API returned empty output");
                    handleResponseFailure("empty_output", res);
                    return;
                }

                final String finalAnswer = normalizeAssistantAnswer(answer);
                runOnUiThread(() -> {
                    Log.d(TAG, "Keyword overlay state before match. keywordImageEnabled=" + keywordImageEnabled + ", topic=" + Topic_nam);

                    boolean shown = tryShowKeywordOverlayForAnswer(finalAnswer);

                    if (shown) {
                        speakOnly(finalAnswer);
                    } else {
                        mouthOn(finalAnswer, FACE_MOUTH_SPEED);
                    }

                    if (resolvedUserId != null) {
                        saveMessageToMySQL(resolvedUserId, "gpt", finalAnswer);
                    }
                });
            }
        });
    }

    private String extractResponseText(JsonObject responseObject) {
        if (responseObject == null) return null;

        if (responseObject.has("output_text") && responseObject.get("output_text").isJsonPrimitive()) {
            return responseObject.get("output_text").getAsString();
        }

        if (!responseObject.has("output") || !responseObject.get("output").isJsonArray()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        JsonArray output = responseObject.getAsJsonArray("output");

        for (int i = 0; i < output.size(); i++) {
            if (!output.get(i).isJsonObject()) continue;

            JsonObject item = output.get(i).getAsJsonObject();
            if (!item.has("content") || !item.get("content").isJsonArray()) continue;

            JsonArray content = item.getAsJsonArray("content");
            for (int j = 0; j < content.size(); j++) {
                if (!content.get(j).isJsonObject()) continue;

                JsonObject part = content.get(j).getAsJsonObject();
                if (part.has("type")
                        && "output_text".equals(part.get("type").getAsString())
                        && part.has("text")
                        && part.get("text").isJsonPrimitive()) {
                    builder.append(part.get("text").getAsString());
                }
            }
        }

        return builder.length() == 0 ? null : builder.toString();
    }

    private String normalizeAssistantAnswer(String answer) {
        if (answer == null) return null;

        String trimmed = answer.trim();
        if (trimmed.isEmpty()) return trimmed;

        String candidate = trimmed;
        if (candidate.startsWith("```")) {
            candidate = candidate.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }

        try {
            JsonObject obj = JsonParser.parseString(candidate).getAsJsonObject();
            if (obj.has("response") && obj.get("response").isJsonPrimitive()) {
                String spoken = obj.get("response").getAsString().trim();
                if (!spoken.isEmpty()) {
                    Log.d(TAG, "Normalized assistant JSON response to spoken text");
                    return spoken;
                }
            }
        } catch (Throwable ignored) {
            // Not a JSON wrapper; use the original assistant text.
        }

        return trimmed;
    }

    // ------------------------------------------------------------------
    // Legacy thread placeholder kept for compatibility with older call sites.
    // ------------------------------------------------------------------
    public void CreateThreadPost() {
        ThreadID = "local-thread";
        Log.d(TAG, "CreateThreadPost fallback thread id=" + ThreadID);
    }

    public void CreateMessagePost(String question) {
        requestResponse(question);
    }

    public void RunAssistantPost() {
        Log.d(TAG, "RunAssistantPost is bypassed; using Responses API flow.");
    }

    private WeakHashMap<String, Object> runAssistant() {
        WeakHashMap<String, Object> weakHashMap = new WeakHashMap<>();
        weakHashMap.put("assistant_id", assistant_id);
        return weakHashMap;
    }

    public void MessageGet() {
        Log.d(TAG, "MessageGet is bypassed; responses are handled in requestResponse().");
    }



    // ------------------------------------------------------------------
    // Poll current run status.
    // ------------------------------------------------------------------
    public String RunStatusGet() {
        Log.d(TAG, "RunStatusGet is deprecated; the active flow uses Responses API.");
        return "deprecated";
    }


    // ============================================================================================
    // Nuwa / face / TTS / STT lifecycle methods.
    // ============================================================================================

    @Override
    protected void onDestroy() {
        restoreNativeWakeupMode();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
        mRobotAPI.release();
        if (topicStayTimer != null) {
            topicStayTimer.cancel();
            topicStayTimer = null;
        }
    }

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss ");
        String currentDateAndTime = sdf.format(new Date());
        return currentDateAndTime;
    }

    ServiceConnectListener FaceControlConnect = new ServiceConnectListener() {
        @Override
        public void onConnectChanged(ComponentName componentName, boolean b) {
            Log.d(TAG, "faceService onbind : " + b);
            mRobotAPI.UnityFaceManager().registerCallback(mUnityFaceCallback);
        }
    };

    RobotEventListener robotEventListener = new RobotEventListener() {
        @Override
        public void onWikiServiceStart() {
            Log.d(TAG, "onWikiServiceStart, robot ready to be control");
            mRobotAPI.registerVoiceEventListener(voiceEventListener);
            mRobotAPI.initFaceControl(mContext, mContext.getClass().getName(), FaceControlConnect);
            mRobotAPI.requestSensor(NuwaRobotAPI.SENSOR_TOUCH | NuwaRobotAPI.SENSOR_PIR | NuwaRobotAPI.SENSOR_DROP );
            // Apply TTS settings after the Nuwa service is ready so the parameters stick.
            mSDKinit = true;
            applyTtsSettings();
            suppressNativeWakeupMode();
        }

        @Override public void onWikiServiceStop() {}
        @Override public void onWikiServiceCrash() {}
        @Override public void onWikiServiceRecovery() {}
        @Override public void onStartOfMotionPlay(String s) {}
        @Override public void onPauseOfMotionPlay(String s) {}
        @Override public void onStopOfMotionPlay(String s) {}
        @Override
        public void onCompleteOfMotionPlay(String s) {
            if (!WAITING_GESTURE_MOTION.equals(s)) {
                return;
            }
            if (!shouldLoopWaitingGesture()) {
                return;
            }
            runOnUiThread(() -> playWaitingGesture());
        }
        @Override public void onPlayBackOfMotionPlay(String s) {}
        @Override public void onErrorOfMotionPlay(int i) {}
        @Override public void onPrepareMotion(boolean b, String s, float v) {}
        @Override public void onCameraOfMotionPlay(String s) {}
        @Override public void onGetCameraPose(float v, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8, float v9, float v10, float v11) {}

        @Override
        public void onTouchEvent(int type, int touch) {
            Log.d(TAG,"onTouchEvent type="+type+" touch="+touch);
            switch(type) {
                case 2:
                    runOnUiThread(() -> {
                        long now = System.currentTimeMillis();
                        if (now - lastBellyToggleAt < BELLY_TOGGLE_COOLDOWN_MS) {
                            Log.d(TAG, "Ignore belly toggle during cooldown");
                            return;
                        }
                        lastBellyToggleAt = now;

                        if (!isRobotSpeaking && !overlayShowing) {
                            if (isSttActive) {
                                pauseAppSpeechToText();
                            } else {
                                startAppSpeechToText();
                            }
                        }
                    });
                    break;
                default:
                    break;
            }
        }

        @Override public void onPIREvent(int i) {}
        @Override public void onTap(int i) {}
        @Override public void onLongPress(int i) {}
        @Override public void onWindowSurfaceReady() {}
        @Override public void onWindowSurfaceDestroy() {}
        @Override public void onTouchEyes(int i, int i1) {}
        @Override public void onRawTouch(int i, int i1, int i2) {}
        @Override public void onFaceSpeaker(float v) {}
        @Override public void onActionEvent(int i, int i1) {}
        @Override public void onDropSensorEvent(int i) {}
        @Override public void onMotorErrorEvent(int i, int i1) {}
    };

    private void hideface() {
        if (mRobotAPI != null) {
            mRobotAPI.UnityFaceManager().hideUnity();
            previousResponseId = null;
            ThreadID = "";
            mRobotAPI.stopSensor(NuwaRobotAPI.SENSOR_TOUCH | NuwaRobotAPI.SENSOR_PIR | NuwaRobotAPI.SENSOR_DROP);
            onDestroy();
            finish();
        } else {
            Log.d(TAG, " === mNuwaRobotAPI null ===  please init");
        }
    }

    private void showface() {
        if (mRobotAPI != null) {
            mRobotAPI.UnityFaceManager().showUnity();
        } else {
            Log.d(TAG, " === mNuwaRobotAPI null ===  please init");
        }
    }

    private void safeHideUnity() {
        try {
            if (mRobotAPI != null && mSDKinit) {
                mRobotAPI.UnityFaceManager().hideUnity();
            }
        } catch (Exception e) {
            Log.e(TAG, "safeHideUnity failed: " + e.getMessage());
        }
    }

    private void safeShowUnity() {
        try {
            if (mRobotAPI != null && mSDKinit) {
                mRobotAPI.UnityFaceManager().showUnity();
            }
        } catch (Exception e) {
            Log.e(TAG, "safeShowUnity failed: " + e.getMessage());
        }
    }

    private void suppressNativeWakeupMode() {
        if (mRobotAPI == null || nativeWakeupSuppressed) return;

        try {
            mRobotAPI.controlAlwaysWakeup(false);
            mRobotAPI.disableSystemAlertUI();
            mRobotAPI.stopListeninAlwaysWakeup();
            mRobotAPI.stopListen();
            nativeWakeupSuppressed = true;
            Log.d(TAG, "Native Kebbi wakeup mode suppressed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to suppress native wakeup mode: " + e.getMessage());
        }
    }

    private void restoreNativeWakeupMode() {
        if (mRobotAPI == null || !nativeWakeupSuppressed) return;

        try {
            mRobotAPI.controlAlwaysWakeup(true);
            mRobotAPI.enableSystemAlertUI();
            nativeWakeupSuppressed = false;
            Log.d(TAG, "Native Kebbi wakeup mode restored");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore native wakeup mode: " + e.getMessage());
        }
    }

    private void startAppSpeechToText() {
        if (mRobotAPI == null) return;

        try {
            suppressNativeWakeupMode();
            mRobotAPI.stopListeninAlwaysWakeup();
            mRobotAPI.stopListen();
            isManualListeningPaused = false;
            ignoreNextSttCompletion = false;
            isSttActive = true;
            Status = "listening";
            playWaitingGesture();
            mRobotAPI.startSpeech2Text(false);
        } catch (Exception e) {
            isSttActive = false;
            Log.e(TAG, "Failed to start app speech-to-text: " + e.getMessage());
        }
    }

    private void pauseAppSpeechToText() {
        if (mRobotAPI == null) return;

        try {
            ignoreNextSttCompletion = true;
            isManualListeningPaused = true;
            isSttActive = false;
            Status = "paused";
            mRobotAPI.stopListen();
            Log.d(TAG, "Speech-to-text paused manually");
        } catch (Exception e) {
            Log.e(TAG, "Failed to pause app speech-to-text: " + e.getMessage());
        }
    }

    private void forwardUserTextToAssistant(String question) {
        if (question == null) return;

        String normalized = question.trim();
        if (normalized.isEmpty()) return;

        if (resolvedUserId != null) {
            saveMessageToMySQL(resolvedUserId, "user", normalized);
        }
        requestResponse(normalized);
    }

    private void applyTtsSettings() {
        if (mRobotAPI == null) return;

        mRobotAPI.setSpeakParameter(
                VoiceEventListener.SpeakType.NORMAL,
                "isForced",
                "true"
        );
        mRobotAPI.setSpeakParameter(
                VoiceEventListener.SpeakType.NORMAL,
                "speed",
                TTS_SPEED
        );
    }

    private void playWaitingGesture() {
        if (mRobotAPI == null || !mSDKinit) return;

        try {
            mRobotAPI.motionPlay(WAITING_GESTURE_MOTION, false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to play waiting gesture: " + e.getMessage());
        }
    }

    private boolean shouldLoopWaitingGesture() {
        if (overlayShowing || isManualListeningPaused) {
            return false;
        }
        return "waiting".equals(Status) || ("listening".equals(Status) && isSttActive);
    }

    private void mouthOn(String tts, long speed) {
        GlobalVariable globalVariable = ((GlobalVariable)getApplicationContext());
        String Motion;
        int Mouth;
        String Emoji;
        if (mRobotAPI != null) {
            isRobotSpeaking = true;
            isSttActive = false;
            isManualListeningPaused = false;
            Status = "speaking";
            mRobotAPI.stopListen();
            mRobotAPI.stopTTS();
            applyTtsSettings();
            mRobotAPI.startTTS(tts);
            Mouth = globalVariable.SelectMouth(globalVariable.MouthRanNum());
            Emoji = globalVariable.SelectEmoji(globalVariable.FaceRanNum());
            mRobotAPI.UnityFaceManager().mouthEmotionOn(speed, Mouth);
            mRobotAPI.UnityFaceManager().playFaceAnimation(Emoji);
            Motion = globalVariable.SelectMotion(globalVariable.RanNum());
            Log.d(TAG,"Motion: " + Motion);
            mRobotAPI.motionPlay(Motion,false);
        } else {
            Log.d(TAG, " === mNuwaRobotAPI null ===  please init");
        }
    }

    private void speakOnly(String tts) {
        if (mRobotAPI == null) return;

        isRobotSpeaking = true;
        isSttActive = false;
        isManualListeningPaused = false;
        Status = "speaking";
        mRobotAPI.stopListen();
        mRobotAPI.stopTTS();
        applyTtsSettings();
        mRobotAPI.startTTS(tts);
    }


    private void mouthOff() {
        if (mRobotAPI != null) {
            isRobotSpeaking = false;
            mRobotAPI.UnityFaceManager().playFaceAnimationDefault();
            mRobotAPI.UnityFaceManager().mouthOff();
            mRobotAPI.stopTTS();
        } else {
            Log.d(TAG, " === mNuwaRobotAPI null ===  please init");
        }
    }

    VoiceEventListener voiceEventListener = new VoiceEventListener() {
        @Override
        public void onWakeup(boolean isError, String score, float direction) {
            suppressNativeWakeupMode();
        }

        @Override
        public void onTTSComplete(boolean isError) {
            runOnUiThread(() -> {
                mouthOff();
                Status = "waiting";
                playWaitingGesture();

                if (overlayShowing) {
                    pendingStartStt = true;   // Restart STT after the overlay closes.
                } else {
                    startAppSpeechToText();
                }
            });
        }


        @Override public void onSpeechRecognizeComplete(boolean isError, ResultType iFlyResult, String json) {}

        @Override
        public void onSpeech2TextComplete(boolean isError, String json) {
            isSttActive = false;
            if (ignoreNextSttCompletion) {
                ignoreNextSttCompletion = false;
                Log.d(TAG, "Ignored speech completion after manual pause");
                return;
            }
            String raw = json == null ? "" : json.trim();
            if (raw.isEmpty()) {
                Log.w(TAG, "Speech2Text returned empty payload");
                return;
            }

            if (raw.contains(STT_TIMEOUT_TOKEN)) {
                Log.w(TAG, "Speech2Text timeout; sending fallback prompt");
                forwardUserTextToAssistant(STT_TIMEOUT_FALLBACK);
                return;
            }

            String question;
            try {
                question = VoiceResultJsonParser.parseVoiceResult(raw);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse speech result", e);
                return;
            }

            if ("\u7d50\u675f\u5c0d\u8a71".equals(question)) {
                hideface();
            } else if (!question.isEmpty()) {
                forwardUserTextToAssistant(question);
            }
        }

        @Override public void onMixUnderstandComplete(boolean isError, ResultType resultType, String s) {}
        @Override public void onSpeechState(ListenType listenType, SpeechState speechState) {}
        @Override public void onSpeakState(SpeakType speakType, SpeakState speakState) {}
        @Override public void onGrammarState(boolean isError, String s) {}
        @Override public void onListenVolumeChanged(ListenType listenType, int i) {}
        @Override
        public void onHotwordChange(HotwordState hotwordState, HotwordType hotwordType, String s) {
            Log.w(TAG, "Hotword event intercepted: state=" + hotwordState + ", type=" + hotwordType);
            suppressNativeWakeupMode();

            if ("waiting".equals(Status) && !overlayShowing && !isManualListeningPaused) {
                runOnUiThread(() -> startAppSpeechToText());
            }
        }
    };

    UnityFaceCallback mUnityFaceCallback = new UnityFaceCallback(){
        @Override public void on_touch_left_eye()  { Log.d("FaceControl", "on_touch_left_eye()"); }
        @Override public void on_touch_right_eye() { Log.d("FaceControl", "on_touch_right_eye()"); }
        @Override public void on_touch_nose()      { Log.d("FaceControl", "on_touch_nose()"); }
        @Override public void on_touch_mouth()     { Log.d("FaceControl", "on_touch_mouth()"); }
        @Override public void on_touch_head()      { Log.d("FaceControl", "on_touch_head()"); }
        @Override public void on_touch_left_edge() { Log.d("FaceControl", "on_touch_left_edge()"); }
        @Override public void on_touch_right_edge(){ Log.d("FaceControl", "on_touch_right_edge()"); }
        @Override public void on_touch_bottom()    { Log.d("FaceControl", "on_touch_bottom()"); }
    };
}
