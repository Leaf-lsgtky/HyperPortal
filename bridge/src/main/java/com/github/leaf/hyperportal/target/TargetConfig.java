// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.target;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Snapshot of the target-menu preferences, safe to read from the SystemUI process. */
public final class TargetConfig {

    public static final String AUTHORITY = "com.github.leaf.hyperportal.files";
    public static final int DEFAULT_LIMIT = 15;

    public final int limit;
    public final Set<String> pinnedKeys;
    public final List<String> pinnedOrder;
    public final Set<String> hiddenKeys;
    public final boolean deduplicateAppTargets;
    public final boolean enableQrScan;
    public final Map<String, Integer> usage;

    public TargetConfig(int limit, Set<String> pinnedKeys, List<String> pinnedOrder,
                        Map<String, Integer> usage) {
        this(limit, pinnedKeys, pinnedOrder, null, true, true, usage);
    }

    public TargetConfig(int limit, Set<String> pinnedKeys, List<String> pinnedOrder,
                        Set<String> hiddenKeys, Map<String, Integer> usage) {
        this(limit, pinnedKeys, pinnedOrder, hiddenKeys, true, true, usage);
    }

    public TargetConfig(int limit, Set<String> pinnedKeys, List<String> pinnedOrder,
                        Set<String> hiddenKeys, boolean deduplicateAppTargets,
                        Map<String, Integer> usage) {
        this(limit, pinnedKeys, pinnedOrder, hiddenKeys, deduplicateAppTargets, true, usage);
    }

    public TargetConfig(int limit, Set<String> pinnedKeys, List<String> pinnedOrder,
                        Set<String> hiddenKeys, boolean deduplicateAppTargets,
                        boolean enableQrScan, Map<String, Integer> usage) {
        this.limit = limit <= 0 ? 15 : limit;
        this.pinnedKeys = pinnedKeys == null ? new LinkedHashSet<>() : pinnedKeys;
        this.pinnedOrder = pinnedOrder == null ? new ArrayList<>() : pinnedOrder;
        this.hiddenKeys = hiddenKeys == null ? new LinkedHashSet<>() : hiddenKeys;
        this.deduplicateAppTargets = deduplicateAppTargets;
        this.enableQrScan = enableQrScan;
        this.usage = usage == null ? new HashMap<>() : usage;
    }

    public static TargetConfig defaults(Context context) {
        return new TargetConfig(
                TargetRuleStore.defaultLimit(context),
                new LinkedHashSet<>(TargetRuleStore.defaultPinned(context)),
                TargetRuleStore.defaultPinned(context),
                new LinkedHashSet<>(),
                true,
                true,
                new HashMap<>());
    }

    public static TargetConfig fromProvider(Context context) {
        if ("com.android.systemui".equals(context.getPackageName())) {
            try {
                Bundle bundle = com.github.leaf.hyperportal.system.PortalSystemUiModule.getCachedConfigBundle(context);
                if (bundle != null && bundle.getBoolean("config_ready", false)) {
                    return fromBundle(bundle, context);
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Uri uri = Uri.parse("content://" + AUTHORITY);
            Bundle bundle = context.getContentResolver().call(uri, "getConfig", null, null);
            if (bundle != null) {
                return fromBundle(bundle, context);
            }
        } catch (Throwable ignored) {
        }
        return defaults(context);
    }

    public static TargetConfig fromBundle(Bundle bundle, Context context) {
        if (bundle == null) {
            return defaults(context);
        }
        int limit = bundle.getInt("target_limit", TargetRuleStore.defaultLimit(context));
        ArrayList<String> pinned = bundle.getStringArrayList("target_pinned");
        if (pinned == null) {
            pinned = new ArrayList<>(TargetRuleStore.defaultPinned(context));
        }
        ArrayList<String> order = bundle.getStringArrayList("target_order");
        if (order == null) {
            order = new ArrayList<>(pinned);
        }
        ArrayList<String> hidden = bundle.getStringArrayList("target_hidden");
        Set<String> hiddenKeys = hidden == null ? new LinkedHashSet<>() : new LinkedHashSet<>(hidden);
        boolean deduplicate = bundle.getBoolean("deduplicate_app_targets", true);
        boolean enableQrScan = bundle.getBoolean("enable_qr_scan", true);
        Map<String, Integer> usage = new HashMap<>();
        String usageText = bundle.getString("target_usage", "");
        if (!TextUtils.isEmpty(usageText)) {
            for (String item : usageText.split("\\n")) {
                // Older SystemUI caches used "key:count"; current writes use
                // "key=count". Keep both readable so a one-time upgrade does
                // not wipe the user's target-affinity counters.
                int split = Math.max(item.lastIndexOf('='), item.lastIndexOf(':'));
                if (split <= 0) {
                    continue;
                }
                try {
                    usage.put(item.substring(0, split), Integer.parseInt(item.substring(split + 1)));
                } catch (Throwable ignored) {
                }
            }
        }
        return new TargetConfig(limit, new LinkedHashSet<>(pinned), order, hiddenKeys, deduplicate, enableQrScan, usage);
    }
}
