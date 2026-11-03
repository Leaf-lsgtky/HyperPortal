// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;

/**
 * 100% ColorOS Smart Sidebar FloatBarView (Edge Handle).
 *
 * Appears on the screen edge when TransferDock contains items.
 * Dimensions:
 * - Visual bar: 5dp width, 52dp height, 2.5dp corner radius
 * - Touch hot zone: 20dp width, 68dp height (easy to touch while visually minimalist)
 * - ColorOS frosted glass: #992C2C2E fill, #33FFFFFF stroke
 */
public class TransferDockFloatBarView extends View {

    public interface OnHandleClickListener {
        void onHandleClick();
    }

    private final float mDensity;
    private final int mTouchSlop;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBarRect = new RectF();

    private OnHandleClickListener mClickListener;
    private float mTouchDownX;
    private float mTouchDownY;
    private boolean mMoved = false;
    private boolean mNight;

    public TransferDockFloatBarView(@NonNull Context context) {
        super(context);
        mDensity = context.getResources().getDisplayMetrics().density;
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mNight = TransferDockTheme.isNight(context);
        init();
    }

    private int dp(float value) {
        return (int) (value * mDensity + 0.5f);
    }

    private void init() {
        mPaint.setStyle(Paint.Style.FILL);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(dp(0.8f));
        applyTheme();
    }

    private void applyTheme() {
        mNight = TransferDockTheme.isNight(getContext());
        if (mNight) {
            mPaint.setColor(Color.parseColor("#992C2C2E"));
            mStrokePaint.setColor(Color.parseColor("#33FFFFFF"));
        } else {
            mPaint.setColor(Color.parseColor("#66000000"));
            mStrokePaint.setColor(Color.parseColor("#1A000000"));
        }
        invalidate();
    }

    public void refreshTheme() {
        applyTheme();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyTheme();
    }

    public void setOnHandleClickListener(OnHandleClickListener listener) {
        this.mClickListener = listener;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // Visual bar is right-aligned to screen edge: 5dp wide, 52dp high
        float barW = dp(5);
        float barH = dp(52);
        float left = w - barW;
        float top = (h - barH) / 2f;
        float right = w;
        float bottom = top + barH;
        float radius = dp(2.5f);

        mBarRect.set(left, top, right, bottom);
        canvas.drawRoundRect(mBarRect, radius, radius, mPaint);
        canvas.drawRoundRect(mBarRect, radius, radius, mStrokePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getRawX();
                mTouchDownY = event.getRawY();
                mMoved = false;
                setAlpha(0.6f);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - mTouchDownX;
                float dy = event.getRawY() - mTouchDownY;
                if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
                    mMoved = true;
                    // Inward swipe (to the left from right edge)
                    if (dx < -dp(10)) {
                        setAlpha(1.0f);
                        triggerClick();
                        return true;
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                setAlpha(1.0f);
                if (!mMoved) {
                    triggerClick();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                setAlpha(1.0f);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void triggerClick() {
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
        if (mClickListener != null) {
            mClickListener.onHandleClick();
        }
    }
}
