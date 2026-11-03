// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.target;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.Collections;
import java.util.Set;

/** One actual share destination. A package with several share activities gets several entries. */
public final class ShareTargetEntry {

    public static final int KIND_SHARE = 0;
    public static final int KIND_COPY = 1;
    public static final int KIND_SAVE = 2;
    public static final int KIND_SEGMENT = 3;
    public static final int KIND_SEARCH = 4;
    public static final int KIND_SYSTEM_SHARE = 5;
    public static final int KIND_QR = 6;
    public static final int KIND_TRANSFER_DOCK = 7;

    public static final String ACTION_SEND = "send";
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_DIAL = "dial";
    public static final String ACTION_MAIL = "mail";
    public static final String ACTION_SMS = "sms";
    public static final String ACTION_SEARCH = "search";

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
    /**
     * Empty means this target is generic. A non-empty set limits the target to
     * semantic content kinds (address/url/phone/email/...); this is intentionally
     * separate from the display label.
     */
    public final Set<String> contentKinds;
    public final String action;
    public final String deeplink;

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
        this(key, label, appName, packageName, activityName, icon, badgeIcon, kind, category,
                priority, supportsText, supportsImage, ACTION_SEND, null,
                Collections.emptySet());
    }

    public ShareTargetEntry(String key, String label, String appName, String packageName,
                            String activityName, Drawable icon, Drawable badgeIcon, int kind,
                            String category, int priority, boolean supportsText,
                            boolean supportsImage, String action, String deeplink,
                            Set<String> contentKinds) {
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
        this.action = action == null || action.isEmpty() ? ACTION_SEND : action;
        this.deeplink = deeplink;
        this.contentKinds = contentKinds == null
                ? Collections.emptySet() : Collections.unmodifiableSet(contentKinds);
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
