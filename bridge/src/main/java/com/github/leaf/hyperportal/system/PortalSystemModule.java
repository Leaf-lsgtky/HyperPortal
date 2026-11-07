package com.github.leaf.hyperportal.system;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.Surface;
import java.io.File;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceControl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * libxposed API 102 entry class.
 *
 * The same APK is both the bridge app and the LSPosed module, so there is only
 * one package to install. This class is only loaded by LSPosed inside
 * system_server (scope "system"); the normal app process never touches it.
 *
 * ColorOS Content Portal System Transformation:
 * - Upper screen drag: Only glow effect at bottom, Task tilt progress remains 0.
 * - Bottom threshold reached: Fullscreen standard task smoothly tilts in 3D
 *   (top wide, bottom narrow) and bottom application bar expands.
 * - Freeform / small windows: Strictly excluded from tilt focus to prevent screen distortion!
 * - Leaves bottom threshold: Smoothly un-tilts back to 0.
 * - Drag ended: Completely restored to normal 2D rendering.
 */
public final class PortalSystemModule {

    private static final String TAG = "DragShareSystem";
    private static final String DRAG_MIME = "application/x-dragshare";
    private static final String DROP_SHARE_PROP = "persist.sys.hyperportal.drop_to_share_enable";
    /**
     * Windows whose package must never receive the drop-to-share fallback:
     * system surfaces, launchers, and our own overlay (its panel window has no
     * ActivityRecord anyway, but the package check keeps service windows out).
     */
    private static final java.util.Set<String> DROP_SHARE_DENY_PACKAGES = new java.util.HashSet<>(
            java.util.Arrays.asList(
                    "android",
                    "com.android.systemui",
                    "com.android.settings",
                    "com.miui.home",
                    "com.miui.securitycenter",
                    "com.miui.permcenter",
                    "com.lbe.security.miui",
                    "com.github.leaf.hyperportal",
                    "com.dragshare.portal"));
    private static final ExecutorService sNotifyExecutor = Executors.newSingleThreadExecutor();
    /**
     * Full settings snapshot, pushed by system_server. Consumers must not treat
     * the per-event {@code position} extra of ACTION_DRAG_STATE as authoritative
     * (three processes send that broadcast, and two of them may be stale), but a
     * snapshot that only system_server produces is safe to cache.
     */
    private static final String ACTION_CONFIG_SNAPSHOT =
            "com.github.leaf.hyperportal.ACTION_CONFIG_SNAPSHOT";

    private static volatile XposedInterface sXposed;

    private static void log(int priority, String tag, String msg) {
        if (sXposed != null) {
            try {
                sXposed.log(priority, tag, msg);
                return;
            } catch (Throwable ignored) {
            }
        }
        Log.println(priority, tag, msg);
    }

    private static void log(int priority, String tag, String msg, Throwable tr) {
        if (sXposed != null) {
            try {
                sXposed.log(priority, tag, msg + "\n" + Log.getStackTraceString(tr));
                return;
            } catch (Throwable ignored) {
            }
        }
        Log.println(priority, tag, msg);
    }

    public static void initSystemServer(XposedInterface xposed, io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam param) {
        sXposed = xposed;
        sSystemClassLoader = resolveSystemClassLoader();
        log(Log.INFO, TAG, "onSystemServerStarting cl=" + sSystemClassLoader);
        installHooks(sSystemClassLoader);
        preloadWallpaperBitmap();
    }

    private static volatile boolean sDragActive;
    private static volatile Object sTask;
    private static volatile SurfaceControl sTaskSurface;
    private static volatile Bitmap sWallpaperBitmap;
    private static volatile SurfaceControl sWallpaperSurface;
    private static volatile float sProgress = 0f;
    private static volatile float sTargetProgress = 0f;
    private static volatile float sLastLogged = -1f;
    private static volatile float sLastNotifiedX = -1f;
    private static volatile float sLastNotifiedY = -1f;
    private static volatile int sCurrentState = 0; // 0 = closed, 1 = glow only, 2 = tilt + appbar
    private static volatile float sDragStartX = -1f;
    private static volatile float sDragStartY = -1f;
    private static volatile String sTriggerPosition = "bottom";
    private static volatile boolean sTickerRunning;
    private static volatile ClassLoader sSystemClassLoader;
    private static volatile Context sSystemContext;
    private static volatile float sTriggerThreshold = 0.76f;
    private static volatile boolean sReceiverRegistered = false;
    /**
     * ColorOS-inspired drop guard: a HyperPortal drag released over the very
     * window it started from is ended as "not consumed" instead of being
     * delivered to that window, so an app like WeChat cannot treat the
     * accidental shadow as a message to send. Other windows of the same app
     * (split screen, floating window, another Activity) stay valid targets.
     */
    private static volatile boolean sSameWindowGuard = true;
    private static volatile boolean sColorosConsumeAnim = true;
    private static volatile boolean sLoggedGateOverride = false;
    private static volatile Object sWms;
    private static volatile String sDragSourcePackage;
    /**
     * False when the drag was started by a SystemUI-hosted surface (the transfer
     * dock panel, the shelf) instead of by an app window. Such a drag has no app
     * task of its own, so tilting "the topmost fullscreen task" would distort an
     * unrelated app that merely happens to be behind the panel.
     */
    private static volatile boolean sTiltAllowed = true;

    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static final Runnable sTicker = new Runnable() {
        @Override
        public void run() {
            float diff = sTargetProgress - sProgress;
            if (Math.abs(diff) < 0.005f) {
                sProgress = sTargetProgress;
                applyTaskProgress(sProgress);
                sTickerRunning = false;
                return;
            }
            sProgress += diff * 0.22f;
            applyTaskProgress(sProgress);
            sHandler.postDelayed(this, 16);
        }
    };

    private static void startTicker() {
        if (!sTickerRunning) {
            sTickerRunning = true;
            sHandler.post(sTicker);
        }
    }

    private static ClassLoader resolveSystemClassLoader() {
        ArrayList<ClassLoader> candidates = new ArrayList<>();
        candidates.add(null);
        candidates.add(ClassLoader.getSystemClassLoader());
        if (ClassLoader.getSystemClassLoader() != null) {
            candidates.add(ClassLoader.getSystemClassLoader().getParent());
        }
        candidates.add(Thread.currentThread().getContextClassLoader());
        candidates.add(PortalSystemModule.class.getClassLoader());
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            candidates.add(activityThreadClass.getClassLoader());
            Object thread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Object systemContext = activityThreadClass.getMethod("getSystemContext").invoke(thread);
            if (systemContext instanceof Context) {
                sSystemContext = (Context) systemContext;
                candidates.add(sSystemContext.getClassLoader());
            }
        } catch (Throwable throwable) {
            Log.i(TAG, "system context classloader unavailable: " + throwable);
        }
        for (ClassLoader candidate : candidates) {
            try {
                Class<?> type = Class.forName("com.android.server.wm.DragDropController", false, candidate);
                Log.i(TAG, "DragDropController found via " + candidate + " -> " + type);
                return candidate;
            } catch (Throwable throwable) {
                Log.i(TAG, "classloader miss " + candidate + ": " + throwable);
            }
        }
        return null;
    }

    private static void installHooks(ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                classLoader = resolveSystemClassLoader();
            }
            Class<?> controller = Class.forName("com.android.server.wm.DragDropController", false, classLoader);
            log(Log.INFO, TAG, "DragDropController=" + controller.getName());
            for (Method method : controller.getDeclaredMethods()) {
                String name = method.getName();
                if ("performDrag".equals(name)) {
                    sXposed.hook(method).setId("ddc-performDrag").intercept(PortalSystemModule::onPerformDrag);
                    log(Log.INFO, TAG, "hooked performDrag " + method);
                } else if ("handleMotionEvent".equals(name)) {
                    sXposed.hook(method).setId("ddc-handleMotionEvent").intercept(PortalSystemModule::onHandleMotion);
                    log(Log.INFO, TAG, "hooked handleMotionEvent " + method);
                } else if ("onDragStateClosedLocked".equals(name)) {
                    sXposed.hook(method).setId("ddc-onDragStateClosed").intercept(PortalSystemModule::onDragClosed);
                    log(Log.INFO, TAG, "hooked onDragStateClosedLocked " + method);
                } else if ("reportDropResult".equals(name) && method.getParameterCount() == 2) {
                    sXposed.hook(method).setId("ddc-reportDropResult").intercept(PortalSystemModule::onReportDropResult);
                    log(Log.INFO, TAG, "hooked reportDropResult " + method);
                }
            }
            Class<?> dragState = Class.forName("com.android.server.wm.DragState", false, classLoader);
            for (Method method : dragState.getDeclaredMethods()) {
                if ("updateDragSurfaceLocked".equals(method.getName())) {
                    sXposed.hook(method).setId("drag-updateSurface").intercept(PortalSystemModule::onHandleMotion);
                    log(Log.INFO, TAG, "hooked updateDragSurfaceLocked " + method);
                } else if ("reportDropWindowLock".equals(method.getName())
                        && method.getParameterCount() == 5) {
                    // Release-time gate: runs before ACTION_DROP is dispatched,
                    // so returning false here means the target window never sees
                    // the drop (and cannot consume it as "send").
                    sXposed.hook(method).setId("drag-reportDropWindow").intercept(PortalSystemModule::onReportDropWindowLock);
                    log(Log.INFO, TAG, "hooked reportDropWindowLock " + method);
                } else if ("endDragLocked".equals(method.getName())
                        && method.getParameterCount() == 2) {
                    sXposed.hook(method).setId("drag-endDragLocked").intercept(PortalSystemModule::onEndDragLocked);
                    log(Log.INFO, TAG, "hooked endDragLocked " + method);
                }
            }

            // SELinux context fixes for re-signed UID 1000 packages (ContentCatcher / AES)
            installSELinuxHooks(classLoader);
            installAppsFilterHooks(classLoader);
            installContentExtensionGateHook(classLoader);
            scheduleInitialConfigLoad();
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "installHooks failed", throwable);
        }
    }

    /**
     * The portal settings live in the HyperPortal app, i.e. in an ordinary
     * background process. HyperOS' freezer (Greeze) can freeze that uid at any
     * moment - the process is still alive but its ContentProvider stops being
     * resolvable for third-party callers, so Bridge.getConfig() fails and every
     * consumer silently falls back to the default "bottom" trigger edge. That is
     * exactly what turned a right-edge setup into a bottom shelf.
     *
     * <p>system_server is the only place that can always read the provider (a
     * uid 1000 caller is allowed to thaw the frozen app), so it keeps the
     * authoritative snapshot here and pushes it out instead of letting the
     * renderers and the source-app bridge pull from a process that may be
     * frozen. Load it once at startup, then keep it fresh through
     * ACTION_CONFIG_CHANGED and the per-drag query.
     */
    private static void scheduleInitialConfigLoad() {
        Thread thread = new Thread(() -> {
            for (int attempt = 0; attempt < 120; attempt++) {
                try {
                    Context ctx = obtainSystemContext();
                    if (ctx != null) {
                        sSystemContext = ctx;
                        ensureConfigReceiver(ctx);
                        queryConfigAsync(ctx);
                        log(Log.INFO, TAG, "config channel ready after " + attempt + " attempt(s)");
                        return;
                    }
                } catch (Throwable ignored) {
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interrupted) {
                    return;
                }
            }
            log(Log.WARN, TAG, "config channel unavailable: no system context");
        }, "HyperPortalConfig");
        thread.setDaemon(true);
        thread.start();
    }

    private static Context obtainSystemContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            if (thread != null) {
                Object ctx = activityThread.getMethod("getSystemContext").invoke(thread);
                if (ctx instanceof Context) {
                    return (Context) ctx;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (sWms != null) {
                Object ctx = getField(sWms, "mContext");
                if (ctx instanceof Context) {
                    return (Context) ctx;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void installSELinuxHooks(ClassLoader classLoader) {
        try {
            Class<?> zygoteProcessClass = Class.forName("android.os.ZygoteProcess", false, classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());
            for (Method method : zygoteProcessClass.getDeclaredMethods()) {
                String name = method.getName();
                if ("zygoteSendArgsAndGetResult".equals(name)) {
                    sXposed.hook(method)
                            .setId("zygote-sendArgs")
                            .intercept(PortalSystemModule::onZygoteSendArgs);
                    log(Log.INFO, TAG, "hooked ZygoteProcess.zygoteSendArgsAndGetResult: " + method);
                } else if ("attemptZygoteSendArgsAndGetResult".equals(name)) {
                    sXposed.hook(method)
                            .setId("zygote-attemptSendArgs")
                            .intercept(PortalSystemModule::onAttemptZygoteSendArgs);
                    log(Log.INFO, TAG, "hooked ZygoteProcess.attemptZygoteSendArgsAndGetResult: " + method);
                } else if ("startViaZygote".equals(name)) {
                    sXposed.hook(method)
                            .setId("zygote-startViaZygote")
                            .intercept(PortalSystemModule::onStartViaZygote);
                    log(Log.INFO, TAG, "hooked ZygoteProcess.startViaZygote: " + method);
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "hook ZygoteProcess failed", t);
        }

        try {
            Class<?> mmacClass = Class.forName("com.android.server.pm.SELinuxMMAC", false, classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());
            for (Method method : mmacClass.getDeclaredMethods()) {
                if ("getSeInfo".equals(method.getName())) {
                    sXposed.hook(method)
                            .setId("mmac-getSeInfo")
                            .intercept(PortalSystemModule::onGetSeInfo);
                    log(Log.INFO, TAG, "hooked SELinuxMMAC.getSeInfo: " + method);
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "hook SELinuxMMAC failed", t);
        }
    }

    private static Object onAttemptZygoteSendArgs(XposedInterface.Chain chain) throws Throwable {
        try {
            List<Object> args = chain.getArgs();
            if (args != null && args.size() > 1 && args.get(1) instanceof String) {
                String msgStr = (String) args.get(1);
                if (msgStr.contains("com.miui.contentcatcher") || (msgStr.contains("--setuid=1000\n") && msgStr.contains("--seinfo=default"))) {
                    String fixed = msgStr.replaceAll("--seinfo=[^\r\n]+", "--seinfo=platform");
                    if (!fixed.equals(msgStr)) {
                        log(Log.INFO, TAG, "onAttemptZygoteSendArgs: replaced seinfo with --seinfo=platform");
                        Object[] newArgs = args.toArray();
                        newArgs[1] = fixed;
                        return chain.proceed(newArgs);
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "onAttemptZygoteSendArgs inspect error", t);
        }
        return chain.proceed();
    }

    private static Object onZygoteSendArgs(XposedInterface.Chain chain) throws Throwable {
        try {
            List<Object> args = chain.getArgs();
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> list = (List<String>) arg;
                        boolean isTarget = false;
                        for (String s : list) {
                            if (s != null && (s.contains("com.miui.contentcatcher") || s.contains("--setuid=1000"))) {
                                isTarget = true;
                                break;
                            }
                        } 
                        if (isTarget) {
                            for (int i = 0; i < list.size(); i++) {
                                String s = list.get(i);
                                if (s != null && s.startsWith("--seinfo=") && !s.equals("--seinfo=platform")) {
                                    log(Log.INFO, TAG, "onZygoteSendArgs: replacing " + s + " with --seinfo=platform");
                                    list.set(i, "--seinfo=platform");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "onZygoteSendArgs inspect error", t);
        }
        return chain.proceed();
    }

    private static Object onStartViaZygote(XposedInterface.Chain chain) throws Throwable {
        try {
            List<Object> args = chain.getArgs();
            if (args != null) {
                boolean isTarget = false;
                for (Object arg : args) {
                    if (arg != null && (arg.toString().contains("com.miui.contentcatcher") || Integer.valueOf(1000).equals(arg))) {
                        isTarget = true;
                        break;
                    }
                }
                if (isTarget) {
                    Object[] newArgs = args.toArray();
                    boolean modified = false;
                    for (int i = 0; i < newArgs.length; i++) {
                        Object a = newArgs[i];
                        if (a instanceof String) {
                            String s = (String) a;
                            if (s.startsWith("default:") || s.equals("default")) {
                                log(Log.INFO, TAG, "onStartViaZygote: replacing arg[" + i + "] " + s + " -> platform");
                                newArgs[i] = "platform";
                                modified = true;
                            }
                        }
                    }
                    if (modified) {
                        return chain.proceed(newArgs);
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "onStartViaZygote inspect error", t);
        }
        return chain.proceed();
    }

    private static Object onGetSeInfo(XposedInterface.Chain chain) throws Throwable {
        Object orig = chain.proceed();
        try {
            List<Object> args = chain.getArgs();
            if (args != null) {
                for (Object arg : args) {
                    if (arg == null) continue;
                    String s = arg.toString();
                    if (s != null && s.contains("com.miui.contentcatcher")) {
                        log(Log.INFO, TAG, "SELinuxMMAC.getSeInfo: overriding " + orig + " -> platform for " + s);
                        return "platform";
                    }
                    try {
                        Method getPkg = arg.getClass().getMethod("getPackageName");
                        Object pkg = getPkg.invoke(arg);
                        if ("com.miui.contentcatcher".equals(pkg)) {
                            log(Log.INFO, TAG, "SELinuxMMAC.getSeInfo: overriding " + orig + " -> platform for " + pkg);
                            return "platform";
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "onGetSeInfo inspect error", t);
        }
        return orig;
    }

    private static void installAppsFilterHooks(ClassLoader classLoader) {
        try {
            Class<?> appsFilterBase = null;
            try {
                appsFilterBase = Class.forName("com.android.server.pm.AppsFilterBase", false, classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());
            } catch (Throwable ignored) {
            }
            if (appsFilterBase == null) {
                try {
                    appsFilterBase = Class.forName("com.android.server.pm.AppsFilterImpl", false, classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());
                } catch (Throwable ignored) {
                }
            }
            if (appsFilterBase != null) {
                for (Method method : appsFilterBase.getDeclaredMethods()) {
                    if ("shouldFilterApplication".equals(method.getName())) {
                        sXposed.hook(method)
                                .setId("appsfilter-shouldFilterApplication-" + method.getParameterCount())
                                .intercept(PortalSystemModule::onShouldFilterApplication);
                        log(Log.INFO, TAG, "hooked AppsFilter.shouldFilterApplication: " + method);
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "installAppsFilterHooks failed", t);
        }
    }

    /**
     * HyperOS gates the whole AES touch pipeline in the framework:
     *
     *   InterceptorProxy.dispatchTouchEvent()
     *     if (!mContentExtensionEnabled) return false;
     *   mContentExtensionEnabled = ContentCatcherManager.getContentExtensionEnabled()
     *                            = SecurityManagerService.getContentExtensionEnabled()
     *                            = Settings.System.open_content_extension_clipboard_mode == 1
     *
     * 传送门's master switch writes that setting to 0, so our Bridge.ping() inside
     * Interceptor.dispatchTouchEvent() is never called even though the long-press
     * fallback itself is self-contained. Keep the framework gate open for normal
     * app processes without touching 传送门's user-facing switch or settings.
     */
    private static void installContentExtensionGateHook(ClassLoader classLoader) {
        try {
            Class<?> securityManager = null;
            ClassLoader[] candidates = new ClassLoader[]{
                    classLoader,
                    ClassLoader.getSystemClassLoader(),
                    Thread.currentThread().getContextClassLoader()
            };
            for (ClassLoader candidate : candidates) {
                if (candidate == null) continue;
                try {
                    securityManager = Class.forName(
                            "com.miui.server.SecurityManagerService", false, candidate);
                    break;
                } catch (Throwable ignored) {
                }
            }
            if (securityManager == null) {
                log(Log.WARN, TAG, "SecurityManagerService class not found");
                return;
            }
            for (Method method : securityManager.getDeclaredMethods()) {
                if ("getContentExtensionEnabled".equals(method.getName())
                        && method.getParameterCount() == 0) {
                    sXposed.hook(method)
                            .setId("security-getContentExtensionEnabled")
                            .intercept(PortalSystemModule::onGetContentExtensionEnabled);
                    log(Log.INFO, TAG, "hooked SecurityManagerService.getContentExtensionEnabled");
                    return;
                }
            }
            log(Log.WARN, TAG, "SecurityManagerService.getContentExtensionEnabled not found");
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "installContentExtensionGateHook failed", throwable);
        }
    }

    private static Object onGetContentExtensionEnabled(XposedInterface.Chain chain) throws Throwable {
        int callingUid = android.os.Binder.getCallingUid();
        // Leave system_server / system-UID callers (Settings, 传送门 itself) on the
        // original path so their UI state still reflects the real user switch.
        if (callingUid == android.os.Process.SYSTEM_UID || callingUid == 0) {
            return chain.proceed();
        }
        if (!sLoggedGateOverride) {
            sLoggedGateOverride = true;
            log(Log.INFO, TAG, "AES content extension gate forced on for app uid=" + callingUid);
        }
        return Boolean.TRUE;
    }

    private static Object onShouldFilterApplication(XposedInterface.Chain chain) throws Throwable {
        try {
            List<Object> args = chain.getArgs();
            if (args != null) {
                for (Object arg : args) {
                    if (arg != null) {
                        String clsName = arg.getClass().getName();
                        if (clsName.contains("PackageState") || clsName.contains("PackageSetting")) {
                            try {
                                Method getPkg = arg.getClass().getMethod("getPackageName");
                                Object pkg = getPkg.invoke(arg);
                                if ("com.github.leaf.hyperportal".equals(pkg)) {
                                    return false;
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "onShouldFilterApplication error", t);
        }
        return chain.proceed();
    }

    private static Object onPerformDrag(XposedInterface.Chain chain) throws Throwable {
        try {
            ClipData clip = null;
            Object windowArg = null;
            for (Object arg : chain.getArgs()) {
                if (arg instanceof ClipData) {
                    clip = (ClipData) arg;
                } else if (arg != null && arg.getClass().getName().contains("IWindow")) {
                    windowArg = arg;
                }
            }
            Object controller = chain.getThisObject();
            Object wms = null;
            try {
                wms = getField(controller, "mService");
                sWms = wms;
            } catch (Throwable ignored) {
            }
            sDragSourcePackage = resolveDragSourcePackage(wms, windowArg);
            if (clip != null && clip.getDescription().hasMimeType(DRAG_MIME)) {
                sDragActive = true;
                sDragStartX = -1f;
                sDragStartY = -1f;
                sProgress = 0f;
                sTargetProgress = 0f;
                sCurrentState = 1; // Drag started: Phase 1 (Glow mode)
                sLastLogged = -1f;
                sLastNotifiedX = -1f;
                sLastNotifiedY = -1f;

                try {
                    Object ctx = getField(wms, "mContext");
                    if (ctx instanceof Context) {
                        sSystemContext = (Context) ctx;
                    }
                } catch (Throwable ignored) {
                }
                Object root = getField(wms, "mRoot");

                sTiltAllowed = !isOverlayDragSource(sDragSourcePackage);
                Object task = resolveTargetTask(wms, root, windowArg);
                sTask = task;
                if (task != null) {
                    sTaskSurface = (SurfaceControl) getField(task, "mSurfaceControl");
                    log(Log.INFO, TAG, "our drag started target task=" + task
                            + " surface=" + sTaskSurface + " tilt=" + sTiltAllowed
                            + " source=" + sDragSourcePackage);
                    if (sTiltAllowed) {
                        prepareWallpaperSurface(task);
                    }
                } else {
                    sTaskSurface = null;
                    log(Log.WARN, TAG, "our drag started but no valid fullscreen task found"
                            + " tilt=" + sTiltAllowed + " source=" + sDragSourcePackage);
                }

                notifyPortalState(1, 0f, 0f);
                if (sSystemContext != null) {
                    ensureConfigReceiver(sSystemContext);
                    queryConfigAsync(sSystemContext);
                }
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "onPerformDrag inspect failed", throwable);
        }
        return chain.proceed();
    }

    /**
     * Looks a WindowState up by the client IWindow binder.
     *
     * WindowManagerService.windowForClient(Session, IWindow) additionally requires
     * the exact Session instance, and callers here have no Session to pass, so it
     * always answers null. mWindowMap is keyed by the client binder, which we do
     * have.
     */
    private static Object findWindowState(Object wms, Object windowArg) {
        if (wms == null || windowArg == null) {
            return null;
        }
        try {
            Object mapObject = getField(wms, "mWindowMap");
            if (!(mapObject instanceof Map)) {
                return null;
            }
            Object binder = null;
            if (windowArg instanceof android.os.IInterface) {
                binder = ((android.os.IInterface) windowArg).asBinder();
            } else if (windowArg instanceof android.os.IBinder) {
                binder = windowArg;
            }
            if (binder == null) {
                return null;
            }
            Object direct = ((Map<?, ?>) mapObject).get(binder);
            if (direct != null) {
                return direct;
            }
            // A client Binder and the BinderProxy the service holds for it are not
            // interchangeable as HashMap keys on every release, so fall back to a
            // linear scan over the (small) window map.
            for (Object value : ((Map<?, ?>) mapObject).values()) {
                Object client = getField(value, "mClient");
                if (client == null) {
                    continue;
                }
                Object clientBinder = client instanceof android.os.IInterface
                        ? ((android.os.IInterface) client).asBinder() : client;
                if (clientBinder == null) {
                    continue;
                }
                if (clientBinder == binder || clientBinder.equals(binder) || binder.equals(clientBinder)) {
                    return value;
                }
            }
            return null;
        } catch (Throwable throwable) {
            log(Log.DEBUG, TAG, "findWindowState failed: " + throwable);
            return null;
        }
    }

    private static String resolveDragSourcePackage(Object wms, Object windowArg) {
        if (wms == null || windowArg == null) {
            return null;
        }
        try {
            Object callingWin = findWindowState(wms, windowArg);
            if (callingWin == null) {
                Method windowForClient = null;
                for (Method method : wms.getClass().getDeclaredMethods()) {
                    if ("windowForClient".equals(method.getName())
                            && method.getParameterTypes().length == 2) {
                        windowForClient = method;
                        break;
                    }
                }
                if (windowForClient == null) {
                    return null;
                }
                windowForClient.setAccessible(true);
                callingWin = windowForClient.invoke(wms, null, windowArg);
            }
            if (callingWin == null) {
                return null;
            }
            Object attrs = getField(callingWin, "mAttrs");
            Object pkg = getField(attrs, "packageName");
            return pkg == null ? null : String.valueOf(pkg);
        } catch (Throwable throwable) {
            log(Log.DEBUG, TAG, "resolveDragSourcePackage failed: " + throwable);
            return null;
        }
    }

    /**
     * SystemUI and this module host drag sources as overlay windows: the transfer
     * dock panel and the portal shelf. They are not app tasks, so the 3D tilt must
     * stay off for them.
     */
    private static boolean isOverlayDragSource(String pkg) {
        return "com.android.systemui".equals(pkg)
                || "com.github.leaf.hyperportal".equals(pkg);
    }

    /**
     * Resolves the fullscreen standard Task that should tilt.
     * CRITICAL BUGFIX: If a small window / freeform window is open or being dragged from,
     * the small window must NEVER be targeted! We strictly find and tilt the underlying
     * FULLSCREEN standard task so the screen never distorts.
     */
    private static Object resolveTargetTask(Object wms, Object root, Object windowArg) {
        // 1. Try finding calling window's task from IWindow
        if (windowArg != null && wms != null) {
            try {
                Object callingWin = findWindowState(wms, windowArg);
                if (callingWin == null) {
                    Method windowForClient = wms.getClass().getDeclaredMethod("windowForClient",
                            Class.forName("com.android.server.wm.Session"),
                            Class.forName("android.view.IWindow"));
                    windowForClient.setAccessible(true);
                    callingWin = windowForClient.invoke(wms, null, windowArg);
                }
                if (callingWin != null) {
                    Object task = invoke(callingWin, "getTask");
                    if (task != null) {
                        int mode = (Integer) invoke(task, "getWindowingMode");
                        int actType = (Integer) invoke(task, "getActivityType");
                        // Mode 1 = FULLSCREEN, ActType 1 = STANDARD
                        if (mode == 1 && actType == 1) {
                            if (true) {
                                log(Log.INFO, TAG, "resolved calling window task: " + task);
                            }
                            return task;
                        }
                    }
                }
            } catch (Throwable t) {
                if (true) {
                    log(Log.WARN, TAG, "resolve calling task failed: " + t);
                }
            }
        }

        // 2. Fallback: find top fullscreen standard root task from DefaultTaskDisplayArea
        if (root != null) {
            try {
                Object tda = invoke(root, "getDefaultTaskDisplayArea");
                if (tda != null) {
                    Method getTop = tda.getClass().getDeclaredMethod("getTopRootTaskInWindowingMode", int.class);
                    getTop.setAccessible(true);
                    Object fsTask = getTop.invoke(tda, 1); // 1 = WINDOWING_MODE_FULLSCREEN
                    if (fsTask != null) {
                        int actType = (Integer) invoke(fsTask, "getActivityType");
                        if (actType == 1) {
                            if (true) {
                                log(Log.INFO, TAG, "resolved top fullscreen root task: " + fsTask);
                            }
                            return fsTask;
                        }
                    }
                }
            } catch (Throwable t) {
                if (true) {
                    log(Log.WARN, TAG, "resolve top fullscreen task failed: " + t);
                }
            }
        }

        // 3. Fallback: check getTopResumedActivity, but strictly ignore freeform windows!
        if (root != null) {
            try {
                Object activity = invoke(root, "getTopResumedActivity");
                if (activity != null) {
                    Object task = getField(activity, "task");
                    if (task != null) {
                        int mode = (Integer) invoke(task, "getWindowingMode");
                        int actType = (Integer) invoke(task, "getActivityType");
                        if (mode == 1 && actType == 1) {
                            if (true) {
                                log(Log.INFO, TAG, "resolved resumed task: " + task);
                            }
                            return task;
                        }
                    }
                }
            } catch (Throwable t) {
                if (true) {
                    log(Log.WARN, TAG, "resolve resumed task failed: " + t);
                }
            }
        }

        return null;
    }

    private static Object onHandleMotion(XposedInterface.Chain chain) throws Throwable {
        if (sDragActive && sTaskSurface != null) {
            try {
                float x = 0f;
                float y = 0f;
                List<Object> args = chain.getArgs();
                if (args != null) {
                    if (args.size() >= 4 && args.get(2) instanceof Number && args.get(3) instanceof Number) {
                        x = ((Number) args.get(2)).floatValue();
                        y = ((Number) args.get(3)).floatValue();
                    } else if (args.size() == 2 && args.get(0) instanceof Number && args.get(1) instanceof Number) {
                        x = ((Number) args.get(0)).floatValue();
                        y = ((Number) args.get(1)).floatValue();
                    } else {
                        try {
                            y = ((Number) chain.getArg(3)).floatValue();
                        } catch (Throwable ignored) {
                        }
                    }
                }
                int width = android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels;
                int height = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;

                if (sDragStartY < 0f) {
                    sDragStartY = y;
                }
                if (sDragStartX < 0f) {
                    sDragStartX = x;
                }

                boolean isRight = "right".equals(sTriggerPosition);
                float enterFactor = sTriggerThreshold > 0f ? sTriggerThreshold : 0.76f;
                float exitFactor = Math.max(0.40f, enterFactor - 0.06f);

                boolean enterCondition;
                boolean exitCondition;

                if (isRight) {
                    float rightThreshold = width * enterFactor;
                    float exitThreshold = width * exitFactor;
                    if (sDragStartX >= rightThreshold) {
                        float rightwardRequired = dp(35);
                        enterCondition = (x >= sDragStartX + rightwardRequired) && (x >= rightThreshold);
                        exitCondition = (x < sDragStartX + dp(10)) || (x < exitThreshold);
                    } else {
                        enterCondition = (x >= rightThreshold);
                        exitCondition = (x < exitThreshold);
                    }
                } else {
                    float bottomThreshold = height * enterFactor;
                    float exitThreshold = height * exitFactor; // Hysteresis to prevent flutter
                    if (sDragStartY >= bottomThreshold) {
                        float downwardRequired = dp(35);
                        enterCondition = (y >= sDragStartY + downwardRequired) && (y >= bottomThreshold);
                        exitCondition = (y < sDragStartY + dp(10)) || (y < exitThreshold);
                    } else {
                        enterCondition = (y >= bottomThreshold);
                        exitCondition = (y < exitThreshold);
                    }
                }

                int newState;
                float target;
                if (sCurrentState == 2 ? !exitCondition : enterCondition) {
                    // Phase 2: Hot zone reached -> 3D tilt & expand app bar
                    target = 1.0f;
                    newState = 2;
                } else if (exitCondition) {
                    // Phase 1: Outside hot zone -> flat page, only glow
                    target = 0.0f;
                    newState = 1;
                } else {
                    target = sTargetProgress;
                    newState = sCurrentState;
                }

                if (Math.abs(target - sTargetProgress) > 0.01f) {
                    sTargetProgress = target;
                    startTicker();
                }

                if (newState != sCurrentState) {
                    sCurrentState = newState;
                    notifyPortalState(newState, x, y);
                    sLastNotifiedX = x;
                    sLastNotifiedY = y;
                } else if (newState == 1) {
                    float dist = isRight ? Math.abs(x - sLastNotifiedX) : Math.abs(y - sLastNotifiedY);
                    if (dist > 25.0f) {
                        sLastNotifiedX = x;
                        sLastNotifiedY = y;
                        notifyPortalState(1, x, y);
                    }
                }
            } catch (Throwable throwable) {
                if (true) {
                    log(Log.WARN, TAG, "onHandleMotion failed: " + throwable);
                }
            }
        }
        return chain.proceed();
    }

    private static Object onDragClosed(XposedInterface.Chain chain) throws Throwable {
        if (sDragActive) {
            sDragActive = false;
            sDragStartX = -1f;
            sDragStartY = -1f;
            sCurrentState = 0;
            sTargetProgress = 0f;
            startTicker();
            notifyPortalState(0, 0f, 0f);
            sHandler.postDelayed(() -> {
                if (!sDragActive) {
                    sProgress = 0f;
                    sTargetProgress = 0f;
                    applyTaskProgress(0f);
                    if (sWallpaperSurface != null) {
                        removeSurface(sWallpaperSurface);
                        sWallpaperSurface = null;
                    }
                    sTask = null;
                    sTaskSurface = null;
                    sTiltAllowed = true;
                }
            }, 300);
            log(Log.INFO, TAG, "our drag closed, restoring");
        }
        Object result = chain.proceed();
        try {
            log(Log.INFO, TAG, "drag closed cleanup check wms=" + (sWms != null)
                    + " source=" + sDragSourcePackage);
            dismissWeChatHandleWindows(sWms, sDragSourcePackage);
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "dismissWeChatHandleWindows failed: " + throwable);
        }
        sDragSourcePackage = null;
        return result;
    }

    /**
     * WeChat's NativeSelectableLayout puts its two green selection handles in
     * TYPE_APPLICATION_PANEL windows.  When a system drag consumes the touch
     * stream, WeChat never gets ACTION_UP and leaves both windows visible.
     * Remove only the two matching 112x92 dp panel windows after the drag
     * closes.  This runs in the existing system_server drag hook; WeChat is not
     * hooked or modified.
     */
    private static void dismissWeChatHandleWindows(Object wms, String sourcePackage) {
        if (wms == null || (sourcePackage != null && !"com.tencent.mm".equals(sourcePackage))) {
            return;
        }
        try {
            Object mapObject = getField(wms, "mWindowMap");
            if (!(mapObject instanceof Map)) {
                return;
            }
            List<Object> targets = new ArrayList<>();
            int wechatWindows = 0;
            for (Object value : new ArrayList<>(((Map<?, ?>) mapObject).values())) {
                try {
                    Object attrs = getField(value, "mAttrs");
                    Object pkg = getField(attrs, "packageName");
                    if (pkg == null || !"com.tencent.mm".equals(String.valueOf(pkg))) {
                        continue;
                    }
                    int type = ((Number) getField(attrs, "type")).intValue();
                    int width = ((Number) getField(attrs, "width")).intValue();
                    int height = ((Number) getField(attrs, "height")).intValue();
                    wechatWindows++;
                    log(Log.INFO, TAG, "WeChat window type=" + type
                            + " size=" + width + "x" + height
                            + " vis=" + getField(value, "mViewVisibility")
                            + " surface=" + getField(value, "mHasSurface"));
                    // The handles may already be GONE (mViewVisibility=8) and
                    // have no surface while WeChat is in the background; they
                    // still remain in mWindowMap and reappear when WeChat
                    // resumes.  Remove them even when currently hidden.
                    if (type != 1000 // TYPE_APPLICATION_PANEL
                            || width < 70 || width > 180
                            || height < 50 || height > 140) {
                        continue;
                    }
                    targets.add(value);
                } catch (Throwable ignored) {
                }
            }
            log(Log.INFO, TAG, "windowMap=" + ((Map<?, ?>) mapObject).size()
                    + " WeChat windows=" + wechatWindows
                    + " panel candidates=" + targets.size()
                    + " source=" + sourcePackage);
            // There can be several stale pairs accumulated from earlier drags;
            // remove every matching handle panel.
            if (targets.size() < 2) {
                return;
            }
            for (Object target : targets) {
                try {
                    Object session = getField(target, "mSession");
                    Object client = getField(target, "mClient");
                    Object binder = invoke(client, "asBinder");
                    invoke(session, "remove", binder);
                    log(Log.INFO, TAG, "removed stale WeChat selection handle window");
                } catch (Throwable throwable) {
                    log(Log.WARN, TAG, "remove WeChat handle failed: " + throwable);
                }
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "dismissWeChatHandleWindows inspect failed: " + throwable);
        }
    }

    /**
     * Same-window drop guard (ColorOS-inspired, but stricter than ColorOS).
     *
     * AOSP's DragState.isValidDropTarget() explicitly exempts the drag source
     * window ("sameWindow"), so an app is free to consume a drop that lands
     * back on the very window the drag started from — WeChat's chat page turns
     * that into "send the message the shadow was made from".
     *
     * We end such a drag right before ACTION_DROP is dispatched:
     * endDragLocked(false, false) makes WMS play the native cancel/return
     * animation, and the target window never sees the drop. Only HyperPortal
     * drags (ClipData MIME application/x-dragshare) are affected, so app-owned
     * in-app drags (text selection, dual-window file moves) are untouched and
     * another window of the same app remains a valid drop target.
     */
    private static Object onReportDropWindowLock(XposedInterface.Chain chain) throws Throwable {
        try {
            Object dragState = chain.getThisObject();
            if (dragState == null) {
                return chain.proceed();
            }
            // Mirror reportDropWindowLock()'s own early-out: never touch a drag
            // that is already animating out or closing.
            try {
                Object animator = getField(dragState, "mAnimator");
                Object closing = getField(dragState, "mIsClosing");
                if (animator != null || Boolean.TRUE.equals(closing)) {
                    return chain.proceed();
                }
            } catch (Throwable ignored) {
            }
            Object clipObject = getField(dragState, "mData");
            if (!(clipObject instanceof ClipData)) {
                return chain.proceed();
            }
            ClipDescription description = ((ClipData) clipObject).getDescription();
            if (description == null || description.filterMimeTypes(DRAG_MIME) == null) {
                return chain.proceed(); // not a HyperPortal drag
            }
            List<Object> args = chain.getArgs();
            if (args == null || args.isEmpty()) {
                return chain.proceed();
            }
            Object wms = getField(dragState, "mService");
            Object targetWin = findWindowByInputToken(wms, args.get(0));
            Object sourceWin = getField(dragState, "mDragWindow");
            if (targetWin == null || sourceWin == null) {
                return chain.proceed();
            }
            if (!isSameWindowOrPage(targetWin, sourceWin)) {
                return chain.proceed();
            }
            // Two independent reasons to kill a same-window drop:
            //  - the finger is back inside the drag circle (deliberate cancel), or
            //  - the same-window guard setting says this app must never eat our drop.
            boolean insideCircle = isInsideDragCircle(description, args);
            if (!insideCircle && !sSameWindowGuard) {
                return chain.proceed();
            }

            log(Log.INFO, TAG, "same-window drop suppressed: "
                    + (insideCircle ? "inside-circle" : "guard")
                    + " pkg=" + packageOf(targetWin) + " window=" + targetWin);
            // Same call sequence AOSP uses for "drop outside a valid window":
            // ends the drag with mDragResult=false, which is what triggers the
            // native cancel/return animation (AGENTS.md Rule 3).
            invoke(dragState, "endDragLocked", false, false);
            return false; // skip reportDropWindowLockInner(): no ACTION_DROP
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "same-window guard failed: " + throwable);
            return chain.proceed();
        }
    }

    /**
     * ColorOS-style drag settlement animation (both consumed success and unconsumed cancel).
     *
     * In native Android / HyperOS:
     *   - Consumed (dragResult == true): DragState.endDragLocked immediately invokes
     *     closeLocked(), destroying the shadow surface with 0ms transition.
     *   - Unconsumed (dragResult == false): WMS plays return animation translating
     *     (x, y) all the way back to the start coordinate.
     *
     * ColorOS instead plays an in-place spring animation centered at the drop point:
     *   - Consumed success: scale 1.0f -> 0.8f (483ms), alpha 0.85f -> 0.0f (617ms) (吸入消散)
     *   - Unconsumed cancel/failure: scale 1.0f -> 1.45f (617ms), alpha 0.85f -> 0.0f (834ms) (放大消散，不飞回)
     * When the animation finishes, it notifies DragDropController (MSG_ANIMATION_END)
     * to execute closeLocked().
     */
    private static Object onEndDragLocked(XposedInterface.Chain chain) throws Throwable {
        try {
            List<Object> args = chain.getArgs();
            if (args == null || args.size() < 2) {
                return chain.proceed();
            }
            boolean dragResult = Boolean.TRUE.equals(args.get(0));
            boolean relinquish = Boolean.TRUE.equals(args.get(1));
            Object dragState = chain.getThisObject();
            if (dragState == null) {
                return chain.proceed();
            }

            if (relinquish || !sColorosConsumeAnim) {
                return chain.proceed();
            }

            if (!isOurDrag(dragState)) {
                return chain.proceed();
            }

            Object existingAnimator = getField(dragState, "mAnimator");
            if (existingAnimator != null) {
                return chain.proceed();
            }

            if (playColorosEndAnimation(dragState, dragResult)) {
                return null; // Handled by custom ColorOS animation, skip native return/close
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "onEndDragLocked hook failed: " + throwable);
        }
        return chain.proceed();
    }

    private static boolean isOurDrag(Object dragState) {
        if (sDragActive) {
            return true;
        }
        try {
            Object clipObject = getField(dragState, "mData");
            if (clipObject instanceof ClipData) {
                ClipDescription description = ((ClipData) clipObject).getDescription();
                if (description != null && description.filterMimeTypes(DRAG_MIME) != null) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean playColorosEndAnimation(Object dragState, boolean success) {
        try {
            Object surfaceObj = getField(dragState, "mSurfaceControl");
            if (!(surfaceObj instanceof SurfaceControl)) {
                return false;
            }
            SurfaceControl surface = (SurfaceControl) surfaceObj;
            if (!surface.isValid()) {
                return false;
            }

            Object wms = getField(dragState, "mService");
            if (wms == null) {
                return false;
            }
            Handler animHandler = null;
            try {
                Object handlerObj = getField(wms, "mAnimationHandler");
                if (handlerObj instanceof Handler) {
                    animHandler = (Handler) handlerObj;
                }
            } catch (Throwable ignored) {
            }
            if (animHandler == null) {
                animHandler = sHandler;
            }

            Object controller = getField(dragState, "mDragDropController");
            if (controller == null) {
                return false;
            }

            float currentX = ((Number) getField(dragState, "mCurrentDisplayX")).floatValue();
            float currentY = ((Number) getField(dragState, "mCurrentDisplayY")).floatValue();
            float thumbX = ((Number) getField(dragState, "mThumbOffsetX")).floatValue();
            float thumbY = ((Number) getField(dragState, "mThumbOffsetY")).floatValue();

            int width = 0;
            int height = 0;
            try {
                Method getWidth = SurfaceControl.class.getMethod("getWidth");
                Method getHeight = SurfaceControl.class.getMethod("getHeight");
                width = ((Number) getWidth.invoke(surface)).intValue();
                height = ((Number) getHeight.invoke(surface)).intValue();
            } catch (Throwable ignored) {
            }
            if (width <= 0) {
                width = Math.round(thumbX * 2f);
            }
            if (height <= 0) {
                height = Math.round(thumbY * 2f);
            }
            if (width <= 0) width = dp(80);
            if (height <= 0) height = dp(80);

            final int surfWidth = width;
            final int surfHeight = height;

            // Set dragState drop result
            setField(dragState, "mDragResult", success);
            setField(dragState, "mRelinquishDragSurfaceToDropTarget", false);

            // Start restoring 3D tilt concurrently with the settlement animation
            sCurrentState = 0;
            sTargetProgress = 0f;
            startTicker();
            notifyPortalState(0, 0f, 0f);

            // ColorOS timing parameters:
            // Success: alpha 617ms, scale 483ms (1.0 -> 0.8)
            // Cancel/Failure: alpha 834ms, scale 617ms (1.0 -> 1.45)
            final long totalDuration = success ? 617L : 834L;
            final long scaleDuration = success ? 483L : 617L;

            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(totalDuration);
            Interpolator scaleInterpolator = success
                    ? new COUISpringInterpolator(0.30, 0.0)
                    : new COUISpringInterpolator(0.40, 0.0);
            Interpolator alphaInterpolator = success
                    ? new COUISpringInterpolator(0.35, 0.0)
                    : new COUISpringInterpolator(0.40, 0.0);

            animator.addUpdateListener(animation -> {
                try {
                    long playTime = animation.getCurrentPlayTime();
                    float scaleProgress = Math.min(1.0f, playTime / (float) scaleDuration);
                    float alphaProgress = Math.min(1.0f, playTime / (float) totalDuration);

                    float scaleInterp = scaleInterpolator.getInterpolation(scaleProgress);
                    float alphaInterp = alphaInterpolator.getInterpolation(alphaProgress);

                    float scale = success
                            ? (1.0f - 0.2f * scaleInterp)    // 1.0 -> 0.8
                            : (1.0f + 0.45f * scaleInterp);  // 1.0 -> 1.45
                    float alpha = 0.85f * (1.0f - alphaInterp); // 0.85 -> 0.0

                    float offsetX = (1.0f - scale) * surfWidth / 2.0f;
                    float offsetY = (1.0f - scale) * surfHeight / 2.0f;
                    float posX = currentX - thumbX + offsetX;
                    float posY = currentY - thumbY + offsetY;

                    SurfaceControl.Transaction t = new SurfaceControl.Transaction();
                    try {
                        if (surface.isValid()) {
                            t.setAlpha(surface, alpha);
                            t.setScale(surface, scale, scale);
                            t.setPosition(surface, posX, posY);
                            t.apply();
                        }
                    } finally {
                        t.close();
                    }
                } catch (Throwable t) {
                    log(Log.WARN, TAG, "ColorOS end animation update error: " + t);
                }
            });

            AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
                private boolean mDone = false;

                @Override
                public void onAnimationEnd(Animator animation) {
                    finish();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    finish();
                }

                private void finish() {
                    if (mDone) return;
                    mDone = true;
                    try {
                        setField(dragState, "mAnimationCompleted", true);
                        invoke(controller, "sendHandlerMessage", 2, null);
                        log(Log.INFO, TAG, "ColorOS end animation ended (success=" + success + "), closed drag state");
                    } catch (Throwable t) {
                        log(Log.WARN, TAG, "ColorOS end animation finish failed: " + t);
                    }
                }
            };

            animator.addListener(listener);
            setField(dragState, "mAnimator", animator);
            animHandler.post(animator::start);
            log(Log.INFO, TAG, "started ColorOS end animation (success=" + success
                    + (success ? ", shrink to 0.8" : ", expand to 1.45, no fly-back") + ")");
            return true;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "playColorosEndAnimation failed: " + throwable);
            return false;
        }
    }

    /** DragState.reportDropWindowLock() resolves its token through this map. */
    private static Object findWindowByInputToken(Object wms, Object token) {
        if (wms == null || token == null) {
            return null;
        }
        try {
            Object inputMap = getField(wms, "mInputToWindowMap");
            if (inputMap instanceof Map) {
                Object win = ((Map<?, ?>) inputMap).get(token);
                if (win != null) {
                    return win;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Object windowMap = getField(wms, "mWindowMap");
            if (windowMap instanceof Map) {
                return ((Map<?, ?>) windowMap).get(token);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * True when the drop point lies inside the drag circle Bridge shipped with
     * the ClipData (hp_circle_x/y/r). Missing extras → false (older drags fall
     * back to the same-window guard alone).
     */
    private static boolean isInsideDragCircle(ClipDescription description, List<Object> args) {
        try {
            android.os.PersistableBundle extras = description.getExtras();
            if (extras == null || args.size() < 3) {
                return false;
            }
            float cx = (float) extras.getDouble("hp_circle_x", Double.NaN);
            float cy = (float) extras.getDouble("hp_circle_y", Double.NaN);
            float radius = (float) extras.getDouble("hp_circle_r", 0.0);
            if (Float.isNaN(cx) || Float.isNaN(cy) || radius <= 0f) {
                return false;
            }
            float x = ((Number) args.get(1)).floatValue();
            float y = ((Number) args.get(2)).floatValue();
            double distance = Math.hypot(x - cx, y - cy);
            if (distance < radius) {
                log(Log.INFO, TAG, "drop inside drag circle: dist=" + Math.round(distance)
                        + "px r=" + Math.round(radius) + "px");
                return true;
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isSameWindowOrPage(Object targetWin, Object sourceWin) {
        if (targetWin == sourceWin) {
            return true;
        }
        try {
            Object targetRecord = getField(targetWin, "mActivityRecord");
            Object sourceRecord = getField(sourceWin, "mActivityRecord");
            return targetRecord != null && targetRecord == sourceRecord;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String packageOf(Object window) {
        try {
            Object attrs = getField(window, "mAttrs");
            Object pkg = getField(attrs, "packageName");
            return pkg == null ? null : String.valueOf(pkg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * OPPO Content Portal "window-level share fallback" port.
     *
     * Reference: docs/OPPO_拖拽未适配窗口_应用分享回退机制.md
     *   OplusGlobalDragAndDropCallback.preReportDropResult() fires when the drop
     *   target window reports consumed=false (the app has no OnDragListener and
     *   ViewRootImpl answered ACTION_DROP with false). system_server then builds a
     *   plain ACTION_SEND / ACTION_SEND_MULTIPLE intent, setPackage()s the target
     *   window's package, and starts it, so the target app's own share entry opens
     *   (QQ: 发送给好友 / 发送到电脑). It also flips the drop result to consumed so
     *   the drag shadow does not fly back.
     *
     * HyperOS has no such callback, so we hook DragDropController.reportDropResult
     * itself: the hook runs before the WMS lock (same place OPPO's preReportDropResult
     * executes), and on success we proceed() with consumed=true.
     */
    private static Object onReportDropResult(XposedInterface.Chain chain) throws Throwable {
        boolean handedOff = false;
        try {
            handedOff = tryDropToShareFallback(chain);
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "dropToShare fallback failed: " + throwable);
        }
        if (handedOff) {
            Object[] newArgs = chain.getArgs().toArray();
            newArgs[1] = Boolean.TRUE; // no cancel fly-back, OPPO sets mConsumeResult=true too
            return chain.proceed(newArgs);
        }
        return chain.proceed();
    }

    private static boolean tryDropToShareFallback(XposedInterface.Chain chain) throws Exception {
        List<Object> args = chain.getArgs();
        if (args == null || args.size() < 2) {
            return false;
        }
        if (!Boolean.FALSE.equals(args.get(1))) {
            return false; // target consumed the drop
        }
        Object window = args.get(0);
        if (window == null) {
            return false;
        }
        if (!dropToShareEnabled()) {
            return false;
        }

        Object controller = chain.getThisObject();
        Object wms = getField(controller, "mService");
        if (wms == null) {
            return false;
        }
        sWms = wms;
        Object dragState = getField(controller, "mDragState");
        if (dragState == null) {
            return false;
        }
        Object clipObject = getField(dragState, "mData");
        if (!(clipObject instanceof ClipData)) {
            return false;
        }
        ClipData clip = (ClipData) clipObject;
        if (clip.getItemCount() == 0) {
            return false;
        }

        Object targetWin = findWindow(wms, window);
        if (targetWin == null) {
            return false;
        }

        // Only real app activity windows may receive the fallback. Our portal overlay
        // and the SystemUI dock are service windows without an ActivityRecord.
        if (getField(targetWin, "mActivityRecord") == null) {
            return false;
        }

        Object targetAttrs = getField(targetWin, "mAttrs");
        Object targetPkgObject = getField(targetAttrs, "packageName");
        String targetPkg = targetPkgObject == null ? null : String.valueOf(targetPkgObject);
        if (targetPkg == null || targetPkg.isEmpty()
                || DROP_SHARE_DENY_PACKAGES.contains(targetPkg)) {
            return false;
        }

        Object sourceWin = getField(dragState, "mDragWindow");
        if (sourceWin == targetWin) {
            return false; // OPPO: never share back into the start window
        }
        String sourcePkg = null;
        if (sourceWin != null) {
            try {
                Object sourceAttrs = getField(sourceWin, "mAttrs");
                Object sourcePkgObject = getField(sourceAttrs, "packageName");
                sourcePkg = sourcePkgObject == null ? null : String.valueOf(sourcePkgObject);
            } catch (Throwable ignored) {
            }
        }
        if (targetPkg.equals(sourcePkg)) {
            return false; // same app: freeform / split windows must not self-share
        }

        Context context = resolveWmContext(wms);
        if (context == null) {
            log(Log.WARN, TAG, "dropToShare skip: no system context");
            return false;
        }

        Intent send = buildDropShareIntent(clip, targetPkg, context);
        if (send == null) {
            return false;
        }
        // The drag URI belongs to HyperPortal's own provider (app-private cache).
        // startActivityAsUser's GRANT_READ flag alone is not enough for legacy
        // share entries that call openFileDescriptor(uri,"r") themselves; grant
        // the target package explicitly so Weibo's fallback copy (into its OWN
        // cache, never the gallery) can read the bytes across UIDs.
        grantShareUris(context, send, targetPkg);
        if (context.getPackageManager().resolveActivity(send, 0) == null) {
            log(Log.INFO, TAG, "dropToShare skip: no ACTION_SEND entry in " + targetPkg
                    + " type=" + send.getType());
            return false;
        }

        int ownerUid = -1;
        try {
            Object uidObj = invoke(targetWin, "getOwningUid");
            if (uidObj instanceof Integer) {
                ownerUid = (Integer) uidObj;
            }
        } catch (Throwable ignored) {
            try {
                Object uidObj = getField(targetWin, "mOwnerUid");
                if (uidObj instanceof Integer) {
                    ownerUid = (Integer) uidObj;
                }
            } catch (Throwable ignored2) {
            }
        }
        android.os.UserHandle targetUser = null;
        if (ownerUid >= 0) {
            // Public API equivalent of UserHandle.of(UserHandle.getUserId(uid));
            // targets cloned apps (parallel space) with their own user.
            targetUser = android.os.UserHandle.getUserHandleForUid(ownerUid);
        }

        // OPPO parity (preReportDropResult): launch INSIDE the target app's own
        // task. Without setLaunchTaskId the disambiguation ResolverActivity gets
        // its own task; the drag-end cleanup then raises the target app's task
        // and the Chooser detects it is no longer resumed and immediately
        // self-finishes ("app-request"), which looked like the share menu
        // closing by itself.
        int launchTaskId = -1;
        try {
            Object task = invoke(targetWin, "getTask");
            if (task != null) {
                Object id = getField(task, "mTaskId");
                if (id instanceof Integer) {
                    launchTaskId = (Integer) id;
                }
            }
        } catch (Throwable ignored) {
        }

        Bundle options = null;
        try {
            android.app.ActivityOptions activityOptions = android.app.ActivityOptions.makeBasic();
            if (launchTaskId >= 0) {
                // @hide, but reflection inside system_server has no hidden-API gate.
                Method setLaunchTaskId = android.app.ActivityOptions.class
                        .getMethod("setLaunchTaskId", int.class);
                setLaunchTaskId.invoke(activityOptions, launchTaskId);
            }
            options = activityOptions.toBundle();
        } catch (Throwable ignored) {
        }

        // Fire the panel only after endDragLocked() and the dock's drag-end
        // task reshuffling have run, so the panel is the last thing on top.
        final Intent finalSend = send;
        final Bundle finalOptions = options;
        final android.os.UserHandle finalUser = targetUser;
        final int finalTaskId = launchTaskId;
        final int finalOwnerUid = ownerUid;
        final String finalSource = sourcePkg;
        final int finalItemCount = clip.getItemCount();
        sHandler.postDelayed(() -> {
            try {
                startActivityWithUser(context, finalSend, finalOptions, finalUser);
                log(Log.INFO, TAG, "dropToShare: " + finalSource + " -> " + targetPkg
                        + " action=" + finalSend.getAction() + " type=" + finalSend.getType()
                        + " items=" + finalItemCount + " ownerUid=" + finalOwnerUid
                        + " task=" + finalTaskId);
            } catch (Throwable throwable) {
                Throwable cause = throwable instanceof java.lang.reflect.InvocationTargetException
                        ? ((java.lang.reflect.InvocationTargetException) throwable).getTargetException()
                        : throwable;
                log(Log.WARN, TAG, "dropToShare start failed for " + targetPkg + ": " + cause);
            }
        }, 250L);
        return true;
    }

    /**
     * Mirrors OPPO createSendIntentInner(): text -> ACTION_SEND text/plain,
     * URI -> ACTION_SEND with EXTRA_STREAM, several URIs -> ACTION_SEND_MULTIPLE
     * (abort on mixed MIME, like OPPO), and it never shares file:// URIs because
     * Android 17 kills the caller on file exposure. The clip keeps our custom
     * application/x-dragshare mime, so the intent type is picked from the
     * standard mimes only.
     */
    private static Intent buildDropShareIntent(ClipData clip, String targetPkg, Context context) {
        ClipDescription description = clip.getDescription();
        Intent send = new Intent();
        send.setPackage(targetPkg);
        send.setClipData(clip);
        int count = clip.getItemCount();

        if (count == 1) {
            ClipData.Item item = clip.getItemAt(0);
            Uri uri = item.getUri();
            CharSequence text = item.getText();
            if (uri != null) {
                if ("file".equals(uri.getScheme())) {
                    return null;
                }
                String mime = pickDropShareMime(description, 0, uri, context);
                send.setAction(Intent.ACTION_SEND);
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.setType(mime != null ? mime : "*/*");
                if (text != null && text.length() > 0) {
                    send.putExtra(Intent.EXTRA_TEXT, text.toString());
                }
            } else if (text != null && text.length() > 0) {
                send.setAction(Intent.ACTION_SEND);
                send.putExtra(Intent.EXTRA_TEXT, text.toString());
                send.setType("text/plain");
            } else {
                return null;
            }
        } else {
            ArrayList<Uri> uris = new ArrayList<>();
            ArrayList<String> texts = new ArrayList<>();
            String firstMime = null;
            for (int i = 0; i < count; i++) {
                ClipData.Item item = clip.getItemAt(i);
                Uri uri = item.getUri();
                CharSequence text = item.getText();
                if (uri != null) {
                    if ("file".equals(uri.getScheme())) {
                        return null;
                    }
                    String mime = pickDropShareMime(description, i, uri, context);
                    if (mime == null) {
                        mime = "*/*";
                    }
                    if (firstMime == null) {
                        firstMime = mime;
                    } else if (!firstMime.equals(mime)) {
                        log(Log.INFO, TAG, "dropToShare skip: mixed mime " + firstMime + " vs " + mime);
                        return null; // OPPO aborts mixed file drags
                    }
                    uris.add(uri);
                } else if (text != null && text.length() > 0) {
                    texts.add(text.toString());
                }
            }
            if (!uris.isEmpty() && !texts.isEmpty()) {
                return null; // mixed text+uri payload
            }
            if (!uris.isEmpty()) {
                send.setAction(Intent.ACTION_SEND_MULTIPLE);
                send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                send.setType(firstMime != null ? firstMime : "*/*");
            } else if (!texts.isEmpty()) {
                send.setAction(Intent.ACTION_SEND);
                send.putExtra(Intent.EXTRA_TEXT, texts.get(0));
                send.setType("text/plain");
            } else {
                return null;
            }
        }

        send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return send;
    }

    private static String pickDropShareMime(ClipDescription description, int itemIndex,
            Uri uri, Context context) {
        if (description != null) {
            if (itemIndex < description.getMimeTypeCount()) {
                String mime = description.getMimeType(itemIndex);
                if (isDropShareMime(mime)) {
                    return mime;
                }
            }
            for (int i = 0; i < description.getMimeTypeCount(); i++) {
                String mime = description.getMimeType(i);
                if (isDropShareMime(mime)) {
                    return mime;
                }
            }
        }
        try {
            String type = context.getContentResolver().getType(uri);
            if (isDropShareMime(type)) {
                return type;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isDropShareMime(String mime) {
        if (mime == null || mime.isEmpty()) {
            return false;
        }
        if (DRAG_MIME.equals(mime)) {
            return false;
        }
        if (ClipDescription.MIMETYPE_TEXT_URILIST.equals(mime)) {
            return false;
        }
        return !"*/*".equals(mime);
    }

    private static boolean dropToShareEnabled() {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("getBoolean", String.class, boolean.class);
            return (Boolean) get.invoke(null, DROP_SHARE_PROP, true);
        } catch (Throwable throwable) {
            return true;
        }
    }

    /**
     * Resolve the WindowState for an IWindow argument the same way
     * DragDropController.reportDropResult() does: WindowManagerService
     * .windowForClient(null, window), with a raw mWindowMap lookup as fallback.
     */
    private static Object findWindow(Object wms, Object window) {
        try {
            Method windowForClient = null;
            for (Method method : wms.getClass().getDeclaredMethods()) {
                if ("windowForClient".equals(method.getName())
                        && method.getParameterTypes().length == 2) {
                    windowForClient = method;
                    break;
                }
            }
            if (windowForClient != null) {
                windowForClient.setAccessible(true);
                Object win = windowForClient.invoke(wms, null, window);
                if (win != null) {
                    return win;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Object windowMap = getField(wms, "mWindowMap");
            if (windowMap instanceof Map) {
                Object binder = invoke(window, "asBinder");
                if (binder != null) {
                    return ((Map<?, ?>) windowMap).get(binder);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void grantShareUris(Context context, Intent send, String targetPkg) {
        int granted = 0;
        try {
            Uri stream = send.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream != null) {
                context.grantUriPermission(targetPkg, stream,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                granted++;
            }
            java.util.ArrayList<Uri> streams =
                    send.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (streams != null) {
                for (Uri uri : streams) {
                    if (uri != null) {
                        context.grantUriPermission(targetPkg, uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        granted++;
                    }
                }
            }
            ClipData clip = send.getClipData();
            if (clip != null) {
                for (int i = 0; i < clip.getItemCount(); i++) {
                    Uri uri = clip.getItemAt(i).getUri();
                    if (uri != null) {
                        context.grantUriPermission(targetPkg, uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        granted++;
                    }
                }
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "dropToShare grantUriPermission failed: " + throwable);
        }
        if (granted > 0) {
            log(Log.INFO, TAG, "dropToShare granted " + granted + " uri(s) to " + targetPkg);
        }
    }

    private static void startActivityWithUser(Context context, Intent intent,
            Bundle options, android.os.UserHandle user) throws Throwable {
        if (user != null) {
            try {
                Method m = Context.class.getMethod("startActivityAsUser",
                        Intent.class, Bundle.class, android.os.UserHandle.class);
                m.invoke(context, intent, options, user);
                return;
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Method m = Context.class.getMethod("startActivityAsUser",
                        Intent.class, android.os.UserHandle.class);
                m.invoke(context, intent, user);
                return;
            } catch (NoSuchMethodException ignored) {
            }
        }
        context.startActivity(intent, options);
    }

    private static Context resolveWmContext(Object wms) {
        Context cached = sSystemContext;
        if (cached != null) {
            return cached;
        }
        try {
            Object ctx = getField(wms, "mContext");
            if (ctx instanceof Context) {
                sSystemContext = (Context) ctx;
                return sSystemContext;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void notifyPortalState(int state, float x, float y) {
        if (sSystemContext == null) {
            return;
        }
        final Context context = sSystemContext;
        sNotifyExecutor.execute(() -> {
            try {
                // Send drag state to SystemUI Overlay only (no need to wake HyperPortal app)
                String[] actions = new String[] {
                        "com.github.leaf.hyperportal.ACTION_DRAG_STATE",
                        "com.github.leaf.hyperportal.DRAG_STATE_CHANGED"
                };

                for (String act : actions) {
                    try {
                        Intent b = new Intent(act);
                        b.setPackage("com.android.systemui");
                        b.putExtra("state", state);
                        b.putExtra("x", x);
                        b.putExtra("y", y);
                        b.putExtra("position", sTriggerPosition);
                        b.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                        if (Build.VERSION.SDK_INT >= 29) {
                            b.addFlags(0x01000000); // Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                        }
                        context.sendBroadcast(b);
                    } catch (Throwable t) {
                        // ignore per-target send failure
                    }
                }

                // The drag shadow lives in the source app process. Send the
                // same state globally so its Bridge receiver can morph the
                // system DragShadowBuilder at the hot zone.
                Intent shadowBroadcast = new Intent("com.dragshare.bridge.SHADOW_STATE");
                shadowBroadcast.putExtra("state", state);
                shadowBroadcast.putExtra("x", x);
                shadowBroadcast.putExtra("y", y);
                shadowBroadcast.putExtra("position", sTriggerPosition);
                shadowBroadcast.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                if (Build.VERSION.SDK_INT >= 29) {
                    shadowBroadcast.addFlags(0x01000000);
                }
                context.sendBroadcast(shadowBroadcast);
            } catch (Throwable throwable) {
                if (true) {
                    log(Log.WARN, TAG, "notifyPortalState failed: " + throwable);
                }
            }
        });
    }

    private static void ensureConfigReceiver(Context ctx) {
        if (sReceiverRegistered || ctx == null) return;
        try {
            IntentFilter filter = new IntentFilter("com.github.leaf.hyperportal.ACTION_CONFIG_CHANGED");
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null) {
                        String pos = intent.getStringExtra("trigger_position");
                        if (pos != null) {
                            sTriggerPosition = pos;
                            log(Log.INFO, TAG, "trigger position updated: " + sTriggerPosition);
                        }
                        int pct = intent.getIntExtra("trigger_threshold", -1);
                        if (pct >= 50 && pct <= 95) {
                            sTriggerThreshold = pct / 100.0f;
                            log(Log.INFO, TAG, "trigger threshold updated: " + sTriggerThreshold);
                        } else {
                            queryConfigAsync(context);
                        }
                        if (intent.hasExtra("same_window_guard")) {
                            sSameWindowGuard = intent.getBooleanExtra("same_window_guard", true);
                            log(Log.INFO, TAG, "same-window guard updated: " + sSameWindowGuard);
                        }
                        if (intent.hasExtra("coloros_consume_anim")) {
                            sColorosConsumeAnim = intent.getBooleanExtra("coloros_consume_anim", true);
                            log(Log.INFO, TAG, "coloros consume anim updated: " + sColorosConsumeAnim);
                        }
                    }
                }
            };
            if (Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(receiver, filter);
            }
            sReceiverRegistered = true;
        } catch (Throwable t) {
            log(Log.WARN, TAG, "ensureConfigReceiver failed: " + t);
        }
    }

    private static void queryConfigAsync(Context ctx) {
        if (ctx == null) return;
        sNotifyExecutor.execute(() -> {
            try {
                Bundle res = null;
                try (android.database.Cursor c = ctx.getContentResolver().query(
                        Uri.parse("content://com.android.systemui.fileprovider/hyperportal_incoming/__config__"),
                        null, null, null, null)) {
                    if (c != null) {
                        res = c.getExtras();
                    }
                } catch (Throwable ignored) {
                }
                if (res == null || res.isEmpty()) {
                    Uri uri = Uri.parse("content://com.github.leaf.hyperportal.files");
                    res = ctx.getContentResolver().call(uri, "getConfig", null, null);
                }
                if (res != null) {
                    String pos = res.getString("trigger_position");
                    if (pos != null) {
                        sTriggerPosition = pos;
                        log(Log.INFO, TAG, "trigger position queried: " + sTriggerPosition);
                    }
                    int pct = res.getInt("trigger_threshold", 76);
                    if (pct >= 50 && pct <= 95) {
                        sTriggerThreshold = pct / 100.0f;
                        log(Log.INFO, TAG, "trigger threshold queried: " + sTriggerThreshold);
                    }
                    sSameWindowGuard = res.getBoolean("same_window_guard", sSameWindowGuard);
                    log(Log.INFO, TAG, "same-window guard queried: " + sSameWindowGuard);
                    sColorosConsumeAnim = res.getBoolean("coloros_consume_anim", sColorosConsumeAnim);
                    log(Log.INFO, TAG, "coloros consume anim queried: " + sColorosConsumeAnim);
                    broadcastConfigSnapshot(ctx);
                }
            } catch (Throwable ignored) {
            }
        });
    }

    /**
     * Pushes the authoritative snapshot to the source-app bridge and to
     * SystemUI. Cheap, background-executor only, and safe to repeat: consumers
     * treat it as "the value system_server last read successfully".
     */
    private static void broadcastConfigSnapshot(Context ctx) {
        if (ctx == null) return;
        try {
            Intent intent = new Intent(ACTION_CONFIG_SNAPSHOT);
            intent.putExtra("from_system_server", true);
            intent.putExtra("trigger_position", sTriggerPosition);
            intent.putExtra("trigger_threshold", Math.round(sTriggerThreshold * 100));
            intent.putExtra("same_window_guard", sSameWindowGuard);
            intent.putExtra("coloros_consume_anim", sColorosConsumeAnim);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            if (Build.VERSION.SDK_INT >= 29) {
                intent.addFlags(0x01000000); // FLAG_RECEIVER_INCLUDE_BACKGROUND
            }
            ctx.sendBroadcast(intent);
            log(Log.INFO, TAG, "config snapshot pushed pos=" + sTriggerPosition
                    + " threshold=" + Math.round(sTriggerThreshold * 100));
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "config snapshot push failed: " + throwable);
        }
    }

    private static int dp(int value) {
        return Math.round(value * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }

    private static void applyTaskProgress(float progress) {
        if (!sTiltAllowed) {
            // Overlay-hosted drag (transfer dock / shelf): never touch the task that
            // happens to be behind the panel.
            return;
        }
        SurfaceControl surface = sTaskSurface;
        if (surface == null || !surface.isValid()) {
            return;
        }

        int width = android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels;
        int height = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;

        boolean logNow = Math.abs(progress - sLastLogged) >= 0.25f
                || (progress == 0f && sLastLogged > 0f)
                || (progress == 1f && sLastLogged < 1f);
        if (logNow) {
            sLastLogged = progress;
        }

        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();

        // 1. Fully reset to 2D normal state when progress reaches 0
        if (progress <= 0.001f) {
            transaction.setScale(surface, 1.0f, 1.0f);
            transaction.setPosition(surface, 0f, 0f);
            transaction.setCrop(surface, null);
            if (sWallpaperSurface != null && sWallpaperSurface.isValid()) {
                transaction.setVisibility(sWallpaperSurface, false);
                transaction.setAlpha(sWallpaperSurface, 0f);
            }
            try {
                Method setCorner = SurfaceControl.Transaction.class.getMethod(
                        "setCornerRadius", SurfaceControl.class, float.class);
                setCorner.invoke(transaction, surface, 0f);
            } catch (Throwable ignored) {
            }
            try {
                Method rotation = SurfaceControl.Transaction.class.getMethod(
                        "setSurfaceRotation", SurfaceControl.class,
                        float[].class, float[].class, float[].class, float.class, float.class);
                // Zero degrees and zero layer offsets explicitly disables rotate state in SurfaceFlinger
                rotation.invoke(transaction, surface,
                        new float[]{0f, 0f, 0f},
                        new float[]{0f, 0f},
                        new float[]{0f, 0f, 0f},
                        30.0f, 5500.0f);
            } catch (Throwable ignored) {
            }
            try {
                Method forceCorner = SurfaceControl.Transaction.class.getMethod(
                        "setForceWindowCorner", SurfaceControl.class, boolean.class);
                forceCorner.invoke(transaction, surface, false);
            } catch (Throwable ignored) {
            }
            if (sTask != null) {
                try {
                    Class<?> callback = Class.forName("miui.app.IHwuiCornerRadiiCallback");
                    Method update = sTask.getClass().getDeclaredMethod(
                            "updateMiWindowCornerRadii", float[].class, callback);
                    update.setAccessible(true);
                    update.invoke(sTask, new float[]{0, 0, 0, 0, 0, 0, 0, 0}, null);
                } catch (Throwable ignored) {
                }
            }
            transaction.apply();
            if (logNow) {
                log(Log.INFO, TAG, "restored normal task 2D progress=0");
            }
            return;
        }

        // Wallpaper background: instantly visible with alpha 1.0f (no fade-in)
        if (sWallpaperSurface == null || !sWallpaperSurface.isValid()) {
            prepareWallpaperSurface(sTask);
        }
        if (sWallpaperSurface != null && sWallpaperSurface.isValid()) {
            transaction.setVisibility(sWallpaperSurface, true);
            transaction.setAlpha(sWallpaperSurface, 1.0f);
            setRelativeLayer(transaction, sWallpaperSurface, surface, -1);
        }

        // 2. 2D scale and position:
        // Subtle 5% scale down, floating like a card
        float scale = 1.0f - 0.05f * progress;
        boolean isRight = "right".equals(sTriggerPosition);
        float x;
        float y;
        if (isRight) {
            x = -dp(14) * progress;
            y = (height - height * scale) / 2.0f;
        } else {
            x = (width - width * scale) / 2.0f;
            y = dp(14) * progress;
        }

        transaction.setScale(surface, scale, scale);
        transaction.setPosition(surface, x, y);

        // Crucial: setCrop ensures the layer snapshot has non-zero geometry bounds,
        // preventing child layers from copying (0,0,0,0) bounds and turning black!
        transaction.setCrop(surface, new Rect(0, 0, width, height));

        // 3. 3D perspective tilt:
        // - bottom trigger: Pitch angle (-12.5 deg), pivot at screen top axis {0f, -height / 2.0f}
        // - right trigger: Yaw angle (+12.5 deg), pivot at screen left axis {-width / 2.0f, 0f}
        boolean rotationApplied = false;
        try {
            float[] rotDegrees;
            float[] centerOffsets;
            if (isRight) {
                float yawRad = (float) Math.toRadians(12.5f * progress);
                rotDegrees = new float[]{0f, yawRad, 0f};
                centerOffsets = new float[]{-width / 2.0f, 0f};
            } else {
                float pitchRad = (float) Math.toRadians(-12.5f * progress);
                rotDegrees = new float[]{pitchRad, 0f, 0f};
                centerOffsets = new float[]{0f, -height / 2.0f};
            }
            float[] layerOffsets = new float[]{0f, 0f, 0f};
            float camDegree = 30.0f;
            float spaceDepth = 5500.0f;

            Method rotation = SurfaceControl.Transaction.class.getMethod(
                "setSurfaceRotation", SurfaceControl.class,
                float[].class, float[].class, float[].class, float.class, float.class);
            rotation.invoke(transaction, surface, rotDegrees, centerOffsets, layerOffsets,
                camDegree, spaceDepth);
            rotationApplied = true;
        } catch (Throwable throwable) {
            Throwable cause = throwable instanceof java.lang.reflect.InvocationTargetException
                ? ((java.lang.reflect.InvocationTargetException) throwable).getTargetException()
                : throwable;
            if (true) {
                log(Log.WARN, TAG, "setSurfaceRotation failed: " + cause);
            }
        }

        // Fallback 2D matrix if setSurfaceRotation is unavailable
        if (!rotationApplied) {
            try {
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                Method setMatrix = SurfaceControl.Transaction.class.getMethod(
                    "setMatrix", SurfaceControl.class, Matrix.class, float[].class);
                setMatrix.invoke(transaction, surface, matrix, new float[9]);
            } catch (Throwable ignored) {
            }
        }

        // 4. Physical screen rounded corners (190px on this hardware display):
        // Crucial: NO gradual 0 -> 190 transition! The instant tilt is triggered,
        // it must immediately be the full physical screen rounded corners.
        float screenCornerRadius = 190.0f;
        try {
            Method setCorner = SurfaceControl.Transaction.class.getMethod(
                    "setCornerRadius", SurfaceControl.class, float.class);
            setCorner.invoke(transaction, surface, screenCornerRadius);
        } catch (Throwable ignored) {
        }

        try {
            Method forceCorner = SurfaceControl.Transaction.class.getMethod(
                "setForceWindowCorner", SurfaceControl.class, boolean.class);
            forceCorner.invoke(transaction, surface, true);
        } catch (Throwable ignored) {
        }

        if (sTask != null) {
            try {
                float[] radii = new float[]{
                    screenCornerRadius, screenCornerRadius, screenCornerRadius, screenCornerRadius,
                    screenCornerRadius, screenCornerRadius, screenCornerRadius, screenCornerRadius
                };
                Class<?> callback = Class.forName("miui.app.IHwuiCornerRadiiCallback");
                Method update = sTask.getClass().getDeclaredMethod(
                    "updateMiWindowCornerRadii", float[].class, callback);
                update.setAccessible(true);
                update.invoke(sTask, radii, null);
            } catch (Throwable ignored) {
            }
        }

        transaction.apply();
        if (logNow) {
            log(Log.INFO, TAG, "task progress=" + progress + " rotOk=" + rotationApplied
                    + " cornerRadius=" + screenCornerRadius);
        }
    }

    private static Object invoke(Object target, String method, Object... args) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            for (Method m : type.getDeclaredMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == args.length) {
                    m.setAccessible(true);
                    return m.invoke(target, args);
                }
            }
            type = type.getSuperclass();
        }
        throw new NoSuchMethodException(method + " in " + target.getClass());
    }

    /**
     * Exact port of ColorOS 17's android.widget.COUISpringInterpolator.
     * Damped harmonic oscillator with physical spring dynamics.
     */
    private static final class COUISpringInterpolator implements Interpolator {
        private final double mUnDampedAngularFreq;
        private final double mDampingRatio;
        private final double mAngularFreq;
        private final double mImpulse;
        private float mFinalValue = -1.0f;

        public COUISpringInterpolator(double response, double damping) {
            double r = response == 0.0 ? 1.0 : response;
            double k = Math.pow((2.0 * Math.PI) / r, 2.0);
            double dampingRatio = 1.0 - damping;
            double initialVel = 0.0;
            double maxVel = 15000.0;

            this.mUnDampedAngularFreq = Math.sqrt(k <= 0 ? 40.0 : k);
            this.mDampingRatio = dampingRatio <= 0 ? 1.15 : dampingRatio;
            double initVelNorm = Math.abs(initialVel) / maxVel;

            if (this.mDampingRatio < 1.0) {
                this.mAngularFreq = this.mUnDampedAngularFreq * Math.sqrt(1.0 - this.mDampingRatio * this.mDampingRatio);
                this.mImpulse = (this.mDampingRatio * this.mUnDampedAngularFreq - initVelNorm) / this.mAngularFreq;
            } else {
                this.mAngularFreq = 0.0;
                this.mImpulse = initVelNorm + this.mUnDampedAngularFreq;
            }
        }

        private float getOriginInterpolation(float input) {
            double t = Math.max(0.0f, input);
            double decay = Math.exp(-this.mDampingRatio * this.mUnDampedAngularFreq * t);
            if (this.mDampingRatio < 1.0) {
                double val = Math.cos(this.mAngularFreq * t) + this.mImpulse * Math.sin(this.mAngularFreq * t);
                return (float) (1.0 - decay * val);
            } else {
                return (float) (1.0 - decay * (1.0 + this.mImpulse * t));
            }
        }

        @Override
        public float getInterpolation(float input) {
            if (mFinalValue == -1.0f) {
                float f = getOriginInterpolation(1.0f);
                mFinalValue = f == 0.0f ? 1.0f : f;
            }
            return getOriginInterpolation(input) / mFinalValue;
        }
    }

    private static Object getField(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " in " + target.getClass());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " in " + target.getClass());
    }

    private static void preloadWallpaperBitmap() {
        if (sWallpaperBitmap != null && !sWallpaperBitmap.isRecycled()) {
            return;
        }
        try {
            File file = new File("/data/system/users/0/blurwallpaper");
            if (file.exists() && file.canRead()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                if (bmp != null) {
                    sWallpaperBitmap = bmp;
                    if (true) {
                        log(Log.INFO, TAG, "preloaded blurwallpaper: " + bmp.getWidth() + "x" + bmp.getHeight());
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (true) {
                log(Log.WARN, TAG, "preload blurwallpaper failed: " + t);
            }
        }
        try {
            File wp = new File("/data/system/users/0/wallpaper");
            if (wp.exists() && wp.canRead()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 8;
                sWallpaperBitmap = BitmapFactory.decodeFile(wp.getAbsolutePath(), opts);
                if (true) {
                    log(Log.INFO, TAG, "preloaded wallpaper fallback: "
                            + (sWallpaperBitmap != null ? sWallpaperBitmap.getWidth() + "x" + sWallpaperBitmap.getHeight() : "null"));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void prepareWallpaperSurface(Object task) {
        try {
            preloadWallpaperBitmap();
            if (sWallpaperBitmap == null) {
                return;
            }

            int width = android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels;
            int height = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;

            if (sWallpaperSurface != null && sWallpaperSurface.isValid()) {
                return;
            }

            SurfaceControl parentSurface = null;
            if (task != null) {
                try {
                    parentSurface = (SurfaceControl) invoke(task, "getParentSurfaceControl");
                } catch (Throwable t) {
                    try {
                        Object p = invoke(task, "getParent");
                        if (p != null) {
                            parentSurface = (SurfaceControl) invoke(p, "getSurfaceControl");
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            SurfaceControl bgSurface = createWallpaperSurface(width, height, parentSurface);
            if (bgSurface == null || !bgSurface.isValid()) {
                if (true) {
                    log(Log.WARN, TAG, "prepareWallpaperSurface: bgSurface is null or invalid");
                }
                return;
            }

            boolean rendered = renderBitmapToSurface(bgSurface, sWallpaperBitmap, width, height);
            if (!rendered) {
                if (true) {
                    log(Log.WARN, TAG, "prepareWallpaperSurface: failed to render bitmap");
                }
                return;
            }

            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            t.setVisibility(bgSurface, false);
            t.setAlpha(bgSurface, 1.0f);
            t.setCrop(bgSurface, new Rect(0, 0, width, height));
            t.setPosition(bgSurface, 0f, 0f);
            if (sTaskSurface != null && sTaskSurface.isValid()) {
                setRelativeLayer(t, bgSurface, sTaskSurface, -1);
            }
            t.apply();

            sWallpaperSurface = bgSurface;
            if (true) {
                log(Log.INFO, TAG, "prepared wallpaper SurfaceControl successfully parent=" + parentSurface);
            }
        } catch (Throwable throwable) {
            if (true) {
                log(Log.WARN, TAG, "prepareWallpaperSurface failed: " + throwable);
            }
        }
    }

    private static SurfaceControl createWallpaperSurface(int width, int height, SurfaceControl parent) {
        try {
            Class<?> builderClass = Class.forName("android.view.SurfaceControl$Builder");
            Object builder = builderClass.getConstructor().newInstance();

            invoke(builder, "setName", "PortalWallpaperBg");
            invoke(builder, "setBufferSize", width, height);
            invoke(builder, "setFormat", 1); // PixelFormat.RGBA_8888
            invoke(builder, "setOpaque", false);
            try {
                invoke(builder, "setBLASTLayer");
            } catch (Throwable ignored) {
            }
            invoke(builder, "setCallsite", "PortalWallpaper");
            if (parent != null && parent.isValid()) {
                invoke(builder, "setParent", parent);
            }
            return (SurfaceControl) invoke(builder, "build");
        } catch (Throwable t) {
            if (true) {
                log(Log.WARN, TAG, "createWallpaperSurface failed: " + t);
            }
            return null;
        }
    }

    private static boolean renderBitmapToSurface(SurfaceControl sc, Bitmap bmp, int width, int height) {
        if (sc == null || !sc.isValid() || bmp == null || bmp.isRecycled()) {
            return false;
        }
        Surface surface = null;
        try {
            surface = new Surface(sc);
            Canvas canvas = surface.lockCanvas(null);
            if (canvas != null) {
                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
                canvas.drawBitmap(bmp, null, new Rect(0, 0, width, height), paint);
                // Subtle darkening (ColorOS has ~15-20% dark scrim behind the card)
                canvas.drawColor(Color.argb(40, 0, 0, 0));
                surface.unlockCanvasAndPost(canvas);
                return true;
            }
        } catch (Throwable t) {
            if (true) {
                log(Log.WARN, TAG, "renderBitmapToSurface failed: " + t);
            }
        } finally {
            if (surface != null) {
                try {
                    surface.release();
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }


    private static void setRelativeLayer(SurfaceControl.Transaction t, SurfaceControl sc, SurfaceControl relativeTo, int z) {
        if (t == null || sc == null || relativeTo == null) return;
        try {
            Method m = SurfaceControl.Transaction.class.getMethod(
                    "setRelativeLayer", SurfaceControl.class, SurfaceControl.class, int.class);
            m.invoke(t, sc, relativeTo, z);
        } catch (Throwable ignored) {
        }
    }

    private static void removeSurface(SurfaceControl sc) {
        if (sc == null) return;
        try {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            Method m = SurfaceControl.Transaction.class.getMethod("remove", SurfaceControl.class);
            m.invoke(t, sc);
            t.apply();
        } catch (Throwable ignored) {
        }
        try {
            sc.release();
        } catch (Throwable ignored) {
        }
    }

}
