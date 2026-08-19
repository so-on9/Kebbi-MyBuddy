package com.example.assistant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.VoiceEventListener;

public class SlideActivity extends AppCompatActivity {

    private static final String TAG = "SlideActivity";

    private ImageView slideImage;
    private TextView prevBtn, nextBtn, skipBtn;

    private Context mContext;
    private IClientId mClientId;
    private NuwaRobotAPI mRobotAPI;

    private boolean mSDKinit = false;
    private int currentIndex = 0;

    private final int[] slides = {
            R.drawable.slide1,
            R.drawable.slide2,
            R.drawable.slide3,
            R.drawable.slide4,
            R.drawable.slide5,
            R.drawable.slide6,
            R.drawable.slide7,
            R.drawable.slide8,
            R.drawable.slide9,
            R.drawable.slide10,
            R.drawable.slide11,
            R.drawable.slide12
    };

    private boolean autoNextEnabled = true;
    private boolean isLeavingSlide = false;
    private long lastManualNavigationTime = 0L;
    private long lastButtonClickTime = 0L;
    private boolean nativeWakeupSuppressed = false;
    private static final String TTS_SPEED = "80";
    private static final long BUTTON_COOLDOWN_MS = 800;

    // 每次開始新的朗讀就 +1
    private int speechToken = 0;

    // 記錄目前這次朗讀的 token
    private int activeSpeechToken = 0;

    // 記錄這次朗讀開始時是第幾頁
    private int activeSpeechSlideIndex = -1;

    private final String[] scripts = {
            "等一下我們要開始上課了，但你可能還不太清楚等一下要做什麼對嗎？沒關係，今天的課很特別，我們會一邊上課一邊跟機器人聊天！所以我們先一起來看這份「和機器人一起玩的說明書」，這樣就可以知道等一下上課要做什麼喔！",
            "這位是國立臺東大學特殊教育學系的珮如老師。她設計了一個有趣的聊天遊戲，因為我們正在找國小的小朋友來幫忙玩玩看這個遊戲，所以就找到了你，請你來幫助我們。\n" +
                    "\n" +
                    "等一下的遊戲中，你需要和這隻凱比機器人互動，跟它說說話。\n" +
                    "\n" +
                    "結束後，我們會問你一些小問題，這樣有聽懂嗎?",
            "這些是今天會跟你一起上課、教你怎麼跟機器人玩遊戲的老師。",
            "剛剛前面有講到，上完課之後老師會問一些小問題，那你知道老師會為什麼問題嗎？",
            "像是我會問你喜不喜歡機器人聊天遊戲的樣子，或是你覺得剛剛的聊天遊戲好不好用\n" +
                    "所以等你上課完之後可以想一想再回答老師的這些問題",
            "這個圖的意思就是我們會保密，等一下你跟我聊的事情，我跟老師都不會告訴別人，所以你可以放心的跟我聊天",
            "這個人手打叉叉，意思就是你可以不回答問題，如果我問了一個問題，但是你不想要回答這個問題，可以跟老師說，我、老師跟爸爸媽媽都不會生氣或是處罰你",
            "那等一下上課跟機器人玩遊戲的時候要注意哪些事情呢？",
            "第一個，你先看一下現在我的臉頰是什麼顏色的？對，是藍色的，\n" +
                    "\n" +
                    "所以如果凱比像圖片中一樣，臉頰亮黃色，才代表他有在聽你說話唷，那如果等一下凱比在說話、他的臉頰亮藍色，你說話他聽得到嗎？對，聽不到，所以你要等他說完話，他的臉頰就會開始亮黃色，這時候就是換你講話了",
            "第二個玩遊戲的過程中，請把雙手放在桌面上，因為如果一直摸我的手，或是身體，我就沒有辦法專心聽你說話了",
            "第三個，如果我的回應太難的話，要勇敢說出來，你可以說「我聽不懂」或是「我不知道」，因為你如果只有搖搖頭，沒有說話，我不知道你怎麼了，所以要記得說出來，你可以試著說說看「我聽不懂」嗎?再試著說說看「我不知道」",
            "最後一個，當你和我講完話，我會需要一點時間想一下你的回答，請你等我一下下唷！"
    };
    private void goToMainActivity() {
        isLeavingSlide = true;
        autoNextEnabled = false;

        Intent intent = new Intent(SlideActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private final VoiceEventListener voiceEventListener = new VoiceEventListener() {
        @Override
        public void onWakeup(boolean b, String s, float v) {
            suppressNativeWakeupMode();
        }

        @Override
        public void onTTSComplete(boolean b) {
            Log.d(TAG, "onTTSComplete: " + b
                    + ", currentIndex=" + currentIndex
                    + ", activeSpeechSlideIndex=" + activeSpeechSlideIndex
                    + ", activeSpeechToken=" + activeSpeechToken
                    + ", speechToken=" + speechToken
                    + ", isLeavingSlide=" + isLeavingSlide);

            if (isLeavingSlide) {
                Log.d(TAG, "Ignore TTS complete because activity is leaving");
                return;
            }

            final int completedToken = activeSpeechToken;
            final int completedSlide = activeSpeechSlideIndex;

            runOnUiThread(() -> {
                if (isLeavingSlide) {
                    return;
                }

                if (!autoNextEnabled) {
                    return;
                }

                long now = System.currentTimeMillis();

                if (now - lastManualNavigationTime < 1200) {
                    Log.d(TAG, "Ignore auto next because user just navigated manually");
                    return;
                }

                if (completedToken != speechToken) {
                    Log.d(TAG, "Ignore old TTS complete event");
                    return;
                }

                if (completedSlide != currentIndex) {
                    Log.d(TAG, "Ignore TTS complete because slide changed");
                    return;
                }

                goNextPage();
            });
        }

        @Override
        public void onSpeechRecognizeComplete(boolean b, ResultType resultType, String s) {
        }

        @Override
        public void onSpeech2TextComplete(boolean b, String s) {
        }

        @Override
        public void onMixUnderstandComplete(boolean b, ResultType resultType, String s) {
        }

        @Override
        public void onSpeechState(ListenType listenType, SpeechState speechState) {
        }

        @Override
        public void onSpeakState(SpeakType speakType, SpeakState speakState) {
            Log.d(TAG, "onSpeakState: " + speakState);
        }

        @Override
        public void onGrammarState(boolean b, String s) {

        }

        @Override
        public void onListenVolumeChanged(ListenType listenType, int i) {

        }

        @Override
        public void onHotwordChange(HotwordState hotwordState, HotwordType hotwordType, String s) {

        }
    };

    RobotEventListener robotEventListener = new RobotEventListener() {
        @Override
        public void onWikiServiceStart() {
            Log.d(TAG, "onWikiServiceStart, robot ready to be control");

            mRobotAPI.registerVoiceEventListener(voiceEventListener);
            mSDKinit = true;
            applyTtsSettings();
            suppressNativeWakeupMode();

            runOnUiThread(() -> {
                updateSlide();
                speakCurrentSlide();
            });
        }

        @Override public void onWikiServiceStop() { mSDKinit = false; }
        @Override public void onWikiServiceCrash() { mSDKinit = false; }
        @Override public void onWikiServiceRecovery() {}
        @Override public void onStartOfMotionPlay(String s) {}
        @Override public void onPauseOfMotionPlay(String s) {}
        @Override public void onStopOfMotionPlay(String s) {}
        @Override public void onCompleteOfMotionPlay(String s) {}
        @Override public void onPlayBackOfMotionPlay(String s) {}
        @Override public void onErrorOfMotionPlay(int i) {}
        @Override public void onPrepareMotion(boolean b, String s, float v) {}
        @Override public void onCameraOfMotionPlay(String s) {}

        @Override
        public void onGetCameraPose(float v, float v1, float v2, float v3, float v4, float v5,
                                    float v6, float v7, float v8, float v9, float v10, float v11) {
        }

        @Override public void onTouchEvent(int type, int touch) {}
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mContext = this;

        slideImage = findViewById(R.id.slideImage);
        prevBtn = findViewById(R.id.prevBtn);
        nextBtn = findViewById(R.id.nextBtn);
        skipBtn = findViewById(R.id.skipBtn);

        updateSlide();

        mClientId = new IClientId(getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);
        mRobotAPI.registerRobotEventListener(robotEventListener);


        nextBtn.setOnClickListener(v -> {
            if (isButtonCooldownActive()) {
                return;
            }

            invalidateCurrentSpeech();
            lastManualNavigationTime = System.currentTimeMillis();

            if (mRobotAPI != null) {
                mRobotAPI.stopTTS();
            }

            goNextPage();
        });

        prevBtn.setOnClickListener(v -> {
            if (isButtonCooldownActive()) {
                return;
            }

            if (currentIndex > 0) {
                invalidateCurrentSpeech();
                lastManualNavigationTime = System.currentTimeMillis();

                if (mRobotAPI != null) {
                    mRobotAPI.stopTTS();
                }

                currentIndex--;
                updateSlide();
                speakCurrentSlide();
            }
        });
        skipBtn.setOnClickListener(v -> {
            isLeavingSlide = true;
            autoNextEnabled = false;
            invalidateCurrentSpeech();

            if (mRobotAPI != null) {
                mRobotAPI.stopTTS();
            }

            goToMainActivity();
        });
    }

    private void updateSlide() {
        if (currentIndex < 0 || currentIndex >= slides.length) {
            Log.e(TAG, "slides index out of range: " + currentIndex);
            return;
        }
        slideImage.setImageResource(slides[currentIndex]);
    }

    private void speakCurrentSlide() {
        if (!mSDKinit || mRobotAPI == null) {
            Log.d(TAG, "SDK not ready yet");
            return;
        }

        if (currentIndex < 0 || currentIndex >= scripts.length) {
            Log.e(TAG, "scripts index out of range: " + currentIndex);
            return;
        }

        speechToken++;
        activeSpeechToken = speechToken;
        activeSpeechSlideIndex = currentIndex;

        mouthOn(scripts[currentIndex], 3000);
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

    private void mouthOn(String tts, long speed) {
        if (mRobotAPI != null) {
            mRobotAPI.stopTTS();
            applyTtsSettings();
            mRobotAPI.startTTS(tts);
        } else {
            Log.d(TAG, "mRobotAPI null, please init");
        }
    }

    private void invalidateCurrentSpeech() {
        speechToken++;
        activeSpeechToken = speechToken;
        activeSpeechSlideIndex = -1;
    }

    private boolean isButtonCooldownActive() {
        long now = System.currentTimeMillis();
        if (now - lastButtonClickTime < BUTTON_COOLDOWN_MS) {
            Log.d(TAG, "Button cooldown active");
            return true;
        }
        lastButtonClickTime = now;
        return false;
    }

    private void goNextPage() {
        if (isLeavingSlide) {
            return;
        }

        if (currentIndex < slides.length - 1) {
            currentIndex++;
            updateSlide();
            speakCurrentSlide();
        } else {
            if (mRobotAPI != null) {
                mRobotAPI.stopTTS();
            }
            goToMainActivity();
        }
    }

    private void suppressNativeWakeupMode() {
        if (mRobotAPI == null || nativeWakeupSuppressed) return;

        try {
            mRobotAPI.controlAlwaysWakeup(false);
            mRobotAPI.stopListeninAlwaysWakeup();
            mRobotAPI.stopListen();
            nativeWakeupSuppressed = true;
            Log.d(TAG, "Native Kebbi wakeup mode suppressed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to suppress native wakeup mode", e);
        }
    }

    private void restoreNativeWakeupMode() {
        if (mRobotAPI == null || !nativeWakeupSuppressed) return;

        try {
            mRobotAPI.controlAlwaysWakeup(true);
            nativeWakeupSuppressed = false;
            Log.d(TAG, "Native Kebbi wakeup mode restored");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore native wakeup mode", e);
        }
    }

    @Override
    protected void onDestroy() {
        restoreNativeWakeupMode();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mRobotAPI != null) {
            try {
                mRobotAPI.stopTTS();
                mRobotAPI.release();
            } catch (Exception e) {
                Log.e(TAG, "release error", e);
            }
        }
        super.onDestroy();
    }
}
