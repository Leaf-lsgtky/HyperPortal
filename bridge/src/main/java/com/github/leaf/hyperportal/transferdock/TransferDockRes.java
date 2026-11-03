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
import android.graphics.drawable.InsetDrawable;

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
        try {
            Context hyper = getHyperContext(ctx);
            int id = hyper.getResources().getIdentifier("recept_panel_right_bg", "drawable", "com.github.leaf.hyperportal");
            if (id != 0) {
                Drawable d = ContextCompat.getDrawable(hyper, id);
                if (d != null) return d;
            }
        } catch (Throwable ignored) {
        }
        int r = (int) (22 * density + 0.5f);
        int stroke = (int) (0.8f * density + 0.5f);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor("#E6202022"));
        shape.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, r, r});
        shape.setStroke(Math.max(1, stroke), Color.parseColor("#33FFFFFF"));
        int insetL = (int) (14 * density + 0.5f);
        int insetT = (int) (22 * density + 0.5f);
        int insetB = (int) (22 * density + 0.5f);
        return new InsetDrawable(shape, insetL, insetT, 0, insetB);
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
