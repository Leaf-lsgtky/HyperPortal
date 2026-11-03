// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.animation.ValueAnimator;
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
    private boolean mRegionDark;
    private boolean mHasRegionSample;
    private TransferDockRegionSampler mRegionSampler;
    private ValueAnimator mColorAnimator;

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
        if (!mHasRegionSample) {
            // Fallback until the compositor sample arrives.
            setBarColors(mNight ? 0x33C7C7C7 : 0x33191919,
                    mNight ? 0xCCFFFFFF : 0xFF979797, false);
        }
    }

    private void setBarColors(int fill, int stroke, boolean animate) {
        if (mColorAnimator != null) {
            mColorAnimator.cancel();
        }
        if (!animate) {
            mPaint.setColor(fill);
            mStrokePaint.setColor(stroke);
            invalidate();
            return;
        }
        final int oldFill = mPaint.getColor();
        final int oldStroke = mStrokePaint.getColor();
        mColorAnimator = ValueAnimator.ofFloat(0f, 1f);
        mColorAnimator.setDuration(180);
        mColorAnimator.addUpdateListener(a -> {
            float f = a.getAnimatedFraction();
            mPaint.setColor(blend(oldFill, fill, f));
            mStrokePaint.setColor(blend(oldStroke, stroke, f));
            invalidate();
        });
        mColorAnimator.start();
    }

    private static int blend(int from, int to, float fraction) {
        int a = (int) (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * fraction);
        int r = (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * fraction);
        int g = (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * fraction);
        int b = (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * fraction);
        return Color.argb(a, r, g, b);
    }

    private void onRegionLuma(float luma) {
        // Same threshold as MIUI security-center RegionSamplingHelper (0.76f).
        boolean dark = luma < 0.76f;
        if (mHasRegionSample && dark == mRegionDark) {
            return;
        }
        mHasRegionSample = true;
        mRegionDark = dark;
        setBarColors(dark ? 0x33C7C7C7 : 0x33191919,
                dark ? 0xCCFFFFFF : 0xFF979797, true);
    }

    public void refreshTheme() {
        applyTheme();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // The MIUI Consumer-based composition listener lives in SystemUI.
        if ("com.android.systemui".equals(getContext().getPackageName())) {
            if (mRegionSampler == null) {
                mRegionSampler = new TransferDockRegionSampler(this, mDensity, this::onRegionLuma);
            }
            mRegionSampler.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mRegionSampler != null) {
            mRegionSampler.stop();
            mRegionSampler = null;
        }
        if (mColorAnimator != null) {
            mColorAnimator.cancel();
            mColorAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mRegionSampler != null) {
            mRegionSampler.updateBounds();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mRegionSampler != null) {
            mRegionSampler.updateBounds();
        }
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
