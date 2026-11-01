// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.target;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Built-in squircle action icon with crisp vector artwork.
 * Renders identically across Compose ImageBitmaps and View ImageViews at any dimension.
 */
public final class BuiltInIconDrawable extends Drawable {

    public static final int TYPE_COPY = 1;
    public static final int TYPE_SAVE = 2;
    public static final int TYPE_SEGMENT = 3;
    public static final int TYPE_SEARCH = 4;
    public static final int TYPE_SHARE = 5;
    public static final int TYPE_QR = 6;

    private final int mType;
    private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BuiltInIconDrawable(int type) {
        this.mType = type;
        mBgPaint.setColor(Color.parseColor("#38383A"));
        mBgPaint.setStyle(Paint.Style.FILL);

        mStrokePaint.setColor(Color.WHITE);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        mStrokePaint.setStrokeJoin(Paint.Join.ROUND);

        mFillPaint.setColor(Color.WHITE);
        mFillPaint.setStyle(Paint.Style.FILL);
    }

    public static Drawable create(int type) {
        return new BuiltInIconDrawable(type);
    }

    @Nullable
    public static Drawable create(@Nullable String key) {
        if (key == null) return null;
        switch (key) {
            case "builtin:copy":
                return new BuiltInIconDrawable(TYPE_COPY);
            case "builtin:save":
                return new BuiltInIconDrawable(TYPE_SAVE);
            case "builtin:segment":
                return new BuiltInIconDrawable(TYPE_SEGMENT);
            case "builtin:search":
                return new BuiltInIconDrawable(TYPE_SEARCH);
            case "builtin:share":
                return new BuiltInIconDrawable(TYPE_SHARE);
            case "builtin:qr":
                return new BuiltInIconDrawable(TYPE_QR);
            default:
                return null;
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        int size = Math.min(bounds.width(), bounds.height());
        if (size <= 0) {
            return;
        }
        float left = bounds.left + (bounds.width() - size) / 2f;
        float top = bounds.top + (bounds.height() - size) / 2f;

        // 1. Dark squircle background
        float cornerRadius = size * 0.28f;
        RectF bgRect = new RectF(left, top, left + size, top + size);
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, mBgPaint);

        // 2. Vector icon glyph
        switch (mType) {
            case TYPE_COPY:
                drawCopy(canvas, left, top, size);
                break;
            case TYPE_SAVE:
                drawSave(canvas, left, top, size);
                break;
            case TYPE_SEGMENT:
                drawSegment(canvas, left, top, size);
                break;
            case TYPE_SEARCH:
                drawSearch(canvas, left, top, size);
                break;
            case TYPE_SHARE:
                drawShare(canvas, left, top, size);
                break;
            case TYPE_QR:
                drawQr(canvas, left, top, size);
                break;
            default:
                drawCopy(canvas, left, top, size);
                break;
        }
    }

    private void drawCopy(Canvas canvas, float left, float top, float size) {
        float pad = size * 0.28f;
        float docW = size * 0.32f;
        float docH = size * 0.38f;
        float rad = size * 0.06f;
        float offset = size * 0.08f;
        mStrokePaint.setStrokeWidth(Math.max(1.5f, size * 0.045f));

        // Back document
        RectF backDoc = new RectF(left + pad + offset, top + pad,
                left + pad + offset + docW, top + pad + docH);
        canvas.drawRoundRect(backDoc, rad, rad, mStrokePaint);

        // Front document
        RectF frontDoc = new RectF(left + pad, top + pad + offset,
                left + pad + docW, top + pad + offset + docH);
        canvas.drawRoundRect(frontDoc, rad, rad, mBgPaint);
        canvas.drawRoundRect(frontDoc, rad, rad, mStrokePaint);
    }

    private void drawSave(Canvas canvas, float left, float top, float size) {
        float cx = left + size * 0.5f;
        float cy = top + size * 0.48f;
        mStrokePaint.setStrokeWidth(Math.max(1.5f, size * 0.045f));

        // Down arrow shaft
        canvas.drawLine(cx, cy - size * 0.16f, cx, cy + size * 0.08f, mStrokePaint);
        // Arrow head
        canvas.drawLine(cx - size * 0.11f, cy, cx, cy + size * 0.08f, mStrokePaint);
        canvas.drawLine(cx + size * 0.11f, cy, cx, cy + size * 0.08f, mStrokePaint);

        // Tray
        float trayY = cy + size * 0.18f;
        float trayW = size * 0.18f;
        float trayH = size * 0.08f;
        canvas.drawLine(cx - trayW, trayY, cx + trayW, trayY, mStrokePaint);
        canvas.drawLine(cx - trayW, trayY, cx - trayW, trayY - trayH, mStrokePaint);
        canvas.drawLine(cx + trayW, trayY, cx + trayW, trayY - trayH, mStrokePaint);
    }

    private void drawSegment(Canvas canvas, float left, float top, float size) {
        float barLeft = left + size * 0.24f;
        float fullRight = left + size * 0.76f;
        float barRadius = Math.max(1.5f, size * 0.038f);
        float[] centers = {0.32f, 0.50f, 0.68f};
        float[] widths = {1.0f, 0.68f, 1.0f};
        for (int i = 0; i < centers.length; i++) {
            float centerY = top + size * centers[i];
            float right = barLeft + (fullRight - barLeft) * widths[i];
            canvas.drawRoundRect(new RectF(barLeft, centerY - barRadius, right, centerY + barRadius),
                    barRadius, barRadius, mFillPaint);
        }
    }

    private void drawSearch(Canvas canvas, float left, float top, float size) {
        float cx = left + size * 0.44f;
        float cy = top + size * 0.44f;
        float r = size * 0.14f;
        mStrokePaint.setStrokeWidth(Math.max(1.5f, size * 0.045f));
        canvas.drawCircle(cx, cy, r, mStrokePaint);

        float offset = (float) (r / Math.sqrt(2));
        float handleLen = size * 0.14f;
        canvas.drawLine(cx + offset, cy + offset,
                cx + offset + handleLen, cy + offset + handleLen, mStrokePaint);
    }

    private void drawShare(Canvas canvas, float left, float top, float size) {
        float cx = left + size * 0.50f;
        float cy = top + size * 0.50f;
        float spreadX = size * 0.14f;
        float spreadY = size * 0.13f;
        float nodeRadius = size * 0.052f;

        float p1X = cx - spreadX;
        float p1Y = cy;
        float p2X = cx + spreadX;
        float p2Y = cy - spreadY;
        float p3X = cx + spreadX;
        float p3Y = cy + spreadY;

        mStrokePaint.setStrokeWidth(Math.max(1.5f, size * 0.045f));
        canvas.drawLine(p1X, p1Y, p2X, p2Y, mStrokePaint);
        canvas.drawLine(p1X, p1Y, p3X, p3Y, mStrokePaint);

        canvas.drawCircle(p1X, p1Y, nodeRadius, mFillPaint);
        canvas.drawCircle(p2X, p2Y, nodeRadius, mFillPaint);
        canvas.drawCircle(p3X, p3Y, nodeRadius, mFillPaint);
    }

    private void drawQr(Canvas canvas, float left, float top, float size) {
        mStrokePaint.setStrokeWidth(Math.max(1.5f, size * 0.04f));
        float qrSize = size * 0.46f;
        float qLeft = left + (size - qrSize) / 2f;
        float qTop = top + (size - qrSize) / 2f;
        float cornerLen = qrSize * 0.28f;

        // 4 corner viewfinder brackets
        // Top-left
        canvas.drawLine(qLeft, qTop + cornerLen, qLeft, qTop, mStrokePaint);
        canvas.drawLine(qLeft, qTop, qLeft + cornerLen, qTop, mStrokePaint);
        // Top-right
        canvas.drawLine(qLeft + qrSize - cornerLen, qTop, qLeft + qrSize, qTop, mStrokePaint);
        canvas.drawLine(qLeft + qrSize, qTop, qLeft + qrSize, qTop + cornerLen, mStrokePaint);
        // Bottom-left
        canvas.drawLine(qLeft, qTop + qrSize - cornerLen, qLeft, qTop + qrSize, mStrokePaint);
        canvas.drawLine(qLeft, qTop + qrSize, qLeft + cornerLen, qTop + qrSize, mStrokePaint);
        // Bottom-right
        canvas.drawLine(qLeft + qrSize - cornerLen, qTop + qrSize, qLeft + qrSize, qTop + qrSize, mStrokePaint);
        canvas.drawLine(qLeft + qrSize, qTop + qrSize - cornerLen, qLeft + qrSize, qTop + qrSize, mStrokePaint);

        // Center scan line
        canvas.drawLine(qLeft + qrSize * 0.15f, top + size * 0.5f,
                qLeft + qrSize * 0.85f, top + size * 0.5f, mStrokePaint);
    }

    @Override
    public int getIntrinsicWidth() {
        return 96;
    }

    @Override
    public int getIntrinsicHeight() {
        return 96;
    }

    @Override
    public void setAlpha(int alpha) {
        mBgPaint.setAlpha(alpha);
        mStrokePaint.setAlpha(alpha);
        mFillPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mBgPaint.setColorFilter(colorFilter);
        mStrokePaint.setColorFilter(colorFilter);
        mFillPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public ConstantState getConstantState() {
        return new BuiltInConstantState(mType);
    }

    private static class BuiltInConstantState extends ConstantState {
        private final int mType;

        BuiltInConstantState(int type) {
            this.mType = type;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new BuiltInIconDrawable(mType);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BuiltInConstantState)) return false;
            return mType == ((BuiltInConstantState) obj).mType;
        }

        @Override
        public int hashCode() {
            return mType;
        }
    }
}
