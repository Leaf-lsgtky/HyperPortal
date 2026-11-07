package com.github.leaf.hyperportal.system;

import android.app.Application;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.leaf.hyperportal.PortalShareContract;
import com.github.leaf.hyperportal.bigbang.BigBangContract;
import com.github.leaf.hyperportal.ocr.AicrOcrClient;
import com.github.leaf.hyperportal.target.ShareTargetEntry;
import com.github.leaf.hyperportal.target.TargetConfig;
import com.github.leaf.hyperportal.target.TargetRanker;
import com.github.leaf.hyperportal.target.BuiltInIconDrawable;
import com.github.leaf.hyperportal.target.TargetRepository;
import com.github.leaf.hyperportal.target.TargetRuleStore;
import com.github.leaf.hyperportal.transferdock.TransferDockController;
import com.github.leaf.hyperportal.transferdock.TransferDockDragGuard;
import com.github.leaf.hyperportal.transferdock.TransferDockItem;
import com.github.leaf.hyperportal.transferdock.TransferDockManager;
import com.github.leaf.hyperportal.util.QrCodeScanner;
import com.github.leaf.hyperportal.view.ColorOSDockBar;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Patterns;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * SystemUI Injection Module for HyperPortal.
 *
 * Runs inside com.android.systemui:
 * - 100% immune to background task killing / battery freeze.
 * - Zero floating-window permission prompt (uses TYPE_NAVIGATION_BAR_PANEL).
 * - 120Hz VSYNC prioritized rendering (SystemUI RenderThread).
 * - Renders ColorOS Aurora GlowView & bottom draggable application shelf.
 */
public final class PortalSystemUiModule {

    private static final String TAG = "HyperPortalSysUI";
    private static final String ACTION_DRAG_STATE = "com.github.leaf.hyperportal.ACTION_DRAG_STATE";
    private static final String ACTION_SHOW = "com.github.leaf.hyperportal.SHOW";
    public static final String ACTION_REQUEST_GRANT = "com.github.leaf.hyperportal.ACTION_REQUEST_GRANT";
    public static final String SYSUI_PROVIDER_AUTHORITY = "com.android.systemui.fileprovider";
    public static final String INCOMING_PATH_PREFIX = "/hyperportal_incoming/";
    public static final String INCOMING_URI_PREFIX =
            "content://com.android.systemui.fileprovider/hyperportal_incoming/";
    private static final String CONFIG_CACHE_PREFS = "hyperportal_systemui";
    private static final String KEY_AES_ENTRIES = "aes_entries";
    private static final int MAX_AES_ENTRIES = 16;
    private static final int MAX_CACHED_FILES = 5;
    private static final long MAX_CACHE_AGE_MS = 12L * 60L * 60L * 1000L;
    /** Never evict a file that a share/drag may still be reading. */
    private static final long CACHE_PROTECT_MS = 3L * 60L * 1000L;
    private static final int TYPE_NAVIGATION_BAR_PANEL = 2024;

    private static final String[] COMMON_SHARE_PACKAGES = new String[]{
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.tencent.tim",
            "com.sina.weibo",
            "com.xingin.xhs",
            "com.coolapk.market",
            "com.ss.android.ugc.aweme",
            "com.smile.gifmaker",
            "tv.danmaku.bili",
            "com.alibaba.android.rimet",
            "com.tencent.wework",
            "com.ss.android.lark",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "tw.nekomimi.nekogram",
            "com.twitter.android",
            "com.instagram.android",
            "com.whatsapp",
            "com.discord",
            "com.baidu.tieba",
            "com.zhihu.android",
            "com.taobao.taobao",
            "com.jingdong.app.mall",
            "com.xunmeng.pinduoduo",
            "com.miui.gallery",
            "com.miui.notes",
            "com.android.mms",
            "com.android.bluetooth",
            "com.miui.mishare.connectivity",
            "com.android.systemui",
            "com.github.leaf.hyperportal"
    };

    private static final Set<String> sHookedProviderClasses =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static volatile String sLastAesPackage;
    private static volatile long sLastAesWriteAt;
    private static volatile XposedInterface sXposed;
    private static volatile Context sSysUiContext;
    private static volatile SystemUiOverlay sOverlay;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    /**
     * Everything that touches the module's ContentProvider, PackageManager or
     * icon decoding runs here. SystemUI's main thread must stay responsive
     * during boot: a >5s stall makes the input dispatcher declare an ANR on
     * the first gesture and the system kills and restarts SystemUI.
     */
    private static final ExecutorService sBgExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "hyperportal-sysui");
        thread.setDaemon(true);
        return thread;
    });

    public static void initSystemUi(XposedInterface xposed, XposedModuleInterface.PackageLoadedParam param) {
        sXposed = xposed;
        if (isSecondarySystemUiProcess()) {
            // Only the main SystemUI process owns the portal window and the
            // FileProvider hooks; a ":xxx" helper process must not install a
            // second overlay or answer drag broadcasts.
            Log.i(TAG, "skipping portal overlay in secondary SystemUI process");
            return;
        }
        try {
            if (xposed.getModuleApplicationInfo() != null) {
                TargetRuleStore.setModuleApkPath(xposed.getModuleApplicationInfo().sourceDir);
            }
        } catch (Throwable ignored) {
        }
        try {
            ClassLoader cl = param != null ? param.getDefaultClassLoader() : null;
            if (cl == null && sSysUiContext != null) {
                cl = sSysUiContext.getClassLoader();
            }
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            installSystemUiProviderHooks(xposed, cl);

            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Object currentApp = activityThreadClass.getMethod("currentApplication").invoke(null);
                if (currentApp instanceof Application) {
                    sSysUiContext = ((Application) currentApp).getApplicationContext();
                    ensureProviderHooksForContext(sSysUiContext);
                    sHandler.post(() -> initOverlay(sSysUiContext));
                    xposed.log(Log.INFO, TAG, "SystemUI already running, overlay initialized immediately!");
                }
            } catch (Throwable t) {
                xposed.log(Log.WARN, TAG, "currentApplication check skipped: " + t.getMessage());
            }

            Class<?> appClass = null;
            try {
                appClass = Class.forName("com.android.systemui.SystemUIApplication", false, cl);
            } catch (Throwable t) {
                appClass = Class.forName("android.app.Application", false, cl);
            }

            Method onCreateMethod = appClass.getDeclaredMethod("onCreate");
            xposed.hook(onCreateMethod).setId("sysui-app-onCreate").intercept(chain -> {
                Object result = chain.proceed();
                try {
                    Object thisObj = chain.getThisObject();
                    if (thisObj instanceof Application) {
                        Application app = (Application) thisObj;
                        sSysUiContext = app.getApplicationContext();
                        ensureProviderHooksForContext(sSysUiContext);
                        sHandler.post(() -> initOverlay(sSysUiContext));
                        xposed.log(Log.INFO, TAG, "SystemUIApplication.onCreate intercepted, overlay initialized!");
                    }
                } catch (Throwable t) {
                    xposed.log(Log.ERROR, TAG, "initOverlay failed", t);
                }
                return result;
            });
            xposed.log(Log.INFO, TAG, "Hooked SystemUIApplication.onCreate successfully!");
        } catch (Throwable throwable) {
            xposed.log(Log.ERROR, TAG, "Failed to hook SystemUI", throwable);
        }
    }

    private static void installSystemUiProviderHooks(XposedInterface xposed, ClassLoader cl) {
        if (xposed == null || cl == null) return;
        String[] candidates = new String[]{
                "androidx.core.content.FileProvider",
                "android.support.v4.content.FileProvider"
        };
        for (String name : candidates) {
            try {
                Class<?> cls = Class.forName(name, false, cl);
                hookProviderClass(xposed, cls);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void ensureProviderHooksForContext(Context context) {
        if (context == null || sXposed == null) return;
        try {
            ClassLoader cl = context.getClassLoader();
            installSystemUiProviderHooks(sXposed, cl);
            android.content.pm.ProviderInfo info = context.getPackageManager()
                    .resolveContentProvider(SYSUI_PROVIDER_AUTHORITY, 0);
            if (info != null && info.name != null) {
                Class<?> cls = Class.forName(info.name, false, cl);
                hookProviderClass(sXposed, cls);
            }
        } catch (Throwable t) {
            Log.w(TAG, "ensureProviderHooksForContext failed", t);
        }
    }

    private static void hookProviderClass(XposedInterface xposed, Class<?> providerClass) {
        if (xposed == null || providerClass == null) return;
        Class<?> cur = providerClass;
        while (cur != null && cur != android.content.ContentProvider.class && cur != Object.class) {
            if (sHookedProviderClasses.add(cur.getName())) {
                try {
                    Method openFile = cur.getDeclaredMethod("openFile", Uri.class, String.class);
                    xposed.hook(openFile)
                            .setId("sysui-fp-openFile-" + cur.getName())
                            .intercept(PortalSystemUiModule::onProviderOpenFile);
                    xposed.log(Log.INFO, TAG, "Hooked provider openFile on " + cur.getName());
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable t) {
                    xposed.log(Log.WARN, TAG, "Failed hooking openFile on " + cur.getName(), t);
                }
                try {
                    Method query = cur.getDeclaredMethod("query",
                            Uri.class, String[].class, String.class, String[].class, String.class);
                    xposed.hook(query)
                            .setId("sysui-fp-query-" + cur.getName())
                            .intercept(PortalSystemUiModule::onProviderQuery);
                    xposed.log(Log.INFO, TAG, "Hooked provider query on " + cur.getName());
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable t) {
                    xposed.log(Log.WARN, TAG, "Failed hooking query on " + cur.getName(), t);
                }
                try {
                    Method getType = cur.getDeclaredMethod("getType", Uri.class);
                    xposed.hook(getType)
                            .setId("sysui-fp-getType-" + cur.getName())
                            .intercept(PortalSystemUiModule::onProviderGetType);
                    xposed.log(Log.INFO, TAG, "Hooked provider getType on " + cur.getName());
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable t) {
                    xposed.log(Log.WARN, TAG, "Failed hooking getType on " + cur.getName(), t);
                }
            }
            cur = cur.getSuperclass();
        }
    }

    private static Context resolveProviderContext(Object providerObj) {
        if (sSysUiContext != null) {
            return sSysUiContext;
        }
        if (providerObj instanceof android.content.ContentProvider) {
            Context c = ((android.content.ContentProvider) providerObj).getContext();
            if (c != null) {
                sSysUiContext = c.getApplicationContext() != null ? c.getApplicationContext() : c;
                return sSysUiContext;
            }
        }
        return null;
    }

    public static File getIncomingDir(Context context) {
        File dir = new File(context.getCacheDir(), "hyperportal_incoming");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static String sanitizeIncomingFileName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "drag_" + System.currentTimeMillis();
        }
        String decoded = Uri.decode(raw);
        int slash = Math.max(decoded.lastIndexOf('/'), decoded.lastIndexOf('\\'));
        if (slash >= 0) {
            decoded = decoded.substring(slash + 1);
        }
        decoded = decoded.replace("..", "_").trim();
        if (decoded.isEmpty()) {
            return "drag_" + System.currentTimeMillis();
        }
        return decoded;
    }

    private static Object onProviderOpenFile(XposedInterface.Chain chain) throws Throwable {
        List<Object> args = chain.getArgs();
        Uri uri = args != null && args.size() > 0 && args.get(0) instanceof Uri
                ? (Uri) args.get(0) : null;
        String mode = args != null && args.size() > 1 && args.get(1) instanceof String
                ? (String) args.get(1) : "r";
        String path = uri != null ? uri.getPath() : null;
        if (path != null && path.startsWith(INCOMING_PATH_PREFIX)) {
            Context ctx = resolveProviderContext(chain.getThisObject());
            if (ctx == null) {
                throw new java.io.FileNotFoundException("No SystemUI context for " + uri);
            }
            String rawName = path.substring(INCOMING_PATH_PREFIX.length());
            String name = sanitizeIncomingFileName(rawName);
            File dir = getIncomingDir(ctx);
            File target = new File(dir, name);
            boolean wantsWrite = mode != null && mode.indexOf('w') >= 0;
            if (wantsWrite) {
                int flags = android.os.ParcelFileDescriptor.MODE_WRITE_ONLY
                        | android.os.ParcelFileDescriptor.MODE_CREATE
                        | android.os.ParcelFileDescriptor.MODE_TRUNCATE;
                android.os.ParcelFileDescriptor pfd = android.os.ParcelFileDescriptor.open(target, flags);
                grantReadToCommonTargets(ctx, uri);
                pruneIncomingCache(dir, target);
                return pfd;
            }
            if (!target.isFile()) {
                throw new java.io.FileNotFoundException("Not found: " + uri);
            }
            return android.os.ParcelFileDescriptor.open(
                    target, android.os.ParcelFileDescriptor.MODE_READ_ONLY);
        }
        return chain.proceed();
    }

    private static Object onProviderQuery(XposedInterface.Chain chain) throws Throwable {
        List<Object> args = chain.getArgs();
        Uri uri = args != null && args.size() > 0 && args.get(0) instanceof Uri
                ? (Uri) args.get(0) : null;
        String[] projection = args != null && args.size() > 1 && args.get(1) instanceof String[]
                ? (String[]) args.get(1) : null;
        String path = uri != null ? uri.getPath() : null;
        if (path != null && path.startsWith(INCOMING_PATH_PREFIX)) {
            Context ctx = resolveProviderContext(chain.getThisObject());
            if (ctx == null) {
                return null;
            }
            String rawName = path.substring(INCOMING_PATH_PREFIX.length());
            if ("__config__".equals(rawName)) {
                recordAesHandshakeInSysUi(ctx, android.os.Binder.getCallingUid());
                android.database.MatrixCursor mc = new android.database.MatrixCursor(new String[]{"key"});
                mc.setExtras(getCachedConfigBundle(ctx));
                return mc;
            }
            if ("__status__".equals(rawName)) {
                if (!isTrustedStatusCaller(ctx, android.os.Binder.getCallingUid())) {
                    return null;
                }
                android.database.MatrixCursor mc = new android.database.MatrixCursor(new String[]{"key"});
                mc.setExtras(buildStatusBundle(ctx));
                return mc;
            }
            String name = sanitizeIncomingFileName(rawName);
            File file = new File(getIncomingDir(ctx), name);
            String[] cols = projection != null ? projection : new String[]{
                    android.provider.OpenableColumns.DISPLAY_NAME,
                    android.provider.OpenableColumns.SIZE
            };
            String[] names = new String[cols.length];
            Object[] values = new Object[cols.length];
            int count = 0;
            for (String col : cols) {
                if (android.provider.OpenableColumns.DISPLAY_NAME.equals(col)) {
                    names[count] = android.provider.OpenableColumns.DISPLAY_NAME;
                    values[count++] = file.getName();
                } else if (android.provider.OpenableColumns.SIZE.equals(col)) {
                    names[count] = android.provider.OpenableColumns.SIZE;
                    values[count++] = file.isFile() ? file.length() : 0L;
                } else if ("_data".equals(col)) {
                    names[count] = "_data";
                    values[count++] = file.getAbsolutePath();
                }
            }
            String[] finalNames = new String[count];
            Object[] finalValues = new Object[count];
            System.arraycopy(names, 0, finalNames, 0, count);
            System.arraycopy(values, 0, finalValues, 0, count);
            android.database.MatrixCursor cursor = new android.database.MatrixCursor(finalNames, 1);
            cursor.addRow(finalValues);
            return cursor;
        }
        return chain.proceed();
    }

    private static Object onProviderGetType(XposedInterface.Chain chain) throws Throwable {
        List<Object> args = chain.getArgs();
        Uri uri = args != null && args.size() > 0 && args.get(0) instanceof Uri
                ? (Uri) args.get(0) : null;
        String path = uri != null ? uri.getPath() : null;
        if (path != null && path.startsWith(INCOMING_PATH_PREFIX)) {
            Context ctx = resolveProviderContext(chain.getThisObject());
            if (ctx == null) {
                return "application/octet-stream";
            }
            String rawName = path.substring(INCOMING_PATH_PREFIX.length());
            File file = new File(getIncomingDir(ctx), sanitizeIncomingFileName(rawName));
            return sniffIncomingMime(file);
        }
        return chain.proceed();
    }

    private static String sniffIncomingMime(File file) {
        if (file != null && file.isFile() && file.length() >= 12) {
            byte[] h = new byte[12];
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                int n = in.read(h);
                if (n >= 6
                        && h[0] == 'G' && h[1] == 'I' && h[2] == 'F'
                        && h[3] == '8' && (h[4] == '7' || h[4] == '9') && h[5] == 'a') {
                    return "image/gif";
                }
                if (n >= 12
                        && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                        && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
                    return "image/webp";
                }
                if (n >= 8
                        && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
                    return "image/png";
                }
                if (n >= 3
                        && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
                    return "image/jpeg";
                }
            } catch (Throwable ignored) {
            }
        }
        String name = file != null ? file.getName() : "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < name.length()) {
            String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    private static void pruneIncomingCache(File dir, File keep) {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        List<File> survivors = new ArrayList<>(files.length);
        for (File file : files) {
            if (!file.isFile()) continue;
            if (file.equals(keep)) {
                survivors.add(file);
                continue;
            }
            if (now - file.lastModified() > MAX_CACHE_AGE_MS) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            } else {
                survivors.add(file);
            }
        }
        if (survivors.size() <= MAX_CACHED_FILES) {
            return;
        }
        survivors.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (int i = MAX_CACHED_FILES; i < survivors.size(); i++) {
            File victim = survivors.get(i);
            if (victim.equals(keep)) {
                continue;
            }
            // A target app may still be streaming a file that was just
            // staged; the old provider protected the last 3 minutes and the
            // SystemUI copy must do the same.
            if (now - victim.lastModified() < CACHE_PROTECT_MS) {
                continue;
            }
            //noinspection ResultOfMethodCallIgnored
            victim.delete();
        }
    }

    public static void grantReadToCommonTargets(Context context, Uri uri) {
        if (context == null || uri == null) return;
        for (String pkg : COMMON_SHARE_PACKAGES) {
            try {
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void grantIncomingPrefixToPackage(Context context, String pkg) {
        if (context == null || TextUtils.isEmpty(pkg)) return;
        try {
            Uri prefixUri = Uri.parse(INCOMING_URI_PREFIX);
            int mode = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
            context.grantUriPermission(pkg, prefixUri, mode);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isTrustedStatusCaller(Context context, int uid) {
        if (context == null || uid <= 0) {
            return false;
        }
        if (uid == android.os.Process.SYSTEM_UID || uid == android.os.Process.myUid()) {
            return true;
        }
        try {
            String[] packages = context.getPackageManager().getPackagesForUid(uid);
            if (packages != null) {
                for (String pkg : packages) {
                    if (PortalShareContract.PORTAL_PACKAGE.equals(pkg)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void recordAesHandshakeInSysUi(Context context, int callingUid) {
        if (context == null || callingUid <= 0
                || callingUid == android.os.Process.SYSTEM_UID
                || callingUid == android.os.Process.myUid()) {
            return;
        }
        try {
            PackageManager pm = context.getPackageManager();
            String[] pkgs = pm.getPackagesForUid(callingUid);
            String pkg = (pkgs != null && pkgs.length > 0 && !TextUtils.isEmpty(pkgs[0]))
                    ? pkgs[0] : "uid:" + callingUid;
            if ("com.android.systemui".equals(pkg)
                    || "android".equals(pkg)
                    || "com.github.leaf.hyperportal".equals(pkg)) {
                return;
            }
            long now = System.currentTimeMillis();
            if (pkg.equals(sLastAesPackage) && now - sLastAesWriteAt < 60_000L) {
                return;
            }
            CharSequence labelCs = pkg;
            try {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                CharSequence loaded = pm.getApplicationLabel(ai);
                if (!TextUtils.isEmpty(loaded)) {
                    labelCs = loaded;
                }
            } catch (Throwable ignored) {
            }
            String safeLabel = labelCs.toString().replace('|', ' ').replace('\n', ' ').trim();
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences(CONFIG_CACHE_PREFS, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_AES_ENTRIES, "");
            StringBuilder sb = new StringBuilder();
            sb.append(pkg).append('|').append(safeLabel).append('|').append(now);
            int count = 1;
            if (raw != null && !raw.isEmpty()) {
                for (String line : raw.split("\n")) {
                    if (line.isEmpty()) continue;
                    if (line.startsWith(pkg + "|")) continue;
                    if (count >= MAX_AES_ENTRIES) break;
                    sb.append('\n').append(line);
                    count++;
                }
            }
            prefs.edit().putString(KEY_AES_ENTRIES, sb.toString()).apply();
            sLastAesPackage = pkg;
            sLastAesWriteAt = now;
        } catch (Throwable ignored) {
        }
    }

    private static Bundle buildStatusBundle(Context context) {
        Bundle b = new Bundle();
        b.putLong("systemui_at", System.currentTimeMillis());
        try {
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences(CONFIG_CACHE_PREFS, Context.MODE_PRIVATE);
            b.putString(KEY_AES_ENTRIES, prefs.getString(KEY_AES_ENTRIES, ""));
        } catch (Throwable ignored) {
        }
        return b;
    }

    public static boolean hasCachedConfig(Context context) {
        if (context == null) return false;
        try {
            return context.getSharedPreferences(CONFIG_CACHE_PREFS, Context.MODE_PRIVATE)
                    .getBoolean("config_ready", false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static Bundle getCachedConfigBundle(Context context) {
        Bundle bundle = new Bundle();
        try {
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences(CONFIG_CACHE_PREFS, Context.MODE_PRIVATE);
            bundle.putBoolean("config_ready", prefs.getBoolean("config_ready", false));
            bundle.putBoolean("master_switch", prefs.getBoolean("master_switch", true));
            bundle.putInt("trigger_threshold", prefs.getInt("trigger_threshold", 76));
            bundle.putString("trigger_position", prefs.getString("trigger_position", "bottom"));
            bundle.putBoolean("landscape_trigger", prefs.getBoolean("landscape_trigger", false));
            bundle.putInt("long_press_duration", prefs.getInt("long_press_duration", 800));
            bundle.putInt("touch_slop", prefs.getInt("touch_slop", 24));
            bundle.putBoolean("prefer_png_for_gif", prefs.getBoolean("prefer_png_for_gif", true));
            bundle.putBoolean("same_window_guard", prefs.getBoolean("same_window_guard", true));
            bundle.putBoolean("coloros_consume_anim", prefs.getBoolean("coloros_consume_anim", true));
            bundle.putInt("edge_scroll_margin", prefs.getInt("edge_scroll_margin", 86));
            Set<String> blacklist = prefs.getStringSet("gesture_blacklist", null);
            bundle.putStringArrayList("gesture_blacklist",
                    blacklist != null ? new ArrayList<>(blacklist) : new ArrayList<>());
            bundle.putInt("target_limit", prefs.getInt("target_limit", TargetConfig.DEFAULT_LIMIT));
            bundle.putStringArrayList("target_pinned",
                    splitCsvToList(prefs.getString("target_pinned", "")));
            bundle.putStringArrayList("target_order",
                    splitCsvToList(prefs.getString("target_order", "")));
            bundle.putStringArrayList("target_hidden",
                    splitCsvToList(prefs.getString("target_hidden", "")));
            bundle.putBoolean("deduplicate_app_targets",
                    prefs.getBoolean("deduplicate_app_targets", true));
            bundle.putBoolean("enable_qr_scan", prefs.getBoolean("enable_qr_scan", true));
            bundle.putString("target_usage", prefs.getString("target_usage", ""));
        } catch (Throwable ignored) {
        }
        return bundle;
    }

    private static ArrayList<String> splitCsvToList(String csv) {
        ArrayList<String> out = new ArrayList<>();
        if (csv == null || csv.isEmpty()) return out;
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static String joinListToCsv(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            if (item == null || item.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(item.trim());
        }
        return sb.toString();
    }

    private static boolean isSecondarySystemUiProcess() {
        try {
            String process = Application.getProcessName();
            return process != null && process.startsWith("com.android.systemui:");
        } catch (Throwable t) {
            return false;
        }
    }

    private static long msSinceProcessStart() {
        try {
            return SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime();
        } catch (Throwable t) {
            return -1L;
        }
    }

    private static void initOverlay(Context context) {
        if (context == null || sOverlay != null) {
            return;
        }
        synchronized (PortalSystemUiModule.class) {
            if (sOverlay != null) {
                return;
            }
            try {
                long started = SystemClock.uptimeMillis();
                SystemUiOverlay overlay = new SystemUiOverlay(context);
                // Register first: from here on every drag is handled, even
                // while the rest of the warm-up is still running.
                overlay.registerReceivers();
                sOverlay = overlay;
                Log.i(TAG, "drag receiver registered in "
                        + (SystemClock.uptimeMillis() - started) + "ms ("
                        + msSinceProcessStart() + "ms after process start)");
                overlay.warmUp();
            } catch (Throwable t) {
                Log.e(TAG, "Failed to instantiate SystemUiOverlay", t);
                return;
            }
        }
        sBgExecutor.execute(() -> {
            ensureProviderHooksForContext(context);
            // The home page reads __status__ (and the AES handshake log) from
            // SystemUI's FileProvider. Grant this once at startup: the earlier
            // on-demand grants only covered apps that had staged a drag file,
            // so a fresh boot with text-only drags showed "not handshaken".
            grantIncomingPrefixToPackage(context, PortalShareContract.PORTAL_PACKAGE);
            Log.i(TAG, "SystemUiOverlay attached and listening for drag events!");
        });
    }

    private static final class SystemUiOverlay {
        private final Context mContext;
        private final WindowManager mWindowManager;
        private FrameLayout mContainer;
        private WindowManager.LayoutParams mParams;
        private GlowView mGlowView;
        private ColorOSDockBar mDockBar;
        private final List<AppTarget> mTargets = new ArrayList<>();
        private boolean mShowing = false;
        private boolean mTransferDockBroadcastDragging = false;
        private long mLastHapticTime = 0L;
        private float mTriggerThreshold = 0.76f;
        private String mTriggerPosition = "bottom";
        private boolean mLandscapeAllowed = false;

        // Current dragged content payload
        private String mText;
        private Bitmap mBitmap;
        private Uri mImageUri;
        private String mImageMime;
        private String mImageFileName;
        private volatile String mImageUrl;
        private float mLastDropX = -1f;
        private float mLastDropY = -1f;
        private boolean mGallerySavedThisDrag = false;

        private final ExecutorService mQrExecutor = Executors.newSingleThreadExecutor();
        private final ExecutorService mImageExecutor = Executors.newSingleThreadExecutor();
        private int mQrSessionId = 0;
        private String mDetectedQrContent = null;
        private TransferDockController mTransferDockController;

        // Cold-boot safety. The overlay used to build its target list (three
        // PackageManager queries plus an icon decode per share activity) and
        // read the config provider from SystemUI's main thread while SystemUI
        // was still starting up. That blocked input dispatch long enough for
        // the first gesture after a cold boot to ANR, so SystemUI was killed
        // and restarted; the first drag therefore never showed the glow or the
        // shelf (only the system_server tilt survived, since it does not
        // depend on SystemUI). Views are now built lazily, config and targets
        // are prepared on sBgExecutor, and nothing on the drag path blocks.
        private static final long CONFIG_TTL_MS = 60_000L;
        /** Last successfully read settings, so a frozen provider cannot reset us to "bottom". */
        private static final String CONFIG_CACHE_PREFS = "hyperportal_systemui";
        private volatile boolean mViewsReady = false;
        private volatile boolean mConfigLoaded = false;
        private volatile boolean mConfigRefreshing = false;
        private volatile long mConfigFetchedAt = 0L;
        private volatile boolean mMasterEnabled = true;
        private volatile boolean mTargetsBuilding = false;
        private volatile boolean mTargetsDirty = false;
        private volatile List<String> mPinnedOrder = new ArrayList<>();
        private boolean mFirstDragLogged = false;
        private int mCurrentState = 0;

        public SystemUiOverlay(Context context) {
            // Deliberately trivial. initOverlay() registers the drag-state
            // receiver as soon as this returns; warmUp() fills in the rest
            // without blocking the main thread.
            this.mContext = context;
            this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }

        private int dp(float value) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                    mContext.getResources().getDisplayMetrics());
        }

        private boolean mAppTargetsResolved = false;
        private List<ShareTargetEntry> mCachedShareTargets = null;
        private long mLastShareTargetsQueryTime = 0;

        /** Starts the background warm-up; called once, right after registration. */
        void warmUp() {
            // The overlay window has to exist before a drag starts: a window
            // added mid-drag never receives ACTION_DRAG_STARTED, so its drop
            // targets would be dead for that drag. Building the views is cheap
            // (no provider or PackageManager work), unlike the target list.
            sHandler.post(this::ensureViews);
            refreshConfigAsync();
            requestTargetsAsync();
        }

        /**
         * Builds the overlay views on demand. View inflation and the AGSL
         * shader compile are cheap compared with the provider/icon work, but
         * they still do not belong in SystemUI's boot window.
         */
        private boolean ensureViews() {
            if (mViewsReady) {
                return true;
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                sHandler.post(this::ensureViews);
                return false;
            }
            try {
                long started = SystemClock.uptimeMillis();
                buildViewHierarchy();
                mViewsReady = true;
                Log.i(TAG, "view hierarchy ready in "
                        + (SystemClock.uptimeMillis() - started) + "ms");
            } catch (Throwable t) {
                Log.e(TAG, "buildViewHierarchy failed", t);
            }
            return mViewsReady;
        }

        /** Reads master switch / threshold / trigger edge without blocking. */
        private void refreshConfigAsync() {
            if (mConfigRefreshing) {
                return;
            }
            mConfigRefreshing = true;
            sBgExecutor.execute(() -> {
                long started = SystemClock.uptimeMillis();
                Bundle result = loadCachedConfig();
                if (result == null) {
                    try {
                        Uri uri = Uri.parse("content://com.github.leaf.hyperportal.files");
                        result = mContext.getContentResolver().call(uri, "getConfig", null, null);
                    } catch (Throwable t) {
                        Log.w(TAG, "initial getConfig fallback failed", t);
                    }
                    if (result != null) {
                        // The app provider returns the authoritative settings
                        // but does not carry SystemUI's cache marker itself.
                        result.putBoolean("config_ready", true);
                        saveCachedConfig(result);
                    } else {
                        result = getCachedConfigBundle(mContext);
                    }
                }
                final Bundle snapshot = result;
                final long cost = SystemClock.uptimeMillis() - started;
                sHandler.post(() -> applyConfig(snapshot, cost));
            });
        }

        private Bundle loadCachedConfig() {
            try {
                if (!hasCachedConfig(mContext)) {
                    return null;
                }
                return getCachedConfigBundle(mContext);
            } catch (Throwable t) {
                return null;
            }
        }

        private void saveCachedConfig(Bundle bundle) {
            if (bundle == null) return;
            try {
                android.content.SharedPreferences prefs =
                        mContext.getSharedPreferences(CONFIG_CACHE_PREFS, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                if (bundle.containsKey("trigger_position")) {
                    editor.putString("trigger_position", bundle.getString("trigger_position"));
                }
                if (bundle.containsKey("trigger_threshold")) {
                    editor.putInt("trigger_threshold", bundle.getInt("trigger_threshold", 76));
                }
                if (bundle.containsKey("master_switch")) {
                    editor.putBoolean("master_switch", bundle.getBoolean("master_switch", true));
                }
                if (bundle.containsKey("landscape_trigger")) {
                    editor.putBoolean("landscape_trigger", bundle.getBoolean("landscape_trigger", false));
                }
                if (bundle.containsKey("long_press_duration")) {
                    editor.putInt("long_press_duration", bundle.getInt("long_press_duration", 800));
                }
                if (bundle.containsKey("touch_slop")) {
                    editor.putInt("touch_slop", bundle.getInt("touch_slop", 24));
                }
                if (bundle.containsKey("prefer_png_for_gif")) {
                    editor.putBoolean("prefer_png_for_gif", bundle.getBoolean("prefer_png_for_gif", true));
                }
                if (bundle.containsKey("same_window_guard")) {
                    editor.putBoolean("same_window_guard", bundle.getBoolean("same_window_guard", true));
                }
                if (bundle.containsKey("coloros_consume_anim")) {
                    editor.putBoolean("coloros_consume_anim", bundle.getBoolean("coloros_consume_anim", true));
                }
                if (bundle.containsKey("edge_scroll_margin")) {
                    editor.putInt("edge_scroll_margin", bundle.getInt("edge_scroll_margin", 86));
                }
                if (bundle.containsKey("gesture_blacklist")) {
                    ArrayList<String> bl = bundle.getStringArrayList("gesture_blacklist");
                    editor.putStringSet("gesture_blacklist",
                            bl != null ? new java.util.HashSet<>(bl) : new java.util.HashSet<>());
                }
                if (bundle.containsKey("target_limit")) {
                    editor.putInt("target_limit", bundle.getInt("target_limit", TargetConfig.DEFAULT_LIMIT));
                }
                if (bundle.containsKey("target_pinned")) {
                    editor.putString("target_pinned",
                            joinListToCsv(bundle.getStringArrayList("target_pinned")));
                }
                if (bundle.containsKey("target_order")) {
                    editor.putString("target_order",
                            joinListToCsv(bundle.getStringArrayList("target_order")));
                }
                if (bundle.containsKey("target_hidden")) {
                    editor.putString("target_hidden",
                            joinListToCsv(bundle.getStringArrayList("target_hidden")));
                }
                if (bundle.containsKey("deduplicate_app_targets")) {
                    editor.putBoolean("deduplicate_app_targets",
                            bundle.getBoolean("deduplicate_app_targets", true));
                }
                if (bundle.containsKey("enable_qr_scan")) {
                    editor.putBoolean("enable_qr_scan",
                            bundle.getBoolean("enable_qr_scan", true));
                }
                if (bundle.containsKey("target_usage")) {
                    // Usage counters are owned by SystemUI now: only seed them
                    // from the app when this cache has never seen usage data.
                    // A later app broadcast must not roll back increments made
                    // by drops handled in SystemUI.
                    String existingUsage = prefs.getString("target_usage", "");
                    if (TextUtils.isEmpty(existingUsage)) {
                        editor.putString("target_usage", bundle.getString("target_usage", ""));
                    }
                }
                if (bundle.containsKey("trigger_position")) {
                    editor.putBoolean("config_ready", true);
                }
                editor.apply();
            } catch (Throwable ignored) {
            }
        }

        private void applyConfig(Bundle result, long cost) {
            mConfigRefreshing = false;
            mConfigFetchedAt = SystemClock.uptimeMillis();
            if (result == null) {
                Log.w(TAG, "config unavailable after " + cost + "ms, keeping defaults");
                return;
            }
            if (!result.getBoolean("config_ready", false)) {
                // A cache that has never received a full app snapshot only
                // contains defaults. Applying it here would overwrite the
                // trigger edge / master switch pushed by system_server.
                Log.w(TAG, "config cache not ready after " + cost + "ms, keeping current values");
                return;
            }
            mConfigLoaded = true;
            if (mDockBar != null) {
                mDockBar.setEdgeScrollMarginDp(result.getInt("edge_scroll_margin", 86));
            }
            int thresholdPct = result.getInt("trigger_threshold", 76);
            if (thresholdPct >= 50 && thresholdPct <= 95) {
                mTriggerThreshold = thresholdPct / 100.0f;
            }
            String pos = result.getString("trigger_position");
            if (pos != null) {
                mTriggerPosition = pos;
            }
            mLandscapeAllowed = result.getBoolean("landscape_trigger", false);
            mMasterEnabled = result.getBoolean("master_switch", true);
            ArrayList<String> pinnedOrder = result.getStringArrayList("target_order");
            if (pinnedOrder == null) {
                pinnedOrder = result.getStringArrayList("target_pinned");
            }
            mPinnedOrder = pinnedOrder == null ? new ArrayList<>() : pinnedOrder;
            Log.i(TAG, "config applied in " + cost + "ms master=" + mMasterEnabled
                    + " threshold=" + thresholdPct + " pos=" + mTriggerPosition);
            // The shelf may have been built with defaults while the config
            // read was still in flight (warmUp runs both in parallel).
            if (mAppTargetsResolved) {
                requestTargetsAsync();
            }
        }

        /** Builds the shelf contents off the main thread; safe from any thread. */
        private void requestTargetsAsync() {
            if (mTargetsBuilding) {
                mTargetsDirty = true;
                return;
            }
            mTargetsBuilding = true;
            mTargetsDirty = false;
            // Snapshot the payload: the main thread may replace it with the
            // next drag while this build runs.
            final String text = mText;
            final String mime = mImageMime;
            final boolean isImage = (mBitmap != null || mImageUri != null);
            sBgExecutor.execute(() -> {
                long started = SystemClock.uptimeMillis();
                List<AppTarget> built = null;
                try {
                    built = buildTargetsNow(text, mime, isImage);
                } catch (Throwable t) {
                    Log.w(TAG, "buildTargets failed", t);
                }
                final List<AppTarget> result = built;
                final long cost = SystemClock.uptimeMillis() - started;
                sHandler.post(() -> applyTargets(result, cost));
            });
        }

        private void applyTargets(List<AppTarget> built, long cost) {
            mTargetsBuilding = false;
            if (built == null) {
                return;
            }
            mTargets.clear();
            mTargets.addAll(built);
            mAppTargetsResolved = !built.isEmpty();
            if (mDockBar != null) {
                List<ColorOSDockBar.DockItem> dockItems = new ArrayList<>();
                for (AppTarget target : built) {
                    dockItems.add(new ColorOSDockBar.DockItem(
                            target.key, target.name, target.icon, target));
                }
                mDockBar.setItems(dockItems);
            }
            Log.i(TAG, "targets ready: " + built.size() + " in " + cost + "ms");
            if (mTargetsDirty) {
                requestTargetsAsync();
                return;
            }
            // A drag may have crossed the hot zone before the first target
            // list was ready (cold boot). Reveal the shelf now instead of
            // waiting for the next drag.
            if (mCurrentState == 2 && mShowing && mDockBar != null
                    && mDockBar.getVisibility() != View.VISIBLE) {
                mDockBar.resetScroll();
                mDockBar.setVisibility(View.VISIBLE);
                mDockBar.setTranslationX(0f);
                mDockBar.setTranslationY(0f);
                mDockBar.setAlpha(1f);
            }
        }

        private List<AppTarget> buildTargetsNow(String text, String mime, boolean isImage) {
            TargetConfig config = TargetConfig.fromProvider(mContext);
            Set<String> contentKinds = TargetRepository.contentKinds(text, isImage, mime);
            List<ShareTargetEntry> candidates = new ArrayList<>();
            candidates.addAll(TargetRepository.builtInEntries(isImage));
            long now = SystemClock.uptimeMillis();
            if (mCachedShareTargets == null || (now - mLastShareTargetsQueryTime) > 30000) {
                mCachedShareTargets = TargetRepository.queryShareTargets(mContext);
                mLastShareTargetsQueryTime = now;
            }
            candidates.addAll(mCachedShareTargets);
            List<ShareTargetEntry> ordered = TargetRanker.rank(
                    mContext, candidates, contentKinds, config);
            List<AppTarget> targets = new ArrayList<>(ordered.size());
            for (ShareTargetEntry entry : ordered) {
                targets.add(new AppTarget(entry, createTargetAction(entry)));
            }
            Log.i(TAG, "targets rebuilt kinds=" + contentKinds
                    + " candidates=" + candidates.size()
                    + " shown=" + ordered.size()
                    + " limit=" + config.limit);
            return targets;
        }

        private DropAction createTargetAction(final ShareTargetEntry entry) {
            return (ctx, text, bmp, uri) -> {
                // Snapshot payload metadata before any worker runs: ACTION_DROP
                // clears mText/mImage* immediately after onDrop returns.
                final String imageMime = mImageMime;
                final String imageFileName = mImageFileName;
                if (entry.kind == ShareTargetEntry.KIND_COPY) {
                    if (bmp != null || uri != null) {
                        copyImageToClipboard(ctx, bmp, uri);
                    } else if (!TextUtils.isEmpty(text)) {
                        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(
                                Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("HyperPortal", text));
                        toast("已复制到剪贴板");
                    }
                    return;
                }
                if (entry.kind == ShareTargetEntry.KIND_TRANSFER_DOCK) {
                    saveToTransferDock(ctx, text, bmp, uri, imageMime, imageFileName);
                    return;
                }
                if (entry.kind == ShareTargetEntry.KIND_SAVE) {
                    if (!mGallerySavedThisDrag) {
                        mGallerySavedThisDrag = true;
                        sBgExecutor.execute(() ->
                                saveImageToGallery(ctx, text, imageMime, imageFileName, bmp, uri));
                    }
                    return;
                }
                if (entry.kind == ShareTargetEntry.KIND_SEGMENT) {
                    if (!TextUtils.isEmpty(text)) {
                        openTextSegmentation(ctx, text);
                    } else if (bmp != null || uri != null) {
                        openImageTextSegmentation(ctx, bmp, uri);
                    }
                    return;
                }
                if (entry.kind == ShareTargetEntry.KIND_SEARCH) {
                    if (!TextUtils.isEmpty(text)) {
                        try {
                            Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
                            search.putExtra(SearchManager.QUERY, text);
                            search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ctx.startActivity(search);
                        } catch (Throwable t) {
                            try {
                                Intent view = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://www.bing.com/search?q="
                                                + Uri.encode(text)));
                                view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ctx.startActivity(view);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    return;
                }
                if (entry.kind == ShareTargetEntry.KIND_SYSTEM_SHARE) {
                    Uri shareUri = uri;
                    if (shareUri == null && bmp != null) {
                        shareUri = stageBitmapInPortal(ctx, bmp, "shared_");
                    }
                    dispatchSystemShare(ctx, shareUri, text);
                    return;
                }

                if (!ShareTargetEntry.ACTION_SEND.equals(entry.action)) {
                    dispatchRuleAction(ctx, entry, text);
                    return;
                }

                if (uri != null || bmp != null) {
                    Uri shareUri = uri;
                    if (shareUri == null) {
                        shareUri = stageBitmapInPortal(ctx, bmp, "shared_");
                    }
                    if (shareUri == null) {
                        Log.w(TAG, "image staging failed target=" + entry.packageName);
                        toast("图片准备失败");
                        return;
                    }
                    dispatchImageShare(ctx, entry.packageName, entry.activityName, shareUri, text);
                    toast("已发送至 " + entry.label);
                    return;
                }
                if (!TextUtils.isEmpty(text)) {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setPackage(entry.packageName);
                    if (entry.component() != null) {
                        share.setComponent(entry.component());
                    }
                    share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, text);
                    try {
                        ctx.startActivity(share);
                        toast("已发送至 " + entry.label);
                    } catch (Throwable t) {
                        Log.w(TAG, "text share failed target=" + entry.packageName, t);
                        try {
                            Intent launchIntent = ctx.getPackageManager()
                                    .getLaunchIntentForPackage(entry.packageName);
                            if (launchIntent != null) {
                                ctx.startActivity(launchIntent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK));
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            };
        }

        /**
         * Starts a target that is not a normal ACTION_SEND share activity:
         * map deeplinks, browser URL opens, dialer, SMS and mail clients.
         */
        private void dispatchRuleAction(Context ctx, ShareTargetEntry entry, String text) {
            try {
                String action = entry.action == null ? ShareTargetEntry.ACTION_SEND : entry.action;
                if (ShareTargetEntry.ACTION_VIEW.equals(action)) {
                    String target = expandDeeplink(entry.deeplink, text);
                    if (TextUtils.isEmpty(target)) {
                        target = extractUrl(text);
                    }
                    if (TextUtils.isEmpty(target)) {
                        toast("没有可打开的链接或地址");
                        return;
                    }
                    if (!target.contains("://") && !target.startsWith("geo:")
                            && !target.startsWith("mailto:") && !target.startsWith("tel:")) {
                        target = "https://" + target;
                    }
                    Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
                    view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (!TextUtils.isEmpty(entry.packageName)) {
                        view.setPackage(entry.packageName);
                    }
                    ctx.startActivity(view);
                    toast("已打开 " + entry.label);
                    return;
                }
                if (ShareTargetEntry.ACTION_DIAL.equals(action)) {
                    String number = normalizePhone(text);
                    if (TextUtils.isEmpty(number)) {
                        toast("没有可拨打的号码");
                        return;
                    }
                    Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
                    dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (!TextUtils.isEmpty(entry.packageName)) {
                        dial.setPackage(entry.packageName);
                    }
                    ctx.startActivity(dial);
                    return;
                }
                if (ShareTargetEntry.ACTION_SMS.equals(action)) {
                    String number = normalizePhone(text);
                    if (TextUtils.isEmpty(number)) {
                        toast("没有可发送的号码");
                        return;
                    }
                    Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
                    sms.putExtra("sms_body", text == null ? "" : text);
                    sms.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (!TextUtils.isEmpty(entry.packageName)) {
                        sms.setPackage(entry.packageName);
                    }
                    ctx.startActivity(sms);
                    return;
                }
                if (ShareTargetEntry.ACTION_MAIL.equals(action)) {
                    Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                    mail.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
                    mail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (!TextUtils.isEmpty(entry.packageName)) {
                        mail.setPackage(entry.packageName);
                    }
                    ctx.startActivity(mail);
                    return;
                }
                if (ShareTargetEntry.ACTION_SEARCH.equals(action)) {
                    Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
                    search.putExtra(SearchManager.QUERY, text == null ? "" : text);
                    search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(search);
                    return;
                }
                toast("无法打开 " + entry.label);
            } catch (Throwable throwable) {
                Log.w(TAG, "rule action failed action=" + entry.action
                        + " target=" + entry.packageName, throwable);
                toast("无法打开 " + entry.label);
            }
        }

        private String expandDeeplink(String template, String text) {
            if (TextUtils.isEmpty(template)) {
                return "";
            }
            String encoded = Uri.encode(text == null ? "" : text);
            if (template.contains("{text}")) {
                return template.replace("{text}", encoded);
            }
            if (template.contains("{url}")) {
                return template.replace("{url}", encoded);
            }
            if (template.endsWith("=") || template.endsWith("?") || template.endsWith("&")) {
                return template + encoded;
            }
            return template;
        }

        private String extractUrl(String text) {
            if (TextUtils.isEmpty(text)) {
                return "";
            }
            java.util.regex.Matcher matcher = Patterns.WEB_URL.matcher(text);
            return matcher.find() ? matcher.group() : text.trim();
        }

        private String normalizePhone(String text) {
            if (TextUtils.isEmpty(text)) {
                return "";
            }
            String digits = text.replaceAll("[^0-9+]", "");
            if (digits.startsWith("00")) {
                digits = "+" + digits.substring(2);
            }
            return digits;
        }

        /**
         * Rebuilds the shelf contents in the background. Callers on the main
         * thread (drag receiver, ACTION_SHOW, boot broadcast) return at once;
         * the dock is filled by applyTargets() when the build finishes.
         */
        private void updateDockTargets() {
            requestTargetsAsync();
        }

        private void openTextSegmentation(Context ctx, String text) {
            try {
                ctx.startActivity(BigBangContract.createIntent(
                        ctx, text, Math.round(mLastDropX), Math.round(mLastDropY)));
            } catch (Throwable t) {
                Log.w(TAG, "open text segmentation failed", t);
                toast("无法打开文本分词");
            }
        }

        private void openImageTextSegmentation(Context ctx, Bitmap bitmap, Uri uri) {
            // The bitmap extra is only a 360px drag preview. OCR needs the
            // full-resolution staged image, so try the URI first and use the
            // preview only when no URI arrived. Decode/scale on a worker; a
            // 4K image decoded on SystemUI's main thread can ANR the device.
            sBgExecutor.execute(() -> {
                Bitmap source = null;
                if (uri != null) {
                    try (InputStream input = ctx.getContentResolver().openInputStream(uri)) {
                        source = BitmapFactory.decodeStream(input);
                    } catch (Throwable t) {
                        Log.w(TAG, "decode OCR image failed uri=" + uri, t);
                    }
                }
                if (source == null) {
                    source = bitmap;
                }
                if (source == null || source.isRecycled()) {
                    toastAsync("无法读取图片");
                    return;
                }
                Bitmap scaled = scaleForOcr(source);
                if (scaled != source && source != bitmap) {
                    source.recycle();
                }
                final Bitmap ocrSource = scaled;
                sHandler.post(() -> {
                    toast("正在识别图片文字…");
                    AicrOcrClient.recognize(ctx, ocrSource, (text, error) -> {
                        if (error != null) {
                            Log.w(TAG, "image OCR failed", error);
                            String message = error.getMessage();
                            toast(TextUtils.isEmpty(message) ? "图片文字识别失败" : message);
                            return;
                        }
                        if (TextUtils.isEmpty(text)) {
                            toast("图片中没有识别到文字");
                            return;
                        }
                        openTextSegmentation(ctx, text);
                    });
                });
            });
        }

        private Bitmap scaleForOcr(Bitmap source) {
            int width = source.getWidth();
            int height = source.getHeight();
            int max = Math.max(width, height);
            if (max <= 4096) {
                return source;
            }
            float scale = 4096f / max;
            Bitmap scaled = Bitmap.createScaledBitmap(source,
                    Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)), true);
            return scaled != null ? scaled : source;
        }

        private void copyImageToClipboard(Context ctx, Bitmap bmp, Uri uri) {
            try {
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                Uri targetUri = uri;
                if (targetUri == null && bmp != null) {
                    targetUri = saveBitmapToCache(ctx, bmp);
                }
                boolean isImage = TextUtils.isEmpty(mImageMime) || mImageMime.startsWith("image/");
                if (targetUri != null) {
                    ClipData clip = ClipData.newUri(ctx.getContentResolver(), isImage ? "image" : "file", targetUri);
                    cm.setPrimaryClip(clip);
                    toast((isImage ? "图片" : "文件") + "已复制到剪贴板");
                } else {
                    toast("复制失败");
                }
            } catch (Throwable t) {
                Log.w(TAG, "copyImageToClipboard failed", t);
                toast("复制失败");
            }
        }

        private void saveToTransferDock(Context ctx, String text, Bitmap bmp, Uri uri,
                                        String mime, String fileName) {
            // Copying the original bytes and generating the thumbnail is disk
            // I/O; never do it on SystemUI's input-dispatch thread.
            sBgExecutor.execute(() -> {
                try {
                    TransferDockManager mgr = TransferDockManager.getInstance(ctx);
                    TransferDockItem item = null;
                    if (uri != null) {
                        // Prefer the staged original over the 360px preview
                        // bitmap: this keeps full resolution and preserves
                        // animated GIF / video files instead of a still frame.
                        String displayName = !TextUtils.isEmpty(fileName)
                                ? fileName
                                : (!TextUtils.isEmpty(text) && text.contains(".") ? text : "文件");
                        item = mgr.saveFile(uri, mime, displayName, "dragshare");
                    } else if (bmp != null) {
                        item = mgr.saveBitmap(bmp, "dragshare");
                    } else if (!TextUtils.isEmpty(text)) {
                        item = mgr.saveText(text, "dragshare");
                    }
                    final TransferDockItem result = item;
                    sHandler.post(() -> {
                        if (result != null) {
                            if (mTransferDockController == null) {
                                mTransferDockController = new TransferDockController(ctx);
                            }
                            mTransferDockController.showCapsule(result);
                            toast("已存入中转站");
                            Log.i(TAG, "saveToTransferDock succeeded, capsule shown: " + result.getTitle());
                        } else {
                            toast("存入中转站失败: 无有效内容");
                            Log.w(TAG, "saveToTransferDock failed: no content");
                        }
                    });
                } catch (Throwable t) {
                    Log.w(TAG, "saveToTransferDock failed", t);
                    toastAsync("存入中转站失败");
                }
            });
        }

        private void saveImageToGallery(Context ctx, String text, String mime,
                                        String fileName, Bitmap bmp, Uri uri) {
            try {
                String extension = "png";
                String mimeType = !TextUtils.isEmpty(mime) ? mime : "image/png";
                if (uri != null) {
                    String path = uri.getPath();
                    if (path != null) {
                        int dot = path.lastIndexOf('.');
                        if (dot >= 0) {
                            extension = path.substring(dot + 1).toLowerCase(Locale.US);
                            String detectedMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                            if (!TextUtils.isEmpty(detectedMime)) {
                                mimeType = detectedMime;
                            }
                        }
                    }
                }
                boolean isImage = mimeType.startsWith("image/");
                String displayName = !TextUtils.isEmpty(fileName)
                        ? fileName
                        : (!TextUtils.isEmpty(text) && text.contains(".")
                        ? text : "HyperPortal_" + System.currentTimeMillis() + "." + extension);
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                Uri baseUri;
                if (Build.VERSION.SDK_INT >= 29) {
                    if (isImage) {
                        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HyperPortal");
                        baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else {
                        values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/HyperPortal");
                        baseUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                    }
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                } else {
                    baseUri = isImage ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                }
                Uri inserted = ctx.getContentResolver().insert(baseUri, values);
                if (inserted != null) {
                    long bytesWritten = 0;
                    try (OutputStream os = ctx.getContentResolver().openOutputStream(inserted)) {
                        if (os != null) {
                            if (uri != null) {
                                // 1. Stream the original file directly with zero recompression!
                                try (java.io.InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                                    if (is != null) {
                                        byte[] buf = new byte[65536];
                                        int len;
                                        while ((len = is.read(buf)) > 0) {
                                            os.write(buf, 0, len);
                                            bytesWritten += len;
                                        }
                                    }
                                } catch (Throwable t) {
                                    Log.w(TAG, "saveImageToGallery failed reading uri", t);
                                }
                            }
                            if (bytesWritten == 0 && bmp != null) {
                                // 2. Fallback to lossless decoded bitmap if URI had 0 bytes
                                if (bmp.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                                    bytesWritten = 1;
                                }
                            }
                            os.flush();
                        }
                    }
                    if (bytesWritten <= 0) {
                        try {
                            ctx.getContentResolver().delete(inserted, null, null);
                        } catch (Throwable ignored) {
                        }
                        toastAsync("保存失败");
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 29) {
                        values.clear();
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        ctx.getContentResolver().update(inserted, values, null, null);
                    }
                    if (isImage) {
                        toastAsync("原图已保存至相册 (Pictures/HyperPortal)");
                    } else {
                        toastAsync("文件已保存至下载目录 (Download/HyperPortal)");
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "saveImageToGallery failed", t);
                toastAsync("保存失败");
            }
        }

        private Uri saveBitmapToCache(Context ctx, Bitmap bmp) {
            return stageBitmapInPortal(ctx, bmp, "shared_");
        }

        /**
         * Best-effort fallback for payloads that only carry an image URL (for
         * example Weibo original images). The bridge normally stages bytes
         * already; this only runs when no bitmap or URI reached SystemUI.
         */
        private void downloadImageAsync(String url) {
            final String sourceUrl = url;
            mImageExecutor.execute(() -> {
                File file = null;
                try {
                    java.net.URL parsed = new java.net.URL(sourceUrl);
                    java.net.HttpURLConnection conn =
                            (java.net.HttpURLConnection) parsed.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(15000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android)");
                    int code = conn.getResponseCode();
                    if (code < 200 || code >= 300) {
                        Log.w(TAG, "downloadImageAsync HTTP " + code + " for " + sourceUrl);
                        return;
                    }
                    String path = parsed.getPath();
                    String ext = ".png";
                    if (path != null) {
                        int dot = path.lastIndexOf('.');
                        if (dot >= 0 && path.length() - dot <= 6) {
                            ext = path.substring(dot).toLowerCase(Locale.US);
                        }
                    }
                    File dir = getIncomingDir(mContext);
                    String name = sanitizeIncomingFileName(
                            "url_" + SystemClock.uptimeMillis() + ext);
                    file = new File(dir, name);
                    long total = 0;
                    try (InputStream in = conn.getInputStream();
                         OutputStream out = new FileOutputStream(file)) {
                        byte[] buffer = new byte[65536];
                        int count;
                        while ((count = in.read(buffer)) > 0) {
                            total += count;
                            if (total > 64L * 1024 * 1024) {
                                throw new java.io.IOException("image too large");
                            }
                            out.write(buffer, 0, count);
                        }
                        out.flush();
                    }
                    if (total <= 0) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        return;
                    }
                    Uri uri = Uri.parse(INCOMING_URI_PREFIX + Uri.encode(file.getName()));
                    grantReadToCommonTargets(mContext, uri);
                    pruneIncomingCache(dir, file);
                    final File staged = file;
                    final String mime = sniffIncomingMime(file);
                    final Uri stagedUri = uri;
                    sHandler.post(() -> {
                        if (!TextUtils.equals(sourceUrl, mImageUrl)) {
                            return;
                        }
                        mImageUri = stagedUri;
                        mImageMime = mime != null && mime.startsWith("image/") ? mime : "image/*";
                        requestTargetsAsync();
                        // ACTION_SHOW already ran triggerQrScanAsync() while
                        // mImageUri was still null; rescan now that the URL
                        // image is actually staged.
                        triggerQrScanAsync();
                        Log.i(TAG, "downloadImageAsync staged " + staged.getName()
                                + " " + staged.length() + " bytes");
                    });
                } catch (Throwable t) {
                    Log.w(TAG, "downloadImageAsync failed for " + sourceUrl, t);
                    if (file != null) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                }
            });
        }

        /**
         * Writes a bitmap into SystemUI's exportable FileProvider incoming cache
         * (content://com.android.systemui.fileprovider/hyperportal_incoming/<name>).
         */
        private Uri stageBitmapInPortal(Context ctx, Bitmap bmp, String prefix) {
            if (bmp == null || bmp.isRecycled()) {
                Log.w(TAG, "stageBitmapInPortal skipped: bitmap unavailable");
                return null;
            }
            String name = sanitizeIncomingFileName(prefix + SystemClock.uptimeMillis() + ".png");
            File dir = getIncomingDir(ctx);
            File file = new File(dir, name);
            Uri target = Uri.parse(INCOMING_URI_PREFIX + Uri.encode(name));
            try (OutputStream output = new FileOutputStream(file)) {
                if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    Log.w(TAG, "stageBitmapInPortal: PNG compression failed");
                    return null;
                }
                output.flush();
                grantReadToCommonTargets(ctx, target);
                pruneIncomingCache(dir, file);
                Log.i(TAG, "stageBitmapInPortal " + name + " "
                        + bmp.getWidth() + "x" + bmp.getHeight());
                return target;
            } catch (Throwable t) {
                Log.w(TAG, "stageBitmapInPortal failed for " + target, t);
                return null;
            }
        }

        /**
         * Image shares are dispatched directly from SystemUI when the URI is hosted
         * on com.android.systemui.fileprovider (which SystemUI owns and can grant),
         * falling back to ShareProxyActivity only for external provider URIs.
         */
        private void dispatchImageShare(Context ctx, String targetPkg,
                                        String targetActivity, Uri uri, String text) {
            try {
                if (uri != null && SYSUI_PROVIDER_AUTHORITY.equals(uri.getAuthority())) {
                    String mime = !TextUtils.isEmpty(mImageMime) ? mImageMime : "image/*";
                    ctx.grantUriPermission(targetPkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType(mime);
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    if (!TextUtils.isEmpty(text)) {
                        share.putExtra(Intent.EXTRA_TEXT, text);
                    }
                    share.setClipData(new ClipData(
                            "HyperPortal", new String[]{mime}, new ClipData.Item(uri)));
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    if (!TextUtils.isEmpty(targetActivity)) {
                        share.setComponent(new ComponentName(targetPkg, targetActivity));
                        try {
                            ctx.startActivity(share);
                            Log.i(TAG, "dispatchImageShare direct component=" + targetPkg + "/" + targetActivity
                                    + " uri=" + uri + " mime=" + mime);
                            return;
                        } catch (Throwable componentError) {
                            Log.w(TAG, "direct component share failed, falling back to package", componentError);
                            share.setComponent(null);
                        }
                    }
                    share.setPackage(targetPkg);
                    ctx.startActivity(share);
                    Log.i(TAG, "dispatchImageShare direct target=" + targetPkg
                            + " uri=" + uri + " mime=" + mime);
                    return;
                }
                Intent proxy = new Intent();
                proxy.setClassName(
                        PortalShareContract.PORTAL_PACKAGE,
                        PortalShareContract.SHARE_PROXY_ACTIVITY);
                proxy.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                proxy.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                proxy.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                proxy.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                proxy.putExtra(PortalShareContract.EXTRA_TARGET_PACKAGE, targetPkg);
                if (!TextUtils.isEmpty(targetActivity)) {
                    proxy.putExtra(PortalShareContract.EXTRA_TARGET_ACTIVITY, targetActivity);
                }
                proxy.putExtra(PortalShareContract.EXTRA_IMAGE_URI, uri.toString());
                if (!TextUtils.isEmpty(mImageMime)) {
                    proxy.putExtra(PortalShareContract.EXTRA_IMAGE_MIME, mImageMime);
                }
                if (!TextUtils.isEmpty(text)) {
                    proxy.putExtra(PortalShareContract.EXTRA_SHARE_TEXT, text);
                }
                ctx.startActivity(proxy);
                Log.i(TAG, "dispatchImageShare proxy target=" + targetPkg
                        + " uri=" + uri + " mime=" + mImageMime);
            } catch (Throwable t) {
                Log.w(TAG, "dispatchImageShare failed target=" + targetPkg
                        + " uri=" + uri, t);
                toast("无法分享图片");
            }
        }

        private void dispatchSystemShare(Context ctx, Uri uri, String text) {
            try {
                if (uri == null || SYSUI_PROVIDER_AUTHORITY.equals(uri.getAuthority())) {
                    String mime = !TextUtils.isEmpty(mImageMime) ? mImageMime : "image/*";
                    Intent send = new Intent(Intent.ACTION_SEND);
                    if (uri != null) {
                        grantReadToCommonTargets(ctx, uri);
                        send.setType(mime);
                        send.putExtra(Intent.EXTRA_STREAM, uri);
                        send.setClipData(new ClipData(
                                "HyperPortal", new String[]{mime}, new ClipData.Item(uri)));
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        if (!TextUtils.isEmpty(text)) {
                            send.putExtra(Intent.EXTRA_TEXT, text);
                        }
                    } else {
                        send.setType("text/plain");
                        send.putExtra(Intent.EXTRA_TEXT, text != null ? text : "");
                    }
                    Intent chooser = Intent.createChooser(send, "分享内容");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    ctx.startActivity(chooser);
                    Log.i(TAG, "dispatchSystemShare direct chooser started uri=" + uri);
                    return;
                }
                Intent proxy = new Intent();
                proxy.setClassName(
                        PortalShareContract.PORTAL_PACKAGE,
                        PortalShareContract.SHARE_PROXY_ACTIVITY);
                proxy.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                proxy.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                proxy.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                proxy.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                proxy.putExtra(PortalShareContract.EXTRA_TARGET_PACKAGE, "system:chooser");
                proxy.putExtra("isChooser", true);
                if (uri != null) {
                    proxy.putExtra(PortalShareContract.EXTRA_IMAGE_URI, uri.toString());
                }
                if (!TextUtils.isEmpty(mImageMime)) {
                    proxy.putExtra(PortalShareContract.EXTRA_IMAGE_MIME, mImageMime);
                }
                if (!TextUtils.isEmpty(text)) {
                    proxy.putExtra(PortalShareContract.EXTRA_SHARE_TEXT, text);
                }
                ctx.startActivity(proxy);
                Log.i(TAG, "dispatchSystemShare chooser started uri=" + uri);
            } catch (Throwable t) {
                Log.w(TAG, "dispatchSystemShare failed", t);
                toast("无法打开系统分享");
            }
        }

        private void buildViewHierarchy() {
            mContainer = new FrameLayout(mContext);
            mContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
            mContainer.setFitsSystemWindows(false);
            mContainer.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

            int navBarHeight = dp(24);
            try {
                int resId = mContext.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                if (resId > 0) {
                    navBarHeight = mContext.getResources().getDimensionPixelSize(resId);
                }
            } catch (Throwable ignored) {
            }

            // 1. ColorOS Aurora GlowView
            mGlowView = new GlowView(mContext);
            FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.BOTTOM);
            mContainer.addView(mGlowView, glowParams);

            // 2. ColorOS Dock Bar (initially hidden in Phase 1)
            mDockBar = new ColorOSDockBar(mContext);
            mDockBar.setVisibility(View.GONE);
            mDockBar.setTranslationY(dp(60));
            mDockBar.setAlpha(0f);
            List<ColorOSDockBar.DockItem> dockItems = new ArrayList<>();
            for (AppTarget target : mTargets) {
                dockItems.add(new ColorOSDockBar.DockItem(
                        target.key, target.name, target.icon, target));
            }
            mDockBar.setItems(dockItems);
            FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(153), Gravity.BOTTOM);
            dockParams.bottomMargin = Math.max(0, navBarHeight + dp(20) - 50);
            mContainer.addView(mDockBar, dockParams);

            // 4. Drag and Drop Listener on Container (ColorOS Dock Interaction & Fly-back)
            mContainer.setOnDragListener((v, event) -> {
                int action = event.getAction();
                if (action == DragEvent.ACTION_DRAG_STARTED) {
                    mGallerySavedThisDrag = false;
                    if (TransferDockDragGuard.isDragging() || mTransferDockBroadcastDragging) {
                        handleDragStateChanged(0, 0f);
                        return false;
                    }
                    return isMasterEnabled();
                } else if (action == DragEvent.ACTION_DRAG_LOCATION) {
                    if (event.getX() == 0f && event.getY() == 0f) {
                        return true;
                    }
                    int[] loc = new int[2];
                    mContainer.getLocationOnScreen(loc);
                    float rawX = loc[0] + event.getX();
                    float rawY = loc[1] + event.getY();
                    if (mGlowView != null) {
                        mGlowView.setTouchPosition(rawX, rawY);
                    }
                    if (mShowing && mDockBar != null && mDockBar.getVisibility() == View.VISIBLE) {
                        mDockBar.onDragLocation(rawX, rawY);
                    }
                    return true;
                } else if (action == DragEvent.ACTION_DRAG_EXITED) {
                    if (mDockBar != null) {
                        mDockBar.onDragExited();
                    }
                    return true;
                } else if (action == DragEvent.ACTION_DROP) {
                    int[] loc = new int[2];
                    mContainer.getLocationOnScreen(loc);
                    float rawX = loc[0] + event.getX();
                    float rawY = loc[1] + event.getY();
                    mLastDropX = rawX;
                    mLastDropY = rawY;
                    ColorOSDockBar.DockItem hit = mDockBar != null ? mDockBar.findHitTarget(rawX, rawY) : null;
                    Log.i(TAG, "ACTION_DROP hit=" + (hit == null ? "null" : hit.label)
                            + " text=" + (mText == null ? "null" : mText.length())
                            + " bmp=" + (mBitmap == null ? "null" : mBitmap.getWidth() + "x" + mBitmap.getHeight())
                            + " uri=" + mImageUri + " mime=" + mImageMime);
                    if (hit != null && hit.tag instanceof AppTarget) {
                        AppTarget target = (AppTarget) hit.tag;
                        try {
                            recordTargetUse(target.key);
                            target.action.onDrop(mContext, mText, mBitmap, mImageUri);
                        } catch (Throwable t) {
                            Log.w(TAG, "drop action failed", t);
                        }
                        handleDragStateChanged(0, 0f);
                        return true; // Drop consumed!
                    }
                    // CRITICAL INVARIANT: Dropped outside targets -> return false!
                    // Instructs WMS to play the cancel fly-back animation!
                    handleDragStateChanged(0, 0f);
                    return false;
                } else if (action == DragEvent.ACTION_DRAG_ENDED) {
                    if (mDockBar != null) {
                        mDockBar.resetMagnification(false);
                    }
                    handleDragStateChanged(0, 0f);
                    return true;
                }
                return true;
            });

            // Layout Params for System Window
            boolean isRight = "right".equals(mTriggerPosition);
            int targetHeight = isRight ? WindowManager.LayoutParams.MATCH_PARENT : dp(380);
            int targetGravity = isRight ? Gravity.FILL : Gravity.BOTTOM;
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    targetHeight,
                    TYPE_NAVIGATION_BAR_PANEL, // SystemUI native window
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT);
            if (Build.VERSION.SDK_INT >= 28) {
                mParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            mParams.gravity = targetGravity;
            mParams.x = 0;
            mParams.y = 0;
            updateDockConfiguration(isRight);
        }

        private void updateWindowParams(boolean isRight) {
            if (mParams == null) return;
            int targetHeight = isRight ? WindowManager.LayoutParams.MATCH_PARENT : dp(380);
            int targetGravity = isRight ? Gravity.FILL : Gravity.BOTTOM;
            if (mParams.height != targetHeight || mParams.gravity != targetGravity) {
                mParams.height = targetHeight;
                mParams.gravity = targetGravity;
                mParams.x = 0;
                mParams.y = 0;
                if (Build.VERSION.SDK_INT >= 28) {
                    mParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                }
                if (mShowing && mContainer != null) {
                    try {
                        mWindowManager.updateViewLayout(mContainer, mParams);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        private void updateDockConfiguration(boolean isRight) {
            if (mDockBar == null) return;
            int targetOrientation = isRight ? ColorOSDockBar.ORIENTATION_VERTICAL : ColorOSDockBar.ORIENTATION_HORIZONTAL;
            if (mDockBar.getOrientation() != targetOrientation) {
                mDockBar.setOrientation(targetOrientation);
            }
            FrameLayout.LayoutParams dockParams;
            ViewGroup.LayoutParams lp = mDockBar.getLayoutParams();
            if (lp instanceof FrameLayout.LayoutParams) {
                dockParams = (FrameLayout.LayoutParams) lp;
            } else {
                dockParams = new FrameLayout.LayoutParams(
                        isRight ? dp(88) : FrameLayout.LayoutParams.MATCH_PARENT,
                        isRight ? FrameLayout.LayoutParams.MATCH_PARENT : dp(153));
            }
            if (isRight) {
                dockParams.width = dp(88);
                dockParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
                dockParams.gravity = Gravity.END | Gravity.TOP;
                dockParams.rightMargin = dp(2);
                dockParams.topMargin = 0;
                dockParams.bottomMargin = 0;
                dockParams.leftMargin = 0;
            } else {
                int navBarHeight = dp(24);
                try {
                    int resId = mContext.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                    if (resId > 0) {
                        navBarHeight = mContext.getResources().getDimensionPixelSize(resId);
                    }
                } catch (Throwable ignored) {
                }
                dockParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
                dockParams.height = dp(153);
                dockParams.gravity = Gravity.BOTTOM;
                dockParams.bottomMargin = Math.max(0, navBarHeight + dp(20) - 50);
                dockParams.rightMargin = 0;
                dockParams.leftMargin = 0;
                dockParams.topMargin = 0;
            }
            mDockBar.setLayoutParams(dockParams);
        }

        


            


        private void recordTargetUse(String key) {
            if (TextUtils.isEmpty(key)) return;
            // Target affinity is runtime state owned by SystemUI. Increment
            // the cached counter locally instead of waking HyperPortal just
            // to write one integer into its preferences.
            sBgExecutor.execute(() -> {
                synchronized (SystemUiOverlay.this) {
                    try {
                        android.content.SharedPreferences prefs =
                                mContext.getSharedPreferences(CONFIG_CACHE_PREFS, Context.MODE_PRIVATE);
                        java.util.LinkedHashMap<String, Integer> usage =
                                new java.util.LinkedHashMap<>();
                        String raw = prefs.getString("target_usage", "");
                        if (!TextUtils.isEmpty(raw)) {
                            for (String line : raw.split("\n")) {
                                int split = Math.max(line.lastIndexOf('='), line.lastIndexOf(':'));
                                if (split <= 0) continue;
                                try {
                                    usage.put(line.substring(0, split),
                                            Integer.parseInt(line.substring(split + 1)));
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        int next = usage.containsKey(key) ? usage.get(key) + 1 : 1;
                        usage.put(key, Math.min(next, 1000));
                        StringBuilder sb = new StringBuilder();
                        for (java.util.Map.Entry<String, Integer> entry : usage.entrySet()) {
                            if (sb.length() > 0) sb.append('\n');
                            sb.append(entry.getKey()).append('=').append(entry.getValue());
                        }
                        prefs.edit().putString("target_usage", sb.toString()).apply();
                    } catch (Throwable ignored) {
                    }
                }
            });
        }

        private void toast(String msg) {
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }

        private void toastAsync(String msg) {
            sHandler.post(() -> toast(msg));
        }

        public void registerReceivers() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_DRAG_STATE);
            filter.addAction("com.github.leaf.hyperportal.DRAG_STATE_CHANGED");
            filter.addAction(ACTION_SHOW);
            filter.addAction(ACTION_REQUEST_GRANT);
            filter.addAction("com.github.leaf.hyperportal.ACTION_CONFIG_CHANGED");
            filter.addAction("com.github.leaf.hyperportal.ACTION_CONFIG_SNAPSHOT");
            filter.addAction(TransferDockDragGuard.ACTION_STATE);
            filter.addAction("com.github.leaf.hyperportal.OPEN_TRANSFER_DOCK");
            filter.addAction("com.github.leaf.hyperportal.ACTION_SAVE_TRANSFER_DOCK");
            filter.addAction(Intent.ACTION_USER_UNLOCKED);
            filter.addAction(Intent.ACTION_BOOT_COMPLETED);
            if (Build.VERSION.SDK_INT >= 33) {
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        try {
                            int senderUid = getSentFromUid();
                            handleIntent(intent, senderUid);
                        } catch (Throwable t) {
                            Log.e(TAG, "handleIntent failed in SystemUI", t);
                        }
                    }
                }, filter, Context.RECEIVER_EXPORTED);
            } else {
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        try {
                            handleIntent(intent, -1);
                        } catch (Throwable t) {
                            Log.e(TAG, "handleIntent failed in SystemUI", t);
                        }
                    }
                }, filter);
            }
        }

        private boolean isSamePackageUid(int uid, String pkg) {
            if (uid <= 0) {
                // No sender information (e.g. a system-injected broadcast).
                return true;
            }
            if (uid == android.os.Process.SYSTEM_UID
                    || uid == android.os.Process.myUid()) {
                return true;
            }
            try {
                String[] packages = mContext.getPackageManager().getPackagesForUid(uid);
                if (packages != null) {
                    for (String candidate : packages) {
                        if (pkg.equals(candidate)) {
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            return false;
        }

        private boolean isLandscape() {
            try {
                return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            } catch (Throwable t) {
                return false;
            }
        }

        /**
         * Answers from the cached config snapshot. This is called from the
         * drag listener on the input path, so it must never do a blocking
         * provider call: a cold-started HyperPortal process used to stall
         * SystemUI's main thread here for seconds. A stale or missing
         * snapshot just schedules a background refresh; the current drag
         * keeps the previous answer.
         */
        private boolean isMasterEnabled() {
            long age = SystemClock.uptimeMillis() - mConfigFetchedAt;
            if (!mConfigRefreshing && (!mConfigLoaded || age > CONFIG_TTL_MS)) {
                refreshConfigAsync();
            }
            if (!mLandscapeAllowed && isLandscape()) {
                return false;
            }
            return mMasterEnabled;
        }

        /**
         * Applies the settings the app just broadcast, so the cached snapshot
         * is never stale for more than one broadcast. No provider call.
         */
        private void applyConfigBroadcast(Intent intent) {
            boolean rebuildTargets = true;
            if (intent.hasExtra("from_system_server")) {
                // The snapshot is re-pushed at every drag start; only the trigger
                // settings changed there, so rebuilding the 100+ entry shelf
                // would be pure jank on the drag path.
                rebuildTargets = false;
            }
            Bundle extras = intent.getExtras();
            if (extras != null && !intent.hasExtra("from_system_server")) {
                // A system_server snapshot only carries trigger/guard/anim
                // fields and may be built from defaults before SystemUI has
                // read the app config. Never let it become the cache.
                // SharedPreferences may hit disk on first load; keep it off
                // the broadcast/input thread.
                final Bundle cacheSnapshot = new Bundle(extras);
                sBgExecutor.execute(() -> saveCachedConfig(cacheSnapshot));
            }
            if (intent.hasExtra("master_switch")) {
                mMasterEnabled = intent.getBooleanExtra("master_switch", mMasterEnabled);
            }
            if (intent.hasExtra("landscape_trigger")) {
                mLandscapeAllowed = intent.getBooleanExtra("landscape_trigger", mLandscapeAllowed);
            }
            int thresholdPct = intent.getIntExtra("trigger_threshold", -1);
            if (thresholdPct >= 50 && thresholdPct <= 95) {
                mTriggerThreshold = thresholdPct / 100.0f;
            }
            String pos = intent.getStringExtra("trigger_position");
            if (pos != null) {
                mTriggerPosition = pos;
            }
            if (mDockBar != null && intent.hasExtra("edge_scroll_margin")) {
                mDockBar.setEdgeScrollMarginDp(intent.getIntExtra("edge_scroll_margin", 86));
            }
            if (!intent.hasExtra("from_system_server") || hasCachedConfig(mContext)) {
                mConfigLoaded = true;
                mConfigFetchedAt = SystemClock.uptimeMillis();
            }
            Log.i(TAG, "config broadcast applied master=" + mMasterEnabled
                    + " threshold=" + mTriggerThreshold + " pos=" + mTriggerPosition);
            // Target limit / pin / hidden changes need a rebuilt shelf.
            if (rebuildTargets) {
                requestTargetsAsync();
            }
        }

        private void handleIntent(Intent intent) {
            handleIntent(intent, -1);
        }

        private void handleIntent(Intent intent, int senderUid) {
            if (intent == null) return;
            String action = intent.getAction();
            if (ACTION_REQUEST_GRANT.equals(action)) {
                String pkg = intent.getStringExtra("package");
                if (TextUtils.isEmpty(pkg)) {
                    pkg = intent.getStringExtra("target_package");
                }
                if (TextUtils.isEmpty(pkg)) {
                    return;
                }
                // This receiver is exported. Only grant the prefix to the
                // package that actually sent the request; otherwise any app
                // could hand read/write to an arbitrary package name.
                if (!isSamePackageUid(senderUid, pkg)) {
                    Log.w(TAG, "rejecting grant request uid=" + senderUid + " pkg=" + pkg);
                    return;
                }
                grantIncomingPrefixToPackage(mContext, pkg);
                return;
            }
            if ("com.github.leaf.hyperportal.ACTION_CONFIG_CHANGED".equals(action)
                    || "com.github.leaf.hyperportal.ACTION_CONFIG_SNAPSHOT".equals(action)) {
                applyConfigBroadcast(intent);
                return;
            }
            if (TransferDockDragGuard.ACTION_STATE.equals(action)) {
                mTransferDockBroadcastDragging = intent.getIntExtra("state", 0) == 1;
                if (mTransferDockBroadcastDragging) {
                    handleDragStateChanged(0, 0f);
                }
                return;
            }
            if ("com.github.leaf.hyperportal.OPEN_TRANSFER_DOCK".equals(action)) {
                if (mTransferDockController == null) {
                    mTransferDockController = new TransferDockController(mContext);
                }
                mTransferDockController.openDrawer();
                return;
            }
            if ("com.github.leaf.hyperportal.ACTION_SAVE_TRANSFER_DOCK".equals(action)) {
                String text = intent.getStringExtra("text");
                String uriStr = intent.getStringExtra("uri");
                Uri uri = uriStr != null ? Uri.parse(uriStr) : null;
                String mime = intent.getStringExtra("mimeType");
                if (TextUtils.isEmpty(mime)) {
                    mime = intent.getStringExtra("imageMimeType");
                }
                saveToTransferDock(mContext, text, null, uri, mime, null);
                return;
            }
            if (Intent.ACTION_USER_UNLOCKED.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                updateDockTargets();
                return;
            }
            if (ACTION_SHOW.equals(action)) {
                String sourcePkg = intent.getStringExtra("source_package");
                if (!TextUtils.isEmpty(sourcePkg)) {
                    grantIncomingPrefixToPackage(mContext, sourcePkg);
                }
                mGallerySavedThisDrag = false;
                mText = intent.getStringExtra("viewContent");
                mBitmap = null;
                mImageUri = null;
                mImageMime = null;
                mImageFileName = intent.getStringExtra("imageFileName");
                mImageUrl = intent.getStringExtra("imageUrl");
                Object bmp = intent.getParcelableExtra("viewBitmap");
                if (bmp instanceof Bitmap) {
                    mBitmap = (Bitmap) bmp;
                }
                String uriStr = intent.getStringExtra("imageUri");
                if (uriStr != null) {
                    mImageUri = Uri.parse(uriStr);
                }
                mImageMime = intent.getStringExtra("imageMimeType");
                Log.i(TAG, "ACTION_SHOW text=" + (mText == null ? "null" : mText.length())
                        + " bitmap=" + (mBitmap == null ? "null" : mBitmap.getWidth() + "x" + mBitmap.getHeight())
                        + " uri=" + mImageUri + " url=" + mImageUrl + " mime=" + mImageMime);
                updateDockTargets();
                if (mImageUri == null && mBitmap == null && !TextUtils.isEmpty(mImageUrl)) {
                    downloadImageAsync(mImageUrl);
                }
                triggerQrScanAsync();
            } else if (ACTION_DRAG_STATE.equals(action) || "com.github.leaf.hyperportal.DRAG_STATE_CHANGED".equals(action)) {
                int state = intent.getIntExtra("state", 0);
                float x = intent.getFloatExtra("x", -1f);
                float y = intent.getFloatExtra("y", 0f);
                // Deliberately ignores the per-event "position" extra: three
                // processes send this broadcast and only one of them can read
                // the real settings (the other two fall back to "bottom" when
                // the HyperPortal provider is frozen). Overwriting our own
                // config here made the right shelf flip to a bottom row
                // mid-drag. Trigger edge comes from applyConfig /
                // applyConfigBroadcast only.
                if (state > 0 && !isMasterEnabled()) {
                    handleDragStateChanged(0, -1f, 0f);
                    return;
                }
                handleDragStateChanged(state, x, y);
            }
        }

        private void setTouchable(boolean touchable) {
            if (mParams == null || mContainer == null || !mShowing) return;
            int oldFlags = mParams.flags;
            if (touchable) {
                mParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            } else {
                mParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            if (mParams.flags != oldFlags) {
                try {
                    mWindowManager.updateViewLayout(mContainer, mParams);
                } catch (Throwable ignored) {
                }
            }
        }

        private void showOverlay() {
            if (!mShowing) {
                try {
                    mWindowManager.addView(mContainer, mParams);
                    mShowing = true;
                } catch (Throwable t) {
                    Log.w(TAG, "addView failed", t);
                }
            }
        }

        private void hideOverlay() {
            if (mShowing) {
                try {
                    mWindowManager.removeView(mContainer);
                    mShowing = false;
                } catch (Throwable t) {
                    Log.w(TAG, "removeView failed", t);
                }
            }
        }

        public void handleDragStateChanged(int state, float y) {
            handleDragStateChanged(state, -1f, y);
        }

        public void handleDragStateChanged(int state, float x, float y) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                sHandler.post(() -> handleDragStateChanged(state, x, y));
                return;
            }
            if (state > 0 && (TransferDockDragGuard.isDragging() || mTransferDockBroadcastDragging)) {
                return;
            }
            if (state > 0 && !mFirstDragLogged) {
                mFirstDragLogged = true;
                Log.i(TAG, "first drag after process start: state=" + state
                        + " at " + msSinceProcessStart() + "ms views=" + mViewsReady
                        + " targets=" + mTargets.size() + " config=" + mConfigLoaded);
            }
            mCurrentState = state;

            if (state == 0) {
                // Drag ended / cancelled
                mQrSessionId++;
                mDetectedQrContent = null;
                if (mGlowView != null) {
                    mGlowView.resetState();
                    mGlowView.setVisibility(View.GONE);
                }
                if (mDockBar != null) {
                    mDockBar.stopAutoScroll();
                    mDockBar.resetMagnification(false);
                    mDockBar.setVisibility(View.GONE);
                }
                hideOverlay();
                mText = null;
                mBitmap = null;
                mImageUri = null;
                mImageMime = null;
                mImageFileName = null;
                mImageUrl = null;
                return;
            }

            if (!ensureViews()) {
                return;
            }

            if (!mAppTargetsResolved || mTargets.isEmpty()) {
                updateDockTargets();
            }

            boolean isRight = "right".equals(mTriggerPosition);
            updateWindowParams(isRight);
            updateDockConfiguration(isRight);

            if (state == 1) {
                // Phase 1: Glow view active, shelf collapsed
                showOverlay();
                setTouchable(false);
                float coord = isRight ? x : y;
                int screenDim = isRight ? mContext.getResources().getDisplayMetrics().widthPixels
                                        : mContext.getResources().getDisplayMetrics().heightPixels;
                float enterFactor = mTriggerThreshold > 0f ? mTriggerThreshold : 0.76f;
                float glowSpan = Math.max(0.2f, enterFactor - 0.15f);
                float progress = Math.max(0f, Math.min(1.0f, (coord - screenDim * 0.15f) / (screenDim * glowSpan)));
                if (mGlowView != null) {
                    mGlowView.setPhase1Progress(progress, isRight, y);
                    mGlowView.setVisibility(View.VISIBLE);
                }
                if (mDockBar != null && mDockBar.getVisibility() == View.VISIBLE) {
                    mDockBar.stopAutoScroll();
                    if (isRight) {
                        mDockBar.animate()
                                .translationX(dp(60))
                                .alpha(0f)
                                .setDuration(160)
                                .withEndAction(() -> {
                                    if (mDockBar != null) mDockBar.setVisibility(View.GONE);
                                })
                                .start();
                    } else {
                        mDockBar.animate()
                                .translationY(dp(60))
                                .alpha(0f)
                                .setDuration(160)
                                .withEndAction(() -> {
                                    if (mDockBar != null) mDockBar.setVisibility(View.GONE);
                                })
                                .start();
                    }
                }
            } else if (state == 2) {
                // Phase 2: Crossed threshold -> Expand shelf & enable touch
                showOverlay();
                setTouchable(true);
                if (mGlowView != null) {
                    mGlowView.setPhase2(isRight, y);
                    mGlowView.setVisibility(View.VISIBLE);
                }
                if (mDockBar != null) {
                    if (mDockBar.getVisibility() != View.VISIBLE) {
                        mDockBar.resetScroll();
                        mDockBar.setVisibility(View.VISIBLE);
                        if (isRight) {
                            mDockBar.setTranslationX(dp(60));
                            mDockBar.setTranslationY(0f);
                        } else {
                            mDockBar.setTranslationX(0f);
                            mDockBar.setTranslationY(dp(60));
                        }
                        mDockBar.setAlpha(0f);
                        if (x >= 0) {
                            mDockBar.startEnterAnimation(x, y);
                        } else {
                            mDockBar.startEnterAnimation();
                        }
                        long now = SystemClock.uptimeMillis();
                        if (now - mLastHapticTime > 500L) {
                            mLastHapticTime = now;
                            try {
                                mDockBar.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    if (isRight) {
                        mDockBar.animate()
                                .translationX(0f)
                                .alpha(1.0f)
                                .setDuration(220)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                    } else {
                        mDockBar.animate()
                                .translationY(0f)
                                .alpha(1.0f)
                                .setDuration(220)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                    }
                }
            }
        }

        private void triggerQrScanAsync() {
            mQrSessionId++;
            final int currentSession = mQrSessionId;
            mDetectedQrContent = null;
            if (mBitmap == null && mImageUri == null) {
                return;
            }
            final Bitmap bmpToScan = mBitmap;
            final Uri uriToScan = mImageUri;
            mQrExecutor.execute(() -> {
                try {
                    // TargetConfig.fromProvider() is a blocking provider call:
                    // keep it off SystemUI's main thread.
                    TargetConfig config = TargetConfig.fromProvider(mContext);
                    if (!config.enableQrScan || config.hiddenKeys.contains("builtin:qr")) {
                        Log.i(TAG, "QR scan skipped: disabled in config");
                        return;
                    }
                    String qrResult = null;
                    if (bmpToScan != null && !bmpToScan.isRecycled()) {
                        qrResult = QrCodeScanner.scan(bmpToScan);
                    }
                    if (qrResult == null && uriToScan != null) {
                        qrResult = QrCodeScanner.scan(mContext, uriToScan);
                    }
                    if (qrResult != null && !qrResult.trim().isEmpty()) {
                        final String finalQr = qrResult.trim();
                        sHandler.post(() -> onQrCodeDetected(finalQr, currentSession));
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "QR scan async error", t);
                }
            });
        }

        private void onQrCodeDetected(String qrContent, int sessionId) {
            if (sessionId != mQrSessionId || (mBitmap == null && mImageUri == null)) {
                return;
            }
            mDetectedQrContent = qrContent;
            Log.i(TAG, "onQrCodeDetected: " + qrContent);

            for (AppTarget t : mTargets) {
                if ("builtin:qr".equals(t.key)) {
                    return;
                }
            }

            final boolean isUrl = isWebUrl(qrContent);
            String label = isUrl ? "打开链接" : "识别二维码";

            DropAction action = (ctx, text, bmp, uri) -> {
                if (isUrl) {
                    try {
                        String url = qrContent;
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(view);
                        toast("正在打开: " + qrContent);
                    } catch (Throwable t) {
                        Log.w(TAG, "open url failed", t);
                        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("HyperPortal QR", qrContent));
                        toast("已复制二维码内容");
                    }
                } else {
                    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("HyperPortal QR", qrContent));
                    toast("已复制二维码内容: " + qrContent);
                }
            };

            Drawable icon = BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_QR);
            AppTarget qrTarget = new AppTarget("builtin:qr", label, icon, action);

            // Pinned order comes from the cached config snapshot: reading it
            // from the provider here would block the main thread mid-drag.
            List<String> pinnedOrder = mPinnedOrder;
            int insertIndex = 0;
            if (pinnedOrder.contains("builtin:qr")) {
                insertIndex = Math.min(mTargets.size(), Math.max(0, pinnedOrder.indexOf("builtin:qr")));
            }
            mTargets.add(insertIndex, qrTarget);

            if (mDockBar != null) {
                List<ColorOSDockBar.DockItem> dockItems = new ArrayList<>();
                for (AppTarget target : mTargets) {
                    dockItems.add(new ColorOSDockBar.DockItem(
                            target.key, target.name, target.icon, target));
                }
                mDockBar.setItems(dockItems);
            }
        }

        private static boolean isWebUrl(String text) {
            if (TextUtils.isEmpty(text)) return false;
            String lower = text.trim().toLowerCase(Locale.US);
            if (lower.startsWith("http://") || lower.startsWith("https://")) {
                return true;
            }
            return Patterns.WEB_URL.matcher(text.trim()).matches();
        }
    }

    private static final class AppTarget {
        final String key;
        final String name;
        final Drawable icon;
        final DropAction action;

        AppTarget(String key, String name, Drawable icon, DropAction action) {
            this.key = key;
            this.name = name;
            this.icon = icon;
            this.action = action;
        }

        AppTarget(ShareTargetEntry entry, DropAction action) {
            this.key = entry.key;
            this.name = entry.label;
            this.icon = entry.displayIcon();
            this.action = action;
        }
    }

    private interface DropAction {
        void onDrop(Context context, String text, Bitmap bitmap, Uri imageUri) throws Throwable;
    }

    /**
     * ColorOS Aurora Bottom Glow / Honor AnyDoor Right Glow View.
     */
    private static final class GlowView extends View {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mHonorPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private RuntimeShader mShader;
        private Bitmap mNoiseBmp;
        private Bitmap mHonorGlowBmp;

        private float mTargetCenterX = 0.5f;
        private float mCurrentCenterX = 0.5f;
        private float mTargetCenterY = -4.134f;
        private float mCurrentCenterY = -4.134f;
        private float mTargetStart = 4.0f;
        private float mCurrentStart = 4.0f;
        private float mTargetSpace = 0.18f;
        private float mCurrentSpace = 0.18f;
        private float mTargetAlpha = 0.0f;
        private float mCurrentAlpha = 0.0f;
        private boolean mIsRight = false;

        // Dynamic right-edge glow tracking finger Y position (decompiled from Honor AnyDoor CO.webp)
        private float mTargetTouchY = -1f;
        private float mCurrentTouchY = -1f;
        private float mTargetScaleX = 0f;
        private float mCurrentScaleX = 0f;

        /*
         * ColorOS bottom glow state machine, taken from
         * assets/glow_effect.coz (process1_StandardPhone / process2_StandardPhone).
         *
         * process1 is NOT a distance-driven wash: it is a thin aurora rim hugging
         * the bottom edge (center 4.02 screen-widths below the edge, radius 4.0,
         * 0.18*width falloff, alpha 1). process2 is the transient bloom played when
         * the drop zone is entered: it expands for 300 ms while alpha falls to 0
         * by 380 ms. The old code held process2's end values as a permanent state,
         * which is why the glow looked far heavier than ColorOS.
         */
        private static final float RIM_CENTER_Y = -4.02f;
        private static final float RIM_START = 4.0f;
        private static final float RIM_SPACE = 0.18f;
        private static final float OPEN_CENTER_Y = -1.16f;
        private static final float OPEN_START = 1.0f;
        private static final float OPEN_SPACE = 0.76f;
        private static final long OPEN_MOVE_MS = 300L;
        private static final long OPEN_ALPHA_DELAY_MS = 80L;
        private static final long OPEN_ALPHA_MS = 300L;
        private static final long OPEN_TOTAL_MS = 380L;

        private final PathInterpolator mOpenMoveInterp =
                new PathInterpolator(0.30f, 0f, 0.10f, 1f);
        private final PathInterpolator mOpenFadeInterp =
                new PathInterpolator(0.17f, 0f, 0.83f, 1f);
        private boolean mOpenAnim = false;
        private long mOpenAnimStart = 0L;
        private float mOpenFromCenterY = RIM_CENTER_Y;
        private float mOpenFromStart = RIM_START;
        private float mOpenFromSpace = RIM_SPACE;
        private float mOpenFromAlpha = 0f;

        public GlowView(Context context) {
            super(context);
            initShader(context);
        }

        private int dp(float val) {
            return (int) (val * getResources().getDisplayMetrics().density + 0.5f);
        }

        private void initShader(Context context) {
            // 1. Honor AnyDoor Right Edge Glow Texture
            try {
                String moduleApk = sXposed != null && sXposed.getModuleApplicationInfo() != null
                        ? sXposed.getModuleApplicationInfo().sourceDir : null;
                if (moduleApk != null && new File(moduleApk).exists()) {
                    ZipFile zip = new ZipFile(moduleApk);
                    ZipEntry entry = zip.getEntry("assets/honor_edge_glow.webp");
                    if (entry != null) {
                        InputStream is = zip.getInputStream(entry);
                        mHonorGlowBmp = BitmapFactory.decodeStream(is);
                        is.close();
                    }
                    zip.close();
                }
                if (mHonorGlowBmp == null) {
                    try {
                        InputStream is = context.getAssets().open("honor_edge_glow.webp");
                        mHonorGlowBmp = BitmapFactory.decodeStream(is);
                        is.close();
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "honor_edge_glow load failed", t);
            }

            // 2. ColorOS Aurora Bottom Glow Shader
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    String moduleApk = sXposed != null && sXposed.getModuleApplicationInfo() != null
                            ? sXposed.getModuleApplicationInfo().sourceDir : null;
                    if (moduleApk != null && new File(moduleApk).exists()) {
                        ZipFile zip = new ZipFile(moduleApk);
                        ZipEntry entry = zip.getEntry("assets/oppo_glow.png");
                        if (entry != null) {
                            InputStream is = zip.getInputStream(entry);
                            mNoiseBmp = BitmapFactory.decodeStream(is);
                            is.close();
                        }
                        zip.close();
                    }
                    if (mNoiseBmp == null) {
                        try {
                            InputStream is = context.getAssets().open("oppo_glow.png");
                            mNoiseBmp = BitmapFactory.decodeStream(is);
                            is.close();
                        } catch (Throwable ignored) {
                        }
                    }

                    if (mNoiseBmp != null) {
                        String agsl = "uniform shader u_HaloColorNoiseTex;\n"
                                + "uniform float2 u_resolution;\n"
                                + "uniform float u_time;\n"
                                + "uniform float4 _HaloColor1;\n"
                                + "uniform float4 _HaloColor2;\n"
                                + "uniform float4 _HaloColor3;\n"
                                + "uniform float4 _HaloColor4;\n"
                                + "uniform float2 _HaloColorShakeDisp;\n"
                                + "uniform float4 _HaloColorShakeSpeed;\n"
                                + "uniform float4 _HaloColorSpeed;\n"
                                + "uniform float _HaloColorScale;\n"
                                + "uniform float _HaloColorPicSize;\n"
                                + "uniform float2 _HaloCenter;\n"
                                + "uniform float _HaloStart;\n"
                                + "uniform float _HaloSpace;\n"
                                + "uniform float _HaloAlpha;\n"
                                + "float2 rotate2d(float2 p, float angle) {\n"
                                + "    float s = sin(angle);\n"
                                + "    float c = cos(angle);\n"
                                + "    return float2(p.x * c + p.y * s, -p.x * s + p.y * c);\n"
                                + "}\n"
                                + "half4 generateColorLayer(float2 uv, float2 center,\n"
                                + "                    float2 colorShakeDisplacement, float colorShakeSpeed, float colorSpeed,\n"
                                + "                    float time, int channel, half3 color, float colorScale) {\n"
                                + "    float2 dynamicCenter = center + colorShakeDisplacement * sin(colorShakeSpeed * time);\n"
                                + "    float2 rotatedUV = rotate2d(uv - dynamicCenter, colorSpeed * time) + dynamicCenter;\n"
                                + "    rotatedUV = (rotatedUV - float2(0.5)) * colorScale + float2(0.5);\n"
                                + "    half4 tex = u_HaloColorNoiseTex.eval(rotatedUV * 1200.0);\n"
                                + "    float textureValue = 0.0;\n"
                                + "    if (channel == 0) {\n"
                                + "        textureValue = tex.r;\n"
                                + "    } else if (channel == 1) {\n"
                                + "        textureValue = tex.g;\n"
                                + "    } else if (channel == 2) {\n"
                                + "        textureValue = tex.b;\n"
                                + "    }\n"
                                + "    return half4(color, textureValue);\n"
                                + "}\n"
                                + "half3 generateRandomColors(float2 muv, float time, float2 colorShakeDisplacement,\n"
                                + "                        float4 colorShakeSpeed, float4 colorSpeed,\n"
                                + "                        half3 color1, half3 color2, half3 color3, half3 color4,\n"
                                + "                        float colorScale) {\n"
                                + "    float2 center = float2(0.5);\n"
                                + "    half4 layer1 = generateColorLayer(muv, center, colorShakeDisplacement, colorShakeSpeed.r, colorSpeed.r, time, 0, color1, colorScale);\n"
                                + "    half4 layer2 = generateColorLayer(muv, center, colorShakeDisplacement, colorShakeSpeed.g, colorSpeed.g, time, 1, color2, colorScale);\n"
                                + "    half4 layer3 = generateColorLayer(muv, center, colorShakeDisplacement, colorShakeSpeed.b, colorSpeed.b, time, 2, color3, colorScale);\n"
                                + "    half4 layer4 = generateColorLayer(muv, center, colorShakeDisplacement, colorShakeSpeed.a, colorSpeed.a, time, 0, color4, colorScale);\n"
                                + "    half3 col = mix(layer1.rgb, layer2.rgb, layer2.a);\n"
                                + "    col = mix(col, layer3.rgb, layer3.a);\n"
                                + "    col = mix(col, layer4.rgb, layer4.a);\n"
                                + "    return col;\n"
                                + "}\n"
                                + "float getHaloMask(float2 fragCoord, float2 resolution, float2 center, float haloStart, float haloSpace) {\n"
                                + "    float x_gl = fragCoord.x / resolution.x;\n"
                                + "    float y_gl = (resolution.y - fragCoord.y) / resolution.x;\n"
                                + "    float dis = length(float2(x_gl, y_gl) - center);\n"
                                + "    return smoothstep(haloStart + haloSpace, haloStart, dis);\n"
                                + "}\n"
                                + "half4 main(float2 fragCoord) {\n"
                                + "    float haloMask = getHaloMask(fragCoord, u_resolution, _HaloCenter, _HaloStart, _HaloSpace);\n"
                                + "    float2 muv = float2(fragCoord.x, u_resolution.y - fragCoord.y) / _HaloColorPicSize + float2(0.5);\n"
                                + "    half3 haloColor = generateRandomColors(muv, u_time, _HaloColorShakeDisp, _HaloColorShakeSpeed, _HaloColorSpeed,\n"
                                + "        _HaloColor1.rgb, _HaloColor2.rgb, _HaloColor3.rgb, _HaloColor4.rgb, _HaloColorScale);\n"
                                + "    half4 finalColor = half4(haloColor, haloMask * _HaloAlpha);\n"
                                + "    finalColor.rgb *= finalColor.a;\n"
                                + "    return finalColor;\n"
                                + "}\n";

                        mShader = new RuntimeShader(agsl);
                        BitmapShader bmpShader = new BitmapShader(mNoiseBmp, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);
                        mShader.setInputShader("u_HaloColorNoiseTex", bmpShader);
                        mShader.setFloatUniform("_HaloColor1", 0.76078f, 0.3647f, 1.0f, 1.0f);
                        mShader.setFloatUniform("_HaloColor2", 0.16078f, 0.8980f, 0.2863f, 1.0f);
                        mShader.setFloatUniform("_HaloColor3", 0.06275f, 0.7490f, 0.9686f, 1.0f);
                        mShader.setFloatUniform("_HaloColor4", 1.0f, 0.7608f, 0.0980f, 1.0f);
                        mShader.setFloatUniform("_HaloColorShakeDisp", 0.8f, 0.8f);
                        mShader.setFloatUniform("_HaloColorShakeSpeed", 1.5f, 1.3f, 1.8f, -1.0f);
                        mShader.setFloatUniform("_HaloColorSpeed", 0.7f, 0.9f, 0.6f, 0.1f);
                        mShader.setFloatUniform("_HaloColorScale", 0.37f);
                        mShader.setFloatUniform("_HaloColorPicSize", 1000.0f);
                        mShader.setFloatUniform("_HaloCenter", 0.5f, -4.134f);
                        mShader.setFloatUniform("_HaloStart", 4.0f);
                        mShader.setFloatUniform("_HaloSpace", 0.18f);
                        mShader.setFloatUniform("_HaloAlpha", 0.0f);
                        mPaint.setShader(mShader);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "RuntimeShader init failed", t);
                    mShader = null;
                }
            }
        }

        public void setTouchPosition(float rawX, float rawY) {
            if (mIsRight && rawY > 0f) {
                mTargetTouchY = rawY;
                if (mCurrentTouchY < 0f) {
                    mCurrentTouchY = rawY;
                }
                postInvalidateOnAnimation();
            }
        }

        public void setPhase1Progress(float progress) {
            setPhase1Progress(progress, false, -1f);
        }

        public void setPhase1Progress(float progress, boolean isRight) {
            setPhase1Progress(progress, isRight, -1f);
        }

        public void setPhase1Progress(float progress, boolean isRight, float touchY) {
            mIsRight = isRight;
            float p = Math.max(0f, Math.min(1.0f, progress));
            if (isRight) {
                mOpenAnim = false;
                int h = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
                if (touchY > 0f) {
                    mTargetTouchY = touchY;
                    if (mCurrentTouchY < 0f) {
                        mCurrentTouchY = touchY;
                    }
                } else if (mCurrentTouchY < 0f) {
                    mTargetTouchY = h * 0.5f;
                    mCurrentTouchY = h * 0.5f;
                }
                mTargetScaleX = 0.35f + 0.65f * p;
                mTargetAlpha = 0.30f + 0.65f * p;
            } else {
                // OPPO process1_StandardPhone: a constant thin rim at the very
                // bottom edge. progress is deliberately ignored (ColorOS does not
                // scale the halo with drag distance).
                mOpenAnim = false;
                mTargetCenterX = 0.5f;
                mTargetCenterY = RIM_CENTER_Y;
                mTargetStart = RIM_START;
                mTargetSpace = RIM_SPACE;
                mTargetAlpha = 1.0f;
            }
            postInvalidateOnAnimation();
        }

        public void setPhase2() {
            setPhase2(false, -1f);
        }

        public void setPhase2(boolean isRight) {
            setPhase2(isRight, -1f);
        }

        public void setPhase2(boolean isRight, float touchY) {
            mIsRight = isRight;
            if (isRight) {
                mOpenAnim = false;
                if (touchY > 0f) {
                    mTargetTouchY = touchY;
                    if (mCurrentTouchY < 0f) {
                        mCurrentTouchY = touchY;
                    }
                }
                mTargetScaleX = 1.15f;
                mTargetAlpha = 1.0f;
            } else {
                // OPPO process2_StandardPhone: bloom upward and fade to zero over
                // 380 ms while the shelf opens. Do not hold the expanded geometry
                // at a visible alpha afterwards.
                mTargetCenterX = 0.5f;
                mTargetCenterY = OPEN_CENTER_Y;
                mTargetStart = OPEN_START;
                mTargetSpace = OPEN_SPACE;
                mTargetAlpha = 0f;
                if (!mOpenAnim) {
                    mOpenAnim = true;
                    mOpenAnimStart = SystemClock.uptimeMillis();
                    mOpenFromCenterY = mCurrentCenterY;
                    mOpenFromStart = mCurrentStart;
                    mOpenFromSpace = mCurrentSpace;
                    mOpenFromAlpha = mCurrentAlpha;
                }
            }
            postInvalidateOnAnimation();
        }

        public void resetState() {
            mOpenAnim = false;
            mTargetCenterX = 0.5f;
            mCurrentCenterX = 0.5f;
            mTargetCenterY = -4.134f;
            mCurrentCenterY = -4.134f;
            mTargetStart = RIM_START;
            mCurrentStart = RIM_START;
            mTargetSpace = RIM_SPACE;
            mCurrentSpace = RIM_SPACE;
            mTargetAlpha = 0f;
            mCurrentAlpha = 0f;
            mTargetScaleX = 0f;
            mCurrentScaleX = 0f;
            mTargetTouchY = -1f;
            mCurrentTouchY = -1f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            if (mIsRight) {
                if (mCurrentTouchY < 0f) {
                    mCurrentTouchY = mTargetTouchY > 0f ? mTargetTouchY : h * 0.5f;
                }
                if (mTargetTouchY < 0f) {
                    mTargetTouchY = mCurrentTouchY;
                }

                mCurrentTouchY += (mTargetTouchY - mCurrentTouchY) * 0.48f;
                mCurrentScaleX += (mTargetScaleX - mCurrentScaleX) * 0.35f;
                mCurrentAlpha += (mTargetAlpha - mCurrentAlpha) * 0.35f;

                if (mCurrentAlpha <= 0.01f) return;

                // Subtle breathing pulsation from Honor AnyDoor (period 1600ms)
                float breath = 0.88f + 0.12f * (float) Math.sin((SystemClock.uptimeMillis() % 1600) / 1600.0f * 2.0 * Math.PI);
                int alpha = Math.max(0, Math.min(255, (int) (255 * mCurrentAlpha * breath)));

                if (mHonorGlowBmp != null && !mHonorGlowBmp.isRecycled()) {
                    float glowWidth = dp(115) * mCurrentScaleX;
                    float glowHeight = Math.max(h, dp(800)) * 1.35f;
                    float top = mCurrentTouchY - (glowHeight * 0.5f);
                    float bottom = mCurrentTouchY + (glowHeight * 0.5f);
                    float right = w;
                    float left = w - glowWidth;

                    mHonorPaint.setAlpha(alpha);
                    Rect srcRect = new Rect(0, 0, mHonorGlowBmp.getWidth(), mHonorGlowBmp.getHeight());
                    RectF dstRect = new RectF(left, top, right, bottom);
                    canvas.drawBitmap(mHonorGlowBmp, srcRect, dstRect, mHonorPaint);
                } else {
                    // High-fidelity procedural Honor AnyDoor color ramp fallback
                    float cx = w;
                    float cy = mCurrentTouchY;
                    float rx = dp(115) * mCurrentScaleX;
                    float ry = Math.max(h, dp(800)) * 0.55f;
                    canvas.save();
                    canvas.translate(cx, cy);
                    canvas.scale(rx / dp(200), ry / dp(200));
                    int[] colors = new int[]{
                            Color.argb(alpha, 245, 252, 255),
                            Color.argb((int) (alpha * 0.85f), 136, 212, 251),
                            Color.argb((int) (alpha * 0.50f), 37, 142, 245),
                            Color.argb((int) (alpha * 0.20f), 26, 51, 255),
                            Color.argb(0, 0, 0, 0)
                    };
                    float[] stops = new float[]{0.0f, 0.25f, 0.55f, 0.80f, 1.0f};
                    RadialGradient rg = new RadialGradient(0, 0, dp(200), colors, stops, Shader.TileMode.CLAMP);
                    mPaint.setShader(rg);
                    canvas.drawCircle(0, 0, dp(200), mPaint);
                    canvas.restore();
                }

                if (mCurrentAlpha > 0.02f || Math.abs(mTargetTouchY - mCurrentTouchY) > 0.5f || Math.abs(mTargetScaleX - mCurrentScaleX) > 0.01f) {
                    postInvalidateOnAnimation();
                }
                return;
            }

            // Bottom Mode (ColorOS Aurora AGSL / RadialGradient)
            if (mOpenAnim) {
                long elapsed = SystemClock.uptimeMillis() - mOpenAnimStart;
                float moveT = mOpenMoveInterp.getInterpolation(
                        Math.min(1f, elapsed / (float) OPEN_MOVE_MS));
                mCurrentCenterX = 0.5f;
                mCurrentCenterY = mOpenFromCenterY + (OPEN_CENTER_Y - mOpenFromCenterY) * moveT;
                mCurrentStart = mOpenFromStart + (OPEN_START - mOpenFromStart) * moveT;
                mCurrentSpace = mOpenFromSpace + (OPEN_SPACE - mOpenFromSpace) * moveT;
                float fadeT = (elapsed - OPEN_ALPHA_DELAY_MS) / (float) OPEN_ALPHA_MS;
                fadeT = Math.max(0f, Math.min(1f, fadeT));
                mCurrentAlpha = mOpenFromAlpha * (1f - mOpenFadeInterp.getInterpolation(fadeT));
                if (elapsed >= OPEN_TOTAL_MS) {
                    mOpenAnim = false;
                    mCurrentCenterY = OPEN_CENTER_Y;
                    mCurrentStart = OPEN_START;
                    mCurrentSpace = OPEN_SPACE;
                    mCurrentAlpha = 0f;
                }
            } else {
                mCurrentCenterX += (mTargetCenterX - mCurrentCenterX) * 0.18f;
                mCurrentCenterY += (mTargetCenterY - mCurrentCenterY) * 0.18f;
                mCurrentStart += (mTargetStart - mCurrentStart) * 0.18f;
                mCurrentSpace += (mTargetSpace - mCurrentSpace) * 0.18f;
                mCurrentAlpha += (mTargetAlpha - mCurrentAlpha) * 0.18f;
            }

            if (mShader != null) {
                float time = (SystemClock.uptimeMillis() % 60000) / 1000.0f;
                mShader.setFloatUniform("u_time", time);
                mShader.setFloatUniform("u_resolution", (float) w, (float) h);
                mShader.setFloatUniform("_HaloCenter", mCurrentCenterX, mCurrentCenterY);
                mShader.setFloatUniform("_HaloStart", mCurrentStart);
                mShader.setFloatUniform("_HaloSpace", mCurrentSpace);
                mShader.setFloatUniform("_HaloAlpha", mCurrentAlpha);
                canvas.drawRect(0, 0, w, h, mPaint);
                postInvalidateOnAnimation();
            } else {
                float cx = w * 0.5f;
                float cy = h * 0.92f;
                float radius = Math.max(w, h) * 0.85f;
                int alpha = Math.max(0, Math.min(255, (int) (mCurrentAlpha * 220)));
                int[] colors = new int[]{
                        Color.argb(alpha, 120, 220, 255),
                        Color.argb((int) (alpha * 0.65f), 170, 110, 255),
                        Color.argb((int) (alpha * 0.35f), 70, 230, 180),
                        Color.argb(0, 0, 0, 0)
                };
                float[] stops = new float[]{0.0f, 0.35f, 0.65f, 1.0f};
                mPaint.setShader(new RadialGradient(cx, cy, radius, colors, stops, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, mPaint);
            }
        }
    }
}
