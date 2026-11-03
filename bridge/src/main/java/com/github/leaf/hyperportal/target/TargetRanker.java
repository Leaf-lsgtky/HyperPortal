// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.target;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Rule + usage ranking. Pinned entries are the only entries allowed to bypass the limit. */
public final class TargetRanker {

    private TargetRanker() {
    }

    public static List<ShareTargetEntry> rank(Context context,
                                              List<ShareTargetEntry> all,
                                              String contentKind,
                                              TargetConfig config) {
        return rank(context, all, Collections.singleton(contentKind), config);
    }

    public static List<ShareTargetEntry> rank(Context context,
                                              List<ShareTargetEntry> all,
                                              Set<String> contentKinds,
                                              TargetConfig config) {
        TargetConfig effective = config == null
                ? new TargetConfig(15, null, null, null) : config;
        Map<String, ShareTargetEntry> byKey = new LinkedHashMap<>();
        for (ShareTargetEntry target : all) {
            if (effective.hiddenKeys.contains(target.key)) {
                continue;
            }
            if (isSupported(target, contentKinds)) {
                byKey.put(target.key, target);
            }
        }

        List<ShareTargetEntry> result = new ArrayList<>();
        List<String> order = effective.pinnedOrder;
        for (String key : order) {
            ShareTargetEntry target = byKey.remove(key);
            if (target != null && effective.pinnedKeys.contains(key)) {
                result.add(target);
            }
        }
        for (String key : effective.pinnedKeys) {
            ShareTargetEntry target = byKey.remove(key);
            if (target != null) {
                result.add(target);
            }
        }

        List<ShareTargetEntry> rest = new ArrayList<>(byKey.values());
        final TargetRuleStore.Rules rules = TargetRuleStore.get(context);
        Collections.sort(rest, new Comparator<ShareTargetEntry>() {
            @Override
            public int compare(ShareTargetEntry left, ShareTargetEntry right) {
                int leftScore = score(left, contentKinds, effective, rules);
                int rightScore = score(right, contentKinds, effective, rules);
                if (leftScore != rightScore) {
                    return Integer.compare(rightScore, leftScore);
                }
                if (left.priority != right.priority) {
                    return Integer.compare(right.priority, left.priority);
                }
                return left.label.compareToIgnoreCase(right.label);
            }
        });

        int limit = effective.limit <= 0 ? 15 : effective.limit;
        Set<String> seenPackages = new HashSet<>();
        if (effective.deduplicateAppTargets) {
            for (ShareTargetEntry pinned : result) {
                if (pinned.packageName != null && !pinned.packageName.isEmpty()
                        && !"builtin".equals(pinned.packageName)) {
                    seenPackages.add(pinned.packageName);
                }
            }
        }

        for (ShareTargetEntry target : rest) {
            if (result.size() >= limit) {
                break;
            }
            if (effective.deduplicateAppTargets) {
                if (target.packageName != null && !target.packageName.isEmpty()
                        && !"builtin".equals(target.packageName)) {
                    if (seenPackages.contains(target.packageName)) {
                        continue;
                    }
                    seenPackages.add(target.packageName);
                }
            }
            result.add(target);
        }
        return result;
    }

    private static boolean isSupported(ShareTargetEntry target, Set<String> contentKinds) {
        if (contentKinds == null || contentKinds.isEmpty()) {
            return target.supportsText || target.supportsImage;
        }
        if (target.contentKinds != null && !target.contentKinds.isEmpty()) {
            boolean intersects = false;
            for (String kind : target.contentKinds) {
                if (contentKinds.contains(kind)) {
                    intersects = true;
                    break;
                }
            }
            if (!intersects) {
                return false;
            }
        }
        if (contentKinds.contains("image")) {
            return target.supportsImage;
        }
        return target.supportsText;
    }

    private static int score(ShareTargetEntry target, Set<String> contentKinds,
                             TargetConfig config, TargetRuleStore.Rules rules) {
        int categoryScore = rules.score(contentKinds, target.category);
        int usage = config.usage.containsKey(target.key) ? config.usage.get(target.key) : 0;
        // Semantic fit must dominate the app's global affinity. The old
        // priority*1000 formula made a high-priority map app outrank WeChat
        // even for ordinary text.
        return categoryScore * 1000 + target.priority * 10 + Math.min(usage, 100) * 2;
    }
}
