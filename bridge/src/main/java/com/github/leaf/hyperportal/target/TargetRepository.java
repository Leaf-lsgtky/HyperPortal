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
import android.util.Patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Discovers one entry per exported share Activity and applies local rules. */
public final class TargetRepository {

    private static final Pattern EMAIL = Pattern.compile(
            "[^\\s@]+@[^\\s@]+\\.[^\\s@]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("[+]?[0-9][0-9\\s\\-()]{6,19}");
    private static final Pattern ADDRESS_REGION = Pattern.compile("[省市区县]");
    private static final Pattern ADDRESS_ROAD = Pattern.compile("[路街道镇乡村]");
    private static final Pattern ADDRESS_NUMBER = Pattern.compile("[号弄栋楼室]");
    private static final Pattern ADDRESS_BUILDING = Pattern.compile(
            "[大厦广场小区园区花园公寓中心]");

    private TargetRepository() {
    }

    public static List<ShareTargetEntry> queryShareTargets(Context context) {
        PackageManager pm = context.getPackageManager();
        TargetRuleStore.Rules rules = TargetRuleStore.get(context);
        Map<String, MutableTarget> byKey = new LinkedHashMap<>();
        queryMime(context, pm, "text/plain", true, false, byKey);
        queryMime(context, pm, "image/*", false, true, byKey);
        // "*/*" means the activity can accept arbitrary files; treat it as
        // both text and image-capable rather than incorrectly classifying a
        // PDF as an image.
        queryMime(context, pm, "*/*", true, true, byKey);

        List<ShareTargetEntry> result = new ArrayList<>();
        Set<String> discoveredTextPackages = new HashSet<>();
        for (MutableTarget target : byKey.values()) {
            if (target.supportsText) {
                discoveredTextPackages.add(target.packageName);
            }
            TargetRuleStore.Rule rule = rules.match(
                    target.packageName, target.activityName, target.rawLabel);
            // Do not replace the activity's original label with a rule label.
            // A package such as QQ has several share activities; using "QQ"
            // for all of them produces duplicate, indistinguishable entries.
            String label = target.rawLabel;
            if (TextUtils.isEmpty(label)) {
                label = target.appName != null && !target.appName.isEmpty()
                        ? target.appName : target.packageName;
            }
            String appName = target.appName != null && !target.appName.isEmpty()
                    ? target.appName : target.packageName;
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
                    target.supportsImage,
                    rule == null || TextUtils.isEmpty(rule.action)
                            ? ShareTargetEntry.ACTION_SEND : rule.action,
                    rule == null ? null : rule.deeplink,
                    rule == null ? Collections.emptySet() : rule.contentKinds));
        }

        // Rules may describe explicit app targets that never publish an
        // ACTION_SEND activity (maps, browsers, dialers, mail clients).
        // Add those only when the package is not already represented, so they
        // do not create duplicate rows for ordinary share apps.
        for (TargetRuleStore.Rule rule : rules.rules) {
            if (!rule.explicit || rule.packageName.isEmpty()) {
                continue;
            }
            if (discoveredTextPackages.contains(rule.packageName)
                    || context.getPackageName().equals(rule.packageName)) {
                continue;
            }
            if (!isInstalled(pm, rule.packageName)) {
                continue;
            }
            String appName = loadAppLabel(pm, rule.packageName);
            Drawable icon = loadAppIcon(pm, rule.packageName);
            String label = TextUtils.isEmpty(rule.label) ? appName : rule.label;
            boolean supportsImage = rule.contentKinds.contains("image");
            boolean supportsText = !supportsImage || rule.contentKinds.size() > 1;
            String key = "explicit:" + rule.packageName;
            result.add(new ShareTargetEntry(
                    key,
                    label,
                    appName,
                    rule.packageName,
                    "",
                    icon,
                    null,
                    ShareTargetEntry.KIND_SHARE,
                    rule.category,
                    rule.priority,
                    supportsText,
                    supportsImage,
                    TextUtils.isEmpty(rule.action)
                            ? ShareTargetEntry.ACTION_SEND : rule.action,
                    rule.deeplink,
                    rule.contentKinds));
        }
        return result;
    }

    private static boolean isInstalled(PackageManager pm, String packageName) {
        try {
            pm.getApplicationInfo(packageName, 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
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
        result.add(new ShareTargetEntry("builtin:transfer_dock", "存入中转站",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_TRANSFER_DOCK), null,
                ShareTargetEntry.KIND_TRANSFER_DOCK, "action", 120, true, true));
        result.add(new ShareTargetEntry("builtin:copy", image ? "复制" : "复制文本",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_COPY), null,
                ShareTargetEntry.KIND_COPY, "action", 110, true, true));
        if (image) {
            result.add(new ShareTargetEntry("builtin:segment", "文本分词",
                    "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SEGMENT), null,
                    ShareTargetEntry.KIND_SEGMENT, "image", 100, true, true));
            result.add(new ShareTargetEntry("builtin:save", "保存",
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
        result.add(new ShareTargetEntry("builtin:transfer_dock", "存入中转站",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_TRANSFER_DOCK), null,
                ShareTargetEntry.KIND_TRANSFER_DOCK, "action", 120, true, true));
        result.add(new ShareTargetEntry("builtin:copy", "复制",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_COPY), null,
                ShareTargetEntry.KIND_COPY, "action", 110, true, true));
        result.add(new ShareTargetEntry("builtin:save", "保存原图",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SAVE), null,
                ShareTargetEntry.KIND_SAVE, "image", 105, false, true));
        result.add(new ShareTargetEntry("builtin:segment", "文本分词",
                "系统内置", "builtin", "", BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SEGMENT), null,
                ShareTargetEntry.KIND_SEGMENT, "text", 100, true, true));
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

    /**
     * Returns every semantic kind that applies to the payload. OPPO's engine
     * keeps a set of detected types, so a single string may be both a URL and
     * an address (for example a map link with a place name). Returning a set
     * also lets the ranker filter a target such as a map to address-only.
     */
    public static Set<String> contentKinds(String text, boolean image, String mime) {
        LinkedHashSet<String> kinds = new LinkedHashSet<>();
        boolean imagePayload = image || (!TextUtils.isEmpty(mime) && mime.startsWith("image/"));
        if (imagePayload) {
            kinds.add("image");
        } else if (!TextUtils.isEmpty(mime)) {
            String lower = mime.toLowerCase(Locale.US);
            if (!lower.startsWith("text/") && !"application/x-dragshare".equals(lower)) {
                kinds.add("file");
            }
        }

        if (!TextUtils.isEmpty(text)) {
            String value = text.trim();
            if (Patterns.WEB_URL.matcher(value).find()) {
                kinds.add("url");
            }
            if (EMAIL.matcher(value).find()) {
                kinds.add("email");
            }
            String digits = value.replaceAll("[^0-9+]", "");
            if (PHONE.matcher(value).find() && digits.length() >= 7 && digits.length() <= 15) {
                kinds.add("phone");
            }
            if (isAddressLike(value)) {
                kinds.add("address");
            }
            if (!imagePayload) {
                kinds.add("text");
            }
        }

        if (kinds.isEmpty()) {
            kinds.add("text");
        }
        return kinds;
    }

    public static String contentKind(String text, boolean image) {
        Set<String> kinds = contentKinds(text, image, null);
        for (String preferred : new String[]{"image", "url", "address", "phone", "email", "file", "text"}) {
            if (kinds.contains(preferred)) {
                return preferred;
            }
        }
        return "text";
    }

    private static boolean isAddressLike(String value) {
        if (value.length() < 4 || value.length() > 160) {
            return false;
        }
        int signals = 0;
        if (ADDRESS_REGION.matcher(value).find()) signals++;
        if (ADDRESS_ROAD.matcher(value).find()) signals++;
        if (ADDRESS_NUMBER.matcher(value).find()) signals++;
        if (ADDRESS_BUILDING.matcher(value).find()) signals++;
        if (value.startsWith("地址") || value.contains("地址：") || value.contains("地址:")) {
            signals++;
        }
        return signals >= 2;
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
