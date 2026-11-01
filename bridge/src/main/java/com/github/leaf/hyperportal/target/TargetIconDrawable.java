// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.target;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * App icon with a smaller target/action icon overlaid at its bottom-right corner.
 * This is used by the ColorOS dock so several share Activities of one app remain
 * recognizable without expanding the label.
 */
public final class TargetIconDrawable extends Drawable {

    private final Drawable mAppIcon;
    private final Drawable mBadgeIcon;
    private final Rect mTmp = new Rect();
    private final RectF mBadgeRect = new RectF();

    private TargetIconDrawable(Drawable appIcon, Drawable badgeIcon) {
        mAppIcon = appIcon;
        mBadgeIcon = badgeIcon;
    }

    public static Drawable create(@Nullable Drawable appIcon, @Nullable Drawable badgeIcon) {
        if (appIcon == null) {
            return badgeIcon;
        }
        if (badgeIcon == null || sameDrawable(appIcon, badgeIcon)) {
            return appIcon;
        }
        return new TargetIconDrawable(appIcon, badgeIcon);
    }

    private static boolean sameDrawable(Drawable left, Drawable right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        Drawable.ConstantState leftState = left.getConstantState();
        Drawable.ConstantState rightState = right.getConstantState();
        return leftState != null && leftState.equals(rightState);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        int size = Math.min(bounds.width(), bounds.height());
        int left = bounds.left + (bounds.width() - size) / 2;
        int top = bounds.top + (bounds.height() - size) / 2;

        if (mAppIcon != null) {
            mTmp.set(left, top, left + size, top + size);
            mAppIcon.setBounds(mTmp);
            mAppIcon.draw(canvas);
        }

        if (mBadgeIcon == null) {
            return;
        }
        int badgeSize = Math.max(1, Math.round(size * 0.46f));
        int padding = Math.max(1, Math.round(size * 0.035f));
        float badgeLeft = left + size - badgeSize - padding;
        float badgeTop = top + size - badgeSize - padding;
        mBadgeRect.set(badgeLeft, badgeTop, badgeLeft + badgeSize, badgeTop + badgeSize);

        int inset = Math.max(1, Math.round(badgeSize * 0.06f));
        mTmp.set(
                Math.round(mBadgeRect.left) + inset,
                Math.round(mBadgeRect.top) + inset,
                Math.round(mBadgeRect.right) - inset,
                Math.round(mBadgeRect.bottom) - inset);
        mBadgeIcon.setBounds(mTmp);
        mBadgeIcon.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        mAppIcon.setAlpha(alpha);
        if (mBadgeIcon != null) {
            mBadgeIcon.setAlpha(alpha);
        }
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mAppIcon.setColorFilter(colorFilter);
        if (mBadgeIcon != null) {
            mBadgeIcon.setColorFilter(colorFilter);
        }
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
