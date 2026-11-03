// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.target;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Local, dependency-free recommendation rules.
 *
 * <p>The rule file is deliberately a plain JSON asset so it can be inspected and
 * replaced without rebuilding the Java ranking code. It only maps installed share
 * activities to semantic categories and priorities; the actual activity list always
 * comes from PackageManager.</p>
 */
public final class TargetRuleStore {

    private static final String TAG = "HyperPortal/Rules";
    private static final String ASSET_NAME = "target_rules.json";
    private static final int DEFAULT_LIMIT = 15;

    private static volatile Rules sRules;
    private static volatile String sModuleApkPath;

    private TargetRuleStore() {
    }

    /** SystemUI cannot see the module assets directly; the hook supplies its APK path. */
    public static void setModuleApkPath(String path) {
        sModuleApkPath = path;
        sRules = null;
    }

    public static final class Rule {
        public final String packageName;
        public final String activityContains;
        public final String labelContains;
        public final String category;
        public final String label;
        public final int priority;
        /** Empty = generic app rule; non-empty = only eligible for these semantic kinds. */
        public final Set<String> contentKinds;
        /** send / view / dial / mail / sms / search. Empty means send. */
        public final String action;
        public final String deeplink;
        /** App-level rule that may not expose an ACTION_SEND activity. */
        public final boolean explicit;

        Rule(String packageName, String activityContains, String labelContains,
             String category, String label, int priority) {
            this(packageName, activityContains, labelContains, category, label, priority,
                    Collections.emptySet(), "", null, false);
        }

        Rule(String packageName, String activityContains, String labelContains,
             String category, String label, int priority, Set<String> contentKinds,
             String action, String deeplink, boolean explicit) {
            this.packageName = packageName == null ? "" : packageName;
            this.activityContains = activityContains == null ? "" : activityContains;
            this.labelContains = labelContains == null ? "" : labelContains;
            this.category = category == null ? "other" : category;
            this.label = label == null ? "" : label;
            this.priority = priority;
            this.contentKinds = contentKinds == null
                    ? Collections.emptySet() : Collections.unmodifiableSet(contentKinds);
            this.action = action == null ? "" : action;
            this.deeplink = deeplink;
            this.explicit = explicit;
        }

        public boolean isSpecific() {
            return !activityContains.isEmpty() || !labelContains.isEmpty();
        }

        int specificity() {
            int score = 0;
            if (!packageName.isEmpty()) score += 2;
            if (!activityContains.isEmpty()) score += 4;
            if (!labelContains.isEmpty()) score += 2;
            if (!contentKinds.isEmpty()) score += 1;
            return score;
        }

        boolean matches(String pkg, String activity, String targetLabel) {
            if (!packageName.isEmpty() && !packageName.equals(pkg)) {
                return false;
            }
            String activityLower = activity == null ? "" : activity.toLowerCase(Locale.US);
            if (!activityContains.isEmpty()
                    && !activityLower.contains(activityContains.toLowerCase(Locale.US))) {
                return false;
            }
            String labelLower = targetLabel == null ? "" : targetLabel.toLowerCase(Locale.US);
            if (!labelContains.isEmpty()
                    && !labelLower.contains(labelContains.toLowerCase(Locale.US))) {
                return false;
            }
            return !packageName.isEmpty() || !activityContains.isEmpty() || !labelContains.isEmpty();
        }
    }

    public static final class Rules {
        public final int defaultLimit;
        public final List<String> defaultPinned;
        public final List<Rule> rules;
        public final Map<String, Map<String, Integer>> categoryScores;

        Rules(int defaultLimit, List<String> defaultPinned,
              List<Rule> rules, Map<String, Map<String, Integer>> categoryScores) {
            this.defaultLimit = defaultLimit;
            this.defaultPinned = defaultPinned;
            List<Rule> ordered = new ArrayList<>(rules);
            // A package-only rule must never shadow a more specific activity rule.
            ordered.sort(Comparator.comparingInt(Rule::specificity).reversed());
            this.rules = ordered;
            this.categoryScores = categoryScores;
        }

        public Rule match(String packageName, String activityName, String label) {
            for (Rule rule : rules) {
                if (rule.matches(packageName, activityName, label)) {
                    return rule;
                }
            }
            return null;
        }

        public int score(String contentKind, String category) {
            Map<String, Integer> scores = categoryScores.get(contentKind);
            if (scores == null) {
                scores = categoryScores.get("text");
            }
            Integer score = scores == null ? null : scores.get(category);
            return score == null ? 0 : score;
        }

        public int score(Set<String> contentKinds, String category) {
            int best = 0;
            if (contentKinds != null) {
                for (String kind : contentKinds) {
                    best = Math.max(best, score(kind, category));
                }
            }
            return best;
        }
    }

    public static Rules get(Context context) {
        Rules cached = sRules;
        if (cached != null) {
            return cached;
        }
        synchronized (TargetRuleStore.class) {
            if (sRules == null) {
                sRules = load(context);
            }
            return sRules;
        }
    }

    public static int defaultLimit(Context context) {
        return get(context).defaultLimit;
    }

    public static List<String> defaultPinned(Context context) {
        return new ArrayList<>(get(context).defaultPinned);
    }

    private static Rules load(Context context) {
        JSONObject json = null;
        try {
            json = new JSONObject(readAsset(context));
        } catch (Throwable throwable) {
            Log.w(TAG, "target_rules.json unavailable, using built-in fallback", throwable);
        }
        if (json == null) {
            return fallback();
        }
        try {
            int limit = json.optInt("defaultLimit", DEFAULT_LIMIT);
            if (limit < 3 || limit > 30) {
                limit = DEFAULT_LIMIT;
            }
            List<String> pinned = stringList(json.optJSONArray("defaultPinned"));
            if (pinned.isEmpty()) {
                pinned = defaultPinnedKeys();
            }
            Map<String, Map<String, Integer>> scores = new HashMap<>();
            JSONObject scoreObject = json.optJSONObject("categoryScores");
            if (scoreObject != null) {
                JSONArray kinds = scoreObject.names();
                if (kinds != null) {
                    for (int i = 0; i < kinds.length(); i++) {
                        String kind = kinds.optString(i);
                        JSONObject values = scoreObject.optJSONObject(kind);
                        if (values == null) {
                            continue;
                        }
                        Map<String, Integer> map = new HashMap<>();
                        JSONArray names = values.names();
                        if (names != null) {
                            for (int j = 0; j < names.length(); j++) {
                                String name = names.optString(j);
                                map.put(name, values.optInt(name, 0));
                            }
                        }
                        scores.put(kind, map);
                    }
                }
            }
            if (scores.isEmpty()) {
                scores = fallbackScores();
            }
            List<Rule> rules = new ArrayList<>();
            JSONArray array = json.optJSONArray("rules");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    rules.add(new Rule(
                            item.optString("package", ""),
                            item.optString("activityContains", ""),
                            item.optString("labelContains", ""),
                            item.optString("category", "other"),
                            item.optString("label", ""),
                            item.optInt("priority", 0),
                            stringSet(item.optJSONArray("contentKinds")),
                            item.optString("action", "").toLowerCase(Locale.US),
                            item.optString("deeplink", ""),
                            item.optBoolean("explicit", false)
                    ));
                }
            }
            return new Rules(limit, pinned, rules, scores);
        } catch (Throwable throwable) {
            Log.w(TAG, "target_rules.json parse failed, using fallback", throwable);
            return fallback();
        }
    }

    private static String readAsset(Context context) throws Exception {
        InputStream input = null;
        String apkPath = sModuleApkPath;
        if (apkPath != null && new File(apkPath).isFile()) {
            try (ZipFile zip = new ZipFile(apkPath)) {
                ZipEntry entry = zip.getEntry("assets/" + ASSET_NAME);
                if (entry != null) {
                    return readStream(zip.getInputStream(entry));
                }
            } catch (Throwable ignored) {
            }
        }
        if (input == null) {
            try {
                input = context.getAssets().open(ASSET_NAME);
            } catch (Throwable ignored) {
            }
        }
        if (input == null) {
            try {
                Context module = context.createPackageContext(
                        "com.github.leaf.hyperportal",
                        Context.CONTEXT_IGNORE_SECURITY);
                input = module.getAssets().open(ASSET_NAME);
            } catch (Throwable ignored) {
            }
        }
        if (input == null) {
            try {
                input = TargetRuleStore.class.getClassLoader()
                        .getResourceAsStream("assets/" + ASSET_NAME);
            } catch (Throwable ignored) {
            }
        }
        if (input == null) {
            return "{}";
        }
        return readStream(input);
    }

    private static String readStream(InputStream input) throws Exception {
        try (InputStream stream = input;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString("UTF-8");
        }
    }

    private static List<String> stringList(JSONArray array) {
        List<String> result = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "");
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private static Set<String> stringSet(JSONArray array) {
        Set<String> result = new HashSet<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "");
                if (!value.isEmpty()) {
                    result.add(value.toLowerCase(Locale.US));
                }
            }
        }
        return result;
    }

    public static List<String> defaultPinnedKeys() {
        return new ArrayList<>(Arrays.asList(
                "builtin:copy", "builtin:save", "builtin:segment", "builtin:share", "builtin:search"));
    }

    private static Rules fallback() {
        Map<String, Map<String, Integer>> scores = fallbackScores();
        List<Rule> rules = new ArrayList<>();
        Set<String> address = Collections.singleton("address");
        Set<String> url = Collections.singleton("url");
        Set<String> phone = Collections.singleton("phone");
        Set<String> email = Collections.singleton("email");
        rules.add(new Rule("com.autonavi.minimap", "", "", "map", "搜索地址", 120,
                address, ShareTargetEntry.ACTION_VIEW,
                "amapuri://search/general?sourceApplication=hyperportal&keyword={text}", true));
        rules.add(new Rule("com.baidu.BaiduMap", "", "", "map", "搜索地址", 118,
                address, ShareTargetEntry.ACTION_VIEW,
                "baidumap://map/place/search?src=hyperportal&query={text}", true));
        rules.add(new Rule("com.tencent.map", "", "", "map", "搜索地址", 116,
                address, ShareTargetEntry.ACTION_VIEW,
                "qqmap://map/search?keyword={text}", true));
        rules.add(new Rule("com.google.android.apps.maps", "", "", "map", "搜索地址", 110,
                address, ShareTargetEntry.ACTION_VIEW, "geo:0,0?q={text}", true));
        rules.add(new Rule("com.tencent.mobileqq", "qzone", "", "social", "QQ空间", 104));
        rules.add(new Rule("com.tencent.mobileqq", "", "", "chat", "QQ", 98));
        rules.add(new Rule("com.tencent.mm", "timeline", "", "social", "朋友圈", 105));
        rules.add(new Rule("com.tencent.mm", "", "", "chat", "微信", 100));
        rules.add(new Rule("com.android.chrome", "", "", "browser", "打开网页", 100,
                url, ShareTargetEntry.ACTION_VIEW, "", true));
        rules.add(new Rule("com.quark.browser", "", "", "browser", "打开网页", 100,
                url, ShareTargetEntry.ACTION_VIEW, "", true));
        rules.add(new Rule("com.miui.gallery", "", "", "gallery", "相册", 100));
        rules.add(new Rule("com.miui.notes", "", "", "notes", "笔记", 100));
        rules.add(new Rule("com.google.android.gm", "", "", "mail", "写邮件", 100,
                email, ShareTargetEntry.ACTION_MAIL, "", true));
        rules.add(new Rule("com.android.contacts", "", "", "dialer", "拨号", 100,
                phone, ShareTargetEntry.ACTION_DIAL, "", true));
        return new Rules(DEFAULT_LIMIT, defaultPinnedKeys(), rules, scores);
    }

    private static Map<String, Map<String, Integer>> fallbackScores() {
        Map<String, Map<String, Integer>> scores = new HashMap<>();
        scores.put("address", mapOf("map", 100, "taxi", 80, "travel", 55,
                "browser", 30, "chat", 15, "notes", 10));
        scores.put("url", mapOf("browser", 100, "readlater", 75, "chat", 30,
                "notes", 20, "mail", 15));
        scores.put("phone", mapOf("dialer", 100, "contacts", 95, "sms", 90, "chat", 40));
        scores.put("email", mapOf("mail", 100, "chat", 10, "notes", 10));
        scores.put("image", mapOf("gallery", 100, "notes", 90, "chat", 80,
                "social", 70, "file", 60, "browser", 20));
        scores.put("text", mapOf("chat", 90, "notes", 80, "social", 70, "mail", 40,
                "browser", 30, "readlater", 35, "file", 20));
        return scores;
    }

    private static Map<String, Integer> mapOf(Object... values) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), (Integer) values[i + 1]);
        }
        return map;
    }
}
