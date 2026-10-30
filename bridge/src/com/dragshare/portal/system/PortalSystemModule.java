package com.dragshare.portal.system;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
public final class PortalSystemModule extends XposedModule {

    private static final String TAG = "DragShareSystem";
    private static final String DRAG_MIME = "application/x-dragshare";
    private static final ExecutorService sNotifyExecutor = Executors.newSingleThreadExecutor();

    private static volatile PortalSystemModule sInstance;

    private static volatile boolean sDragActive;
    private static volatile Object sTask;
    private static volatile SurfaceControl sTaskSurface;
    private static volatile Bitmap sWallpaperBitmap;
    private static volatile SurfaceControl sWallpaperSurface;
    private static volatile float sProgress = 0f;
    private static volatile float sTargetProgress = 0f;
    private static volatile float sLastLogged = -1f;
    private static volatile float sLastNotifiedY = -1f;
    private static volatile int sCurrentState = 0; // 0 = closed, 1 = glow only, 2 = tilt + appbar
    private static volatile boolean sTickerRunning;
    private static volatile ClassLoader sSystemClassLoader;
    private static volatile Context sSystemContext;

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

    public PortalSystemModule() {
        sInstance = this;
    }

    private static void startTicker() {
        if (!sTickerRunning) {
            sTickerRunning = true;
            sHandler.post(sTicker);
        }
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        sInstance = this;
        log(Log.INFO, TAG, "onModuleLoaded systemServer=" + param.isSystemServer()
                + " process=" + param.getProcessName());
    }

    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        sInstance = this;
        sSystemClassLoader = resolveSystemClassLoader();
        log(Log.INFO, TAG, "onSystemServerStarting cl=" + sSystemClassLoader);
        installHooks(sSystemClassLoader);
        preloadWallpaperBitmap();
    }

    @Override
    public boolean onHotReloading(HotReloadingParam param) {
        log(Log.INFO, TAG, "onHotReloading, allow");
        param.setSavedInstanceState(sSystemClassLoader);
        return true;
    }

    @Override
    public void onHotReloaded(HotReloadedParam param) {
        sInstance = this;
        ClassLoader classLoader = resolveSystemClassLoader();
        log(Log.INFO, TAG, "onHotReloaded cl=" + classLoader);
        super.onHotReloaded(param);
        installHooks(classLoader);
    }

    private static ClassLoader resolveSystemClassLoader() {
        ArrayList<ClassLoader> candidates = new ArrayList<>();
        candidates.add(null);
        candidates.add(ClassLoader.getSystemClassLoader());
        candidates.add(ClassLoader.getPlatformClassLoader());
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

    private void installHooks(ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                classLoader = resolveSystemClassLoader();
            }
            Class<?> controller = Class.forName("com.android.server.wm.DragDropController", false, classLoader);
            log(Log.INFO, TAG, "DragDropController=" + controller.getName());
            for (Method method : controller.getDeclaredMethods()) {
                String name = method.getName();
                if ("performDrag".equals(name)) {
                    hook(method).setId("ddc-performDrag").intercept(this::onPerformDrag);
                    log(Log.INFO, TAG, "hooked performDrag " + method);
                } else if ("handleMotionEvent".equals(name)) {
                    hook(method).setId("ddc-handleMotionEvent").intercept(this::onHandleMotion);
                    log(Log.INFO, TAG, "hooked handleMotionEvent " + method);
                } else if ("onDragStateClosedLocked".equals(name)) {
                    hook(method).setId("ddc-onDragStateClosed").intercept(this::onDragClosed);
                    log(Log.INFO, TAG, "hooked onDragStateClosedLocked " + method);
                }
            }
            Class<?> dragState = Class.forName("com.android.server.wm.DragState", false, classLoader);
            for (Method method : dragState.getDeclaredMethods()) {
                if ("updateDragSurfaceLocked".equals(method.getName())) {
                    hook(method).setId("drag-updateSurface").intercept(this::onHandleMotion);
                    log(Log.INFO, TAG, "hooked updateDragSurfaceLocked " + method);
                }
            }
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "installHooks failed", throwable);
        }
    }

    private Object onPerformDrag(XposedInterface.Chain chain) throws Throwable {
        try {
            Object clipObject = chain.getArg(chain.getArgs().size() - 1);
            if (clipObject instanceof ClipData) {
                ClipData clip = (ClipData) clipObject;
                if (clip.getDescription().hasMimeType(DRAG_MIME)) {
                    sDragActive = true;
                    sProgress = 0f;
                    sTargetProgress = 0f;
                    sCurrentState = 1; // Drag started: Phase 1 (Glow mode)
                    sLastLogged = -1f;
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

                    // Third argument of performDrag is IWindow window
                    Object windowArg = chain.getArgs().size() > 2 ? chain.getArg(2) : null;
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

                    notifyPortalState(1, 0f);
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
                            if (sInstance != null) {
                                sInstance.log(Log.INFO, TAG, "resolved calling window task: " + task);
                            }
                            return task;
                        }
                    }
                }
            } catch (Throwable t) {
                if (sInstance != null) {
                    sInstance.log(Log.WARN, TAG, "resolve calling task failed: " + t);
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
                            if (sInstance != null) {
                                sInstance.log(Log.INFO, TAG, "resolved top fullscreen root task: " + fsTask);
                            }
                            return fsTask;
                        }
                    }
                }
            } catch (Throwable t) {
                if (sInstance != null) {
                    sInstance.log(Log.WARN, TAG, "resolve top fullscreen task failed: " + t);
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
                            if (sInstance != null) {
                                sInstance.log(Log.INFO, TAG, "resolved resumed task: " + task);
                            }
                            return task;
                        }
                    }
                }
            } catch (Throwable t) {
                if (sInstance != null) {
                    sInstance.log(Log.WARN, TAG, "resolve resumed task failed: " + t);
                }
            }
        }

        return null;
    }

    private Object onHandleMotion(XposedInterface.Chain chain) throws Throwable {
        if (sDragActive && sTaskSurface != null) {
            try {
                Object yArg = chain.getArg(3);
                float y = ((Number) yArg).floatValue();
                int height = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;

                // Thresholds based on ColorOS reverse engineering:
                // Upper/middle screen: only bottom glow effect, tilt remains 0.
                // Bottom threshold (bottom ~18% of screen): page tilts and app bar expands!
                float bottomThreshold = height * 0.76f;
                float exitThreshold = height * 0.70f; // 5% hysteresis to prevent flutter

                int newState;
                float target;
                if (y >= bottomThreshold) {
                    // Phase 2: Bottom threshold reached -> 3D tilt & expand app bar
                    target = 1.0f;
                    newState = 2;
                } else if (y < exitThreshold) {
                    // Phase 1: Above threshold -> flat page, only glow
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
                    notifyPortalState(newState, y);
                    sLastNotifiedY = y;
                } else if (newState == 1 && Math.abs(y - sLastNotifiedY) > 25.0f) {
                    // While dragging down towards threshold, update glow intensity smoothly
                    sLastNotifiedY = y;
                    notifyPortalState(1, y);
                }
            } catch (Throwable throwable) {
                if (sInstance != null) {
                    sInstance.log(Log.WARN, TAG, "onHandleMotion failed: " + throwable);
                }
            }
        }
        return chain.proceed();
    }

    private Object onDragClosed(XposedInterface.Chain chain) throws Throwable {
        if (sDragActive) {
            sDragActive = false;
            sCurrentState = 0;
            sTargetProgress = 0f;
            startTicker();
            notifyPortalState(0, 0f);
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

    private static void notifyPortalState(int state, float y) {
        if (sSystemContext == null) {
            return;
        }
        final Context context = sSystemContext;
        sNotifyExecutor.execute(() -> {
            try {
                // Foreground broadcast: bypasses HyperOS Greezer Denial
                Intent broadcast = new Intent("com.dragshare.portal.DRAG_STATE_CHANGED");
                broadcast.setPackage("com.dragshare.portal");
                broadcast.putExtra("state", state);
                broadcast.putExtra("y", y);
                broadcast.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                if (Build.VERSION.SDK_INT >= 29) {
                    broadcast.addFlags(0x01000000); // Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                }
                context.sendBroadcast(broadcast);

                // The drag shadow lives in the source app process. Send the
                // same state globally so its Bridge receiver can morph the
                // system DragShadowBuilder at the bottom hot zone.
                Intent shadowBroadcast = new Intent("com.dragshare.bridge.SHADOW_STATE");
                shadowBroadcast.putExtra("state", state);
                shadowBroadcast.putExtra("y", y);
                shadowBroadcast.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                if (Build.VERSION.SDK_INT >= 29) {
                    shadowBroadcast.addFlags(0x01000000);
                }
                context.sendBroadcast(shadowBroadcast);
            } catch (Throwable throwable) {
                if (sInstance != null) {
                    sInstance.log(Log.WARN, TAG, "notifyPortalState failed: " + throwable);
                }
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
            if (logNow && sInstance != null) {
                sInstance.log(Log.INFO, TAG, "restored normal task 2D progress=0");
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
        // Subtle 5% scale down and slight downward shift, floating like a card
        float scale = 1.0f - 0.05f * progress;
        float x = (width - width * scale) / 2.0f;
        float y = dp(14) * progress;

        transaction.setScale(surface, scale, scale);
        transaction.setPosition(surface, x, y);

        // Crucial: setCrop ensures the layer snapshot has non-zero geometry bounds,
        // preventing child layers from copying (0,0,0,0) bounds and turning black!
        transaction.setCrop(surface, new Rect(0, 0, width, height));

        // 3. 3D perspective tilt:
        // - pitch angle: -12.5 degrees in radians
        // - centerOffsets: { 0f, -height / 2.0f } -> TOP AXIS of the screen!
        // - layerOffsets: { 0f, 0f, 0f }
        // - camDegree: 30.0f, spaceDepth: 5500.0f
        boolean rotationApplied = false;
        try {
            float pitchRad = (float) Math.toRadians(-12.5f * progress);
            float[] rotDegrees = new float[]{pitchRad, 0f, 0f};
            float[] centerOffsets = new float[]{0f, -height / 2.0f};
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
            if (sInstance != null) {
                sInstance.log(Log.WARN, TAG, "setSurfaceRotation failed: " + cause);
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
        if (logNow && sInstance != null) {
            sInstance.log(Log.INFO, TAG, "task progress=" + progress + " rotOk=" + rotationApplied
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
                    if (sInstance != null) {
                        sInstance.log(Log.INFO, TAG, "preloaded blurwallpaper: " + bmp.getWidth() + "x" + bmp.getHeight());
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (sInstance != null) {
                sInstance.log(Log.WARN, TAG, "preload blurwallpaper failed: " + t);
            }
        }
        try {
            File wp = new File("/data/system/users/0/wallpaper");
            if (wp.exists() && wp.canRead()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 8;
                sWallpaperBitmap = BitmapFactory.decodeFile(wp.getAbsolutePath(), opts);
                if (sInstance != null) {
                    sInstance.log(Log.INFO, TAG, "preloaded wallpaper fallback: "
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
                if (sInstance != null) {
                    sInstance.log(Log.WARN, TAG, "prepareWallpaperSurface: bgSurface is null or invalid");
                }
                return;
            }

            boolean rendered = renderBitmapToSurface(bgSurface, sWallpaperBitmap, width, height);
            if (!rendered) {
                if (sInstance != null) {
                    sInstance.log(Log.WARN, TAG, "prepareWallpaperSurface: failed to render bitmap");
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
            if (sInstance != null) {
                sInstance.log(Log.INFO, TAG, "prepared wallpaper SurfaceControl successfully parent=" + parentSurface);
            }
        } catch (Throwable throwable) {
            if (sInstance != null) {
                sInstance.log(Log.WARN, TAG, "prepareWallpaperSurface failed: " + throwable);
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
            if (sInstance != null) {
                sInstance.log(Log.WARN, TAG, "createWallpaperSurface failed: " + t);
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
            if (sInstance != null) {
                sInstance.log(Log.WARN, TAG, "renderBitmapToSurface failed: " + t);
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
