// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.leaf.hyperportal.target.BuiltInIconDrawable;

/**
 * Resilient resource resolver for TransferDock UI components.
 * Loads original ColorOS drawables when available, and provides
 * pixel-perfect programmatic fallbacks that work reliably under SystemUI.
 */
public final class TransferDockRes {

    private static Context sHyperContext;

    public static Context getHyperContext(Context ctx) {
        if (sHyperContext != null) return sHyperContext;
        try {
            sHyperContext = ctx.createPackageContext("com.github.leaf.hyperportal", Context.CONTEXT_IGNORE_SECURITY);
        } catch (Throwable ignored) {
            sHyperContext = ctx;
        }
        return sHyperContext;
    }

    public static Drawable getCapsuleBg(Context ctx, float density) {
        return getCapsuleBg(ctx, density, TransferDockTheme.isNight(ctx));
    }

    public static Drawable getCapsuleBg(Context ctx, float density, boolean night) {
        int r = (int) (20 * density + 0.5f);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(night ? Color.parseColor("#E6202022") : Color.parseColor("#F2F7F7F7"));
        // Rounded rectangle instead of the old D-shaped right-flat capsule.
        shape.setCornerRadius(r);
        return shape;
    }

    public static Drawable getFileDockIcon(Context ctx) {
        try {
            Context hyper = getHyperContext(ctx);
            int id = hyper.getResources().getIdentifier("ic_file_dock", "drawable", "com.github.leaf.hyperportal");
            if (id != 0) {
                Drawable d = ContextCompat.getDrawable(hyper, id);
                if (d != null) return d;
            }
        } catch (Throwable ignored) {
        }
        return BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_TRANSFER_DOCK);
    }

    public static Drawable getBadgeBg(Context ctx, float density) {
        try {
            Context hyper = getHyperContext(ctx);
            int id = hyper.getResources().getIdentifier("shape_transfer_count_group_bg", "drawable", "com.github.leaf.hyperportal");
            if (id != 0) {
                Drawable d = ContextCompat.getDrawable(hyper, id);
                if (d != null) return d;
            }
        } catch (Throwable ignored) {
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#3696FE"));
        bg.setCornerRadius(10f * density);
        return bg;
    }

    /**
     * 24dp COUI-style back chevron used by the transfer panel title bar.
     */
    public static Drawable getBackIcon(Context ctx, float density, int color) {
        final float strokeWidth = 2.0f * density;
        return new Drawable() {
            private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                mPaint.setColor(color);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(strokeWidth);
                mPaint.setStrokeCap(Paint.Cap.ROUND);
                mPaint.setStrokeJoin(Paint.Join.ROUND);
            }

            @Override
            public int getIntrinsicWidth() {
                return (int) (24 * density + 0.5f);
            }

            @Override
            public int getIntrinsicHeight() {
                return (int) (24 * density + 0.5f);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                Rect b = getBounds();
                float inset = b.width() * 0.30f;
                float midY = b.exactCenterY();
                canvas.drawLine(b.left + inset, midY, b.right - inset, midY, mPaint);
                canvas.drawLine(b.left + inset, midY, b.left + inset * 1.75f, b.top + inset, mPaint);
                canvas.drawLine(b.left + inset, midY, b.left + inset * 1.75f, b.bottom - inset, mPaint);
            }

            @Override
            public void setAlpha(int alpha) {
                mPaint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                mPaint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }

    /**
     * Vertical three-dot overflow icon used by the reference transfer panel.
     */
    public static Drawable getMoreIcon(Context ctx, float density, int color) {
        return new Drawable() {
            private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                mPaint.setColor(color);
                mPaint.setStyle(Paint.Style.FILL);
            }

            @Override
            public int getIntrinsicWidth() {
                return (int) (24 * density + 0.5f);
            }

            @Override
            public int getIntrinsicHeight() {
                return (int) (24 * density + 0.5f);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                Rect b = getBounds();
                float cx = b.exactCenterX();
                float radius = 1.75f * density;
                float gap = 5.0f * density;
                canvas.drawCircle(cx, b.exactCenterY() - gap, radius, mPaint);
                canvas.drawCircle(cx, b.exactCenterY(), radius, mPaint);
                canvas.drawCircle(cx, b.exactCenterY() + gap, radius, mPaint);
            }

            @Override
            public void setAlpha(int alpha) {
                mPaint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                mPaint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }

    public static Drawable getCloseIcon(Context ctx, float density) {
        try {
            Context hyper = getHyperContext(ctx);
            int id = hyper.getResources().getIdentifier("ic_transfer_dock_close", "drawable", "com.github.leaf.hyperportal");
            if (id != 0) {
                Drawable d = ContextCompat.getDrawable(hyper, id);
                if (d != null) return d;
            }
        } catch (Throwable ignored) {
        }
        final float strokeWidth = 1.8f * density;
        return new Drawable() {
            private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                mPaint.setColor(Color.parseColor("#B0FFFFFF"));
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(strokeWidth);
                mPaint.setStrokeCap(Paint.Cap.ROUND);
            }
            @Override
            public void draw(@NonNull Canvas canvas) {
                Rect b = getBounds();
                float pad = b.width() * 0.28f;
                canvas.drawLine(b.left + pad, b.top + pad, b.right - pad, b.bottom - pad, mPaint);
                canvas.drawLine(b.right - pad, b.top + pad, b.left + pad, b.bottom - pad, mPaint);
            }
            @Override
            public void setAlpha(int alpha) { mPaint.setAlpha(alpha); }
            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) { mPaint.setColorFilter(colorFilter); }
            @Override
            public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }
}
