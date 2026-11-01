// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.target;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Discovers one entry per exported share Activity and applies local rules. */
public final class TargetRepository {

    private static final Pattern URL = Pattern.compile("^(https?://|www\\.).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[+]?[0-9\\s\\-()]{5,20}$");
    private static final Pattern ADDRESS = Pattern.compile(
            ".*(省|市|区|县|路|街|号|大厦|广场|小区|园区|楼|室|镇|村).*");

    private TargetRepository() {
    }

    public static List<ShareTargetEntry> queryShareTargets(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, MutableTarget> byKey = new LinkedHashMap<>();
        queryMime(context, pm, "text/plain", true, false, byKey);
        queryMime(context, pm, "image/*", false, true, byKey);
        queryMime(context, pm, "*/*", false, true, byKey);

        List<ShareTargetEntry> result = new ArrayList<>();
        for (MutableTarget target : byKey.values()) {
            TargetRuleStore.Rule rule = TargetRuleStore.get(context)
                    .match(target.packageName, target.activityName, target.rawLabel);
            String label = target.rawLabel;
            if (TextUtils.isEmpty(label)) {
                label = target.appName != null && !target.appName.isEmpty() ? target.appName : target.packageName;
            }
            String appName = target.appName != null && !target.appName.isEmpty() ? target.appName : target.packageName;
            result.add(new ShareTargetEntry(
                    target.key,
                    label,
                    appName,
                    target.packageName,
                    target.activityName,
                    target.icon,
                    target.badgeIcon,
                    ShareTargetEntry.KIND_SHARE,
                    rule == null ? "other" : rule.category,
                    rule == null ? 0 : rule.priority,
                    target.supportsText,
                    target.supportsImage));
        }
        return result;
    }

    private static void queryMime(Context context, PackageManager pm, String mime,
                                  boolean text, boolean image,
                                  Map<String, MutableTarget> byKey) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mime);
        List<ResolveInfo> resolved;
        try {
            resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        } catch (Throwable throwable) {
            return;
        }
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null) {
                continue;
            }
            String pkg = info.activityInfo.packageName;
            String activity = info.activityInfo.name;
            if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(activity)
                    || !info.activityInfo.exported
                    || "android".equals(pkg)
                    || "com.android.intentresolver".equals(pkg)
                    || context.getPackageName().equals(pkg)) {
                continue;
            }
            String key = pkg + "/" + activity;
            MutableTarget target = byKey.get(key);
            if (target == null) {
                target = new MutableTarget();
                target.key = key;
                target.packageName = pkg;
                target.activityName = activity;
                target.appName = loadAppLabel(pm, pkg);
                target.rawLabel = loadLabel(info, pm, pkg);
                target.icon = loadAppIcon(pm, pkg);
                target.badgeIcon = loadActivityIcon(info, pm, pkg);
                if (sameDrawable(target.icon, target.badgeIcon)) {
                    target.badgeIcon = null;
                }
                byKey.put(key, target);
            }
            target.supportsText |= text;
            target.supportsImage |= image;
        }
    }

    private static String loadAppLabel(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString().trim();
        } catch (Throwable ignored) {
            return pkg;
        }
    }

    private static String loadLabel(ResolveInfo info, PackageManager pm, String pkg) {
        try {
            CharSequence label = info.loadLabel(pm);
            if (!TextUtils.isEmpty(label)) {
                return label.toString().trim();
            }
        } catch (Throwable ignored) {
        }
        return loadAppLabel(pm, pkg);
    }

    private static Drawable loadActivityIcon(ResolveInfo info, PackageManager pm, String pkg) {
        if (info.activityInfo == null) {
            return null;
        }
        int activityIcon = info.activityInfo.icon;
        int appIcon = info.activityInfo.applicationInfo == null
                ? 0 : info.activityInfo.applicationInfo.icon;
        // Most share Activities do not declare their own icon. In that case
        // ResolveInfo.loadIcon() is just the application icon; drawing it again
        // as a corner badge is both redundant and visually noisy.
        if (activityIcon == 0 || activityIcon == appIcon) {
            return null;
        }
        try {
            return info.loadIcon(pm);
        } catch (Throwable ignored) {
            return null;
        }
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
        if (leftState != null && leftState.equals(rightState)) {
            return true;
        }
        // Different resource ids can still resolve to the same artwork (for
        // example an app icon and an activity alias). Compare a small raster so
        // an identical badge is not drawn on top of the app icon.
        try {
            Bitmap leftBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
            Bitmap rightBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
            Canvas leftCanvas = new Canvas(leftBitmap);
            Canvas rightCanvas = new Canvas(rightBitmap);
            left.setBounds(0, 0, 32, 32);
            right.setBounds(0, 0, 32, 32);
            left.draw(leftCanvas);
            right.draw(rightCanvas);
            return leftBitmap.sameAs(rightBitmap);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Drawable loadAppIcon(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationIcon(pkg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static List<ShareTargetEntry> builtInEntries(boolean image) {
        List<ShareTargetEntry> result = new ArrayList<>();
        result.add(new ShareTargetEntry("builtin:copy", image ? "复制图片" : "复制文本",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_COPY), null,
                ShareTargetEntry.KIND_COPY, "action", 110, true, true));
        if (image) {
            result.add(new ShareTargetEntry("builtin:save", "保存原图",
                    "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SAVE), null,
                    ShareTargetEntry.KIND_SAVE, "image", 105, false, true));
        } else {
            result.add(new ShareTargetEntry("builtin:segment", "文本分词",
                    "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SEGMENT), null,
                    ShareTargetEntry.KIND_SEGMENT, "text", 100, true, false));
        }
        result.add(new ShareTargetEntry("builtin:share", "系统分享",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SHARE), null,
                ShareTargetEntry.KIND_SYSTEM_SHARE, "action", 95, true, true));
        if (!image) {
            result.add(new ShareTargetEntry("builtin:search", "搜索",
                    "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SEARCH), null,
                    ShareTargetEntry.KIND_SEARCH, "text", 90, true, false));
        }
        return result;
    }

    public static List<ShareTargetEntry> allBuiltInEntries() {
        List<ShareTargetEntry> result = new ArrayList<>();
        result.add(new ShareTargetEntry("builtin:copy", "复制",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_COPY), null,
                ShareTargetEntry.KIND_COPY, "action", 110, true, true));
        result.add(new ShareTargetEntry("builtin:save", "保存原图",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SAVE), null,
                ShareTargetEntry.KIND_SAVE, "image", 105, false, true));
        result.add(new ShareTargetEntry("builtin:segment", "文本分词",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SEGMENT), null,
                ShareTargetEntry.KIND_SEGMENT, "text", 100, true, false));
        result.add(new ShareTargetEntry("builtin:share", "系统分享",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SHARE), null,
                ShareTargetEntry.KIND_SYSTEM_SHARE, "action", 95, true, true));
        result.add(new ShareTargetEntry("builtin:search", "搜索",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SEARCH), null,
                ShareTargetEntry.KIND_SEARCH, "text", 90, true, false));
        result.add(new ShareTargetEntry("builtin:qr", "识别二维码",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_QR), null,
                ShareTargetEntry.KIND_QR, "image", 98, false, true));
        return result;
    }

    public static List<ShareTargetEntry> allTargetsForSettings(Context context) {
        List<ShareTargetEntry> result = new ArrayList<>(allBuiltInEntries());
        result.addAll(queryShareTargets(context));
        return result;
    }

    public static String contentKind(String text, boolean image) {
        if (image) {
            return "image";
        }
        if (TextUtils.isEmpty(text)) {
            return "text";
        }
        String value = text.trim();
        if (URL.matcher(value).matches()) {
            return "url";
        }
        if (EMAIL.matcher(value).matches()) {
            return "email";
        }
        if (PHONE.matcher(value).matches()) {
            return "phone";
        }
        if (value.length() >= 4 && ADDRESS.matcher(value).matches()) {
            return "address";
        }
        return "text";
    }

    private static final class MutableTarget {
        String key;
        String packageName;
        String activityName;
        String appName;
        String rawLabel;
        Drawable icon;
        Drawable badgeIcon;
        boolean supportsText;
        boolean supportsImage;
    }
}
