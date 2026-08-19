package com.example.assistant.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AchievementBoardView extends View {

    private static final int TOTAL_CELLS = 21;

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<RectF> cells = new ArrayList<>(TOTAL_CELLS);

    private float boardInsetPx;
    private float gapPx;
    private float cornerRadiusPx;

    private int progress = 0;

    public AchievementBoardView(Context context) {
        super(context);
        init();
    }

    public AchievementBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AchievementBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        boardInsetPx = dp(4);     // 更外擴：越小越貼近邊緣
        gapPx = dp(16);          // 更鬆散：越大格子越分開
        cornerRadiusPx = dp(14);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2));
        strokePaint.setColor(0x80FFFFFF);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0x00000000);

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(sp(12));
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(dp(3), 0, dp(1), 0xB0000000);
    }

    public void setProgress(int value) {
        if (value < 0) value = 0;
        if (value > TOTAL_CELLS) value = TOTAL_CELLS;
        progress = value;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildCells(w, h);
    }

    private void buildCells(int w, int h) {
        cells.clear();

        float size = Math.min(w, h);
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;

        RectF outer = new RectF(
                left + boardInsetPx,
                top + boardInsetPx,
                left + size - boardInsetPx,
                top + size - boardInsetPx
        );

        int topInner = 5;
        int rightInner = 4;
        int bottomInner = 4;
        int leftInner = 4;

        float corner = outer.width() * 0.18f;

        // 格子厚度不要跟 corner 一樣大，縮薄會更鬆散
        float thickness = corner * 0.72f;

        float topAvail = outer.width() - corner * 2f - gapPx * (topInner + 1);
        float topLen = topAvail / topInner;

        float rightAvail = outer.height() - corner * 2f - gapPx * (rightInner + 1);
        float rightLen = rightAvail / rightInner;

        // 0: 左上角 HOME
        cells.add(new RectF(
                outer.left,
                outer.top,
                outer.left + corner,
                outer.top + corner
        ));

        // 1..5: 上邊
        for (int i = 0; i < topInner; i++) {
            float x1 = outer.left + corner + gapPx + i * (topLen + gapPx);
            float y1 = outer.top;
            cells.add(new RectF(
                    x1, y1,
                    x1 + topLen, y1 + thickness
            ));
        }

        // 6: 右上角
        cells.add(new RectF(
                outer.right - corner,
                outer.top,
                outer.right,
                outer.top + corner
        ));

        // 7..10: 右邊
        for (int i = 0; i < rightInner; i++) {
            float x1 = outer.right - thickness;
            float y1 = outer.top + corner + gapPx + i * (rightLen + gapPx);
            cells.add(new RectF(
                    x1, y1,
                    x1 + thickness, y1 + rightLen
            ));
        }

        // 11: 右下角
        cells.add(new RectF(
                outer.right - corner,
                outer.bottom - corner,
                outer.right,
                outer.bottom
        ));

        float bottomAvail = outer.width() - corner * 2f - gapPx * (bottomInner + 1);
        float bottomLen = bottomAvail / bottomInner;

        // 12..15: 下邊（由右往左）
        for (int i = 0; i < bottomInner; i++) {
            float x2 = outer.right - corner - gapPx - i * (bottomLen + gapPx);
            float y1 = outer.bottom - thickness;
            cells.add(new RectF(
                    x2 - bottomLen, y1,
                    x2, y1 + thickness
            ));
        }

        // 16: 左下角
        cells.add(new RectF(
                outer.left,
                outer.bottom - corner,
                outer.left + corner,
                outer.bottom
        ));

        float leftAvail = outer.height() - corner * 2f - gapPx * (leftInner + 1);
        float leftLen = leftAvail / leftInner;

        // 17..20: 左邊（由下往上）
        for (int i = 0; i < leftInner; i++) {
            float x1 = outer.left;
            float y2 = outer.bottom - corner - gapPx - i * (leftLen + gapPx);
            cells.add(new RectF(
                    x1, y2 - leftLen,
                    x1 + thickness, y2
            ));
        }

        while (cells.size() < TOTAL_CELLS) {
            cells.add(new RectF(0, 0, 0, 0));
        }
        if (cells.size() > TOTAL_CELLS) {
            cells.subList(TOTAL_CELLS, cells.size()).clear();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < cells.size(); i++) {
            RectF r = cells.get(i);
            if (r.width() <= 0 || r.height() <= 0) continue;

            if (i < progress) {
                fillPaint.setColor(0x2200FFFF);
                canvas.drawRoundRect(r, cornerRadiusPx, cornerRadiusPx, fillPaint);
            }

            canvas.drawRoundRect(r, cornerRadiusPx, cornerRadiusPx, strokePaint);

            if (i == 0) {
                float cx = r.centerX();
                float cy = r.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
                canvas.drawText("HOME", cx, cy, textPaint);
            }
        }
    }

    private float dp(float v) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()
        );
    }

    private float sp(float v) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics()
        );
    }
}
