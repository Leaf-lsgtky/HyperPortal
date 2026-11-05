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
import java.util.Collections;
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
    private static final String TAG_Q = "PortalFileProvider";
    private static final String DATA_COLUMN = "_data";

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

    public static final int MAX_CACHE_FILES = 5;
    public static final long MAX_CACHE_AGE_MS = 12 * 3600 * 1000L; // 12 hours

    public static void pruneCacheAsync(Context context, String excludeName) {
        if (context == null) return;
        new Thread(() -> {
            try {
                pruneCache(context, excludeName);
            } catch (Throwable ignored) {}
        }, "hyperportal-cache-prune").start();
    }

    public static void pruneCache(Context context, String excludeName) {
        if (context == null) return;
        File dir = new File(context.getCacheDir(), INCOMING);
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return;

        long now = System.currentTimeMillis();
        List<File> survivors = new ArrayList<>();

        for (File f : files) {
            if (!f.isFile()) continue;
            if (excludeName != null && excludeName.equals(f.getName())) {
                survivors.add(f);
                continue;
            }
            long age = now - f.lastModified();
            // 1. Delete files older than 12 hours (protect newly created files within 5 minutes)
            if (age > MAX_CACHE_AGE_MS && (now - f.lastModified() > 5 * 60 * 1000L)) {
                if (f.delete()) {
                    Log.i("PortalFileProvider", "pruned expired cache file: " + f.getName() + " (age " + (age / 3600000L) + "h)");
                }
            } else {
                survivors.add(f);
            }
        }

        // 2. If surviving files exceed MAX_CACHE_FILES (5), sort by lastModified ascending (oldest first)
        if (survivors.size() > MAX_CACHE_FILES) {
            Collections.sort(survivors, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            int toDelete = survivors.size() - MAX_CACHE_FILES;
            for (File f : survivors) {
                if (toDelete <= 0) break;
                if (excludeName != null && excludeName.equals(f.getName())) {
                    continue;
                }
                // Never delete files modified within the last 3 minutes (protect in-flight drop transfers)
                if (now - f.lastModified() < 3 * 60 * 1000L) {
                    continue;
                }
                if (f.delete()) {
                    toDelete--;
                    Log.i("PortalFileProvider", "pruned LRU cache file: " + f.getName());
                }
            }
        }
    }

    @Override
    public boolean onCreate() {
        pruneCacheAsync(getContext(), null);
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
        int uid = android.os.Binder.getCallingUid();
        if (uid != android.os.Process.myUid()) {
            Log.i(TAG_Q, "openFile caller=" + callerLabel(uid)
                    + " name=" + name + " mode=" + mode + " isFile=" + file.isFile());
        }
        if (mode == null || mode.isEmpty() || mode.startsWith("r")) {
            if (!file.isFile()) {
                throw new FileNotFoundException("missing " + name);
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        if (mode.contains("w")) {
            pruneCacheAsync(getContext(), name);
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
            if (mime.startsWith("image/")) {
                return new String[]{mime, "image/*"};
            } else {
                return new String[]{mime, "*/*"};
            }
        }
        return new String[]{"*/*"};
    }

    @Override
    public String getType(Uri uri) {
        return resolveType(uri);
    }

    private String resolveType(Uri uri) {
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
        int uid = android.os.Binder.getCallingUid();
        boolean external = uid != android.os.Process.myUid();
        if (uri == null || getContext() == null) return null;
        String name = uri.getLastPathSegment();
        if (name == null) return null;
        File file = new File(new File(getContext().getCacheDir(), INCOMING), name);
        if (!file.exists()) {
            if (external) Log.i(TAG_Q, "query MISSING caller=" + callerLabel(uid) + " name=" + name);
            return null;
        }

        // Two opposite requirements from real drop targets, both verified on-device.
        //
        //  * Weibo "发送给好友" (com.sina.weibo.weiyou.utils.ah.a(Context, Uri))
        //    projects exactly {"_data"} and, when that column exists, returns its
        //    value verbatim as the upload path. Every path we can hand out lives in
        //    the app-private cacheDir and is unreadable for the receiving uid, so the
        //    upload reads zero bytes. When the column is absent it falls back to
        //    openFileDescriptor() and copies the stream into its own cache.
        //
        //  * WeChat's chat-page drop target (com.tencent.mm.ui.chatting.component.db
        //    .onDrag → com.tencent.mm.pluginsdk.ui.tools.m7, "MicroMsg.UriFileHelper")
        //    queries with a null projection and gives up with a "发送失败" toast as
        //    soon as getColumnIndex("_data") == -1. It only uses the value to derive
        //    the file name and for a first copy attempt; when that copy yields <= 0
        //    bytes it retries with the content:// URI itself, which our exported
        //    provider serves fine. So the value never has to be readable.
        //
        // The two callers are distinguishable by projection: only Weibo asks for
        // "_data" explicitly, WeChat asks for every column. Serve "_data" for a null
        // projection, keep hiding it from an explicit "_data" request.
        String[] cols;
        if (projection == null || projection.length == 0) {
            cols = new String[]{DATA_COLUMN, OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        } else {
            java.util.LinkedHashSet<String> wanted = new java.util.LinkedHashSet<>();
            for (String col : projection) {
                if (col == null || DATA_COLUMN.equals(col)) continue;
                wanted.add(col);
            }
            wanted.add(OpenableColumns.DISPLAY_NAME);
            cols = wanted.toArray(new String[0]);
        }
        if (external) {
            Log.i(TAG_Q, "query caller=" + callerLabel(uid) + " name=" + name
                    + " projection=" + java.util.Arrays.toString(projection)
                    + " served=" + java.util.Arrays.toString(cols)
                    + " size=" + file.length());
        }
        MatrixCursor cursor = new MatrixCursor(cols);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String col : cols) {
            if (DATA_COLUMN.equals(col)) {
                // Deliberately the app-private path: unreadable on purpose so that
                // targets which *can* fall back to the stream always take that
                // branch, while targets that only need a non-null value (WeChat)
                // still work. See the comment above.
                row.add(file.getAbsolutePath());
            } else if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                row.add(name);
            } else if (OpenableColumns.SIZE.equals(col)) {
                row.add(file.length());
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    private String callerLabel(int uid) {
        try {
            Context ctx = getContext();
            if (ctx != null) {
                String[] pkgs = ctx.getPackageManager().getPackagesForUid(uid);
                if (pkgs != null && pkgs.length > 0) return pkgs[0] + "(" + uid + ")";
            }
        } catch (Throwable ignored) {
        }
        return String.valueOf(uid);
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
