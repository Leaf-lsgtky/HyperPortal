// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 100% ColorOS TransferDockStackView implementation.
 * Renders stacked cards with rounded corners (11dp) and layer offsets (4dp)
 * behind the primary thumbnail.
 */
public class TransferDockStackView extends FrameLayout {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final RectF mRectF = new RectF();

    private float mDensity;
    private float mRadius;
    private float mLayerDistance;

    private int mLayerBgColor;
    private int mLayerSecondBgColor;
    private int mLayerShadowColor;

    private int mCount = 1;
    private Bitmap mTopBitmap;
    private View mTopCustomView;

    public TransferDockStackView(@NonNull Context context) {
        super(context);
        init();
    }

    public TransferDockStackView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mDensity = getResources().getDisplayMetrics().density;
        mRadius = 11f * mDensity;
        mLayerDistance = 4f * mDensity;

        boolean isDark = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDark) {
            mLayerBgColor = Color.parseColor("#383838");
            mLayerSecondBgColor = Color.parseColor("#333333");
            mLayerShadowColor = Color.parseColor("#40000000");
        } else {
            mLayerBgColor = Color.parseColor("#FAFAFA");
            mLayerSecondBgColor = Color.parseColor("#F2F2F2");
            mLayerShadowColor = Color.parseColor("#1A000000");
        }
    }

    public void setTopBitmap(Bitmap bitmap, int count) {
        this.mTopBitmap = bitmap;
        this.mCount = Math.max(1, count);
        if (mTopCustomView != null) {
            removeView(mTopCustomView);
            mTopCustomView = null;
        }
        invalidate();
    }

    public void setTopCustomView(View view, int count) {
        this.mTopBitmap = null;
        this.mCount = Math.max(1, count);
        if (mTopCustomView != null) {
            removeView(mTopCustomView);
        }
        mTopCustomView = view;
        if (view != null) {
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            addView(view, lp);
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Draw stacked background cards if count >= 2
        if (mCount >= 3) {
            drawBackLayer(canvas, w, h, 2, mLayerSecondBgColor);
        }
        if (mCount >= 2) {
            drawBackLayer(canvas, w, h, 1, mLayerBgColor);
        }

        // Draw top card layer if bitmap is present
        if (mTopBitmap != null && !mTopBitmap.isRecycled()) {
            drawTopBitmap(canvas, w, h);
        }
    }

    private void drawBackLayer(Canvas canvas, int w, int h, int layerIndex, int bgColor) {
        float insetX = layerIndex * mLayerDistance * 1.5f;
        float offsetY = -layerIndex * mLayerDistance;

        mRectF.set(insetX, offsetY + layerIndex * 2f * mDensity,
                w - insetX, h - (2 - layerIndex) * 2f * mDensity);

        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mLayerShadowColor);
        canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaint);

        mPaint.setColor(bgColor);
        canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaint);
    }

    private void drawTopBitmap(Canvas canvas, int w, int h) {
        float inset = (mCount > 1 ? mLayerDistance : 0f);
        mRectF.set(inset, inset, w - inset, h - inset);

        int saveCount = canvas.saveLayer(0, 0, w, h, null);

        mPath.reset();
        mPath.addRoundRect(mRectF, mRadius, mRadius, Path.Direction.CW);
        canvas.clipPath(mPath);

        // Center-crop draw the bitmap
        RectF dst = new RectF(mRectF);
        float scale = Math.max(dst.width() / mTopBitmap.getWidth(), dst.height() / mTopBitmap.getHeight());
        float bw = mTopBitmap.getWidth() * scale;
        float bh = mTopBitmap.getHeight() * scale;
        float bx = dst.left + (dst.width() - bw) / 2f;
        float by = dst.top + (dst.height() - bh) / 2f;

        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        canvas.drawBitmap(mTopBitmap, null, new RectF(bx, by, bx + bw, by + bh), mPaint);

        canvas.restoreToCount(saveCount);
    }
}
