package com.example.assistant;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import com.example.assistant.data.model.User;

public class AchievementBoardActivity extends AppCompatActivity {

    private static final int TOTAL_SLOTS = 48;
    private static final int COLS = 8;
    private static final int ROWS = 6;

    private GridLayout gridStamps;
    private TextView tvProgress;
    private ImageButton btnBack;

    private AchievementManager achievementManager;
    private int userId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievement_board);

        gridStamps = findViewById(R.id.gridStamps);
        tvProgress = findViewById(R.id.tvProgress);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        achievementManager = new AchievementManager(this);

        User u = ((GlobalVariable) getApplication()).getCurrentUser();
        if (u == null || u.id <= 0) {
            Log.e(TAG, "Current user is null or invalid.");
            finish();
            return;
        }
        userId = u.id;

        // 先用快取畫面，避免空白
        int cached = achievementManager.getCachedStampCount(userId);
        cached = clamp(cached);
        updateUI(cached);

        // 再從 MySQL 拉最新
        refreshFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFromServer();
    }

    private void refreshFromServer() {
        if (achievementManager == null || userId <= 0) return;

        achievementManager.fetchStampCount(userId, new AchievementManager.IntCallback() {
            @Override
            public void onSuccess(int value) {
                int count = clamp(value);
                runOnUiThread(() -> updateUI(count));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "fetchStampCount error: " + message);
                // 失敗就維持快取畫面，不要讓 UI 消失
            }
        });
    }

    private void updateUI(int completedCount) {
        if (tvProgress != null) {
            tvProgress.setText("進度：" + completedCount + " / " + TOTAL_SLOTS);
        }
        if (gridStamps == null) return;
        if (gridStamps.getWidth() == 0 || gridStamps.getHeight() == 0) {
            gridStamps.post(() -> buildStampGrid(completedCount));
            return;
        }
        buildStampGrid(completedCount);
    }

    private void buildStampGrid(int completedCount) {
        if (gridStamps == null) return;

        gridStamps.removeAllViews();
        gridStamps.setColumnCount(COLS);
        gridStamps.setRowCount(ROWS);

        int gap = dpToPx(4);
        int[] cellSize = calcCellSizePx(gap);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            View slot = inflater.inflate(R.layout.item_stamp_slot, gridStamps, false);
            ImageView ivStamp = slot.findViewById(R.id.ivStamp);

            boolean unlocked = i < completedCount;

            if (unlocked) {
                int iconRes = achievementManager.getStampIcon(userId, i);
                ivStamp.setImageResource(iconRes);
                ivStamp.setAlpha(1.0f);
            } else {
                ivStamp.setAlpha(0.0f);
            }

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cellSize[0];
            lp.height = cellSize[1];

            int row = i / COLS;
            int col = i % COLS;
            lp.rowSpec = GridLayout.spec(row, 1);
            lp.columnSpec = GridLayout.spec(col, 1);

            lp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);

            slot.setLayoutParams(lp);
            gridStamps.addView(slot);
        }
    }

    private int[] calcCellSizePx(int gap) {
        int availableWidth = gridStamps.getWidth() - gridStamps.getPaddingLeft() - gridStamps.getPaddingRight();
        int availableHeight = gridStamps.getHeight() - gridStamps.getPaddingTop() - gridStamps.getPaddingBottom();

        int horizontalGaps = gap * (COLS - 1);
        int verticalGaps = gap * (ROWS - 1);

        int cellWidth = (availableWidth - horizontalGaps) / COLS;
        int cellHeight = (availableHeight - verticalGaps) / ROWS;

        int minCell = dpToPx(24);
        if (cellWidth < minCell) cellWidth = minCell;
        if (cellHeight < minCell) cellHeight = minCell;

        return new int[]{cellWidth, cellHeight};
    }

    private int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }

    private int clamp(int v) {
        if (v < 0) return 0;
        if (v > TOTAL_SLOTS) return TOTAL_SLOTS;
        return v;
    }
}
