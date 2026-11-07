// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import com.github.leaf.hyperportal.data.HyperPortalPreferences;
import com.github.leaf.hyperportal.data.PortalStatusStore;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Cross-process configuration bootstrap provider.
 *
 * Drag payload staging lives in SystemUI's FileProvider now; this provider is
 * kept only so SystemUI / system_server / the AES bridge can recover the
 * authoritative preferences before SystemUI's cache has been populated (for
 * example on the very first boot after install).
 *
 * It also doubles as the AES handshake: a third-party app's
 * {@code Bridge.ensureConfig()} reaches {@code getConfig()} here when it
 * cannot read SystemUI's {@code __config__} endpoint, and the caller uid is
 * recorded for the home page.
 */
public class PortalFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.github.leaf.hyperportal.files";
    private static final String TAG = "PortalFileProvider";

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("getConfig".equals(method)) {
            Context ctx = getContext();
            if (ctx == null) return null;
            int callerUid = android.os.Binder.getCallingUid();
            if (callerUid != android.os.Process.myUid()) {
                PortalStatusStore.INSTANCE.recordAesHandshake(ctx, callerUid);
            }
            HyperPortalPreferences.Config cfg = HyperPortalPreferences.INSTANCE.getConfig(ctx);
            Bundle bundle = new Bundle();
            bundle.putBoolean("master_switch", cfg.getMasterSwitch());
            bundle.putBoolean("landscape_trigger", cfg.getLandscapeTrigger());
            bundle.putInt("edge_scroll_margin", cfg.getEdgeScrollMargin());
            bundle.putInt("long_press_duration", cfg.getLongPressDuration());
            bundle.putInt("touch_slop", cfg.getTouchSlop());
            bundle.putBoolean("same_window_guard", cfg.getSameWindowGuard());
            bundle.putBoolean("coloros_consume_anim", cfg.getColorosConsumeAnim());
            bundle.putInt("trigger_threshold", cfg.getTriggerThreshold());
            bundle.putString("trigger_position", cfg.getTriggerPosition());
            bundle.putStringArrayList("gesture_blacklist", new ArrayList<>(cfg.getBlacklist()));
            bundle.putInt("target_limit", cfg.getTargetLimit());
            bundle.putStringArrayList("target_pinned", new ArrayList<>(cfg.getPinnedTargetKeys()));
            bundle.putStringArrayList("target_order", new ArrayList<>(cfg.getPinnedTargetOrder()));
            bundle.putStringArrayList("target_hidden", new ArrayList<>(cfg.getHiddenTargetKeys()));
            bundle.putBoolean("deduplicate_app_targets", cfg.getDeduplicateAppTargets());
            bundle.putBoolean("enable_qr_scan", cfg.getEnableQrScan());
            StringBuilder usage = new StringBuilder();
            for (java.util.Map.Entry<String, Integer> entry : cfg.getTargetUsage().entrySet()) {
                if (usage.length() > 0) usage.append('\n');
                usage.append(entry.getKey()).append('=').append(entry.getValue());
            }
            bundle.putString("target_usage", usage.toString());
            return bundle;
        }
        return super.call(method, arg, extras);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        throw new FileNotFoundException("PortalFileProvider no longer hosts files");
    }
}
