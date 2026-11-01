// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.target;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

/** One actual share destination. A package with several share activities gets several entries. */
public final class ShareTargetEntry {

    public static final int KIND_SHARE = 0;
    public static final int KIND_COPY = 1;
    public static final int KIND_SAVE = 2;
    public static final int KIND_SEGMENT = 3;
    public static final int KIND_SEARCH = 4;
    public static final int KIND_SYSTEM_SHARE = 5;
    public static final int KIND_QR = 6;

    public final String key;
    public final String label;
    public final String appName;
    public final String packageName;
    public final String activityName;
    public final Drawable icon;
    public final Drawable badgeIcon;
    public final int kind;
    public final String category;
    public final int priority;
    public final boolean supportsText;
    public final boolean supportsImage;

    public ShareTargetEntry(String key, String label, String packageName, String activityName,
                            Drawable icon, int kind, String category, int priority,
                            boolean supportsText, boolean supportsImage) {
        this(key, label, label, packageName, activityName, icon, null, kind, category, priority,
                supportsText, supportsImage);
    }

    public ShareTargetEntry(String key, String label, String packageName, String activityName,
                            Drawable icon, Drawable badgeIcon, int kind, String category,
                            int priority, boolean supportsText, boolean supportsImage) {
        this(key, label, label, packageName, activityName, icon, badgeIcon, kind, category, priority,
                supportsText, supportsImage);
    }

    public ShareTargetEntry(String key, String label, String appName, String packageName,
                            String activityName, Drawable icon, Drawable badgeIcon, int kind,
                            String category, int priority, boolean supportsText,
                            boolean supportsImage) {
        this.key = key;
        this.label = label == null ? "" : label;
        this.appName = appName == null ? (label == null ? "" : label) : appName;
        this.packageName = packageName == null ? "" : packageName;
        this.activityName = activityName == null ? "" : activityName;
        this.icon = icon;
        this.badgeIcon = badgeIcon;
        this.kind = kind;
        this.category = category == null ? "other" : category;
        this.priority = priority;
        this.supportsText = supportsText;
        this.supportsImage = supportsImage;
    }

    public Drawable displayIcon() {
        return TargetIconDrawable.create(icon, badgeIcon);
    }

    public boolean isBuiltIn() {
        return kind != KIND_SHARE;
    }

    public ComponentName component() {
        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(activityName)) {
            return null;
        }
        return new ComponentName(packageName, activityName);
    }

    public String activitySimpleName() {
        if (TextUtils.isEmpty(activityName)) {
            return "";
        }
        int dot = activityName.lastIndexOf('.');
        return dot >= 0 ? activityName.substring(dot + 1) : activityName;
    }
}
