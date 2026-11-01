package com.github.leaf.hyperportal.system;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
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
    private static final ExecutorService sNotifyExecutor = Executors.newSingleThreadExecutor();

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
                }
            }
            Class<?> dragState = Class.forName("com.android.server.wm.DragState", false, classLoader);
            for (Method method : dragState.getDeclaredMethods()) {
                if ("updateDragSurfaceLocked".equals(method.getName())) {
                    sXposed.hook(method).setId("drag-updateSurface").intercept(PortalSystemModule::onHandleMotion);
                    log(Log.INFO, TAG, "hooked updateDragSurfaceLocked " + method);
                }
            }

            // SELinux context fixes for re-signed UID 1000 packages (ContentCatcher / AES)
            installSELinuxHooks(classLoader);
            installAppsFilterHooks(classLoader);
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "installHooks failed", throwable);
        }
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

                Object controller = chain.getThisObject();
                Object wms = getField(controller, "mService");
                try {
                    Object ctx = getField(wms, "mContext");
                    if (ctx instanceof Context) {
                        sSystemContext = (Context) ctx;
                    }
                } catch (Throwable ignored) {
                }
                Object root = getField(wms, "mRoot");

                Object task = resolveTargetTask(wms, root, windowArg);
                sTask = task;
                if (task != null) {
                    sTaskSurface = (SurfaceControl) getField(task, "mSurfaceControl");
                    log(Log.INFO, TAG, "our drag started target task=" + task
                            + " surface=" + sTaskSurface);
                    prepareWallpaperSurface(task);
                } else {
                    sTaskSurface = null;
                    log(Log.WARN, TAG, "our drag started but no valid fullscreen task found");
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
     * Resolves the fullscreen standard Task that should tilt.
     * CRITICAL BUGFIX: If a small window / freeform window is open or being dragged from,
     * the small window must NEVER be targeted! We strictly find and tilt the underlying
     * FULLSCREEN standard task so the screen never distorts.
     */
    private static Object resolveTargetTask(Object wms, Object root, Object windowArg) {
        // 1. Try finding calling window's task from IWindow
        if (windowArg != null && wms != null) {
            try {
                Method windowForClient = wms.getClass().getDeclaredMethod("windowForClient",
                        Class.forName("com.android.server.wm.Session"),
                        Class.forName("android.view.IWindow"));
                windowForClient.setAccessible(true);
                Object callingWin = windowForClient.invoke(wms, null, windowArg);
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
                }
            }, 300);
            log(Log.INFO, TAG, "our drag closed, restoring");
        }
        return chain.proceed();
    }

    private static void notifyPortalState(int state, float x, float y) {
        if (sSystemContext == null) {
            return;
        }
        final Context context = sSystemContext;
        sNotifyExecutor.execute(() -> {
            try {
                // Send both action names to both Portal Companion App and SystemUI Overlay
                String[] actions = new String[] {
                        "com.github.leaf.hyperportal.ACTION_DRAG_STATE",
                        "com.github.leaf.hyperportal.DRAG_STATE_CHANGED"
                };
                String[] packages = new String[] {
                        "com.github.leaf.hyperportal",
                        "com.android.systemui"
                };

                for (String act : actions) {
                    for (String pkg : packages) {
                        try {
                            Intent b = new Intent(act);
                            b.setPackage(pkg);
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
                Uri uri = Uri.parse("content://com.github.leaf.hyperportal.files");
                Bundle res = ctx.getContentResolver().call(uri, "getConfig", null, null);
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
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private static int dp(int value) {
        return Math.round(value * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }

    private static void applyTaskProgress(float progress) {
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
