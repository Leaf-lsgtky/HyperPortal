// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import com.github.leaf.hyperportal.data.HyperPortalPreferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Write-only staging provider & cross-process configuration server.
 *
 * AES runs in the target app process, so it cannot write directly into the
 * portal's private cache. This provider exposes a single write-only
 * "incoming/" namespace. The bridge streams the original file here first and
 * then sends only the cache file name to PortalService, avoiding both the
 * 1MB Binder limit and the Android 14 "no file descriptors in startService"
 * restriction.
 *
 * It also handles cross-process configuration queries (master switch, duration,
 * touch slop, gesture blacklist) via call("getConfig", ...).
 */
public class PortalFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.github.leaf.hyperportal.files";
    public static final String INCOMING = "incoming";

    private static volatile boolean sSystemUiActive = false;

    public static boolean isSystemUiActive() {
        return sSystemUiActive;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("reportSystemUiActive".equals(method)) {
            sSystemUiActive = true;
            return new Bundle();
        }
        if ("grantDragPermissions".equals(method)) {
            Context ctx = getContext();
            if (ctx != null && arg != null) {
                try {
                    Uri targetUri = Uri.parse(arg);
                    grantToCommonTargets(ctx, targetUri);
                } catch (Throwable t) {
                    Log.w("PortalFileProvider", "grantDragPermissions failed", t);
                }
            }
            return new Bundle();
        }
        if ("getConfig".equals(method)) {
            Context ctx = getContext();
            if (ctx == null) return null;
            HyperPortalPreferences.Config cfg = HyperPortalPreferences.INSTANCE.getConfig(ctx);
            Bundle bundle = new Bundle();
            bundle.putBoolean("master_switch", cfg.getMasterSwitch());
            bundle.putBoolean("landscape_trigger", cfg.getLandscapeTrigger());
            bundle.putInt("edge_scroll_margin", cfg.getEdgeScrollMargin());
            bundle.putInt("long_press_duration", cfg.getLongPressDuration());
            bundle.putInt("touch_slop", cfg.getTouchSlop());
            bundle.putInt("trigger_threshold", cfg.getTriggerThreshold());
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
        if ("recordTargetUse".equals(method)) {
            Context ctx = getContext();
            if (ctx != null && arg != null) {
                HyperPortalPreferences.INSTANCE.recordTargetUse(ctx, arg);
            }
            return new Bundle();
        }
        return super.call(method, arg, extras);
    }

    private static void grantToCommonTargets(Context ctx, Uri uri) {
        String[] targets = new String[]{
                "com.miui.notes",
                "com.tencent.mm",
                "com.tencent.mobileqq",
                "com.sina.weibo",
                "org.telegram.messenger",
                "com.coolapk.market",
                "com.android.fileexplorer",
                "com.zhihu.android",
                "com.xingin.xhs",
                "com.baidu.tieba",
                "com.android.systemui"
        };
        for (String pkg : targets) {
            try {
                ctx.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Throwable throwable) {
                Log.w("PortalFileProvider", "grant " + pkg + " failed for " + uri, throwable);
            }
        }
        Log.i("PortalFileProvider", "granted drag uri " + uri + " to " + targets.length + " targets");
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<android.app.ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                if (procs != null) {
                    for (android.app.ActivityManager.RunningAppProcessInfo pi : procs) {
                        if (pi.pkgList != null) {
                            for (String p : pi.pkgList) {
                                try {
                                    ctx.grantUriPermission(p, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("..")) {
            throw new FileNotFoundException("bad name");
        }
        File dir = new File(getContext().getCacheDir(), INCOMING);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new FileNotFoundException("cannot create cache dir");
        }
        File file = new File(dir, name);
        if (mode == null || mode.isEmpty() || mode.startsWith("r")) {
            if (!file.isFile()) {
                throw new FileNotFoundException("missing " + name);
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        if (mode.contains("w")) {
            return ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_CREATE
                            | ParcelFileDescriptor.MODE_WRITE_ONLY
                            | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        try {
            int modeBits = ParcelFileDescriptor.parseMode(mode);
            return ParcelFileDescriptor.open(file, modeBits);
        } catch (Throwable t) {
            throw new FileNotFoundException("unsupported mode: " + mode);
        }
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        ParcelFileDescriptor pfd = openFile(uri, mode);
        return pfd != null ? new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH) : null;
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
        return openAssetFile(uri, "r");
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        String mime = getType(uri);
        if (mime != null) {
            return new String[]{mime, "image/*"};
        }
        return new String[]{"image/png", "image/jpeg", "image/*"};
    }

    @Override
    public String getType(Uri uri) {
        if (uri != null) {
            String name = uri.getLastPathSegment();
            if (name != null) {
                String lower = name.toLowerCase(Locale.US);
                if (lower.endsWith(".png")) return "image/png";
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
                if (lower.endsWith(".webp")) return "image/webp";
                if (lower.endsWith(".gif")) return "image/gif";
                int dot = lower.lastIndexOf('.');
                if (dot >= 0) {
                    String ext = lower.substring(dot + 1);
                    String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                    if (mime != null) return mime;
                }
                String guess = java.net.URLConnection.guessContentTypeFromName(name);
                if (guess != null) return guess;
            }
        }
        return "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (uri == null || getContext() == null) return null;
        String name = uri.getLastPathSegment();
        if (name == null) return null;
        File file = new File(new File(getContext().getCacheDir(), INCOMING), name);
        if (!file.exists()) return null;

        String[] cols = projection != null ? projection : new String[]{
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        };
        MatrixCursor cursor = new MatrixCursor(cols);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String col : cols) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                row.add(name);
            } else if (OpenableColumns.SIZE.equals(col)) {
                row.add(file.length());
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri == null || getContext() == null) return 0;
        String name = uri.getLastPathSegment();
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("..")) return 0;
        File file = new File(new File(getContext().getCacheDir(), INCOMING), name);
        if (file.exists() && file.delete()) {
            return 1;
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
