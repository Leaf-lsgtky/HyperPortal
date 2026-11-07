package dev.dragshare.catcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.HapticFeedbackConstants;
import java.util.concurrent.atomic.AtomicInteger;
import android.view.MotionEvent;
import android.view.View;
import android.view.TextureView;
import android.view.ViewParent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Plan-B bridge for the AES 5.1.7 PoC.
 *
 * All methods are called from smali patches inside classes.dex, so this class
 * must stay dependency-free (Android SDK only) and must never throw.
 */
public final class Bridge {

    private static final String TAG = "DragSharePoC";
    private static final String PORTAL_PACKAGE = "com.github.leaf.hyperportal";
    private static final String ACTION_CONFIG_CHANGED =
            "com.github.leaf.hyperportal.ACTION_CONFIG_CHANGED";
    /**
     * Pushed by the system_server module, which is the only component that can
     * always read the portal settings: the HyperPortal process is a normal
     * background app, and HyperOS' freezer can freeze its uid at any time. A
     * frozen uid keeps its pid but its ContentProvider is no longer resolvable
     * for third-party callers, so getConfig() fails and everything that pulls
     * from it silently keeps the default trigger edge.
     */
    private static final String ACTION_CONFIG_SNAPSHOT =
            "com.github.leaf.hyperportal.ACTION_CONFIG_SNAPSHOT";
    private static volatile boolean sMasterSwitch = true;
    private static volatile boolean sLandscapeTrigger = false;
    private static volatile float sDragStartX = -1f;
    private static volatile float sDragStartY = -1f;
    private static volatile String sTriggerPosition = "bottom";
    private static volatile int sLongPressDurationMs = 800;
    private static volatile int sTouchSlopDp = 24;
    // Centre of the drag circle: the finger position at the moment the long
    // press completed. Re-baselined there (not at ACTION_DOWN) so a drift
    // during the hold cannot shrink the "drag out to start" distance.
    private static volatile float sDragOriginX = -1f;
    private static volatile float sDragOriginY = -1f;
    private static volatile float sTriggerThreshold = 0.76f;
    private static volatile java.util.Set<String> sBlacklist = new java.util.HashSet<>();
    private static volatile long sLastConfigCheckTime = 0;
    private static volatile long sLastConfigFailTime = 0;
    private static volatile boolean sConfigReceiverRegistered = false;

    private static final BroadcastReceiver sConfigReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sLastConfigCheckTime = 0;
            sLastConfigFailTime = 0;
            boolean hadPosition = applyPushedConfig(intent);
            // The snapshot from system_server already carries everything we
            // need; only fall back to (possibly blocked) provider reads when the
            // broadcast did not contain a position.
            if (!hadPosition) {
                refreshConfig(context);
            }
        }
    };

    /**
     * Adopts trigger position / threshold pushed over a broadcast. Returns true
     * when the intent carried a position, i.e. the caller can trust it as the
     * current setting.
     */
    private static boolean applyPushedConfig(Intent intent) {
        if (intent == null) {
            return false;
        }
        String pos = intent.getStringExtra("position");
        if (pos == null) {
            pos = intent.getStringExtra("trigger_position");
        }
        boolean hasPosition = "right".equals(pos) || "bottom".equals(pos);
        if (hasPosition) {
            if (!pos.equals(sTriggerPosition)) {
                Log.i(TAG, "trigger position pushed: " + sTriggerPosition + " -> " + pos);
            }
            sTriggerPosition = pos;
        }
        int pct = intent.getIntExtra("trigger_threshold", -1);
        if (pct >= 50 && pct <= 95) {
            sTriggerThreshold = pct / 100.0f;
        }
        return hasPosition;
    }

    private static void ensureConfig(Context context) {
        if (context == null) return;
        long now = SystemClock.uptimeMillis();
        // A single failed read used to lock the trigger edge to the default
        // "bottom" for 30s. On a device whose freezer keeps HyperPortal frozen
        // that meant *every* drag in the window used the wrong edge, which is
        // what produced a bottom shelf for a right-edge user. Retry quickly;
        // the push channel above covers the frozen case anyway.
        if (now - sLastConfigFailTime < 2000) {
            return;
        }
        if (now - sLastConfigCheckTime > 5000 || sLastConfigCheckTime == 0) {
            sLastConfigCheckTime = now;
            refreshConfig(context);
        }
        if (!sConfigReceiverRegistered) {
            try {
                IntentFilter filter = new IntentFilter(ACTION_CONFIG_CHANGED);
                filter.addAction(ACTION_CONFIG_SNAPSHOT);
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    context.registerReceiver(sConfigReceiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(sConfigReceiver, filter);
                }
                sConfigReceiverRegistered = true;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void refreshConfig(Context context) {
        try {
            Uri uri = Uri.parse("content://com.github.leaf.hyperportal.files");
            Bundle res = context.getContentResolver().call(uri, "getConfig", null, null);
            if (res != null) {
                sMasterSwitch = res.getBoolean("master_switch", true);
                sLandscapeTrigger = res.getBoolean("landscape_trigger", false);
                sLongPressDurationMs = res.getInt("long_press_duration", 800);
                sTouchSlopDp = res.getInt("touch_slop", 24);
                int thresholdPct = res.getInt("trigger_threshold", 76);
                if (thresholdPct >= 50 && thresholdPct <= 95) {
                    sTriggerThreshold = thresholdPct / 100.0f;
                }
                String pos = res.getString("trigger_position");
                if (pos != null) {
                    sTriggerPosition = pos;
                }
                java.util.ArrayList<String> list = res.getStringArrayList("gesture_blacklist");
                if (list != null) {
                    sBlacklist = new java.util.HashSet<>(list);
                }
                sRuleEngineEnabled = res.getBoolean("drag_rules", true);
                Log.d(TAG, "config refreshed: master=" + sMasterSwitch + " landscape=" + sLandscapeTrigger
                        + " duration=" + sLongPressDurationMs + " radius=" + sTouchSlopDp
                        + " threshold=" + sTriggerThreshold
                        + " blacklist=" + sBlacklist.size());
            } else {
                sLastConfigFailTime = SystemClock.uptimeMillis();
                requestSystemWakePortal(context);
            }
        } catch (Throwable t) {
            sLastConfigFailTime = SystemClock.uptimeMillis();
            requestSystemWakePortal(context);
        }
    }

    /**
     * Asks system_server (UID 1000) and PortalReceiver to thaw or cold-start
     * the HyperPortal process so PortalFileProvider is ready before the finger
     * finishes a long-press and starts dragging.
     */
    private static void requestSystemWakePortal(Context context) {
        if (context == null) return;
        try {
            Intent wake = new Intent("com.github.leaf.hyperportal.ACTION_WAKE_PORTAL");
            wake.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(wake);

            Intent appWake = new Intent("com.github.leaf.hyperportal.SHOW");
            appWake.setPackage(PORTAL_PACKAGE);
            appWake.putExtra("keepAlive", true);
            appWake.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(appWake);
        } catch (Throwable ignored) {
        }
    }

    public static int getLongPressDuration() {
        return sLongPressDurationMs > 0 ? sLongPressDurationMs : 800;
    }

    private static boolean isLandscape(Context context) {
        if (context == null) return false;
        try {
            return context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isBlacklisted(Context context) {
        if (context == null || sBlacklist.isEmpty()) return false;
        String pkg = context.getPackageName();
        return pkg != null && sBlacklist.contains(pkg);
    }
    private static final String PORTAL_SERVICE = PORTAL_PACKAGE + ".PortalService";
    private static volatile WeakReference<Context> sContextRef;
    private static volatile PointF sLastTouchPoint;
    private static volatile View sLastTouchView;
    private static volatile Activity sLastActivity;
    private static volatile long sLastTouchAt;

    private static final int CUSTOM_LONG_PRESS_TIMEOUT_MS = 500; // Fallback after AES u/a (450ms)
    private static final int STATE_IDLE = 0;
    private static final int STATE_TRIGGERED = 1;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_PROCESSED = 3;
    private static final AtomicInteger sGestureState = new AtomicInteger(STATE_IDLE);

    private static final Runnable sCustomLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            onCustomLongPressTriggered();
        }
    };
    private static volatile View sTiltRoot;
    private static volatile View sTiltDecor;
    private static volatile View sTiltBar;
    private static volatile View sTiltDim;
    private static volatile int sTiltBarHeight;
    private static volatile float sTiltProgress;
    private static volatile float sDownRawX;
    private static volatile float sDownRawY;
    private static volatile float sLastMoveRawX;
    private static volatile float sLastMoveRawY;
    private static volatile boolean sMovedSinceDown;
    private static volatile Bundle sPendingPayload;
    private static volatile Object sPendingContent;
    private static volatile boolean sGestureActive;
    private static volatile boolean sCancelSent;
    private static volatile View sActiveDragView;
    private static volatile String sActiveDragText;
    private static volatile Bitmap sActiveDragBitmap;
    private static volatile float sActiveDragDensity;
    private static volatile int sActiveDragSourceWidth;
    private static volatile int sActiveDragScreenWidth;
    private static volatile int sActiveDragScreenHeight;
    private static volatile boolean sActiveShadowSquare;
    private static volatile float sShadowMorphProgress;
    /** True while the shadow is dimmed because the finger is back inside the drag circle. */
    private static volatile boolean sShadowDim;
    private static volatile ValueAnimator sShadowMorphAnimator;
    private static volatile boolean sShadowReceiverRegistered;
    private static final BroadcastReceiver sShadowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !"com.dragshare.bridge.SHADOW_STATE".equals(intent.getAction())) {
                return;
            }
            final int state = intent.getIntExtra("state", 0);
            // system_server sends this together with every drag-state change and
            // knows the real trigger edge even while HyperPortal is frozen.
            applyPushedConfig(intent);
            Log.i(TAG, "shadow receiver state=" + state + " pos=" + sTriggerPosition);
            if (state == 0) {
                cancelShadowMorphAnimator();
                return;
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                updateSystemShadow(state >= 2);
            } else {
                sMainHandler.post(() -> updateSystemShadow(state >= 2));
            }
        }
    };
    private static volatile long sDownAt;
    private static volatile int sGestureGeneration;
    private static volatile int sPendingGeneration;
    private static volatile int sTriggeredGeneration = -1;
    private static volatile int sDragStartedGeneration = -1;
    // Cache the reflective "what kind of view is this" answer per class. A long
    // press walks every leaf in the window; doing getDeclaredMethods() for each
    // one is a large part of the old 100-200ms stall.
    private static final WeakHashMap<Class<?>, Integer> sViewKindCache = new WeakHashMap<>();
    private static final int VIEW_KIND_NORMAL = 0;
    private static final int VIEW_KIND_COMPOSE = 1;
    private static final int VIEW_KIND_WEBVIEW = 2;
    private static final int VIEW_KIND_CUSTOM_CELL = 3;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final Runnable sTiltTimeout = new Runnable() {
        @Override
        public void run() {
            Log.w(TAG, "tilt timeout, restoring");
            hideTiltEffect();
        }
    };
    private static final WeakHashMap<View, WebCache> sWebCache = new WeakHashMap<>();
    private static final WeakHashMap<Class<?>, Method> sWebEvalMethodCache = new WeakHashMap<>();
    private static final WeakHashMap<Class<?>, Boolean> sWebEvalMethodMiss = new WeakHashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong sWebRequestId =
            new java.util.concurrent.atomic.AtomicLong(0);
    private static final java.util.concurrent.ExecutorService sWebExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "HyperPortal-WebImg");
                t.setDaemon(true);
                return t;
            });
    private static final Pattern WEB_CSS_URL_PATTERN = Pattern.compile("url\\((.*?)\\)");
    private static volatile int sWebAwaitingGeneration = -1;
    private static volatile WeakReference<View> sWebAwaitingViewRef;
    private static final Runnable sWebPrefetchRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!sGestureActive || sMovedSinceDown || sGestureState.get() != STATE_IDLE) {
                    return;
                }
                View root = sLastTouchView != null ? sLastTouchView.getRootView() : null;
                if (root == null && sLastActivity != null && sLastActivity.getWindow() != null) {
                    root = sLastActivity.getWindow().getDecorView();
                }
                if (root != null) {
                    prefetchWebViewAt(root, sDownRawX, sDownRawY);
                }
            } catch (Throwable ignored) {
            }
        }
    };

    static {
        try {
            Log.i(TAG, "Bridge loaded v44 pid=" + android.os.Process.myPid()
                    + " uid=" + android.os.Process.myUid()
                    + " (classes2.dex)");
        } catch (Throwable ignored) {
        }
    }

    private Bridge() {
    }

    /** Called at the very beginning of Interceptor.dispatchTouchEvent(). */
    private static void ensureShadowReceiver(Context context) {
        if (context == null || sShadowReceiverRegistered) {
            return;
        }
        try {
            IntentFilter filter = new IntentFilter("com.dragshare.bridge.SHADOW_STATE");
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(sShadowReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(sShadowReceiver, filter);
            }
            sShadowReceiverRegistered = true;
            Log.i(TAG, "shadow state receiver registered");
        } catch (Throwable throwable) {
            Log.w(TAG, "register shadow receiver failed", throwable);
        }
    }

    public static void ping(MotionEvent event, View view, Activity activity) {
        try {
            rememberContext(activity);
            Context appContext = activity != null ? activity.getApplicationContext() : currentContext();
            ensureShadowReceiver(appContext);
            ensureConfig(appContext);
            if (!sMasterSwitch || isBlacklisted(appContext) || (!sLandscapeTrigger && isLandscape(appContext))) {
                return;
            }
            if (activity != null) {
                sLastActivity = activity;
            }
            if (event != null) {
                int action = event.getActionMasked();
                float rawX = event.getRawX();
                float rawY = event.getRawY();
                if (action == MotionEvent.ACTION_DOWN) {
                    sGestureState.set(STATE_IDLE);
                    sMainHandler.removeCallbacks(sCustomLongPressRunnable);
                    sMainHandler.removeCallbacks(sWebPrefetchRunnable);
                    if (view != null) {
                        sLastTouchView = view;
                    }
                    sDownRawX = rawX;
                    sDownRawY = rawY;
                    sDragOriginX = rawX;
                    sDragOriginY = rawY;
                    sLastMoveRawX = rawX;
                    sLastMoveRawY = rawY;
                    sLastTouchPoint = new PointF(rawX, rawY);
                    sLastTouchAt = SystemClock.uptimeMillis();
                    sDownAt = sLastTouchAt;
                    sMovedSinceDown = false;
                    sPendingPayload = null;
                    sPendingContent = null;
                    sGestureActive = true;
                    sCancelSent = false;
                    resetRuleHit();
                    sActiveDragView = null;
                    sActiveShadowSquare = false;
                    sShadowMorphProgress = 0f;
                    cancelShadowMorphAnimator();
                    int generation = sGestureGeneration + 1;
                    sGestureGeneration = generation;
                    sPendingGeneration = generation;
                    sTriggeredGeneration = -1;
                    sDragStartedGeneration = -1;
                    sWebAwaitingGeneration = -1;
                    sWebAwaitingViewRef = null;
                    // Proactively prefetch WebView DOM/image under the finger 50ms into the press:
                    // short taps/flings cancel it without running JS, while long-presses get a
                    // ~750ms head-start for async evaluateJavascript + image download.
                    sMainHandler.postDelayed(sWebPrefetchRunnable, 50);
                    // Schedule fallback at sLongPressDurationMs + 150ms (after AES u/a's timer).
                    // If u/a fires, recordTouch() cancels this runnable so it never duplicates work;
                    // if u/a was cancelled by micro-tremor or a non-DecorView window, this fallback catches it cleanly.
                    int fallbackDelay = (sLongPressDurationMs > 0 ? sLongPressDurationMs : 800) + 150;
                    sMainHandler.postDelayed(sCustomLongPressRunnable, fallbackDelay);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    sLastMoveRawX = rawX;
                    sLastMoveRawY = rawY;
                    float dx = rawX - sDownRawX;
                    float dy = rawY - sDownRawY;
                    float dist = (float) Math.hypot(dx, dy);

                    float density = appContext != null ? appContext.getResources().getDisplayMetrics().density : 3.0f;
                    float slopPx = Math.max(8f, sTouchSlopDp * density);
                    // Anti-mistouch for the pre-long-press phase: drift beyond
                    // the circle cancels the pending long press.
                    if (dist > slopPx) {
                        sMovedSinceDown = true;
                        sMainHandler.removeCallbacks(sWebPrefetchRunnable);
                        if (sGestureState.get() == STATE_IDLE) {
                            sMainHandler.removeCallbacks(sCustomLongPressRunnable);
                        }
                    }
                    // Drag gate for the post-long-press phase: the same radius,
                    // but measured from the point where the long press
                    // completed. Inside the circle the shadow does not exist.
                    if (sPendingPayload != null && sDragOriginX >= 0f) {
                        float originDx = rawX - sDragOriginX;
                        float originDy = rawY - sDragOriginY;
                        if (Math.hypot(originDx, originDy) > slopPx) {
                            sMovedSinceDown = true;
                        }
                    }



                    if (sGestureState.get() == STATE_DRAGGING
                            || sDragStartedGeneration == sGestureGeneration) {
                        // System drag is already active or already attempted for this gesture
                    } else if (sMovedSinceDown && sPendingPayload != null) {
                        sMainHandler.removeCallbacks(sCustomLongPressRunnable);
                        Bundle pending = sPendingPayload;
                        Object pendingContent = sPendingContent;
                        sPendingPayload = null;
                        sPendingContent = null;
                        Log.i(TAG, "pending drag starts on move");
                        forwardToPortal(pending, pendingContent, true);
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    sMainHandler.removeCallbacks(sCustomLongPressRunnable);
                    sMainHandler.removeCallbacks(sWebPrefetchRunnable);
                    sGestureActive = false;
                    sWebAwaitingGeneration = -1;
                    sWebAwaitingViewRef = null;
                    if ((action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)
                            && (sGestureState.get() == STATE_DRAGGING || sActiveDragView != null)) {
                        // During active system drag, touch UP/CANCEL must NOT destroy drag context!
                        // System DragEvent.ACTION_DRAG_ENDED handles lifecycle cleanup.
                        Log.d(TAG, "ignore touch up/cancel while dragging");
                        return;
                    }
                    cancelShadowMorphAnimator();
                    sShadowMorphProgress = 0f;
                    sActiveDragView = null;
                    sActiveDragText = null;
                    sActiveDragBitmap = null;
                    sPendingGeneration = -1;
                    sGestureState.set(STATE_IDLE);
                    if (sPendingPayload != null) {
                        Log.i(TAG, "pending drag cancelled on up/cancel");
                        sPendingPayload = null;
                        sPendingContent = null;
                    }
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "ping failed", throwable);
        }
    }

    private static void onCustomLongPressTriggered() {
        try {
            Context ctx = currentContext();
            if (!sMasterSwitch || isBlacklisted(ctx) || (!sLandscapeTrigger && isLandscape(ctx))) {
                return;
            }
            if (!sGestureActive || sPendingGeneration != sGestureGeneration || sPendingPayload != null
                    || sTriggeredGeneration == sGestureGeneration
                    || sDragStartedGeneration == sGestureGeneration
                    || sGestureState.get() != STATE_IDLE || sActiveDragView != null) {
                return;
            }
            if (!sGestureState.compareAndSet(STATE_IDLE, STATE_TRIGGERED)) {
                return;
            }
            sPendingGeneration = -1;
            long startedAt = SystemClock.uptimeMillis();
            View root = sLastTouchView;
            Activity act = sLastActivity;
            if (root == null && act != null && act.getWindow() != null) {
                root = act.getWindow().getDecorView();
            }
            if (root == null) {
                sGestureState.set(STATE_PROCESSED);
                return;
            }
            Log.i(TAG, "customLongPress fallback triggered at " + sDownRawX + "," + sDownRawY
                    + " after " + (startedAt - sDownAt) + "ms");

            View touchTarget = findDeepTouchView(root, sDownRawX, sDownRawY);
            if (touchTarget == null) {
                touchTarget = root;
            }

            Bundle content = preExtract(touchTarget, 3, false);
            if (content == null || !isBundleMeaningful(content)) {
                View nonComposeTarget = findDeepTouchView(root, sDownRawX, sDownRawY, true /* skipCompose */);
                Log.i(TAG, "customLongPress pass-through: " + (nonComposeTarget == null ? "null" : nonComposeTarget.getClass().getName()));
                if (nonComposeTarget != null && nonComposeTarget != touchTarget) {
                    touchTarget = nonComposeTarget;
                    content = preExtract(touchTarget, 3, false);
                }
            }
            if ((content == null || !isBundleMeaningful(content)) && isWeChat(root)) {
                View wechatMsgView = findWeChatViewUnderTouch(root, sDownRawX, sDownRawY);
                if (wechatMsgView != null) {
                    Log.i(TAG, "customLongPress hit WeChat message view: " + wechatMsgView.getClass().getName());
                    touchTarget = wechatMsgView;
                    content = preExtract(touchTarget, 3, false);
                }
            }

            // USER EXPLICIT CONSTRAINT: "不要全量兜底截屏"
            if (content == null || !isBundleMeaningful(content)) {
                if (sWebAwaitingGeneration == sGestureGeneration) {
                    Log.i(TAG, "customLongPress: awaiting async webview extraction"
                            + " took=" + (SystemClock.uptimeMillis() - startedAt) + "ms");
                    sGestureState.set(STATE_IDLE);
                    return;
                }
                Log.i(TAG, "customLongPress: no real content found, aborting drag (no fallback screenshot)"
                        + " took=" + (SystemClock.uptimeMillis() - startedAt) + "ms");
                sGestureState.set(STATE_PROCESSED);
                return;
            }

            Log.i(TAG, "customLongPress: hit real content! type=" + content.getInt("catchType", 0)
                    + " text=" + content.getString("viewContent")
                    + " bitmap=" + describeBitmap(content)
                    + " took=" + (SystemClock.uptimeMillis() - startedAt) + "ms");

            sTriggeredGeneration = sGestureGeneration;
            content.putString("coordinate", ((int) sDownRawX) + "-" + ((int) sDownRawY));
            sLastTouchAt = SystemClock.uptimeMillis();
            if (touchTarget.isAttachedToWindow()) {
                sLastTouchView = touchTarget;
            }
            // Dismiss the app's text selection UI as soon as our long-press
            // enters drag mode.  WeChat's custom markdown selector otherwise
            // leaves its two green handle popups behind even if the drag never
            // reaches startSystemDrag().
            dismissTextSelectionUi(touchTarget);
            if (sMovedSinceDown) {
                sGestureState.set(STATE_DRAGGING);
                forwardToPortal(content, null, true);
            } else {
                // Long press completed here: this is the centre of the drag
                // circle the user has to pull out of.
                sDragOriginX = getTouchRawX();
                sDragOriginY = getTouchRawY();
                sPendingPayload = content;
                sPendingContent = null;
                sGestureState.set(STATE_IDLE);
                // Preview only: do not stage the image or start a system drag
                // until the finger actually moves. Staging here used to create
                // a second MediaStore copy for every stationary long-press.
                forwardToPortal(content, null, false);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "onCustomLongPressTriggered failed", throwable);
            sGestureState.set(STATE_PROCESSED);
        }
    }

    /** Called at the beginning of Interceptor.l(PointF, PointF, View). */
    public static void recordTouch(PointF first, PointF second, View view) {
        try {
            sMainHandler.removeCallbacks(sCustomLongPressRunnable);
            long elapsed = SystemClock.uptimeMillis() - sDownAt;
            if (sDownAt > 0 && elapsed < (sLongPressDurationMs - 100)) {
                Log.w(TAG, "recordTouch arrived too early (" + elapsed + "ms < " + sLongPressDurationMs
                        + "ms), ignoring premature AES trigger");
                return;
            }
            if (!sGestureActive || sTriggeredGeneration == sGestureGeneration
                    || sDragStartedGeneration == sGestureGeneration
                    || sPendingPayload != null || sGestureState.get() != STATE_IDLE) {
                Log.d(TAG, "recordTouch ignored (custom engine active or already triggered)");
                return;
            }
            // The pre-long-press anti-mistouch circle already cancelled our own
            // fallback timer; without this guard a short long-press-duration
            // lets AES' native 450ms trigger walk straight past the circle.
            if (sMovedSinceDown) {
                Log.i(TAG, "recordTouch ignored: finger moved beyond slop before long press");
                return;
            }
            sLastTouchPoint = first;
            sLastTouchView = view;
            sLastTouchAt = SystemClock.uptimeMillis();
            Log.i(TAG, "longPress first=" + first + " second=" + second
                    + " view=" + (view == null ? "null" : view.getClass().getName()));
        } catch (Throwable throwable) {
            Log.w(TAG, "recordTouch failed", throwable);
        }
    }

    /**
     * Called from the patched Interceptor.d ViewGroup check.
     *
     * ComposeView and WebView are ViewGroups but have no useful child Views.
     * Returning false makes AES pass them to fetchViewContent as leaf views,
     * where the original accessibility fallback (and later our extractors) can run.
     */
    public static boolean shouldHandleAsViewGroup(View view) {
        try {
            if (!(view instanceof android.view.ViewGroup)) {
                return false;
            }
            int kind = viewKind(view);
            if (kind != VIEW_KIND_NORMAL) {
                String name = view.getClass().getName();
                Log.i(TAG, "special host "
                        + (kind == VIEW_KIND_COMPOSE ? "compose"
                        : (kind == VIEW_KIND_WEBVIEW ? "webview" : "customCell"))
                        + " " + name);
                return false;
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "shouldHandleAsViewGroup failed", throwable);
        }
        return view instanceof android.view.ViewGroup;
    }

    /**
     * Classify a view once per class. The old implementation ran
     * Class.getDeclaredMethods()/getMethods() for every node of the window on
     * every long press, which is why simple pages could still stutter.
     */
    private static boolean isObfuscatedAndroidComposeView(View view) {
        return (view instanceof ViewGroup)
                && (view instanceof android.view.ViewTreeObserver.OnGlobalLayoutListener)
                && (view instanceof android.view.ViewTreeObserver.OnScrollChangedListener)
                && (view instanceof android.view.ViewTreeObserver.OnTouchModeChangeListener);
    }

    /**
     * Compose's AndroidViewsHandler is an interop container for AndroidView nodes.
     * It lives in androidx.compose.ui.platform, but it has no semantics owner. It is
     * also full-screen and transparent when no AndroidView is present, so a naive
     * top-down hit test selects it and extractCompose() then logs
     * "compose semanticsOwner miss on androidx.compose.ui.platform.e0".
     * Treat it as a native passthrough ViewGroup instead.
     */
    private static boolean isAndroidViewsHandler(View view) {
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        String name = view.getClass().getName();
        if (name.equals("androidx.compose.ui.platform.AndroidViewsHandler")
                || (name.startsWith("androidx.compose.ui.platform.") && name.endsWith(".e0"))) {
            return true;
        }
        // R8-renamed AndroidViewsHandler keeps these two unique method names.
        return findMethod(view.getClass(), "getHolderToLayoutNode") != null
                && findMethod(view.getClass(), "getLayoutNodeToHolder") != null;
    }

    private static int viewKind(View view) {
        if (view == null) {
            return VIEW_KIND_NORMAL;
        }
        Class<?> clazz = view.getClass();
        Integer cached = sViewKindCache.get(clazz);
        if (cached != null) {
            return cached;
        }
        int kind = VIEW_KIND_NORMAL;
        try {
            String name = clazz.getName();
            if (isAndroidViewsHandler(view)) {
                // Compose interop container: traverse its native children instead of
                // treating it as a semantics owner (it has none).
                kind = VIEW_KIND_NORMAL;
            } else if (name.contains("AndroidComposeView") || name.contains("compose.ui.platform")
                    || isObfuscatedAndroidComposeView(view)
                    || findMethod(clazz, "getSemanticsOwner") != null) {
                kind = VIEW_KIND_COMPOSE;
            } else if (isWebViewLikeRaw(view)) {
                kind = VIEW_KIND_WEBVIEW;
            } else if (name.contains("ChatMessageCell") || name.contains("BackupImageView")) {
                kind = VIEW_KIND_CUSTOM_CELL;
            }
        } catch (Throwable ignored) {
        }
        sViewKindCache.put(clazz, kind);
        return kind;
    }

    private static boolean isWebViewLike(View view) {
        return viewKind(view) == VIEW_KIND_WEBVIEW;
    }

    /**
     * ColorOS OplusWebViewDragProcessor.getWebViewMethod(View, ViewDragInfo) parity:
     * any View exposing a 2-parameter evaluateJavascript(String, CallbackInterface)
     * method is a WebView host (standard WebView, Tencent X5, WeChat MMWebView/XWeb,
     * UC WebView, CrossWalk, or obfuscated custom WebViews like "c" / "AnalysisBrowser").
     */
    private static Method findWebEvaluateMethod(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        Method cached = sWebEvalMethodCache.get(clazz);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(sWebEvalMethodMiss.get(clazz))) {
            return null;
        }
        try {
            for (Method method : clazz.getMethods()) {
                if ("evaluateJavascript".equals(method.getName())) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 2 && params[0] == String.class && params[1].isInterface()) {
                        method.setAccessible(true);
                        sWebEvalMethodCache.put(clazz, method);
                        return method;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        sWebEvalMethodMiss.put(clazz, Boolean.TRUE);
        return null;
    }

    private static boolean isWebViewLikeRaw(View view) {
        if (view instanceof WebView) {
            return true;
        }
        if (view == null) {
            return false;
        }
        String name = view.getClass().getName();
        if (name.contains("WebViewClient") || name.contains("WebChromeClient")) {
            return false;
        }
        return findWebEvaluateMethod(view.getClass()) != null;
    }

    /**
     * Resolves the actual View instance that implements evaluateJavascript when a
     * wrapper ViewGroup (or an inner surface child of a third-party WebView) is hit.
     */
    private static View resolveWebViewTarget(View view) {
        if (view == null) {
            return null;
        }
        if (isWebViewLike(view)) {
            return view;
        }
        View current = view;
        for (int depth = 0; depth < 3; depth++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) {
                break;
            }
            current = (View) parent;
            if (isWebViewLike(current)) {
                return current;
            }
        }
        if (view instanceof ViewGroup) {
            float rawX = getTouchRawX();
            float rawY = getTouchRawY();
            View childWeb = findWebViewInDescendants((ViewGroup) view, rawX, rawY, 0);
            if (childWeb != null) {
                return childWeb;
            }
        }
        return null;
    }

    private static View findWebViewInDescendants(ViewGroup group, float rawX, float rawY, int depth) {
        if (group == null || depth > 3 || group.getVisibility() != View.VISIBLE) {
            return null;
        }
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (rawX > 0 && rawY > 0 && !rectContainsPoint(child, rawX, rawY, 8)) {
                continue;
            }
            if (isWebViewLike(child)) {
                return child;
            }
            if (child instanceof ViewGroup) {
                View found = findWebViewInDescendants((ViewGroup) child, rawX, rawY, depth + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static View findComposeViewAt(View root, float rawX, float rawY) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return null;
        }
        int[] location = new int[2];
        root.getLocationOnScreen(location);
        if (rawX < location[0] || rawX > location[0] + root.getWidth()
                || rawY < location[1] || rawY > location[1] + root.getHeight()) {
            return null;
        }
        if (isComposeView(root)) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View found = findComposeViewAt(group.getChildAt(i), rawX, rawY);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void prefetchWebViewAt(View root, float rawX, float rawY) {
        try {
            View webView = findWebViewAt(root, rawX, rawY);
            if (webView != null) {
                Log.i(TAG, "prefetch found " + webView.getClass().getName()
                        + " at " + rawX + "," + rawY);
                prefetchWebView(webView, rawX, rawY);
            } else {
                Log.d(TAG, "prefetch no webview at " + rawX + "," + rawY);
            }
        } catch (Throwable throwable) {
            Log.d(TAG, "prefetchWebViewAt failed: " + throwable);
        }
    }

    private static View findWebViewAt(View root, float rawX, float rawY) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return null;
        }
        // Prefer the actual hit-tested view so native toolbars/overlays sitting
        // above a WebView are respected first.
        View deep = findDeepTouchView(root, rawX, rawY);
        if (deep != null) {
            View resolved = resolveWebViewTarget(deep);
            if (resolved != null) {
                return resolved;
            }
        }
        return findWebViewInTree(root, rawX, rawY);
    }

    private static View findWebViewInTree(View root, float rawX, float rawY) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return null;
        }
        int[] location = new int[2];
        root.getLocationOnScreen(location);
        if (rawX < location[0] || rawX > location[0] + root.getWidth()
                || rawY < location[1] || rawY > location[1] + root.getHeight()) {
            return null;
        }
        if (isWebViewLike(root)) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View found = findWebViewInTree(group.getChildAt(i), rawX, rawY);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * ColorOS OplusWebViewDragProcessor.getTouchTargetJsFunction(float, float) +
     * processView(View, MotionEvent, int, float, BiConsumer) port.
     */
    private static void prefetchWebView(View rawView, float rawX, float rawY) {
        try {
            final View webView = resolveWebViewTarget(rawView);
            if (webView == null) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            int generation = sGestureGeneration;
            WebCache previous = sWebCache.get(webView);
            if (previous != null && previous.generation == generation
                    && (previous.inFlight || previous.ready)
                    && Math.hypot(rawX - previous.rawX, rawY - previous.rawY) < 24f) {
                return;
            }
            float density = webView.getResources().getDisplayMetrics().density;
            if (density <= 0f) {
                density = 3.0f;
            }
            int[] location = new int[2];
            webView.getLocationOnScreen(location);
            final float cssX = (rawX - location[0]) / density;
            final float cssY = (rawY - location[1]) / density;

            final WebCache cache = new WebCache();
            cache.generation = generation;
            cache.rawX = rawX;
            cache.rawY = rawY;
            cache.cssX = cssX;
            cache.cssY = cssY;
            cache.density = density;
            cache.timestamp = now;
            cache.inFlight = true;
            cache.ready = false;
            sWebCache.put(webView, cache);

            // ColorOS OplusWebViewDragProcessor.getTouchTargetJsFunction(cssX, cssY)
            // Returns a 10-element JSON array:
            //   [0] text, [1] outerHTML, [2] imgSrc,
            //   [3] rect.x, [4] rect.y, [5] rect.width, [6] rect.height,
            //   [7] fontSize, [8] lineHeight, [9] isInArticle
            String js = String.format(Locale.US,
                    "javascript:(function(){"
                            + "try{"
                            + "var x=%.2f,y=%.2f,doc=document;"
                            + "var touchElement=doc.elementFromPoint(x,y);"
                            + "while(touchElement&&touchElement.tagName==='IFRAME'){"
                            + "try{var ir=touchElement.getBoundingClientRect();"
                            + "var idoc=touchElement.contentDocument||(touchElement.contentWindow&&touchElement.contentWindow.document);"
                            + "if(!idoc)break;x-=ir.left;y-=ir.top;doc=idoc;"
                            + "var inner=doc.elementFromPoint(x,y);if(!inner||inner===touchElement)break;"
                            + "touchElement=inner;}catch(ie){break;}}"
                            + "if(!touchElement)return null;"
                            + "var sel='';try{sel=(doc.getSelection?doc.getSelection().toString():(window.getSelection?window.getSelection().toString():'')).trim();}catch(se){}"
                            + "var imgSrc=touchElement.currentSrc||touchElement.src||(touchElement.getAttribute&&(touchElement.getAttribute('data-src')||touchElement.getAttribute('data-original')||touchElement.getAttribute('src')))||'';"
                            + "if(!imgSrc&&touchElement.querySelectorAll){"
                            + "var imgs=touchElement.querySelectorAll('img');"
                            + "for(var k=0;k<imgs.length;k++){"
                            + "var r=imgs[k].getBoundingClientRect();"
                            + "if(x>=r.left-4&&x<=r.right+4&&y>=r.top-4&&y<=r.bottom+4&&r.width>=24&&r.height>=24){"
                            + "var cand=imgs[k].currentSrc||imgs[k].src||(imgs[k].getAttribute&&(imgs[k].getAttribute('data-src')||imgs[k].getAttribute('data-original')))||'';"
                            + "if(cand){imgSrc=cand;touchElement=imgs[k];break;}}}}"
                            + "var tag=(touchElement.tagName||'').toUpperCase();"
                            + "var rawText='';"
                            + "if(sel){rawText=sel;}else if(tag!=='BODY'&&tag!=='HTML'){rawText=touchElement.innerText||touchElement.textContent||'';}"
                            + "var outer=(touchElement.outerHTML||'').substring(0,4096);"
                            + "var compStyles=window.getComputedStyle?window.getComputedStyle(touchElement,null):null;"
                            + "var bg=compStyles?(compStyles.getPropertyValue('background-image')||''):'';"
                            + "if(bg&&bg!=='none'&&outer.indexOf('url(')<0){outer=outer+' '+bg;}"
                            + "var rect=touchElement.getBoundingClientRect();"
                            + "var i=0,array=new Array();"
                            + "array[i++]=(rawText||'').substring(0,20000);"
                            + "array[i++]=outer;"
                            + "array[i++]=imgSrc;"
                            + "array[i++]=rect?rect.x:0;"
                            + "array[i++]=rect?rect.y:0;"
                            + "array[i++]=rect?rect.width:0;"
                            + "array[i++]=rect?rect.height:0;"
                            + "array[i++]=compStyles?(parseFloat(compStyles.getPropertyValue('font-size'))||0):0;"
                            + "array[i++]=compStyles?(parseFloat(compStyles.getPropertyValue('line-height'))||0):0;"
                            + "var isInArticle=false,parentEle=touchElement.parentElement;"
                            + "while(parentEle){"
                            + "if(parentEle.className==='article'||parentEle.id==='js_content'||parentEle.id==='mc-article'||parentEle.id==='article'){isInArticle=true;break;}"
                            + "parentEle=parentEle.parentElement;}"
                            + "array[i++]=isInArticle;"
                            + "return array;"
                            + "}catch(x){return null;}"
                            + "})()",
                    cssX, cssY);

            if (webView instanceof WebView) {
                ((WebView) webView).evaluateJavascript(js, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        handleWebResult(webView, cache, value, cssX, cssY);
                    }
                });
            } else {
                Method evalMethod = findWebEvaluateMethod(webView.getClass());
                if (evalMethod != null) {
                    Class<?> callbackType = evalMethod.getParameterTypes()[1];
                    Object proxy = Proxy.newProxyInstance(
                            callbackType.getClassLoader(),
                            new Class<?>[]{callbackType},
                            (proxyObject, proxyMethod, args) -> {
                                if ("onReceiveValue".equals(proxyMethod.getName())
                                        && args != null && args.length > 0) {
                                    handleWebResult(webView, cache,
                                            args[0] == null ? null : String.valueOf(args[0]),
                                            cssX, cssY);
                                }
                                return null;
                            });
                    evalMethod.invoke(webView, js, proxy);
                } else {
                    cache.inFlight = false;
                }
            }
        } catch (Throwable throwable) {
            Log.d(TAG, "prefetchWebView failed: " + throwable);
        }
    }

    private static String optCleanJsonString(JSONArray array, int index) {
        if (array == null || index < 0 || index >= array.length() || array.isNull(index)) {
            return null;
        }
        String val = array.optString(index, null);
        if (val == null || "null".equals(val) || "undefined".equals(val)) {
            return null;
        }
        return val;
    }

    private static String normalizeWebUrl(String rawUrl) {
        if (TextUtils.isEmpty(rawUrl)) {
            return null;
        }
        String url = rawUrl.trim();
        int quotStart = url.indexOf("&quot;");
        if (quotStart >= 0) {
            int quotEnd = url.lastIndexOf("&quot;");
            if (quotEnd > quotStart + 6) {
                url = url.substring(quotStart + 6, quotEnd).trim();
            } else {
                url = url.replace("&quot;", "").trim();
            }
        }
        if ((url.startsWith("\"") && url.endsWith("\""))
                || (url.startsWith("'") && url.endsWith("'"))) {
            if (url.length() >= 2) {
                url = url.substring(1, url.length() - 1).trim();
            }
        }
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        if (url.isEmpty() || "null".equals(url) || "undefined".equals(url) || "about:blank".equals(url)) {
            return null;
        }
        return url;
    }

    /**
     * ColorOS OplusWebViewDragProcessor.handleCallback(View, BiConsumer, String, float, float) port.
     */
    private static void handleWebResult(final View webView, final WebCache cache,
                                        String value, float cssX, float cssY) {
        try {
            if (value == null || value.isEmpty() || "null".equals(value)) {
                cache.inFlight = false;
                cache.ready = true;
                return;
            }
            String raw = value.trim();
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                raw = new JSONArray("[" + raw + "]").getString(0);
            }
            String text = null;
            String outerHTML = null;
            String imgSrc = null;
            if (raw.startsWith("[")) {
                JSONArray array = new JSONArray(raw);
                text = optCleanJsonString(array, 0);
                outerHTML = optCleanJsonString(array, 1);
                imgSrc = normalizeWebUrl(optCleanJsonString(array, 2));
                cache.rectX = (float) array.optDouble(3, 0);
                cache.rectY = (float) array.optDouble(4, 0);
                cache.rectW = (float) array.optDouble(5, 0);
                cache.rectH = (float) array.optDouble(6, 0);
                cache.fontSize = (float) array.optDouble(7, 0);
                cache.lineHeight = (float) array.optDouble(8, 0);
                cache.isInArticle = array.optBoolean(9, false);
            } else if (raw.startsWith("{")) {
                JSONObject json = new JSONObject(raw);
                text = json.optString("t", null);
                imgSrc = normalizeWebUrl(json.optString("s", null));
            }

            // ColorOS rule: when touchElement.src is empty, inspect outerHTML / background-image
            // for url(...). If the element ALSO has text, it is a styled text/button container
            // rather than a standalone image.
            if (TextUtils.isEmpty(imgSrc) && !TextUtils.isEmpty(outerHTML)) {
                Matcher matcher = WEB_CSS_URL_PATTERN.matcher(outerHTML);
                if (matcher.find()) {
                    String bgUrl = normalizeWebUrl(matcher.group(1));
                    if (!TextUtils.isEmpty(bgUrl)) {
                        if (!TextUtils.isEmpty(text) && !text.trim().isEmpty()) {
                            text = "";
                            imgSrc = "";
                        } else {
                            imgSrc = bgUrl;
                        }
                    }
                }
            }

            String trimmedText = text == null ? null : text.trim();
            // When the finger is directly on an image element (imgSrc present and no text on that img node),
            // treat as image; otherwise if meaningful text is present, treat as text.
            if (!TextUtils.isEmpty(trimmedText) && TextUtils.isEmpty(imgSrc)
                    && isMeaningfulUserText(trimmedText)) {
                cache.text = trimmedText;
                cache.src = null;
                cache.inFlight = false;
                cache.ready = true;
                cache.timestamp = SystemClock.uptimeMillis();
                Log.i(TAG, "webview hit text len=" + cache.text.length()
                        + " inArticle=" + cache.isInArticle + " font=" + cache.fontSize);
                onWebCacheReady(webView, cache);
                return;
            }

            if (!TextUtils.isEmpty(imgSrc)) {
                cache.src = imgSrc;
                cache.text = null;
                cache.timestamp = SystemClock.uptimeMillis();
                Log.i(TAG, "webview hit imgSrc=" + imgSrc
                        + " rect=" + cache.rectW + "x" + cache.rectH);
                startFetchWebImage(webView, cache, imgSrc);
                return;
            }

            if (!TextUtils.isEmpty(trimmedText) && isMeaningfulUserText(trimmedText)) {
                cache.text = trimmedText;
                cache.inFlight = false;
                cache.ready = true;
                cache.timestamp = SystemClock.uptimeMillis();
                Log.i(TAG, "webview fallback text len=" + cache.text.length());
                onWebCacheReady(webView, cache);
                return;
            }

            cache.inFlight = false;
            cache.ready = true;
        } catch (Throwable throwable) {
            cache.inFlight = false;
            cache.ready = true;
            Log.d(TAG, "webview callback failed: " + throwable);
        }
    }

    /**
     * ColorOS OplusWebViewDragProcessor.startDragImg / lambda$startDragImg$2 port,
     * extended to preserve animated GIF/WebP payloads and support data:/blob: URLs.
     */
    private static void startFetchWebImage(final View webView, final WebCache cache, final String imgSrc) {
        final long requestId = sWebRequestId.incrementAndGet();
        final Context appContext = webView.getContext() != null
                ? webView.getContext().getApplicationContext() : currentContext();
        String cookie = null;
        try {
            android.webkit.CookieManager cm = android.webkit.CookieManager.getInstance();
            if (cm != null && (imgSrc.startsWith("http://") || imgSrc.startsWith("https://"))) {
                cookie = cm.getCookie(imgSrc);
            }
        } catch (Throwable ignored) {
        }
        final String cookieHeader = cookie;

        sWebExecutor.execute(() -> {
            byte[] data = null;
            Bitmap decoded = null;
            String animatedPath = null;
            String mimeType = null;
            try {
                if (sWebRequestId.get() != requestId) {
                    return;
                }
                data = downloadWebImageBytes(appContext, imgSrc, cookieHeader);
                if (data != null && data.length > 0) {
                    decoded = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (isAnimatedHead(data)) {
                        File animFile = bytesToTempFile(appContext, data);
                        if (animFile != null && animFile.isFile()) {
                            animatedPath = animFile.getAbsolutePath();
                            mimeType = (data[0] == 0x52 && data[1] == 0x49) ? "image/webp" : "image/gif";
                        }
                    }
                }
            } catch (Throwable throwable) {
                Log.d(TAG, "downloadWebImageBytes failed: " + throwable);
            }

            final Bitmap finalBitmap = decoded != null ? toSoftwareBitmap(decoded) : null;
            final String finalAnimatedPath = animatedPath;
            final String finalMime = mimeType;
            sMainHandler.post(() -> {
                if (sWebRequestId.get() != requestId) {
                    return;
                }
                Bitmap bmp = finalBitmap;
                if (bmp == null) {
                    bmp = cropWebViewElement(webView, cache);
                }
                cache.bitmap = bmp;
                cache.animatedPath = finalAnimatedPath;
                cache.mimeType = finalMime;
                cache.inFlight = false;
                cache.ready = true;
                cache.timestamp = SystemClock.uptimeMillis();
                Log.i(TAG, "webview img ready bitmap="
                        + (bmp == null ? "null" : (bmp.getWidth() + "x" + bmp.getHeight()))
                        + " animated=" + (finalAnimatedPath != null)
                        + " src=" + imgSrc);
                onWebCacheReady(webView, cache);
            });
        });
    }

    private static byte[] downloadWebImageBytes(Context context, String imgSrc, String cookie) {
        if (TextUtils.isEmpty(imgSrc)) {
            return null;
        }
        try {
            if (imgSrc.startsWith("data:")) {
                int comma = imgSrc.indexOf(',');
                if (comma > 0) {
                    String meta = imgSrc.substring(0, comma);
                    String payload = imgSrc.substring(comma + 1);
                    if (meta.contains(";base64")) {
                        return android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                    }
                }
                return null;
            }
            if (imgSrc.startsWith("file://") || imgSrc.startsWith("/")) {
                File f = fileFromString(imgSrc);
                if (f != null && f.isFile() && f.length() > 0 && f.length() <= 32 * 1024 * 1024) {
                    try (FileInputStream in = new FileInputStream(f)) {
                        return readStreamBytes(in, 32 * 1024 * 1024);
                    }
                }
                return null;
            }
            if (imgSrc.startsWith("content://") && context != null) {
                try (InputStream in = context.getContentResolver().openInputStream(Uri.parse(imgSrc))) {
                    return readStreamBytes(in, 32 * 1024 * 1024);
                }
            }
            if (imgSrc.startsWith("http://") || imgSrc.startsWith("https://")) {
                java.net.URL url = new java.net.URL(imgSrc);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(4500);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
                conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
                if (!TextUtils.isEmpty(url.getHost())) {
                    conn.setRequestProperty("Referer", url.getProtocol() + "://" + url.getHost() + "/");
                }
                if (!TextUtils.isEmpty(cookie)) {
                    conn.setRequestProperty("Cookie", cookie);
                }
                try (InputStream in = conn.getInputStream()) {
                    return readStreamBytes(in, 32 * 1024 * 1024);
                } finally {
                    try {
                        conn.disconnect();
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable throwable) {
            Log.d(TAG, "downloadWebImageBytes error for " + imgSrc + ": " + throwable);
        }
        return null;
    }

    private static byte[] readStreamBytes(InputStream in, int maxBytes) throws Exception {
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > maxBytes) {
                break;
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * Element-level crop fallback (for blob: URLs or CORS/canvas-restricted web images)
     * using the exact DOM getBoundingClientRect() of the <img> element.
     */
    private static Bitmap cropWebViewElement(View webView, WebCache cache) {
        if (webView == null || cache == null || cache.rectW < 24 || cache.rectH < 24) {
            return null;
        }
        try {
            int vw = webView.getWidth();
            int vh = webView.getHeight();
            if (vw <= 0 || vh <= 0) {
                return null;
            }
            float d = cache.density > 0 ? cache.density : webView.getResources().getDisplayMetrics().density;
            int left = Math.max(0, Math.round(cache.rectX * d));
            int top = Math.max(0, Math.round(cache.rectY * d));
            int right = Math.min(vw, Math.round((cache.rectX + cache.rectW) * d));
            int bottom = Math.min(vh, Math.round((cache.rectY + cache.rectH) * d));
            int cw = right - left;
            int ch = bottom - top;
            if (cw < 32 || ch < 32) {
                return null;
            }
            Bitmap bmp = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.translate(-left - webView.getScrollX(), -top - webView.getScrollY());
            webView.draw(canvas);
            if (!bitmapHasContent(bmp)) {
                bmp.recycle();
                return null;
            }
            return bmp;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * ColorOS OplusWebViewDragProcessor$1.onClipDataReady(ClipData) parity:
     * if the long-press already fired while JS evaluation or image download was in-flight,
     * deliver the extracted Bundle as soon as it finishes on the main thread.
     */
    private static void onWebCacheReady(View webView, WebCache cache) {
        try {
            if (!sGestureActive || cache == null || cache.generation != sGestureGeneration) {
                return;
            }
            if (sWebAwaitingGeneration != cache.generation) {
                return;
            }
            if (sPendingPayload != null || sTriggeredGeneration == sGestureGeneration
                    || sDragStartedGeneration == sGestureGeneration
                    || sGestureState.get() == STATE_DRAGGING || sActiveDragView != null) {
                return;
            }
            sWebAwaitingGeneration = -1;
            sWebAwaitingViewRef = null;
            Bundle bundle = buildBundleFromWebCache(cache);
            if (bundle == null || !isBundleMeaningful(bundle)) {
                return;
            }
            sTriggeredGeneration = sGestureGeneration;
            sMainHandler.removeCallbacks(sCustomLongPressRunnable);
            bundle.putString("coordinate", ((int) sDownRawX) + "-" + ((int) sDownRawY));
            sLastTouchAt = SystemClock.uptimeMillis();
            if (webView != null && webView.isAttachedToWindow()) {
                sLastTouchView = webView;
            }
            dismissTextSelectionUi(webView);
            Log.i(TAG, "async webview ready -> delivering catchType=" + bundle.getInt("catchType", 0)
                    + " moved=" + sMovedSinceDown);
            if (sMovedSinceDown) {
                sGestureState.set(STATE_DRAGGING);
                forwardToPortal(bundle, null, true);
            } else {
                sDragOriginX = getTouchRawX();
                sDragOriginY = getTouchRawY();
                sPendingPayload = bundle;
                sPendingContent = null;
                sPendingGeneration = sGestureGeneration;
                sGestureState.set(STATE_IDLE);
                forwardToPortal(bundle, null, false);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "onWebCacheReady failed", throwable);
        }
    }

    private static Bundle buildBundleFromWebCache(WebCache cache) {
        if (cache == null || !cache.ready) {
            return null;
        }
        if (!TextUtils.isEmpty(cache.text) && isMeaningfulUserText(cache.text)) {
            Bundle bundle = new Bundle();
            bundle.putString("viewContent", cache.text);
            bundle.putInt("catchType", 1);
            return bundle;
        }
        if (cache.bitmap != null && !cache.bitmap.isRecycled()) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("viewBitmap", cache.bitmap);
            if (!TextUtils.isEmpty(cache.src)) {
                bundle.putString("imageUrl", cache.src);
                bundle.putString("imgUrl", cache.src);
            }
            if (!TextUtils.isEmpty(cache.animatedPath)) {
                bundle.putString("animatedSourcePath", cache.animatedPath);
                bundle.putString("mimeType", TextUtils.isEmpty(cache.mimeType) ? "image/gif" : cache.mimeType);
            }
            bundle.putInt("catchType", 2);
            return bundle;
        }
        return null;
    }

    private static Bundle webViewBundle(View view) {
        View target = resolveWebViewTarget(view);
        if (target == null) {
            return null;
        }
        float rawX = getTouchRawX();
        float rawY = getTouchRawY();
        WebCache cache = sWebCache.get(target);
        long now = SystemClock.uptimeMillis();
        if (cache == null || (cache.generation != sGestureGeneration && now - cache.timestamp > 1500)) {
            sWebAwaitingGeneration = sGestureGeneration;
            sWebAwaitingViewRef = new WeakReference<>(target);
            prefetchWebView(target, rawX, rawY);
            return null;
        }
        if (cache.inFlight && !cache.ready) {
            sWebAwaitingGeneration = sGestureGeneration;
            sWebAwaitingViewRef = new WeakReference<>(target);
            Log.i(TAG, "webViewBundle: async extraction in-flight for " + target.getClass().getName());
            return null;
        }
        return buildBundleFromWebCache(cache);
    }

    private static void clearWebViewSelection(View view) {
        View target = resolveWebViewTarget(view);
        if (target == null) {
            return;
        }
        try {
            String js = "javascript:(function(){try{if(window.getSelection)window.getSelection().removeAllRanges();}catch(e){}})()";
            if (target instanceof WebView) {
                ((WebView) target).evaluateJavascript(js, null);
            } else {
                Method m = findWebEvaluateMethod(target.getClass());
                if (m != null) {
                    m.invoke(target, js, null);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static final class WebCache {
        int generation;
        boolean inFlight;
        boolean ready;
        String text;
        String src;
        Bitmap bitmap;
        String animatedPath;
        String mimeType;
        float rawX;
        float rawY;
        float cssX;
        float cssY;
        float density;
        float rectX;
        float rectY;
        float rectW;
        float rectH;
        float fontSize;
        float lineHeight;
        boolean isInArticle;
        long timestamp;
    }

    /**
     * Called instead of ContentCatcherManager.getPageConfig(Token).
     * Returns a synthetic PageConfig when AES service is absent, and augments
     * an existing one with pick_mode / image_pick_mode when it is present.
     */
    public static Object getPageConfig(Object token) {
        try {
            Object config = null;
            try {
                Object manager = Class.forName("miui.contentcatcher.sdk.ContentCatcherManager")
                        .getMethod("getInstance").invoke(null);
                Class<?> tokenClass = Class.forName("miui.contentcatcher.sdk.Token");
                Method getPageConfig = manager.getClass().getMethod("getPageConfig", tokenClass);
                config = getPageConfig.invoke(manager, token);
            } catch (Throwable throwable) {
                // AES service is not running: fall through to synthetic config.
                Log.w(TAG, "system PageConfig unavailable: "
                        + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            }
            return augment(config);
        } catch (Throwable throwable) {
            Log.w(TAG, "getPageConfig failed", throwable);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object augment(Object config) throws Exception {
        Class<?> pageConfigClass = Class.forName("miui.contentcatcher.sdk.data.PageConfig");
        if (config == null) {
            config = pageConfigClass.newInstance();
            Log.i(TAG, "synthesised PageConfig (AES service absent)");
        }

        Object featuresObject = null;
        try {
            featuresObject = pageConfigClass.getMethod("getFeatures").invoke(config);
        } catch (Throwable ignored) {
        }
        List<Object> features;
        if (featuresObject instanceof List) {
            features = (List<Object>) featuresObject;
        } else {
            features = new ArrayList<>();
            try {
                pageConfigClass.getMethod("setFeatures", ArrayList.class).invoke(config, features);
            } catch (Throwable ignored) {
            }
        }

        addFeature(features, "pick_mode", "text_pick", "NativeTextCatcher");
        addFeature(features, "image_pick_mode", "image_pick", "ImageViewCatcher");
        Log.i(TAG, "PageConfig features=" + features.size());
        return config;
    }

    private static void addFeature(List<Object> features, String name, String jobTag, String catchers)
            throws Exception {
        for (Object feature : features) {
            if (name.equals(getField(feature, "name"))) {
                setField(feature, "enable", Boolean.TRUE);
                setField(feature, "jobTag", jobTag);
                Object params = getField(feature, "mParams");
                if (params instanceof Map) {
                    ((Map<String, Object>) params).put("catchers", catchers);
                }
                return;
            }
        }
        Class<?> featureClass = Class.forName("miui.contentcatcher.sdk.data.FeatureInfo");
        Object feature = featureClass.newInstance();
        setField(feature, "name", name);
        setField(feature, "target", "com.miui.contentextension");
        setField(feature, "jobTag", jobTag);
        setField(feature, "enable", Boolean.TRUE);
        Object params = getField(feature, "mParams");
        if (params instanceof Map) {
            ((Map<String, Object>) params).put("catchers", catchers);
        }
        features.add(feature);
    }

    /** Called before the original AES fetcher, so WebView/Compose never crash it. */
    public static Bundle preExtract(View view, int flags, boolean needHint) {
        try {
            if (view == null) {
                return null;
            }
            rememberContext(view.getContext());
            if (!isTouchInsideView(view)) {
                return null;
            }
            // A tagged file item is cheap to detect and should keep working even
            // when the host class name does not say "File"/"Image".
            if (isFileValue(view.getTag())) {
                Bundle file = extractFile(view);
                if (file != null && isBundleMeaningful(file)) {
                    return file;
                }
            }
            if (isWeChat(view)) {
                Bundle wechatFile = extractWeChatFile(view);
                if (wechatFile != null && isBundleMeaningful(wechatFile)) {
                    Log.i(TAG, "preExtract hit WeChat file: " + wechatFile.getString("fileName"));
                    return wechatFile;
                }
            }
            // Rule first (after the app-specific file shortcuts): a per-app rule
            // pins this view class to one modality, so the heuristic cascade
            // below never gets the chance to swap image for caption text.
            Bundle ruled = applyRule(view, flags);
            if (ruled != null) {
                return ruled;
            }
            if (!isPlausibleContentHost(view)) {
                return null;
            }
            Bundle result = extractContentColorOS(view, flags);
            if (result != null && isBundleMeaningful(result)) {
                attachAnimatedSource(view, result);
                Log.d(TAG, "preExtract hit type=" + result.getInt("catchType", 0)
                        + " view=" + view.getClass().getName());
                return result;
            }
            return null;
        } catch (Throwable throwable) {
            Log.w(TAG, "preExtract failed", throwable);
            return null;
        }
    }

    /**
     * True when the view can plausibly own the content under the finger.
     * This is deliberately cheap: AES walks every leaf of the window during a
     * long press, so a full extraction attempt per leaf is what made even
     * simple pages feel slow.
     */
    private static boolean isPlausibleContentHost(View view) {
        if (view == null) {
            return false;
        }
        if (view instanceof TextView || view instanceof ImageView || view instanceof TextureView
                || view instanceof ViewGroup) {
            return true;
        }
        int kind = viewKind(view);
        if (kind == VIEW_KIND_COMPOSE || kind == VIEW_KIND_WEBVIEW || kind == VIEW_KIND_CUSTOM_CELL) {
            return true;
        }
        if (!TextUtils.isEmpty(view.getContentDescription())) {
            return true;
        }
        String name = view.getClass().getName();
        return name.contains("Text") || name.contains("Label") || name.contains("Title")
                || name.contains("Caption") || isImageLikeView(view);
    }

    private static boolean isTouchInsideView(View view) {
        if (view == null) {
            return false;
        }
        float rawX = getTouchRawX();
        float rawY = getTouchRawY();
        if (rawX <= 0f && rawY <= 0f) {
            return true;
        }
        try {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            return rawX >= location[0] - 48
                    && rawX <= location[0] + view.getWidth() + 48
                    && rawY >= location[1] - 48
                    && rawY <= location[1] + view.getHeight() + 48;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean isImageLikeView(View view) {
        if (view == null) {
            return false;
        }
        if (view instanceof ImageView || view instanceof TextureView) {
            return true;
        }
        String name = view.getClass().getName();
        return name.contains("Image") || name.contains("Pic") || name.contains("Photo")
                || name.contains("Gallery") || name.contains("Preview") || name.contains("Thumb");
    }

    private static boolean isTextLikeView(View view) {
        if (view == null) {
            return false;
        }
        if (view instanceof TextView) {
            return true;
        }
        String name = view.getClass().getName();
        return name.contains("Text") || name.contains("Label")
                || name.contains("Title") || name.contains("Caption");
    }

    private static Bundle extractFile(View view) {
        if (view == null) {
            return null;
        }
        try {
            Object candidate = findFileCandidate(view);
            if (candidate == null) {
                String remoteUrl = findWeiboOriginalUrl(view);
                if (remoteUrl != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("imageUrl", remoteUrl);
                    bundle.putInt("catchType", 2);
                    Bitmap preview = extractImageBitmap(view);
                    if (preview != null) {
                        bundle.putParcelable("viewBitmap", preview);
                    }
                    Log.i(TAG, "weibo remote image url hit " + remoteUrl);
                    return bundle;
                }
                return null;
            }
            Uri uri = null;
            File file = null;
            ParcelFileDescriptor directPfd = null;
            String name = null;
            if (candidate instanceof Uri) {
                uri = (Uri) candidate;
                name = uri.getLastPathSegment();
            } else if (candidate instanceof File) {
                file = (File) candidate;
                name = file.getName();
            } else if (candidate instanceof ParcelFileDescriptor) {
                directPfd = (ParcelFileDescriptor) candidate;
                name = "shared_image";
            } else if (candidate instanceof String) {
                String value = (String) candidate;
                if (value.startsWith("content://")) {
                    uri = Uri.parse(value);
                    name = uri.getLastPathSegment();
                } else if (value.startsWith("file://")) {
                    uri = Uri.parse(value);
                    name = uri.getLastPathSegment();
                } else {
                    file = new File(value);
                    name = file.getName();
                }
            }
            ParcelFileDescriptor pfd = directPfd != null
                    ? directPfd : openPfd(view.getContext(), uri, file);
            if (pfd == null) {
                return null;
            }
            long statSize = -1;
            try {
                statSize = pfd.getStatSize();
            } catch (Throwable ignored) {
            }
            if (statSize == 0) {
                // An empty 0-byte file must not shadow the Bitmap fallback
                try {
                    pfd.close();
                } catch (Throwable ignored) {
                }
                return null;
            }
            String mime = null;
            try {
                if (uri != null) {
                    mime = view.getContext().getContentResolver().getType(uri);
                }
            } catch (Throwable ignored) {
            }
            if (TextUtils.isEmpty(mime) && name != null) {
                mime = URLConnection.guessContentTypeFromName(name);
            }
            if (TextUtils.isEmpty(name)) {
                name = "shared_file";
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable("filePfd", pfd);
            bundle.putString("fileName", name);
            bundle.putString("mimeType", mime == null ? "application/octet-stream" : mime);
            bundle.putLong("fileSize", statSize);
            bundle.putInt("catchType", 2);
            // Keep a small in-memory preview so the drag shadow can follow the
            // finger; the PFD still carries the untouched original file.
            if (isImageLikeView(view)) {
                try {
                    Bitmap preview = extractImageBitmap(view);
                    if (preview != null) {
                        bundle.putParcelable("viewBitmap", scaleDown(preview, 720));
                    }
                } catch (Throwable ignored) {
                }
            }
            Log.i(TAG, "file hit " + name + " size=" + bundle.getLong("fileSize", -1)
                    + " preview=" + describeBitmap(bundle));
            return bundle;
        } catch (Throwable throwable) {
            Log.d(TAG, "extractFile failed: " + throwable);
            return null;
        }
    }

    private static String findWeiboOriginalUrl(View view) {
        try {
            if (view == null || view.getContext() == null
                    || !"com.sina.weibo".equals(view.getContext().getPackageName())) {
                return null;
            }
            for (Class<?> type = view.getClass(); type != null && type != Object.class;
                 type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (!"ae".equals(field.getName())) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object picInfo = field.get(view);
                    if (picInfo == null) {
                        continue;
                    }
                    for (String methodName : new String[]{
                            "getOriginalUrl", "getLargestUrl", "getLargeUrl",
                            "getMw2000Url", "getPicBigUrl", "getLocalPath"}) {
                        try {
                            Method method = picInfo.getClass().getMethod(methodName);
                            Object value = method.invoke(picInfo);
                            if (value instanceof String) {
                                String url = (String) value;
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    return url;
                                }
                                if (methodName.equals("getLocalPath")
                                        && url.startsWith("/") && new File(url).isFile()) {
                                    return url;
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object findFileCandidate(View view) {
        View current = view;
        for (int depth = 0; current != null && depth < 3; depth++) {
            Object tag = current.getTag();
            if (isFileValue(tag)) {
                return tag;
            }
            // Some apps (Weibo's LargeImageView2, for example) keep the source
            // Uri in an obfuscated field. Typed traversal recovers it without
            // depending on the field name.
            if (isImageLikeView(current) || isFileLikeView(current)) {
                Object typed = findTypedFileValue(current);
                if (typed != null) {
                    return typed;
                }
            }
            Class<?> type = current.getClass();
            while (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    try {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        String name = field.getName().toLowerCase(Locale.US);
                        if (!name.contains("uri") && !name.contains("path") && !name.contains("file")
                                && !name.contains("url") && !name.contains("src")
                                && !name.contains("data")) {
                            continue;
                        }
                        Class<?> fieldType = field.getType();
                        if (fieldType != Uri.class && fieldType != File.class
                                && fieldType != ParcelFileDescriptor.class
                                && fieldType != String.class) {
                            continue;
                        }
                        field.setAccessible(true);
                        Object value = field.get(current);
                        if (isFileValue(value)) {
                            return value;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                type = type.getSuperclass();
            }
            if (!(current.getParent() instanceof View)) {
                break;
            }
            current = (View) current.getParent();
        }
        return null;
    }

    private static Object findTypedFileValue(View view) {
        Object pfdFallback = null;
        Class<?> type = view.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    Class<?> fieldType = field.getType();
                    if (fieldType != Uri.class && fieldType != File.class
                            && fieldType != ParcelFileDescriptor.class) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(view);
                    if (!isFileValue(value)) {
                        continue;
                    }
                    if (value instanceof Uri || value instanceof File) {
                        return value;
                    }
                    if (pfdFallback == null) {
                        pfdFallback = value;
                    }
                } catch (Throwable ignored) {
                }
            }
            // Obfuscated getters are common; only accept zero-arg getters that
            // return a file-ish type, never arbitrary Strings.
            for (Method method : type.getDeclaredMethods()) {
                try {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers())
                            || method.getParameterCount() != 0) {
                        continue;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType != Uri.class && returnType != File.class
                            && returnType != ParcelFileDescriptor.class) {
                        continue;
                    }
                    method.setAccessible(true);
                    Object value = method.invoke(view);
                    if (!isFileValue(value)) {
                        continue;
                    }
                    if (value instanceof Uri || value instanceof File) {
                        return value;
                    }
                    if (pfdFallback == null) {
                        pfdFallback = value;
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return pfdFallback;
    }

    private static boolean isFileLikeView(View view) {
        if (view == null) {
            return false;
        }
        String name = view.getClass().getName();
        return name.contains("File") || name.contains("Video") || name.contains("Media");
    }

    private static boolean isFileValue(Object value) {
        if (value instanceof Uri) {
            Uri uri = (Uri) value;
            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                String path = uri.getPath();
                if (path != null) {
                    try {
                        File f = new File(path);
                        return f.isFile() && f.length() > 0;
                    } catch (Throwable ignored) {
                        return false;
                    }
                }
                return false;
            }
            return "content".equals(scheme);
        }
        if (value instanceof File) {
            File f = (File) value;
            return f.isFile() && f.length() > 0;
        }
        if (value instanceof ParcelFileDescriptor) {
            try {
                ParcelFileDescriptor p = (ParcelFileDescriptor) value;
                return p.getFileDescriptor() != null && p.getStatSize() != 0;
            } catch (Throwable ignored) {
                return false;
            }
        }
        if (value instanceof String) {
            String string = (String) value;
            if (string.length() >= 4096) return false;
            if (string.startsWith("content://")) return true;
            if (string.startsWith("file://")) {
                try {
                    Uri u = Uri.parse(string);
                    String path = u.getPath();
                    if (path != null) {
                        File f = new File(path);
                        return f.isFile() && f.length() > 0;
                    }
                } catch (Throwable ignored) {
                    return false;
                }
            }
            if (string.startsWith("/") && string.indexOf('\n') < 0) {
                try {
                    File f = new File(string);
                    return f.isFile() && f.length() > 0;
                } catch (Throwable ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private static ParcelFileDescriptor openPfd(Context context, Uri uri, File file) {
        try {
            if (file != null) {
                if (!file.isFile() || file.length() <= 0) return null;
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            }
            if (uri == null || context == null) {
                return null;
            }
            if ("file".equals(uri.getScheme())) {
                String path = uri.getPath();
                if (path == null) return null;
                File f = new File(path);
                if (!f.isFile() || f.length() <= 0) return null;
                return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
            }
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null && pfd.getStatSize() == 0) {
                try {
                    pfd.close();
                } catch (Throwable ignored) {
                }
                return null;
            }
            return pfd;
        } catch (Throwable throwable) {
            Log.d(TAG, "openPfd failed: " + throwable);
            return null;
        }
    }


    private static float getTouchRawX() {
        if (sLastTouchPoint != null && sLastTouchPoint.x > 0) {
            return sLastTouchPoint.x;
        }
        return sDownRawX;
    }

    private static float getTouchRawY() {
        if (sLastTouchPoint != null && sLastTouchPoint.y > 0) {
            return sLastTouchPoint.y;
        }
        return sDownRawY;
    }

    /**
     * Replicated ColorOS Content Extraction Pipeline:
     *   1. Jetpack Compose: Semantics tree traversal, priority sorting (text > image > deeper > smaller),
     *      image crop from compose view, accessibility node fallback.
     *   2. WebView: document.elementFromPoint, innerText / textContent, src, CSS backgroundImage.
     *   3. TextView: full getText(), contentDescription fallback, WeChat sender prefix stripping, password filtering.
     *   4. ImageView: size filtering (>=150x150), app-specific thumbnail ID blacklisting (WeChat, XHS, Weibo, Douyin, Bilibili, Taobao).
     */
    public static Bundle enhanceFetchViewContent(View view, int flags, boolean needHint, Bundle origin) {
        try {
            if (sGestureState.get() == STATE_DRAGGING) {
                return null;
            }
            if (isWebViewLike(view)) {
                return webViewBundle(view);
            }
            // The stock fetcher already found content. Keep its result instead
            // of running a second, potentially expensive extraction pass.
            if (origin != null && isBundleMeaningful(origin)) {
                return sanitizeBundle(origin);
            }
            if (view != null) {
                rememberContext(view.getContext());
            }
            // AES calls fetchViewContent for every leaf of the window. Keep the
            // non-candidates on a microsecond path and preserve any result the
            // stock fetcher already produced.
            if (!isPlausibleContentHost(view) || !isTouchInsideView(view)) {
                return sanitizeBundle(origin);
            }
            Bundle result = extractContentColorOS(view, flags | 2);
            if (result != null && isBundleMeaningful(result)) {
                Log.i(TAG, "enhance hit " + result.getString("viewContent", "")
                        + " catchType=" + result.getInt("catchType", 0)
                        + " bitmap=" + describeBitmap(result)
                        + " view=" + view.getClass().getName());
                return result;
            }
            Log.i(TAG, "enhance miss view=" + (view == null ? "null" : view.getClass().getName())
                    + " flags=" + flags
                    + " origin=" + (origin == null ? "null" : origin.getString("viewContent")));
            return sanitizeBundle(origin);
        } catch (Throwable throwable) {
            Log.w(TAG, "enhance failed", throwable);
            return sanitizeBundle(origin);
        }
    }

    private static boolean isBundleMeaningful(Bundle bundle) {
        if (bundle == null) return false;
        if (bundle.getParcelable("filePfd") != null) {
            return true;
        }
        if (bundle.getParcelable("viewBitmap") != null) {
            return true;
        }
        String text = bundle.getString("viewContent");
        if (!TextUtils.isEmpty(text)) {
            return isMeaningfulUserText(text);
        }
        if (!TextUtils.isEmpty(bundle.getString("imgUrl"))) {
            return true;
        }
        if (!TextUtils.isEmpty(bundle.getString("imageUrl"))) {
            return true;
        }
        return false;
    }

    private static Bundle sanitizeBundle(Bundle bundle) {
        if (bundle == null) return null;
        String text = bundle.getString("viewContent");
        if (!TextUtils.isEmpty(text)) {
            if (!isMeaningfulUserText(text)) {
                Log.i(TAG, "sanitized junk text from origin: '" + text + "'");
                bundle.remove("viewContent");
                if (bundle.getParcelable("viewBitmap") == null
                        && bundle.getParcelable("filePfd") == null
                        && TextUtils.isEmpty(bundle.getString("imgUrl"))) {
                    return null;
                }
            }
        }
        return isBundleMeaningful(bundle) ? bundle : null;
    }

    private static Bundle extractTelegramContent(View view, float rawX, float rawY, boolean wantText, boolean wantBitmap) {
        if (view == null) return null;
        String name = view.getClass().getName();
        if (!name.contains("ChatMessageCell") && !name.contains("BackupImageView")
                && !name.contains("PhotoViewer")) {
            return null;
        }
        try {
            // Telegram's full-screen viewer draws the photo itself and exposes a
            // caption elsewhere in the tree, so clicking the picture used to
            // return that caption. A viewer with a bitmap always means "image".
            if (name.contains("PhotoViewer")) {
                Bitmap viewerBitmap = extractTelegramViewerBitmap(view);
                if (viewerBitmap != null && !viewerBitmap.isRecycled()
                        && viewerBitmap.getWidth() >= 60 && viewerBitmap.getHeight() >= 60) {
                    Bundle b = new Bundle();
                    b.putParcelable("viewBitmap", toSoftwareBitmap(viewerBitmap));
                    b.putInt("catchType", 2);
                    Log.i(TAG, "telegram hit viewer photo " + viewerBitmap.getWidth() + "x" + viewerBitmap.getHeight());
                    return b;
                }
            }
            // A Telegram message cell holds BOTH the photo and its caption, so the
            // modality must follow the finger position, not a fixed order. Asking
            // for text first is what made "long press the picture" drag the caption.
            Rect photoRect = wantBitmap ? telegramPhotoRect(view) : null;
            boolean onPhoto = photoRect != null && rectContainsLocalPoint(view, photoRect, rawX, rawY);
            if (photoRect != null) {
                Log.i(TAG, "telegram photoRect=" + photoRect.toShortString() + " onPhoto=" + onPhoto);
            } else {
                logTelegramDiagnostics(view);
            }
            if (onPhoto) {
                Bitmap bmp = extractTelegramBitmap(view);
                if (bmp != null && !bmp.isRecycled() && bmp.getWidth() >= 60 && bmp.getHeight() >= 60) {
                    Bundle b = new Bundle();
                    b.putParcelable("viewBitmap", toSoftwareBitmap(bmp));
                    b.putInt("catchType", 2);
                    Log.i(TAG, "telegram hit photo (touch on image) " + bmp.getWidth() + "x" + bmp.getHeight());
                    return b;
                }
            } else if (wantBitmap && photoRect == null) {
                // photoImage coords are not always populated; fall back to the
                // inverse test — if the finger is outside the text block of a
                // message that does have a picture, the user pressed the picture.
                Rect textRect = telegramTextRect(view);
                if (textRect != null) {
                    boolean onText = rectContainsLocalPoint(view, textRect, rawX, rawY);
                    Log.i(TAG, "telegram textRect=" + textRect.toShortString() + " onText=" + onText);
                    if (!onText) {
                        Bitmap bmp = extractTelegramBitmap(view);
                        if (bmp != null && !bmp.isRecycled() && bmp.getWidth() >= 60 && bmp.getHeight() >= 60) {
                            Bundle b = new Bundle();
                            b.putParcelable("viewBitmap", toSoftwareBitmap(bmp));
                            b.putInt("catchType", 2);
                            Log.i(TAG, "telegram hit photo (touch outside text block) "
                                    + bmp.getWidth() + "x" + bmp.getHeight());
                            return b;
                        }
                    }
                }
            }
            // 1. Text extraction from Telegram message
            if (wantText) {
                String text = extractTelegramText(view);
                if (!TextUtils.isEmpty(text) && isMeaningfulUserText(text)) {
                    Bundle b = new Bundle();
                    b.putString("viewContent", text);
                    b.putInt("catchType", 1);
                    Log.i(TAG, "telegram hit text=" + text.length() + " chars");
                    return b;
                }
            }
            // 2. Image extraction from Telegram photoImage
            if (wantBitmap) {
                Bitmap bmp = extractTelegramBitmap(view);
                if (bmp != null && !bmp.isRecycled() && bmp.getWidth() >= 60 && bmp.getHeight() >= 60) {
                    Bundle b = new Bundle();
                    b.putParcelable("viewBitmap", toSoftwareBitmap(bmp));
                    b.putInt("catchType", 2);
                    Log.i(TAG, "telegram hit photo " + bmp.getWidth() + "x" + bmp.getHeight());
                    return b;
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "extractTelegramContent failed", throwable);
        }
        return null;
    }

    /**
     * One-shot diagnostic: dumps the real field names of the image receiver and
     * of the cell, because Telegram renames/relocates them between versions.
     */
    private static void logTelegramDiagnostics(View view) {
        try {
            Log.i(TAG, "telegram diag cell=" + view.getClass().getName()
                    + " size=" + view.getWidth() + "x" + view.getHeight());
            for (Class<?> c = view.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    String type = f.getType().getName();
                    String fName = f.getName().toLowerCase(Locale.US);
                    if (f.getType() == int.class || f.getType() == float.class) {
                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                        if (fName.contains("text") || fName.contains("image") || fName.contains("photo")) {
                            try {
                                f.setAccessible(true);
                                Object v = f.getType() == int.class ? (Object) f.getInt(view) : (Object) f.getFloat(view);
                                Log.i(TAG, "   cell." + f.getName() + "=" + v);
                            } catch (Throwable ignored) {
                            }
                        }
                        continue;
                    }
                    if (!type.contains("ImageReceiver") && !type.contains("image")) continue;
                    Log.i(TAG, "telegram diag field " + c.getSimpleName() + "." + f.getName() + " : " + type);
                    try {
                        f.setAccessible(true);
                        Object receiver = f.get(view);
                        if (receiver == null) {
                            Log.i(TAG, "   (null)");
                            continue;
                        }
                        StringBuilder sb = new StringBuilder("   recv");
                        for (String rn : new String[]{"imageX", "imageY", "imageW", "imageH"}) {
                            sb.append(' ').append(rn).append('=').append(floatFieldOf(receiver, rn));
                        }
                        sb.append(" bitmap=").append(describeReceiverBitmap(receiver));
                        Log.i(TAG, sb.toString());
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "logTelegramDiagnostics failed", throwable);
        }
    }

    private static float floatFieldOf(Object target, String name) {
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                if (f.getType() == float.class) return f.getFloat(target);
                if (f.getType() == int.class) return f.getInt(target);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return -1f;
            }
        }
        return -1f;
    }

    private static String describeReceiverBitmap(Object receiver) {
        try {
            Bitmap b = extractBitmapFromReceiver(receiver);
            return b == null ? "none" : b.getWidth() + "x" + b.getHeight();
        } catch (Throwable ignored) {
            return "err";
        }
    }

    /**
     * Bitmap of a Telegram full-screen viewer. The viewer draws with its own
     * canvas and keeps the ImageReceivers on the *outer* PhotoViewer instance
     * (centre / left / right page), so the cell-level lookup finds nothing.
     */
    private static Bitmap extractTelegramViewerBitmap(View view) {
        Bitmap direct = extractTelegramBitmap(view);
        if (direct != null && !direct.isRecycled()) {
            return direct;
        }
        try {
            Object outer = null;
            for (Class<?> c = view.getClass(); c != null && c != Object.class && outer == null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    String fName = f.getName();
                    boolean looksOuter = fName.startsWith("this$")
                            || f.getType().getName().contains("PhotoViewer");
                    if (!looksOuter) continue;
                    f.setAccessible(true);
                    Object value = f.get(view);
                    if (value != null && value.getClass().getName().contains("PhotoViewer")) {
                        outer = value;
                        break;
                    }
                }
            }
            if (outer == null) {
                Log.i(TAG, "telegram viewer: no outer PhotoViewer found");
                return null;
            }
            Bitmap best = null;
            for (Class<?> c = outer.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!f.getType().getName().contains("ImageReceiver")) continue;
                    try {
                        f.setAccessible(true);
                        Bitmap b = extractBitmapFromReceiver(f.get(outer));
                        if (b != null && !b.isRecycled()
                                && (best == null || b.getWidth() * b.getHeight() > best.getWidth() * best.getHeight())) {
                            best = b;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            return best;
        } catch (Throwable throwable) {
            Log.w(TAG, "extractTelegramViewerBitmap failed", throwable);
            return null;
        }
    }

    /** Photo rectangle of a Telegram message cell, in cell-local coordinates. */
    private static Rect telegramPhotoRect(View view) {
        try {
            Object receiver = findTelegramImageReceiver(view);
            if (receiver == null) return null;
            // ImageReceiver coordinates are float in current Telegram builds and
            // int in older ones — read either, never assume.
            float x = floatFieldOf(receiver, "imageX");
            float y = floatFieldOf(receiver, "imageY");
            float w = floatFieldOf(receiver, "imageW");
            float h = floatFieldOf(receiver, "imageH");
            if (w < 1f || h < 1f) return null;
            return new Rect((int) x, (int) y, (int) (x + w), (int) (y + h));
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Caption/text rectangle of a Telegram message cell, cell-local. */
    private static Rect telegramTextRect(View view) {
        float x = floatFieldOf(view, "textX");
        float y = floatFieldOf(view, "textY");
        float w = floatFieldOf(view, "textLayoutWidth");
        float h = floatFieldOf(view, "textLayoutHeight");
        if (w < 1f || h < 1f) {
            return null;
        }
        return new Rect((int) x, (int) y, (int) (x + w), (int) (y + h));
    }

    private static Object findTelegramImageReceiver(View view) {
        try {
            for (Method m : view.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String mName = m.getName().toLowerCase(Locale.US);
                if (mName.equals("getimagereceiver") || mName.equals("getphotoimage")) {
                    m.setAccessible(true);
                    Object receiver = m.invoke(view);
                    if (receiver != null) return receiver;
                }
            }
            for (Class<?> c = view.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    String fName = f.getName().toLowerCase(Locale.US);
                    if (fName.contains("photoimage") || fName.contains("imagereceiver")) {
                        f.setAccessible(true);
                        Object receiver = f.get(view);
                        if (receiver != null) return receiver;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int intFieldOf(Object target, String name) {
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.getInt(target);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return 0;
            }
        }
        return 0;
    }

    /** True when the raw touch point falls inside {@code localRect} of {@code view}. */
    private static boolean rectContainsLocalPoint(View view, Rect localRect, float rawX, float rawY) {
        if (view == null || localRect == null) return false;
        try {
            int[] loc = new int[2];
            view.getLocationOnScreen(loc);
            float lx = rawX - loc[0] + view.getScrollX();
            float ly = rawY - loc[1] + view.getScrollY();
            return lx >= localRect.left - 8 && lx <= localRect.right + 8
                    && ly >= localRect.top - 8 && ly <= localRect.bottom + 8;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String extractTelegramText(View view) {
        try {
            for (Class<?> c = view.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    String fName = f.getName().toLowerCase(Locale.US);
                    if (fName.contains("messageobject") || fName.equals("currentmessageobject")) {
                        f.setAccessible(true);
                        Object msgObj = f.get(view);
                        if (msgObj != null) {
                            for (Field mf : msgObj.getClass().getFields()) {
                                String mfName = mf.getName();
                                if (mfName.equals("messageText") || mfName.equals("caption")) {
                                    Object txt = mf.get(msgObj);
                                    if (txt != null && isMeaningfulUserText(txt.toString())) {
                                        return txt.toString();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // =========================================================================
    // ColorOS-Parity WeChat File Extraction (OplusFileViewDragProcessor port)
    // =========================================================================

    private static boolean isWeChat(View view) {
        if (view == null) return false;
        Context ctx = view.getContext();
        if (ctx == null) return false;
        return "com.tencent.mm".equals(ctx.getPackageName());
    }

    private static Bundle extractWeChatFile(View view) {
        try {
            if (view == null) return null;
            Context ctx = view.getContext();
            if (ctx == null) return null;

            // 1. Locate message XML metadata from view/tag hierarchy
            String xml = findWeChatMsgXml(view);
            if (TextUtils.isEmpty(xml)) {
                return null;
            }

            // 2. Parse XML fields
            String title = extractXmlTag(xml, "title");
            if (title != null) {
                title = unescapeXml(title);
            }
            String ext = extractXmlTag(xml, "fileext");
            if (ext != null) {
                ext = unescapeXml(ext);
            }
            String totalLenStr = extractXmlTag(xml, "totallen");
            long totalLen = -1;
            if (!TextUtils.isEmpty(totalLenStr)) {
                try {
                    totalLen = Long.parseLong(totalLenStr.trim());
                } catch (Throwable ignored) {}
            }
            String md5 = extractXmlTag(xml, "md5");

            // Ignore non-file messages (e.g. plain text chat)
            if (TextUtils.isEmpty(title) || (TextUtils.isEmpty(ext) && TextUtils.isEmpty(md5) && totalLen <= 0)) {
                return null;
            }

            if (TextUtils.isEmpty(ext) && title.contains(".")) {
                ext = title.substring(title.lastIndexOf('.') + 1);
            }

            Log.i(TAG, "extractWeChatFile meta: title=" + title + " ext=" + ext + " len=" + totalLen + " md5=" + md5);

            // 3. Search for physical file in WeChat internal and external storage
            File file = findWeChatPhysicalFile(ctx, title, ext, totalLen, md5);
            if (file == null || !file.isFile() || file.length() <= 0) {
                Log.w(TAG, "extractWeChatFile: physical file not found on disk for title=" + title);
                sMainHandler.post(() -> {
                    try {
                        Toast.makeText(ctx, "请先在微信中下载该文件", Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {}
                });
                return null;
            }

            Log.i(TAG, "extractWeChatFile hit file=" + file.getAbsolutePath() + " (" + file.length() + " bytes)");

            // 4. Open ParcelFileDescriptor
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            if (pfd == null) {
                return null;
            }

            // 5. Construct Bundle
            String mime = null;
            if (!TextUtils.isEmpty(ext)) {
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
            }
            if (TextUtils.isEmpty(mime)) {
                mime = URLConnection.guessContentTypeFromName(title);
            }
            if (TextUtils.isEmpty(mime)) {
                mime = "application/octet-stream";
            }

            Bundle bundle = new Bundle();
            bundle.putParcelable("filePfd", pfd);
            bundle.putString("fileName", title);
            bundle.putString("mimeType", mime);
            bundle.putLong("fileSize", file.length());
            bundle.putInt("catchType", 2);
            bundle.putString("viewContent", title);

            // Visual snapshot of the message bubble for drag card shadow
            try {
                View bubble = findWeChatBubbleView(view);
                if (bubble == null) bubble = view;
                Log.i(TAG, "extractWeChatFile: bubble view=" + bubble.getClass().getName()
                        + " (" + bubble.getWidth() + "x" + bubble.getHeight() + ")");
                Bitmap preview = viewToBitmap(bubble);
                if (preview == null) {
                    preview = extractImageBitmap(bubble);
                }
                if (preview != null) {
                    Log.i(TAG, "extractWeChatFile: bubble bitmap=" + preview.getWidth() + "x" + preview.getHeight());
                    bundle.putParcelable("viewBitmap", scaleDown(toSoftwareBitmap(preview), 720));
                }
            } catch (Throwable t) {
                Log.w(TAG, "extractWeChatFile: snapshot bubble failed", t);
            }

            return bundle;
        } catch (Throwable t) {
            Log.w(TAG, "extractWeChatFile failed", t);
            return null;
        }
    }

    private static String findWeChatMsgXml(View view) {
        if (view == null) return null;
        View current = view;
        for (int depth = 0; current != null && depth < 8; depth++) {
            Object tag = current.getTag();
            if (tag != null) {
                String xml = resolveWeChatXmlFromObject(tag);
                if (xml != null && (xml.contains("<msg") || xml.contains("<appmsg"))) {
                    return xml;
                }
            }
            String xml = resolveWeChatXmlFromView(current);
            if (xml != null && (xml.contains("<msg") || xml.contains("<appmsg"))) {
                return xml;
            }
            ViewParent parent = current.getParent();
            current = (parent instanceof View) ? (View) parent : null;
        }
        if (view instanceof ViewGroup) {
            String descendantXml = findWeChatMsgXmlInDescendants((ViewGroup) view, 0);
            if (descendantXml != null) {
                return descendantXml;
            }
        }
        return null;
    }

    private static String findWeChatMsgXmlInDescendants(ViewGroup group, int depth) {
        if (group == null || depth > 4) return null;
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child == null) continue;
            Object tag = child.getTag();
            if (tag != null) {
                String xml = resolveWeChatXmlFromObject(tag);
                if (xml != null && (xml.contains("<msg") || xml.contains("<appmsg"))) {
                    return xml;
                }
            }
            String xml = resolveWeChatXmlFromView(child);
            if (xml != null && (xml.contains("<msg") || xml.contains("<appmsg"))) {
                return xml;
            }
            if (child instanceof ViewGroup) {
                String descendantXml = findWeChatMsgXmlInDescendants((ViewGroup) child, depth + 1);
                if (descendantXml != null) return descendantXml;
            }
        }
        return null;
    }

    private static String resolveWeChatXmlFromObject(Object obj) {
        if (obj == null) return null;
        // 1. ColorOS standard hook: tag.c() returns MsgInfo
        Object msgInfo = tryInvokeNoArg(obj, "c");
        if (msgInfo != null) {
            String xml = extractXmlFromMsgInfo(msgInfo);
            if (xml != null) return xml;
        }
        // 2. Direct MsgInfo check
        String xml = extractXmlFromMsgInfo(obj);
        if (xml != null) return xml;

        // 3. Inspect fields
        for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val == null) continue;
                    if (val instanceof String) {
                        String s = (String) val;
                        if (s.contains("<msg") || s.contains("<appmsg")) {
                            return s;
                        }
                    } else {
                        String subXml = extractXmlFromMsgInfo(val);
                        if (subXml != null) return subXml;
                    }
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static String resolveWeChatXmlFromView(View v) {
        if (v == null) return null;
        for (Class<?> c = v.getClass(); c != null && c != View.class && c != ViewGroup.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    String fname = f.getName().toLowerCase(Locale.US);
                    if (!fname.contains("msg") && !fname.contains("tag") && !fname.contains("item")) continue;
                    f.setAccessible(true);
                    Object val = f.get(v);
                    if (val != null) {
                        String xml = resolveWeChatXmlFromObject(val);
                        if (xml != null) return xml;
                    }
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static String extractXmlFromMsgInfo(Object msgInfo) {
        if (msgInfo == null) return null;
        String[] methods = {"S1", "m57129S1", "j", "mo57172j", "getContent"};
        for (String m : methods) {
            Object res = tryInvokeNoArg(msgInfo, m);
            if (res instanceof String) {
                String s = (String) res;
                if (s.contains("<msg") || s.contains("<appmsg")) {
                    return s;
                }
            }
        }
        for (Class<?> c = msgInfo.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    String fname = f.getName();
                    if ("field_content".equals(fname) || "content".equals(fname)) {
                        f.setAccessible(true);
                        Object val = f.get(msgInfo);
                        if (val instanceof String) {
                            String s = (String) val;
                            if (s.contains("<msg") || s.contains("<appmsg")) {
                                return s;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static Object tryInvokeNoArg(Object obj, String methodName) {
        if (obj == null || TextUtils.isEmpty(methodName)) return null;
        for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(methodName);
                m.setAccessible(true);
                return m.invoke(obj);
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {
                break;
            }
        }
        return null;
    }

    private static String unescapeXml(String s) {
        if (TextUtils.isEmpty(s) || !s.contains("&")) return s;
        s = s.replace("&amp;", "&")
             .replace("&lt;", "<")
             .replace("&gt;", ">")
             .replace("&quot;", "\"")
             .replace("&apos;", "'")
             .replace("&#39;", "'")
             .replace("&#x20;", " ")
             .replace("&#32;", " ")
             .replace("&nbsp;", " ");
        if (s.contains("&#")) {
            try {
                Pattern p = Pattern.compile("&#(?:x([0-9a-fA-F]+)|([0-9]+));");
                Matcher m = p.matcher(s);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    try {
                        int code = m.group(1) != null ? Integer.parseInt(m.group(1), 16) : Integer.parseInt(m.group(2));
                        m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(code))));
                    } catch (Throwable t) {
                        m.appendReplacement(sb, m.group(0));
                    }
                }
                m.appendTail(sb);
                s = sb.toString();
            } catch (Throwable ignored) {}
        }
        return s;
    }

    private static String extractXmlTag(String xml, String tag) {
        if (xml == null || tag == null) return null;
        Pattern p = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        if (m.find()) {
            String val = m.group(1).trim();
            if (val.startsWith("<![CDATA[") && val.endsWith("]]>")) {
                val = val.substring(9, val.length() - 3).trim();
            }
            return unescapeXml(val);
        }
        return null;
    }

    private static boolean isLikelyUuidOrHash(String s) {
        if (TextUtils.isEmpty(s)) return false;
        int len = s.length();
        if (len != 32 && len != 36) return false;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || c == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static File resolveWeChatRefFile(File f) {
        if (f == null || !f.isFile()) return f;
        long len = f.length();
        // WeChat .ref pointers are 36 bytes (UUID) or 32 bytes (hash)
        if (len >= 32 && len <= 40) {
            try {
                byte[] buf = new byte[(int) len];
                try (FileInputStream in = new FileInputStream(f)) {
                    int read = in.read(buf);
                    if (read > 0) {
                        String uuid = new String(buf, 0, read, StandardCharsets.UTF_8).trim();
                        if (isLikelyUuidOrHash(uuid)) {
                            File parent = f.getParentFile();
                            if (parent != null) {
                                // 1. Same dir .ref/d/<uuid> (e.g. attachment/.ref/d/<uuid>)
                                File refD = new File(new File(parent, ".ref"), "d/" + uuid);
                                if (refD.isFile() && refD.length() > 0) {
                                    Log.i(TAG, "resolveWeChatRefFile: resolved " + f.getName()
                                            + " -> " + refD.getAbsolutePath() + " (" + refD.length() + " bytes)");
                                    return refD;
                                }
                                // 2. Account level sibling attachment/.ref/d/ or message/media/.ref/d/
                                File accountDir = parent.getParentFile();
                                if (accountDir != null) {
                                    File attachRef = new File(accountDir, "attachment/.ref/d/" + uuid);
                                    if (attachRef.isFile() && attachRef.length() > 0) {
                                        Log.i(TAG, "resolveWeChatRefFile: resolved " + f.getName()
                                                + " -> " + attachRef.getAbsolutePath() + " (" + attachRef.length() + " bytes)");
                                        return attachRef;
                                    }
                                    File mediaRef = new File(accountDir, "message/media/.ref/d/" + uuid);
                                    if (mediaRef.isFile() && mediaRef.length() > 0) {
                                        Log.i(TAG, "resolveWeChatRefFile: resolved " + f.getName()
                                                + " -> " + mediaRef.getAbsolutePath() + " (" + mediaRef.length() + " bytes)");
                                        return mediaRef;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return f;
    }

    private static File resolveAndMatchCandidate(File f, long totalLen, String md5) {
        if (f == null || !f.isFile() || f.length() <= 0) return null;
        File real = resolveWeChatRefFile(f);
        if (real == null || !real.isFile() || real.length() <= 0) return null;
        if (totalLen > 0 && real.length() != totalLen) return null;
        if (!TextUtils.isEmpty(md5) && totalLen > 0) {
            String fMd5 = calculateFileMd5(real);
            if (!TextUtils.isEmpty(fMd5) && !md5.equalsIgnoreCase(fMd5)) {
                return null;
            }
        }
        return real;
    }

    private static void collectCandidateFiles(File dir, int depth, int maxDepth, List<File> result, int maxTotal) {
        if (dir == null || !dir.isDirectory() || result.size() >= maxTotal) return;
        File[] entries = dir.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            if (result.size() >= maxTotal) break;
            if (entry.isFile()) {
                if (entry.length() > 0) {
                    result.add(entry);
                }
            } else if (entry.isDirectory() && depth < maxDepth) {
                if (!".ref".equals(entry.getName())) {
                    collectCandidateFiles(entry, depth + 1, maxDepth, result, maxTotal);
                }
            }
        }
    }

    private static File findWeChatPhysicalFile(Context ctx, String title, String ext, long totalLen, String md5) {
        List<File> searchDirs = collectWeChatSearchDirs(ctx);
        if (searchDirs.isEmpty()) {
            return null;
        }

        // Fast path: Direct file check in all known search directories
        for (File dir : searchDirs) {
            if (!dir.isDirectory()) continue;
            File direct = resolveAndMatchCandidate(new File(dir, title), totalLen, md5);
            if (direct != null) return direct;

            File ref = resolveAndMatchCandidate(new File(dir, title + "\u2316"), totalLen, md5);
            if (ref != null) return ref;

            File pref1 = resolveAndMatchCandidate(new File(dir, "1_" + title), totalLen, md5);
            if (pref1 != null) return pref1;

            File pref2 = resolveAndMatchCandidate(new File(dir, "2_" + title), totalLen, md5);
            if (pref2 != null) return pref2;
        }

        // Directory scan with candidate collection
        File bestFile = null;
        int bestScore = -1;

        List<File> candidates = new ArrayList<>();
        for (File dir : searchDirs) {
            if (!dir.isDirectory()) continue;
            int maxDepth = (dir.getAbsolutePath().contains("opendata") || dir.getAbsolutePath().contains("appmsg")) ? 3 : 1;
            collectCandidateFiles(dir, 0, maxDepth, candidates, 800);
            if (candidates.size() >= 800) break;
        }

        for (File f : candidates) {
            File real = resolveWeChatRefFile(f);
            if (real == null || !real.isFile() || real.length() <= 0) continue;
            String name = f.getName();
            String cleanName = name.endsWith("\u2316") ? name.substring(0, name.length() - 1) : name;
            if (!TextUtils.isEmpty(title) && cleanName.equalsIgnoreCase(title)) {
                if (totalLen <= 0 || real.length() == totalLen) {
                    return real;
                }
            }
            int score = scoreWeChatCandidate(real, title, ext, totalLen, md5);
            if (score > bestScore) {
                bestScore = score;
                bestFile = real;
                if (score >= 120) {
                    return bestFile;
                }
            }
        }

        if (bestScore >= 70) {
            return bestFile;
        }
        return null;
    }

    private static int scoreWeChatCandidate(File file, String title, String ext, long totalLen, String md5) {
        if (file == null || !file.isFile() || file.length() <= 0) return -1;
        String name = file.getName();

        // Check ext
        if (!TextUtils.isEmpty(ext)) {
            String lowerName = name.toLowerCase(Locale.US);
            String lowerExt = ext.toLowerCase(Locale.US);
            if (!lowerName.endsWith("." + lowerExt) && !lowerName.endsWith("." + lowerExt + "\u2316")) {
                if (TextUtils.isEmpty(md5) || totalLen <= 0 || file.length() != totalLen) {
                    return -1;
                }
            }
        }

        // MD5 match
        if (!TextUtils.isEmpty(md5) && totalLen > 0 && file.length() == totalLen) {
            String fMd5 = calculateFileMd5(file);
            if (!TextUtils.isEmpty(fMd5) && md5.equalsIgnoreCase(fMd5)) {
                return 250;
            }
        }

        int score = 0;
        if (!TextUtils.isEmpty(title)) {
            String cleanName = name.endsWith("\u2316") ? name.substring(0, name.length() - 1) : name;
            if (cleanName.equalsIgnoreCase(title)) {
                score += 100;
            } else if (cleanName.toLowerCase(Locale.US).contains(title.toLowerCase(Locale.US))
                    || title.toLowerCase(Locale.US).contains(cleanName.toLowerCase(Locale.US))) {
                score += 45;
            }
        }
        if (!TextUtils.isEmpty(ext)) {
            score += 30;
        }
        if (totalLen > 0) {
            long len = file.length();
            if (len == totalLen) {
                score += 45;
            } else if (Math.abs(len - totalLen) <= 1024) {
                score += 20;
            } else if (len / 1024 == totalLen / 1024) {
                score += 10;
            }
        }
        return score;
    }

    private static List<File> collectWeChatSearchDirs(Context ctx) {
        List<File> dirs = new ArrayList<>();
        // 1. Data dir accounts (/data/user/0/com.tencent.mm/...)
        try {
            File dataDir = ctx.getDataDir();
            if (dataDir != null) {
                File shareTemp = new File(dataDir, "cache/temp/share_file_temp");
                if (shareTemp.isDirectory()) dirs.add(shareTemp);
                File temp = new File(dataDir, "cache/temp");
                if (temp.isDirectory() && !dirs.contains(temp)) dirs.add(temp);
                File filesShare = new File(dataDir, "files/share");
                if (filesShare.isDirectory()) dirs.add(filesShare);

                File mm = new File(dataDir, "MicroMsg");
                if (mm.isDirectory()) {
                    File[] subs = mm.listFiles();
                    if (subs != null) {
                        for (File s : subs) {
                            if (s.isDirectory() && s.getName().length() == 32) {
                                File attach = new File(s, "attachment");
                                if (attach.isDirectory()) dirs.add(attach);
                                File video = new File(s, "video");
                                if (video.isDirectory()) dirs.add(video);
                                File opendata = new File(s, "message/media/opendata");
                                if (opendata.isDirectory()) dirs.add(opendata);
                                File appmsg = new File(s, "message/media/appmsg");
                                if (appmsg.isDirectory()) dirs.add(appmsg);
                                File draft = new File(s, "draft");
                                if (draft.isDirectory()) dirs.add(draft);
                                File record = new File(s, "record");
                                if (record.isDirectory()) dirs.add(record);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Cache dir temp
        try {
            File cacheDir = ctx.getCacheDir();
            if (cacheDir != null) {
                File shareTemp = new File(cacheDir, "temp/share_file_temp");
                if (shareTemp.isDirectory() && !dirs.contains(shareTemp)) dirs.add(shareTemp);
                File temp = new File(cacheDir, "temp");
                if (temp.isDirectory() && !dirs.contains(temp)) dirs.add(temp);
            }
        } catch (Throwable ignored) {}

        // 3. External storage accounts (/sdcard/Android/data/com.tencent.mm/MicroMsg/)
        try {
            File ext = ctx.getExternalFilesDir(null);
            File extMm = ext != null ? new File(ext.getParentFile(), "MicroMsg") : null;
            if (extMm == null || !extMm.isDirectory()) {
                extMm = new File(Environment.getExternalStorageDirectory(), "Android/data/com.tencent.mm/MicroMsg");
            }
            if (extMm.isDirectory()) {
                File dl = new File(extMm, "Download");
                if (dl.isDirectory()) dirs.add(dl);
                File[] subs = extMm.listFiles();
                if (subs != null) {
                    for (File s : subs) {
                        if (s.isDirectory() && s.getName().length() == 32) {
                            File attach = new File(s, "attachment");
                            if (attach.isDirectory()) dirs.add(attach);
                            File video = new File(s, "video");
                            if (video.isDirectory()) dirs.add(video);
                            File opendata = new File(s, "message/media/opendata");
                            if (opendata.isDirectory()) dirs.add(opendata);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 4. Public Download / WeiXin
        try {
            File pub = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WeiXin");
            if (pub.isDirectory()) dirs.add(pub);
        } catch (Throwable ignored) {}

        return dirs;
    }

    private static String calculateFileMd5(File file) {
        if (file == null || !file.isFile() || file.length() <= 0) return null;
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) > 0) {
                digest.update(buf, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static View findWeChatViewUnderTouch(View root, float rawX, float rawY) {
        if (root == null || root.getVisibility() != View.VISIBLE) return null;
        int[] loc = new int[2];
        root.getLocationOnScreen(loc);
        if (rawX < loc[0] - 10 || rawX > loc[0] + root.getWidth() + 10
                || rawY < loc[1] - 10 || rawY > loc[1] + root.getHeight() + 10) {
            return null;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                View hit = findWeChatViewUnderTouch(child, rawX, rawY);
                if (hit != null) return hit;
            }
        }
        if (findWeChatMsgXml(root) != null) {
            return root;
        }
        return null;
    }

    private static View findWeChatBubbleView(View view) {
        if (view == null) return null;
        int screenW = view.getResources().getDisplayMetrics().widthPixels;

        // Diagnostic log: inspect the hierarchy chain
        View cur = view;
        for (int d = 0; cur != null && d < 8; d++) {
            Drawable bg = cur.getBackground();
            Log.i(TAG, "wechat hierarchy [" + d + "] " + cur.getClass().getName()
                    + " " + cur.getWidth() + "x" + cur.getHeight()
                    + " bg=" + (bg != null ? bg.getClass().getSimpleName() : "null")
                    + " pad=" + cur.getPaddingLeft() + "," + cur.getPaddingTop()
                    + "," + cur.getPaddingRight() + "," + cur.getPaddingBottom());
            ViewParent p = cur.getParent();
            cur = (p instanceof View) ? (View) p : null;
        }

        View bestWithBg = null;
        View bestAny = null;
        cur = view;
        for (int depth = 0; cur != null && depth < 8; depth++) {
            if (cur instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) cur;
                int w = vg.getWidth();
                int h = vg.getHeight();
                if (w > 0 && h > 0) {
                    if (w >= screenW * 0.90f) {
                        break;
                    }
                    if (w >= dp(view.getContext(), 140)) {
                        bestAny = vg;
                        if (vg.getBackground() != null) {
                            bestWithBg = vg;
                        }
                    }
                }
            }
            ViewParent p = cur.getParent();
            cur = (p instanceof View) ? (View) p : null;
        }

        if (bestWithBg != null) {
            return bestWithBg;
        }
        return bestAny != null ? bestAny : view;
    }

    private static boolean hasDescendantImageView(ViewGroup group) {
        if (group == null) return false;
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageView) return true;
            if (child instanceof ViewGroup) {
                if (hasDescendantImageView((ViewGroup) child)) return true;
            }
        }
        return false;
    }

    private static Bitmap viewToBitmap(View view) {
        if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) return null;
        try {
            Bitmap bmp = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            view.draw(canvas);
            return bmp;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // =========================================================================
    // ColorOS-style drag rules: "rule first, heuristics as fallback".
    //
    // ColorOS never guesses the modality from geometry — its per-app config
    // (/system_ext/oplus/sys_view_drag_list.xml) pins a *view class* to exactly
    // one content type, e.g. <view name="XYImageView" type="image"/> next to
    // <view name="NoteDetailTextView" type="text"/>. A text view can therefore
    // never yield a bitmap and an image view can never yield text, which is why
    // "text above, image below" cards are unambiguous there.
    //
    // DragRules.java (generated from that file) carries the same table reduced
    // to the matchers we implement: package, activity, type, view class name,
    // parent / brother / uncle / nephews class names, resource id, and the
    // touch-view / bottom-view flags. Apps without a rule keep using the
    // heuristic cascade in extractContentColorOS().
    // =========================================================================

    private static final class DragRule {
        final String type;
        final String name;
        final String parent;
        final String brother;
        final String id;
        final String uncle;
        final String nephews;
        final boolean touchOnly;
        final boolean bottomView;

        DragRule(String[] cols) {
            type = cols[2];
            name = cols[3];
            parent = cols.length > 4 ? cols[4] : "";
            brother = cols.length > 5 ? cols[5] : "";
            id = cols.length > 6 ? cols[6] : "";
            uncle = cols.length > 7 ? cols[7] : "";
            nephews = cols.length > 8 ? cols[8] : "";
            String flags = cols.length > 9 ? cols[9] : "";
            touchOnly = flags.indexOf('t') >= 0;
            bottomView = flags.indexOf('b') >= 0;
        }

        String describe() {
            return name + "/" + type;
        }
    }

    private static final class RuleHit {
        final View view;
        final DragRule rule;
        final String activity;

        RuleHit(View view, DragRule rule, String activity) {
            this.view = view;
            this.rule = rule;
            this.activity = activity;
        }
    }

    private static volatile java.util.HashMap<String, java.util.HashMap<String, java.util.ArrayList<DragRule>>> sRuleTable;
    private static volatile boolean sRuleTableReady;
    private static volatile boolean sRuleEngineEnabled = true;
    private static volatile long sRuleHitGestureAt = -1;
    private static volatile java.util.ArrayList<RuleHit> sRuleHitCache;

    private static void ensureRuleTable() {
        if (sRuleTableReady) {
            return;
        }
        synchronized (DragRules.class) {
            if (sRuleTableReady) {
                return;
            }
            java.util.HashMap<String, java.util.HashMap<String, java.util.ArrayList<DragRule>>> table =
                    new java.util.HashMap<>();
            int rows = 0;
            for (String[] chunk : DragRules.CHUNKS) {
                for (String row : chunk) {
                    String[] cols = row.split("\\|", -1);
                    if (cols.length < 4 || cols[0].isEmpty() || cols[1].isEmpty()) {
                        continue;
                    }
                    DragRule rule = new DragRule(cols);
                    java.util.HashMap<String, java.util.ArrayList<DragRule>> byActivity = table.get(cols[0]);
                    if (byActivity == null) {
                        byActivity = new java.util.HashMap<>();
                        table.put(cols[0], byActivity);
                    }
                    java.util.ArrayList<DragRule> list = byActivity.get(cols[1]);
                    if (list == null) {
                        list = new java.util.ArrayList<>();
                        byActivity.put(cols[1], list);
                    }
                    list.add(rule);
                    rows++;
                }
            }
            sRuleTable = table;
            sRuleTableReady = true;
            Log.i(TAG, "drag rules ready: " + rows + " rules / " + table.size() + " packages");
        }
    }

    /** Wipes the per-gesture rule cache; called from ACTION_DOWN. */
    private static void resetRuleHit() {
        sRuleHitGestureAt = -1;
        sRuleHitCache = null;
    }

    private static String currentActivityName() {
        Activity activity = sLastActivity;
        if (activity == null) {
            return null;
        }
        String name = activity.getClass().getName();
        return name == null || name.isEmpty() ? null : name;
    }

    private static java.util.ArrayList<RuleHit> resolveRuleHits(View root) {
        if (!sRuleEngineEnabled || root == null) {
            return null;
        }
        if (sRuleHitGestureAt == sDownAt) {
            return sRuleHitCache;
        }
        java.util.ArrayList<RuleHit> hits = null;
        try {
            hits = computeRuleHits(root);
        } catch (Throwable throwable) {
            Log.w(TAG, "rule resolve failed", throwable);
        }
        sRuleHitCache = hits;
        sRuleHitGestureAt = sDownAt;
        return hits;
    }

    /**
     * Resolves the rule candidates for this gesture, innermost first: the view
     * the finger is actually on wins over any container rule above it.
     */
    private static java.util.ArrayList<RuleHit> computeRuleHits(View root) {
        ensureRuleTable();
        Context ctx = currentContext();
        if (ctx == null) {
            return null;
        }
        String pkg = ctx.getPackageName();
        if (pkg == null) {
            return null;
        }
        java.util.HashMap<String, java.util.ArrayList<DragRule>> byActivity = sRuleTable.get(pkg);
        if (byActivity == null) {
            return null;
        }
        String activity = currentActivityName();
        java.util.ArrayList<DragRule> rules = activity == null ? null : byActivity.get(activity);
        if (rules == null && activity != null) {
            int dot = activity.lastIndexOf('.');
            if (dot > 0) {
                rules = byActivity.get(activity.substring(dot + 1));
            }
        }
        if (rules == null) {
            rules = byActivity.get("*");
        }
        if (rules == null || rules.isEmpty()) {
            Log.d(TAG, "rule miss (activity) pkg=" + pkg + " act=" + activity);
            return null;
        }
        float rawX = getTouchRawX();
        float rawY = getTouchRawY();
        java.util.ArrayList<View> chain = new java.util.ArrayList<>();
        collectPointChain(root, rawX, rawY, chain, 0, true);
        if (chain.isEmpty()) {
            return null;
        }
        java.util.ArrayList<RuleHit> hits = new java.util.ArrayList<>();
        // Sanity guard on top of the rules: a container-level rule must not
        // override the modality of what the finger is literally on. Our rule
        // table is a reduction of ColorOS's (which also lists the leaf views),
        // so this keeps a partially-covered app from regressing.
        View innermostView = chain.get(chain.size() - 1);
        boolean fingerOnText = innermostView instanceof TextView
                && !TextUtils.isEmpty(((TextView) innermostView).getText());
        boolean fingerOnImage = innermostView instanceof ImageView
                && ((ImageView) innermostView).getDrawable() != null;
        boolean fingerOnWeb = isWebViewLike(innermostView) || resolveWebViewTarget(innermostView) != null;
        for (int i = chain.size() - 1; i >= 0; i--) {
            View view = chain.get(i);
            boolean innermost = i == chain.size() - 1;
            for (DragRule rule : rules) {
                if (rule.touchOnly && !innermost) {
                    continue;
                }
                if (!innermost && view != innermostView) {
                    if (fingerOnWeb && !"web".equals(rule.type)) {
                        Log.d(TAG, "rule skip " + rule.type + "@" + view.getClass().getSimpleName()
                                + " (finger is on webview)");
                        continue;
                    }
                    if (fingerOnText && "image".equals(rule.type)) {
                        Log.d(TAG, "rule skip image@" + view.getClass().getSimpleName()
                                + " (finger is on text)");
                        continue;
                    }
                    if (fingerOnImage && "text".equals(rule.type)) {
                        Log.d(TAG, "rule skip text@" + view.getClass().getSimpleName()
                                + " (finger is on image)");
                        continue;
                    }
                }
                if (!ruleMatches(rule, view)) {
                    continue;
                }
                hits.add(new RuleHit(view, rule, activity));
                break;
            }
        }
        if (hits.isEmpty()) {
            Log.i(TAG, "rule miss (view) pkg=" + pkg + " act=" + activity
                    + " rules=" + rules.size() + " chain=" + chain.size());
            return null;
        }
        return hits;
    }

    /**
     * Collects the chain of views under the touch point, outermost first.
     * The last element is the view the finger is really on.
     */
    private static void collectPointChain(View view, float rawX, float rawY,
                                          java.util.List<View> out, int depth, boolean singleBranch) {
        if (view == null || depth > 32 || view.getVisibility() != View.VISIBLE) {
            return;
        }
        if (!rectContainsPoint(view, rawX, rawY, 0)) {
            return;
        }
        out.add(view);
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (!rectContainsPoint(child, rawX, rawY, 0)) {
                continue;
            }
            collectPointChain(child, rawX, rawY, out, depth + 1, singleBranch);
            if (singleBranch) {
                break;
            }
        }
    }

    private static boolean rectContainsPoint(View view, float rawX, float rawY, int slop) {
        if (view == null) {
            return false;
        }
        try {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            return rawX >= location[0] - slop && rawX <= location[0] + view.getWidth() + slop
                    && rawY >= location[1] - slop && rawY <= location[1] + view.getHeight() + slop;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean ruleMatches(DragRule rule, View view) {
        if (view == null || TextUtils.isEmpty(rule.name)) {
            return false;
        }
        if (!classNameMatches(rule.name, view)) {
            return false;
        }
        if (!TextUtils.isEmpty(rule.parent) && !classNameMatches(rule.parent, viewParentOf(view))) {
            return false;
        }
        if (!TextUtils.isEmpty(rule.brother) && !hasSiblingOfClass(view, rule.brother)) {
            return false;
        }
        if (!TextUtils.isEmpty(rule.id) && !idMatches(view, rule.id)) {
            return false;
        }
        if (!TextUtils.isEmpty(rule.uncle) && !hasUncleOfClass(view, rule.uncle)) {
            return false;
        }
        if (!TextUtils.isEmpty(rule.nephews) && !hasNephewOfClass(view, rule.nephews)) {
            return false;
        }
        return true;
    }

    private static boolean classNameMatches(String expected, View view) {
        if (view == null || expected == null || expected.isEmpty()) {
            return false;
        }
        String full = view.getClass().getName();
        String simple = view.getClass().getSimpleName();
        if (expected.equals(simple) || expected.equals(full)) {
            return true;
        }
        if (expected.indexOf('.') > 0) {
            return full.endsWith("." + expected) || full.endsWith(expected);
        }
        return false;
    }

    private static View viewParentOf(View view) {
        try {
            android.view.ViewParent parent = view.getParent();
            return parent instanceof View ? (View) parent : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasSiblingOfClass(View view, String expected) {
        View parent = viewParentOf(view);
        if (!(parent instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) parent;
        for (int i = 0; i < group.getChildCount(); i++) {
            View sibling = group.getChildAt(i);
            if (sibling == null || sibling == view) {
                continue;
            }
            if (classNameMatches(expected, sibling)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUncleOfClass(View view, String expected) {
        View parent = viewParentOf(view);
        View grandParent = parent == null ? null : viewParentOf(parent);
        if (!(grandParent instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) grandParent;
        for (int i = 0; i < group.getChildCount(); i++) {
            View uncle = group.getChildAt(i);
            if (uncle == null || uncle == parent) {
                continue;
            }
            if (classNameMatches(expected, uncle) || hasDescendantOfClass(uncle, expected, 2)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNephewOfClass(View view, String expected) {
        View parent = viewParentOf(view);
        if (!(parent instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) parent;
        for (int i = 0; i < group.getChildCount(); i++) {
            View sibling = group.getChildAt(i);
            if (sibling == null || sibling == view) {
                continue;
            }
            if (hasDescendantOfClass(sibling, expected, 2)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDescendantOfClass(View view, String expected, int depth) {
        if (view == null || depth < 0) {
            return false;
        }
        if (classNameMatches(expected, view)) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (hasDescendantOfClass(group.getChildAt(i), expected, depth - 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean idMatches(View view, String expected) {
        try {
            int id = view.getId();
            if (id == View.NO_ID || view.getResources() == null) {
                return false;
            }
            String entry = view.getResources().getResourceEntryName(id);
            return expected.equals(entry);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isAncestorOf(View ancestor, View view) {
        if (ancestor == null || view == null) {
            return false;
        }
        if (ancestor == view) {
            return true;
        }
        android.view.ViewParent parent = view.getParent();
        int guard = 0;
        while (parent != null && guard++ < 64) {
            if (parent == ancestor) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Called from preExtract() before any heuristic. The rule's view wins even
     * when AES offers us one of its descendants, so a card declared as "image"
     * is never turned into its caption text.
     */
    private static Bundle applyRule(View view, int flags) {
        if (!sRuleEngineEnabled || view == null) {
            return null;
        }
        java.util.ArrayList<RuleHit> hits = resolveRuleHits(view.getRootView());
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        // Prefer a hit whose view is the queried view or one of its ancestors;
        // if AES is asking about an ancestor of every hit (it climbed past the
        // rule target), the innermost hit is still the right answer.
        Bundle result = tryRuleHits(hits, view, flags, true);
        if (result == null) {
            result = tryRuleHits(hits, view, flags, false);
        }
        return result;
    }

    private static Bundle tryRuleHits(java.util.ArrayList<RuleHit> hits, View view,
                                      int flags, boolean requireAncestor) {
        for (RuleHit hit : hits) {
            if (requireAncestor && !isAncestorOf(hit.view, view)) {
                continue;
            }
            Bundle extracted = extractRuleContent(hit, flags);
            if (extracted == null || !isBundleMeaningful(extracted)) {
                continue;
            }
            extracted.putString("ruleHit", hit.rule.describe());
            Log.i(TAG, "preExtract rule=" + hit.rule.describe()
                    + " view=" + hit.view.getClass().getName()
                    + " asked=" + view.getClass().getName()
                    + (requireAncestor ? "" : " (fallback)"));
            return extracted;
        }
        return null;
    }

    private static Bundle extractRuleContent(RuleHit hit, int flags) {
        View view = hit.view;
        String type = hit.rule.type;
        Bundle result = null;
        try {
            if ("text".equals(type)) {
                result = extractTextStrict(view);
            } else if ("image".equals(type)) {
                result = extractFile(view);
                if (!isBundleMeaningful(result)) {
                    Bitmap bitmap = extractImageBitmap(view);
                    if (bitmap != null) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("viewBitmap", bitmap);
                        bundle.putInt("catchType", 2);
                        result = bundle;
                    }
                } else {
                    attachAnimatedSource(view, result);
                }
            } else if ("file".equals(type) || "video".equals(type)) {
                result = extractFile(view);
            } else if ("web".equals(type)) {
                result = webViewBundle(view);
            } else if ("compose".equals(type)) {
                float rawX = getTouchRawX();
                float rawY = getTouchRawY();
                result = extractHybridComposeContent(view, rawX, rawY, true, true);
                if (!isBundleMeaningful(result)) {
                    result = extractCompose(view, rawX, rawY, true, true);
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "extractRuleContent failed", throwable);
        }
        return result;
    }

    /** Text extraction bound to exactly this view (no class-name guessing). */
    private static Bundle extractTextStrict(View view) {
        if (view == null) {
            return null;
        }
        if (view instanceof TextView) {
            Bundle text = extractTextView((TextView) view);
            if (isBundleMeaningful(text)) {
                return text;
            }
        }
        Bundle custom = extractCustomText(view, true);
        if (isBundleMeaningful(custom)) {
            return custom;
        }
        CharSequence description = view.getContentDescription();
        if (isMeaningfulUserText(description)) {
            Bundle bundle = new Bundle();
            bundle.putString("viewContent", description.toString().trim());
            bundle.putInt("catchType", 1);
            return bundle;
        }
        if (view instanceof ViewGroup) {
            Bundle group = extractViewGroupText((ViewGroup) view);
            if (isBundleMeaningful(group)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Rule-free fallback: the direct child that the finger is really on.
     * Used instead of aggregating every TextView of the card.
     */
    private static View childContainingPoint(ViewGroup group, float rawX, float rawY) {
        if (group == null) {
            return null;
        }
        int count = group.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (isPassthroughOverlay(child)) {
                continue;
            }
            if (!rectContainsPoint(child, rawX, rawY, 0)) {
                continue;
            }
            return child;
        }
        return null;
    }

    private static Bundle extractContentColorOS(View view, int flags) {
        if (view == null) {
            return null;
        }
        boolean wantText = (flags & 1) != 0;
        boolean wantBitmap = (flags & 2) != 0;
        float rawX = getTouchRawX();
        float rawY = getTouchRawY();

        // 0. Telegram ChatMessageCell / BackupImageView direct extraction
        Bundle tg = extractTelegramContent(view, rawX, rawY, wantText, wantBitmap);
        if (tg != null) {
            return tg;
        }

        // 0.1 WeChat Chatting Item File extraction
        if (isWeChat(view)) {
            Bundle wechatFile = extractWeChatFile(view);
            if (wechatFile != null && isBundleMeaningful(wechatFile)) {
                Log.i(TAG, "extractContentColorOS: WeChat file hit: " + wechatFile.getString("fileName"));
                return wechatFile;
            }
        }

        // 0.5 Original file / content Uri (Weibo's LargeImageView2 and similar
        // custom viewers keep the source Uri in a private field). Returning the
        // PFD preserves the original image instead of a re-encoded screenshot.
        if (wantBitmap && (isAcceptableImageView(view) || isFileLikeView(view))) {
            Bundle file = extractFile(view);
            if (file != null && isBundleMeaningful(file)) {
                return file;
            }
        }

        // 1. ComposeView extraction (ColorOS OplusComposeViewDragProcessor).
        // Never recursively search the whole window for a ComposeView here:
        // AES already walks into children, and that scan used to run for every
        // leaf on every long press.
        if (isComposeView(view)) {
            // Hybrid app check (e.g. Coolapk): Embedded native Views (ImageView / TextView)
            // take strict precedence over Compose screenshot fallback to preserve original lossless content!
            Bundle hybrid = extractHybridComposeContent(view, rawX, rawY, wantText, wantBitmap);
            if (hybrid != null && isBundleMeaningful(hybrid)) {
                return hybrid;
            }
            Bundle compose = extractCompose(view, rawX, rawY, wantText, wantBitmap);
            if (compose != null && isBundleMeaningful(compose)) {
                return compose;
            }
        }

        // 2. WebView extraction (ColorOS OplusWebViewDragProcessor)
        if (isWebViewLike(view)) {
            Bundle web = webViewBundle(view);
            if (web != null && isBundleMeaningful(web)) {
                return web;
            }
            return null;
        }

        // 3. Find target leaf view
        View target = view;
        if (view instanceof ViewGroup && !isComposeView(view) && !isWebViewLike(view)) {
            View leaf = findDeepTouchView(view, rawX, rawY);
            if (leaf != null) {
                target = leaf;
            }
        }
        if (isComposeView(target)) {
            Bundle hybrid = extractHybridComposeContent(target, rawX, rawY, wantText, wantBitmap);
            if (hybrid != null && isBundleMeaningful(hybrid)) {
                return hybrid;
            }
            Bundle compose = extractCompose(target, rawX, rawY, wantText, wantBitmap);
            if (compose != null && isBundleMeaningful(compose)) {
                return compose;
            }
            // Hybrid app fix (e.g. Coolapk com.coolapk.market):
            // The top-most ComposeView overlay has no Compose content under (rawX, rawY).
            // Pass through to the underlying non-Compose view in the window hierarchy!
            View rootView = target.getRootView();
            if (rootView != null) {
                View nonComposeLeaf = findDeepTouchView(rootView, rawX, rawY, true /* skipCompose */);
                Log.i(TAG, "hybrid pass-through: " + (nonComposeLeaf == null ? "null" : nonComposeLeaf.getClass().getName()));
                if (nonComposeLeaf != null && nonComposeLeaf != target) {
                    target = nonComposeLeaf;
                }
            }
        }
        if (isWebViewLike(target)) {
            Bundle web = webViewBundle(target);
            if (web != null && isBundleMeaningful(web)) {
                return web;
            }
            return null;
        }

        // 4. Hierarchy search starting from target: custom text views,
        // TextViews and ImageViews.
        View candidate = target;
        if (candidate instanceof ViewGroup && !isComposeView(candidate) && !isWebViewLike(candidate)) {
            // 4a. Point-anchored descent (ColorOS parity): follow the child the
            // finger is really on. Only when no child covers the touch point do
            // we fall through to whole-card text aggregation below — that
            // aggregation is what turned "long press the image" into "the
            // caption text" on mixed text+image cards.
            View anchored = childContainingPoint((ViewGroup) candidate, rawX, rawY);
            if (anchored != null) {
                Bundle deep = extractContentColorOS(anchored, flags);
                if (deep != null && isBundleMeaningful(deep)) {
                    Log.d(TAG, "point-anchored hit " + anchored.getClass().getName());
                    return deep;
                }
            }
            if (wantText) {
                Bundle grpText = extractViewGroupText((ViewGroup) candidate);
                if (grpText != null && isBundleMeaningful(grpText)) {
                    return grpText;
                }
            }
            if (wantBitmap) {
                Bundle grpImg = extractViewGroupImage((ViewGroup) candidate);
                if (grpImg != null && isBundleMeaningful(grpImg)) {
                    return grpImg;
                }
            }
        }
        for (int depth = 0; candidate != null && depth < 5; depth++) {
            // Check candidate itself
            if (wantText) {
                Bundle custom = extractCustomText(candidate);
                if (custom != null && isBundleMeaningful(custom)) {
                    return custom;
                }
            }
            if (wantText && candidate instanceof TextView) {
                Bundle text = extractTextView((TextView) candidate);
                if (text != null && isBundleMeaningful(text)) {
                    return text;
                }
            }
            if (wantBitmap && isAcceptableImageView(candidate)) {
                Bitmap bitmap = extractImageBitmap(candidate);
                if (bitmap != null) {
                    Bundle b = new Bundle();
                    b.putParcelable("viewBitmap", bitmap);
                    b.putInt("catchType", 2);
                    return b;
                }
            }
            // Check siblings of candidate inside candidate's parent
            if (candidate.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) candidate.getParent();
                for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                    View sibling = parent.getChildAt(i);
                    if (sibling == candidate || sibling.getVisibility() != View.VISIBLE) continue;
                    int[] loc = new int[2];
                    sibling.getLocationOnScreen(loc);
                    if (rawX >= loc[0] - 30 && rawX <= loc[0] + sibling.getWidth() + 30
                            && rawY >= loc[1] - 30 && rawY <= loc[1] + sibling.getHeight() + 30) {
                        if (wantText) {
                            Bundle custom = extractCustomText(sibling);
                            if (custom != null && isBundleMeaningful(custom)) {
                                return custom;
                            }
                        }
                        if (wantText && sibling instanceof TextView) {
                            Bundle text = extractTextView((TextView) sibling);
                            if (text != null && isBundleMeaningful(text)) {
                                return text;
                            }
                        }
                        if (wantBitmap && isAcceptableImageView(sibling)) {
                            Bitmap bitmap = extractImageBitmap(sibling);
                            if (bitmap != null) {
                                Bundle b = new Bundle();
                                b.putParcelable("viewBitmap", bitmap);
                                b.putInt("catchType", 2);
                                return b;
                            }
                        }
                    }
                }
                candidate = parent;
            } else {
                break;
            }
        }

        // 5. Accessibility text fallback. Only run the expensive accessibility
        // tree walk for views that plausibly draw text; a generic overlay view
        // has nothing to contribute and this was a major source of jank.
        if (wantText && isTextLikeView(target)) {
            View a11yRoot = target.getRootView() != null ? target.getRootView() : view;
            String text = accessibilityText(a11yRoot, new PointF(rawX, rawY));
            if (isMeaningfulUserText(text)) {
                Bundle bundle = new Bundle();
                bundle.putString("viewContent", text);
                bundle.putInt("catchType", 1);
                return bundle;
            }
        }

        // 6. ContentDescription fallback
        if (wantText) {
            CharSequence description = target.getContentDescription();
            if (!isMeaningfulUserText(description) && target != view) {
                description = view.getContentDescription();
            }
            if (isMeaningfulUserText(description)) {
                Bundle bundle = new Bundle();
                bundle.putString("viewContent", description.toString());
                bundle.putInt("catchType", 1);
                return bundle;
            }
        }

        return null;
    }

    // ColorOS OplusComposeViewDragProcessor exact text filter algorithms
    private static final java.util.regex.Pattern OBFUSCATED_X_ID_PATTERN =
            java.util.regex.Pattern.compile("^[Xx]\\.[0-9A-Za-z_]+(?:\\.[0-9A-Za-z_]+)?$");

    private static boolean containsNaturalLanguage(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                return true;
            }
            if (c >= 0x4e00 && c <= 0x9fff) {
                return true;
            }
            if (c > 0x7f && Character.isLetter(c)) {
                return true;
            }
            if (Character.isLetterOrDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDragableUserText(String s) {
        if (TextUtils.isEmpty(s)) {
            return false;
        }
        String trimmed = s.trim();
        if (trimmed.length() < 1) {
            return false;
        }
        if (OBFUSCATED_X_ID_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        if (trimmed.matches("^[Xx]\\.[0-9A-Za-z_]+.*") && !containsNaturalLanguage(trimmed)) {
            return false;
        }
        if (trimmed.contains("SemanticsPropertyKey") || trimmed.startsWith("androidx.compose.")) {
            return false;
        }
        if (trimmed.matches("^[\\w.$]+$") && trimmed.contains(".") && !trimmed.contains(" ") && !containsNaturalLanguage(trimmed)) {
            return false;
        }
        if (!containsNaturalLanguage(trimmed)) {
            return false;
        }
        return true;
    }

    private static boolean isInternalDragLabel(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        String lower = text.toString().trim().toLowerCase(Locale.ROOT);
        return lower.equals("drag_file") || lower.equals("drag-file") || lower.equals("drag file");
    }

    private static boolean isMeaningfulUserText(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        String s = text.toString().trim();
        if (s.isEmpty()) {
            return false;
        }
        if (isInternalDragLabel(s)) {
            return false;
        }
        if (!isDragableUserText(s)) {
            return false;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        // 1. Generic media tag matches (exact or with timestamp/status, e.g. "图片, 12:30", "Photo, 12:30 PM")
        if (lower.matches("^(?s)(图片|photo|image|picture|avatar|头像|视频|video|gif)([,，:：\\s\\r\\n].*)?$")) {
            return false;
        }
        // 2. Generic UI tag exact matches
        if (lower.equals("图标") || lower.equals("icon") || lower.equals("表情") || lower.equals("emoji")
                || lower.equals("按钮") || lower.equals("button") || lower.equals("btn")
                || lower.equals("未选中") || lower.equals("已选中") || lower.equals("复选框") || lower.equals("单选按钮")
                || lower.equals("返回") || lower.equals("关闭") || lower.equals("更多") || lower.equals("分享")
                || lower.equals("点赞") || lower.equals("评论") || lower.equals("收藏")
                || lower.equals("查看图片") || lower.equals("查看原图") || lower.equals("原图") || lower.equals("表情包")
                || lower.equals("查看大图") || lower.equals("动图") || lower.equals("gif图") || lower.equals("语音")
                || lower.equals("语音消息") || lower.equals("语音输入") || lower.equals("已发送") || lower.equals("发送中")
                || lower.equals("浮窗") || lower.equals("按住说话") || lower.equals("按住 说话")
                || lower.equals("切换到键盘") || lower.equals("切换到语音") || lower.equals("聊天信息")) {
            return false;
        }
        // 3. Control suffix matches (e.g. 暂停视频按钮, 播放按钮, 发送按钮)
        if (s.endsWith("按钮") || s.endsWith("按键") || s.endsWith("控件")
                || s.endsWith("Button") || s.endsWith("btn")) {
            return false;
        }
        // 4. Action prompt prefixes
        if (s.startsWith("轻触以") || s.startsWith("点击以") || s.startsWith("双击以")
                || s.startsWith("长按以") || s.startsWith("滑动以")) {
            return false;
        }
        // 5. Douyin / video player specific controls
        if (s.contains("暂停视频") || s.contains("播放视频") || s.contains("全屏播放")
                || s.contains("倍速播放") || s.contains("清屏")) {
            return false;
        }
        // 6. Compose / code symbols (ColorOS rule)
        if (s.startsWith("androidx.compose.") || s.contains("SemanticsPropertyKey") || s.startsWith("AccessibilityAction(") || s.contains("action=")
                || s.matches("^[Xx]\\.[0-9A-Za-z_]+.*")) {
            return false;
        }
        // 7. Framework class names / identifiers, not user content
        if (s.startsWith("android.") || s.startsWith("com.android.") || s.startsWith("java.")
                || s.startsWith("kotlin.") || s.matches("^[a-z]+(\\.[a-z0-9_]+){2,}$")) {
            return false;
        }
        return true;
    }

    private static Bundle extractViewGroupText(ViewGroup group) {
        if (group == null) return null;
        StringBuilder sb = new StringBuilder();
        collectTextViewsRecursively(group, sb, 0);
        String text = sb.toString().trim();
        if (isMeaningfulUserText(text)) {
            Bundle b = new Bundle();
            b.putString("viewContent", text);
            b.putInt("catchType", 1);
            return b;
        }
        return null;
    }

    private static void collectTextViewsRecursively(ViewGroup group, StringBuilder sb, int depth) {
        if (group == null || depth > 6 || group.getVisibility() != View.VISIBLE) return;
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;
            if (child instanceof TextView && !(child instanceof EditText)) {
                CharSequence cs = ((TextView) child).getText();
                if (!TextUtils.isEmpty(cs) && isMeaningfulUserText(cs)) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(cs);
                }
            } else if (child instanceof ViewGroup && !isComposeView(child) && !isWebViewLike(child)) {
                collectTextViewsRecursively((ViewGroup) child, sb, depth + 1);
            }
        }
    }

    private static Bundle extractViewGroupImage(ViewGroup group) {
        if (group == null) return null;
        return findFirstImageRecursively(group, 0);
    }

    private static Bundle findFirstImageRecursively(ViewGroup group, int depth) {
        if (group == null || depth > 6 || group.getVisibility() != View.VISIBLE) return null;
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;
            if (isAcceptableImageView(child)) {
                Bitmap bmp = extractImageBitmap(child);
                if (bmp != null) {
                    Bundle b = new Bundle();
                    b.putParcelable("viewBitmap", bmp);
                    b.putInt("catchType", 2);
                    return b;
                }
            } else if (child instanceof ViewGroup && !isComposeView(child) && !isWebViewLike(child)) {
                Bundle b = findFirstImageRecursively((ViewGroup) child, depth + 1);
                if (b != null) return b;
            }
        }
        return null;
    }

    private static Bundle extractTextView(TextView tv) {
        if (tv instanceof EditText) {
            return null;
        }
        int inputType = tv.getInputType();
        int variation = inputType & 0x00000ff0; // TYPE_MASK_VARIATION
        if (variation == 0x80 || variation == 0x90 || variation == 0xe0) {
            return null;
        }
        CharSequence text = tv.getText();
        if (!isMeaningfulUserText(text)) {
            text = tv.getContentDescription();
        }
        if (isMeaningfulUserText(text)) {
            Context ctx = tv.getContext();
            if (ctx != null && "com.tencent.mm".equals(ctx.getPackageName())) {
                text = stripWechatSenderPrefix(text);
            }
            if (isMeaningfulUserText(text)) {
                Bundle bundle = new Bundle();
                bundle.putString("viewContent", text.toString());
                bundle.putInt("catchType", 1);
                return bundle;
            }
        }
        return null;
    }

    /**
     * Many Chinese apps draw text with their own View subclass instead of
     * TextView (Weibo's LayoutTextView, for example). Those views are invisible
     * to the normal TextView path but expose a CharSequence field or getter.
     */
    private static Bundle extractCustomText(View view) {
        return extractCustomText(view, false);
    }

    /**
     * @param force when true the class-name heuristic is skipped: the caller
     *              (a per-app drag rule) already declared this view as text.
     */
    private static Bundle extractCustomText(View view, boolean force) {
        if (view == null) {
            return null;
        }
        if (force) {
            // Never lift an editable/password field, even when a rule names it.
            if (view instanceof EditText) {
                return null;
            }
        } else if (view instanceof TextView || !isTextLikeView(view)) {
            return null;
        }
        String best = null;
        try {
            for (Class<?> type = view.getClass(); type != null && type != Object.class
                    && type != View.class; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    try {
                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers())
                                || method.getParameterCount() != 0) {
                            continue;
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType != CharSequence.class && returnType != String.class
                                && !CharSequence.class.isAssignableFrom(returnType)) {
                            continue;
                        }
                        String methodName = method.getName();
                        if ("toString".equals(methodName) || "getClass".equals(methodName)
                                || "getPackageName".equals(methodName)
                                || methodName.contains("Accessibility")
                                || methodName.contains("ClassName")
                                || methodName.contains("Transition")
                                || methodName.startsWith("set")) {
                            continue;
                        }
                        method.setAccessible(true);
                        Object value = method.invoke(view);
                        if (value instanceof CharSequence && isMeaningfulUserText((CharSequence) value)) {
                            best = pickLongerText(best, value.toString().trim());
                        }
                    } catch (Throwable ignored) {
                    }
                }
                for (Field field : type.getDeclaredFields()) {
                    try {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        if (!CharSequence.class.isAssignableFrom(field.getType())) {
                            continue;
                        }
                        field.setAccessible(true);
                        Object value = field.get(view);
                        if (value instanceof CharSequence && isMeaningfulUserText((CharSequence) value)) {
                            best = pickLongerText(best, value.toString().trim());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (best == null) {
            return null;
        }
        Bundle bundle = new Bundle();
        bundle.putString("viewContent", best);
        bundle.putInt("catchType", 1);
        return bundle;
    }

    private static final java.util.regex.Pattern WECHAT_SENDER_PREFIX_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_\\-]{6,}$");

    private static CharSequence stripWechatSenderPrefix(CharSequence cs) {
        if (TextUtils.isEmpty(cs)) {
            return cs;
        }
        String s = cs.toString();
        int colon = s.indexOf(':');
        if (colon > 0 && colon < s.length() - 1) {
            String prefix = s.substring(0, colon);
            if (!prefix.contains("<")) {
                if (prefix.startsWith("wxid_") || prefix.startsWith("gh_")
                        || WECHAT_SENDER_PREFIX_PATTERN.matcher(prefix).matches()) {
                    return s.substring(colon + 1).trim();
                }
            }
        }
        return cs;
    }

    private static int getViewDepth(View view) {
        int depth = 1;
        ViewParent parent = view != null ? view.getParent() : null;
        while (parent != null) {
            depth++;
            parent = parent.getParent();
        }
        return depth;
    }

    private static boolean isAcceptableImageView(View view) {
        if (view == null) {
            return false;
        }
        String name = view.getClass().getName();
        if (name.contains("Overlay") || name.contains("Background") || name.contains("Decor")
                || name.contains("Layout")) {
            return false;
        }
        if (name.contains("ChatMessageCell") || name.contains("BackupImageView")) {
            return view.getWidth() >= 60 && view.getHeight() >= 60;
        }
        if (view instanceof ImageView) {
            Drawable d = ((ImageView) view).getDrawable();
            if (view.getWidth() < 60 || view.getHeight() < 60) {
                return false;
            }
            // Stock ImageView path, plus custom ImageView subclasses that draw
            // a Bitmap directly and therefore report a null drawable (Weibo's
            // LargeImageView2 / SplitDraggableImageViewV4 are exactly this).
            if (d != null) {
                return !isInvalidViewId(view);
            }
            return isImageLikeView(view) && !isInvalidViewId(view);
        }
        if (view instanceof TextureView) {
            return view.getWidth() >= 60 && view.getHeight() >= 60;
        }
        if (isImageLikeView(view) && view.getWidth() >= 150 && view.getHeight() >= 150) {
            return !isInvalidViewId(view);
        }
        return false;
    }

    private static boolean isLargeImageOnScreen(View view) {
        try {
            DisplayMetrics metrics = view.getResources().getDisplayMetrics();
            return view.getWidth() >= metrics.widthPixels * 0.60f
                    && view.getHeight() >= metrics.heightPixels * 0.25f;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isInvalidViewId(View view) {
        if (view == null || view.getContext() == null) {
            return false;
        }
        String pkg = view.getContext().getPackageName();
        if (pkg == null) {
            return false;
        }
        // Thumbnail blacklists must not reject the full-screen image viewer.
        if (isLargeImageOnScreen(view)) {
            return false;
        }
        // 1. WeChat (com.tencent.mm): reject avatar/thumbnail with empty contentDescription
        if ("com.tencent.mm".equals(pkg)) {
            if ("ImageView".equals(view.getClass().getSimpleName())
                    && TextUtils.isEmpty(view.getContentDescription())) {
                return true;
            }
        }
        // 2. WeCom (com.tencent.wework)
        if ("com.tencent.wework".equals(pkg)) {
            ViewParent p = view.getParent();
            if (p != null && "MessageListVideoContentView".equals(p.getClass().getSimpleName())) {
                return true;
            }
        }

        String entryName = null;
        try {
            int id = view.getId();
            if (id != View.NO_ID && view.getResources() != null) {
                entryName = view.getResources().getResourceEntryName(id);
            }
        } catch (Throwable ignored) {
        }

        // 3. Douyin / TikTok (com.ss.android.ugc.aweme, .lite): only allow specific cover IDs
        if ("com.ss.android.ugc.aweme".equals(pkg) || "com.ss.android.ugc.aweme.lite".equals(pkg)) {
            if (view.getWidth() >= 300 && view.getHeight() >= 300) {
                return false; // large video player / cover view accepted!
            }
            if (entryName == null) {
                return true; // thumbnail rejected
            }
            if (!"q-s".equals(entryName) && !"q-x".equals(entryName) && !"jpy".equals(entryName)
                    && !"jv1".equals(entryName) && !"shh".equals(entryName) && !"kf+".equals(entryName)) {
                return true; // not one of allowed covers -> thumbnail rejected
            }
            return false;
        }

        // 4. Kuaishou (com.smile.gifmaker, com.kuaishou.nebula)
        if ("com.smile.gifmaker".equals(pkg) || "com.kuaishou.nebula".equals(pkg)) {
            boolean ok = "image_view".equals(entryName);
            if (!ok) {
                try {
                    ViewParent p1 = view.getParent();
                    if (p1 != null) {
                        ViewParent p2 = p1.getParent();
                        if (p2 instanceof View && ((View) p2).getId() != View.NO_ID) {
                            String parentEntry = view.getResources().getResourceEntryName(((View) p2).getId());
                            if ("image_root_view".equals(parentEntry)) {
                                ok = true;
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (!ok) {
                return true;
            }
        }

        // 5. JingDong (com.jingdong.app.mall)
        if ("com.jingdong.app.mall".equals(pkg)) {
            if (!"da".equals(entryName) && !"SimpleTouchImageView".equals(view.getClass().getSimpleName())) {
                return true;
            }
        }

        if (entryName == null) {
            return false;
        }

        // 6. Xiaohongshu (com.xingin.xhs): reject icon/avatar IDs
        if ("com.xingin.xhs".equals(pkg)) {
            if ("kdd".equals(entryName) || "jzf".equals(entryName) || "bo4".equals(entryName)
                    || "bo6".equals(entryName) || "k14".equals(entryName) || "kl4".equals(entryName)) {
                return true;
            }
        }
        // 7. Taobao (com.taobao.taobao)
        if ("com.taobao.taobao".equals(pkg)) {
            if ("iv_video_img".equals(entryName)) {
                return true;
            }
        }
        // 8. Bilibili (tv.danmaku.bili)
        if ("tv.danmaku.bili".equals(pkg)) {
            if ("opus_author_avatar".equals(entryName) || "emoticon_icon".equals(entryName)) {
                return true;
            }
        }
        // 9. Weibo (com.sina.weibo)
        if ("com.sina.weibo".equals(pkg)) {
            if ("share_button".equals(entryName) || "ivButton".equals(entryName)
                    || "iv_share_icon".equals(entryName) || "iv_comment_icon".equals(entryName)
                    || "iv_like_icon".equals(entryName)) {
                return true;
            }
        }
        // 10. Zhihu (com.zhihu.android)
        if ("com.zhihu.android".equals(pkg)) {
            if ("emoji_item".equals(entryName) || "sticker".equals(entryName) || "icon".equals(entryName)) {
                return true;
            }
        }
        return false;
    }

    private static Bitmap extractTelegramBitmap(View view) {
        if (view == null) return null;
        try {
            Class<?> clazz = view.getClass();
            String name = clazz.getName();
            if (!name.contains("ChatMessageCell") && !name.contains("BackupImageView")
                    && !name.contains("Photo") && !name.contains("Image")) {
                return null;
            }
            for (Method m : clazz.getMethods()) {
                if (m.getParameterCount() == 0) {
                    String mName = m.getName().toLowerCase(Locale.US);
                    if (mName.equals("getimagereceiver") || mName.equals("getphotoimage")) {
                        m.setAccessible(true);
                        Object receiver = m.invoke(view);
                        Bitmap b = extractBitmapFromReceiver(receiver);
                        if (b != null) return b;
                    }
                }
            }
            for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    String fName = f.getName().toLowerCase(Locale.US);
                    if (fName.contains("photoimage") || fName.contains("imagereceiver")
                            || fName.equals("image")) {
                        f.setAccessible(true);
                        Object receiver = f.get(view);
                        Bitmap b = extractBitmapFromReceiver(receiver);
                        if (b != null) return b;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Bitmap extractBitmapFromReceiver(Object receiver) {
        if (receiver == null) return null;
        try {
            Class<?> rClass = receiver.getClass();
            for (String mName : new String[]{"getBitmapSafe", "getBitmap", "getDrawable"}) {
                try {
                    Method m = rClass.getMethod(mName);
                    m.setAccessible(true);
                    Object res = m.invoke(receiver);
                    if (res instanceof Bitmap && !((Bitmap) res).isRecycled()) {
                        return (Bitmap) res;
                    }
                    if (res instanceof BitmapDrawable) {
                        Bitmap b = ((BitmapDrawable) res).getBitmap();
                        if (b != null && !b.isRecycled()) return b;
                    }
                } catch (Throwable ignored) {}
            }
            for (Field f : rClass.getDeclaredFields()) {
                try {
                    if (Bitmap.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object res = f.get(receiver);
                        if (res instanceof Bitmap && !((Bitmap) res).isRecycled()) {
                            return (Bitmap) res;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Bitmap extractImageBitmap(View view) {
        try {
            Bitmap tg = extractTelegramBitmap(view);
            if (tg != null && !tg.isRecycled() && tg.getWidth() >= 60 && tg.getHeight() >= 60) {
                Log.i(TAG, "extractImageBitmap: extracted Telegram bitmap " + tg.getWidth() + "x" + tg.getHeight());
                return toSoftwareBitmap(tg);
            }
            Bitmap source = null;
            if (view instanceof ImageView) {
                Drawable drawable = ((ImageView) view).getDrawable();
                if (drawable == null) {
                    // Custom ImageView subclasses (Weibo LargeImageView2) draw
                    // the bitmap themselves and return null from getDrawable().
                    source = extractCustomBitmap(view);
                } else if (drawable instanceof BitmapDrawable) {
                    source = ((BitmapDrawable) drawable).getBitmap();
                } else {
                    source = drawableToBitmap(drawable, view.getWidth(), view.getHeight());
                }
            } else if (view instanceof TextureView) {
                source = ((TextureView) view).getBitmap();
            } else if (isImageLikeView(view)) {
                source = extractCustomBitmap(view);
            }
            if (source == null && view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    Bitmap childTg = extractTelegramBitmap(child);
                    if (childTg != null && !childTg.isRecycled()) {
                        source = childTg;
                        break;
                    }
                    if (child instanceof TextureView) {
                        source = ((TextureView) child).getBitmap();
                        if (source != null) break;
                    } else if (child instanceof ImageView) {
                        Drawable d = ((ImageView) child).getDrawable();
                        if (d instanceof BitmapDrawable) {
                            source = ((BitmapDrawable) d).getBitmap();
                            if (source != null) break;
                        } else if (d == null) {
                            source = extractCustomBitmap(child);
                            if (source != null) break;
                        }
                    }
                }
            }
// Removed arbitrary view.draw fallback to prevent capturing background overlays as images
            if (source != null && !source.isRecycled()) {
                if (source.getWidth() >= 60 && source.getHeight() >= 60) {
                    return toSoftwareBitmap(source);
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "extractImageBitmap failed", throwable);
        }
        return null;
    }

    /**
     * Recover a Bitmap from a custom image view that does not expose one via
     * ImageView.getDrawable(). Weibo's LargeImageView2 keeps Bitmap fields E/F,
     * and MblogItemPicView keeps several Drawable fields, so a typed scan is
     * both generic and reliable.
     */
    private static Bitmap extractCustomBitmap(View view) {
        return extractCustomBitmap(view, 0);
    }

    private static List<Bitmap> collectBitmapFieldCandidates(View view) {
        List<Bitmap> result = new ArrayList<>();
        try {
            for (Class<?> type = view.getClass(); type != null && type != Object.class
                    && type != View.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    try {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        Class<?> fieldType = field.getType();
                        if (fieldType == Bitmap.class) {
                            field.setAccessible(true);
                            addBitmapCandidate(result, field.get(view));
                        } else if (fieldType == Bitmap[].class) {
                            field.setAccessible(true);
                            Object value = field.get(view);
                            if (value instanceof Bitmap[]) {
                                for (Bitmap bitmap : (Bitmap[]) value) {
                                    addBitmapCandidate(result, bitmap);
                                }
                            }
                        } else if (fieldType == Object[].class) {
                            field.setAccessible(true);
                            Object value = field.get(view);
                            if (value instanceof Object[]) {
                                for (Object item : (Object[]) value) {
                                    addBitmapCandidate(result, item);
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    private static Bitmap extractCustomBitmap(View view, int depth) {
        if (view == null || depth > 6) {
            return null;
        }
        List<Bitmap> candidates = new ArrayList<>();
        // Weibo's MblogItemPicView / FeedItemPicView keep the real decoded
        // images in an Object[] field N (or direct Bitmap fields). Drawable
        // fields Q..V are placeholders/loading masks and are often fully
        // transparent; scanning them first produced a transparent 755x1006
        // "empty image". Prefer real Bitmap fields/arrays.
        List<Bitmap> directBitmaps = collectBitmapFieldCandidates(view);
        if (!directBitmaps.isEmpty()) {
            Bitmap directBest = chooseBestBitmap(directBitmaps);
            if (directBest != null) {
                return directBest;
            }
        }
        try {
            for (Class<?> type = view.getClass(); type != null && type != Object.class
                    && type != View.class; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    try {
                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers())
                                || method.getParameterCount() != 0) {
                            continue;
                        }
                        String name = method.getName();
                        if ("getDrawingCache".equals(name) || name.startsWith("set")) {
                            continue;
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType == Bitmap.class) {
                            method.setAccessible(true);
                            Object value = method.invoke(view);
                            addBitmapCandidate(candidates, value);
                        } else if (Drawable.class.isAssignableFrom(returnType)) {
                            method.setAccessible(true);
                            Object value = method.invoke(view);
                            addDrawableCandidate(candidates, value, view);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                for (Field field : type.getDeclaredFields()) {
                    try {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        Class<?> fieldType = field.getType();
                        if (fieldType == Bitmap.class) {
                            field.setAccessible(true);
                            addBitmapCandidate(candidates, field.get(view));
                        } else if (fieldType == Bitmap[].class) {
                            field.setAccessible(true);
                            Object value = field.get(view);
                            if (value instanceof Bitmap[]) {
                                for (Bitmap bitmap : (Bitmap[]) value) {
                                    addBitmapCandidate(candidates, bitmap);
                                }
                            }
                        } else if (Drawable.class.isAssignableFrom(fieldType)) {
                            field.setAccessible(true);
                            addDrawableCandidate(candidates, field.get(view), view);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // A custom container may host a real ImageView child.
        if (candidates.isEmpty() && view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = Math.min(group.getChildCount(), 16);
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    Drawable drawable = ((ImageView) child).getDrawable();
                    if (drawable instanceof BitmapDrawable) {
                        addBitmapCandidate(candidates, ((BitmapDrawable) drawable).getBitmap());
                    }
                }
                if (isImageLikeView(child)) {
                    Bitmap nested = extractCustomBitmap(child, depth + 1);
                    addBitmapCandidate(candidates, nested);
                }
            }
        }
        return chooseBestBitmap(candidates);
    }

    private static void addBitmapCandidate(List<Bitmap> candidates, Object value) {
        if (value instanceof Bitmap) {
            Bitmap bitmap = (Bitmap) value;
            if (!bitmap.isRecycled()) {
                candidates.add(bitmap);
            }
        }
    }

    private static void addDrawableCandidate(List<Bitmap> candidates, Object value, View view) {
        if (value instanceof Drawable) {
            Bitmap bitmap = drawableToBitmap((Drawable) value,
                    view.getWidth(), view.getHeight());
            addBitmapCandidate(candidates, bitmap);
        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable, int fallbackWidth, int fallbackHeight) {
        if (drawable == null) {
            return null;
        }
        try {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width <= 0 || height <= 0) {
                width = fallbackWidth;
                height = fallbackHeight;
            }
            if (width <= 0 || height <= 0) {
                return null;
            }
            width = Math.min(width, 4096);
            height = Math.min(height, 4096);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return bitmap;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Bitmap chooseBestBitmap(List<Bitmap> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Bitmap medium = null;
        long mediumArea = -1;
        Bitmap smallest = null;
        int smallestMax = Integer.MAX_VALUE;
        long smallestArea = -1;
        for (Bitmap bitmap : candidates) {
            if (bitmap == null || bitmap.isRecycled()) {
                continue;
            }
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width < 60 || height < 60) {
                continue;
            }
            int max = Math.max(width, height);
            long area = (long) width * height;
            if (max <= 2160 && max >= 300 && area > mediumArea) {
                medium = bitmap;
                mediumArea = area;
            }
            if (max < smallestMax || (max == smallestMax && area > smallestArea)) {
                smallest = bitmap;
                smallestMax = max;
                smallestArea = area;
            }
        }
        return medium != null ? medium : smallest;
    }

    private static Bitmap toSoftwareBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        try {
            Bitmap.Config cfg = bitmap.getConfig();
            if (cfg == Bitmap.Config.HARDWARE || cfg == Bitmap.Config.RGBA_F16) {
                Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                if (copy != null) {
                    return copy;
                }
                Bitmap software = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(software);
                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(bitmap, 0, 0, paint);
                return software;
            }
        } catch (Throwable t) {
            Log.w(TAG, "toSoftwareBitmap failed", t);
            try {
                Bitmap software = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(software);
                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(bitmap, 0, 0, paint);
                return software;
            } catch (Throwable ignored) {
            }
        }
        return bitmap;
    }

    private static boolean isComposeView(View view) {
        if (view == null) {
            return false;
        }
        return viewKind(view) == VIEW_KIND_COMPOSE;
    }

    private static View resolveAndroidComposeView(View view) {
        if (view == null) {
            return null;
        }
        String name = view.getClass().getName();
        if (name.contains("AndroidComposeView")
                || isObfuscatedAndroidComposeView(view)
                || findMethod(view.getClass(), "getSemanticsOwner") != null) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = resolveAndroidComposeView(vg.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
            if (name.contains("ComposeView") && vg.getChildCount() > 0) {
                return vg.getChildAt(0);
            }
        }
        return null;
    }

    private static Bitmap trimLetterbox(Bitmap src) {
        if (src == null || src.isRecycled()) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w < 80 || h < 80) return src;

        try {
            // Row brightness analysis: identify black letterbox rows
            boolean[] isBlackRow = new boolean[h];
            int[] row = new int[w];
            int step = Math.max(1, w / 60);
            for (int y = 0; y < h; y++) {
                src.getPixels(row, 0, w, 0, y, w, 1);
                long sumR = 0, sumG = 0, sumB = 0;
                int maxVal = 0;
                int samples = 0;
                for (int x = 0; x < w; x += step) {
                    int c = row[x];
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int b = c & 0xFF;
                    sumR += r;
                    sumG += g;
                    sumB += b;
                    if (r > maxVal) maxVal = r;
                    if (g > maxVal) maxVal = g;
                    if (b > maxVal) maxVal = b;
                    samples++;
                }
                float mean = (sumR + sumG + sumB) / (3.0f * Math.max(1, samples));
                isBlackRow[y] = (mean < 4.5f) && (maxVal < 40);
            }

            // Find contiguous non-black row segments (filters out floating indicators like "1/9")
            int bestY1 = 0, bestY2 = h - 1;
            int maxH = 0;
            int curStart = -1;
            for (int y = 0; y < h; y++) {
                if (!isBlackRow[y]) {
                    if (curStart < 0) curStart = y;
                } else {
                    if (curStart >= 0) {
                        int segH = y - curStart;
                        if (segH > maxH) {
                            maxH = segH;
                            bestY1 = curStart;
                            bestY2 = y - 1;
                        }
                        curStart = -1;
                    }
                }
            }
            if (curStart >= 0) {
                int segH = h - curStart;
                if (segH > maxH) {
                    maxH = segH;
                    bestY1 = curStart;
                    bestY2 = h - 1;
                }
            }

            if (maxH < 40) {
                return src;
            }

            // Column brightness analysis on the selected row segment
            int croppedH = bestY2 - bestY1 + 1;
            boolean[] isBlackCol = new boolean[w];
            int[] col = new int[croppedH];
            int colStep = Math.max(1, croppedH / 60);
            for (int x = 0; x < w; x++) {
                src.getPixels(col, 0, 1, x, bestY1, 1, croppedH);
                long sumR = 0, sumG = 0, sumB = 0;
                int maxVal = 0;
                int samples = 0;
                for (int y = 0; y < croppedH; y += colStep) {
                    int c = col[y];
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int b = c & 0xFF;
                    sumR += r;
                    sumG += g;
                    sumB += b;
                    if (r > maxVal) maxVal = r;
                    if (g > maxVal) maxVal = g;
                    if (b > maxVal) maxVal = b;
                    samples++;
                }
                float mean = (sumR + sumG + sumB) / (3.0f * Math.max(1, samples));
                isBlackCol[x] = (mean < 4.5f) && (maxVal < 40);
            }

            int bestX1 = 0, bestX2 = w - 1;
            int maxW = 0;
            curStart = -1;
            for (int x = 0; x < w; x++) {
                if (!isBlackCol[x]) {
                    if (curStart < 0) curStart = x;
                } else {
                    if (curStart >= 0) {
                        int segW = x - curStart;
                        if (segW > maxW) {
                            maxW = segW;
                            bestX1 = curStart;
                            bestX2 = x - 1;
                        }
                        curStart = -1;
                    }
                }
            }
            if (curStart >= 0) {
                int segW = w - curStart;
                if (segW > maxW) {
                    maxW = segW;
                    bestX1 = curStart;
                    bestX2 = w - 1;
                }
            }

            if (maxW < 40) {
                bestX1 = 0;
                bestX2 = w - 1;
            }

            if (bestX1 > 0 || bestX2 < w - 1 || bestY1 > 0 || bestY2 < h - 1) {
                int newW = bestX2 - bestX1 + 1;
                int newH = bestY2 - bestY1 + 1;
                Log.i(TAG, "trimLetterbox: (" + w + "x" + h + ") -> (" + newW + "x" + newH + ") at " + bestX1 + "," + bestY1);
                Bitmap trimmed = Bitmap.createBitmap(src, bestX1, bestY1, newW, newH);
                src.recycle();
                return trimmed;
            }
        } catch (Throwable e) {
            Log.w(TAG, "trimLetterbox error", e);
        }
        return src;
    }

    private static void collectAllViews(View root, List<View> out) {
        if (root == null || out.size() > 500) return;
        out.add(root);
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectAllViews(vg.getChildAt(i), out);
            }
        }
    }

    private static Bitmap findNativeImageViewBitmap(View root, float rawX, float rawY) {
        return findNativeImageViewBitmap(root, rawX, rawY, null);
    }

    /**
     * @param nodeBounds when non-null, only native ImageViews that sit *inside*
     *                   this screen rectangle and roughly match its aspect ratio
     *                   are accepted. Without it a Compose page happily returns
     *                   some background ImageView that merely happens to be
     *                   behind the finger (that is how an empty-state panel got
     *                   dragged instead of the comment photo).
     */
    private static Bitmap findNativeImageViewBitmap(View root, float rawX, float rawY, Rect nodeBounds) {
        if (root == null) return null;
        View windowRoot = root.getRootView();
        if (windowRoot == null) windowRoot = root;
        List<View> imageViews = new ArrayList<>();
        collectAllViews(windowRoot, imageViews);

        float nodeAspect = nodeBounds == null ? 0f
                : nodeBounds.width() / (float) Math.max(1, nodeBounds.height());
        Bitmap bestBmp = null;
        int maxArea = 0;
        for (View v : imageViews) {
            if (v instanceof ImageView || v.getClass().getName().contains("ImageView") || v.getClass().getName().contains("PhotoView")) {
                int[] loc = new int[2];
                v.getLocationOnScreen(loc);
                Rect r = new Rect(loc[0], loc[1], loc[0] + v.getWidth(), loc[1] + v.getHeight());
                if (nodeBounds != null) {
                    if (!nodeBounds.contains(r) || r.width() < 32 || r.height() < 32) {
                        continue;
                    }
                    float viewAspect = r.width() / (float) Math.max(1, r.height());
                    if (viewAspect < nodeAspect * 0.7f || viewAspect > nodeAspect * 1.4f) {
                        continue;
                    }
                }
                Bitmap bmp = extractImageBitmap(v);
                if (bmp != null && !bmp.isRecycled() && bmp.getWidth() > 100 && bmp.getHeight() > 100) {
                    if (r.contains((int) rawX, (int) rawY)) {
                        Log.i(TAG, "findNativeImageViewBitmap: hit under touch (" + bmp.getWidth() + "x" + bmp.getHeight() + ") " + v.getClass().getName()
                                + (nodeBounds == null ? "" : " node=" + nodeBounds.toShortString()));
                        return bmp;
                    }
                    int area = bmp.getWidth() * bmp.getHeight();
                    if (area > maxArea) {
                        maxArea = area;
                        bestBmp = bmp;
                    }
                }
            }
        }
        if (bestBmp != null) {
            Log.i(TAG, "findNativeImageViewBitmap: picked largest candidate (" + bestBmp.getWidth() + "x" + bestBmp.getHeight() + ")");
        }
        return bestBmp;
    }

    /**
     * Cheap "is there actually a picture here" check for bitmap payloads that
     * came from a cropped render: a flat panel / solid background is rejected.
     */
    private static boolean bitmapHasContent(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return false;
        try {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (w < 16 || h < 16) return false;
            int stepX = Math.max(1, w / 24);
            int stepY = Math.max(1, h / 24);
            long sum = 0, sumSq = 0;
            int n = 0;
            java.util.HashSet<Integer> colors = new java.util.HashSet<>();
            for (int y = stepY / 2; y < h; y += stepY) {
                for (int x = stepX / 2; x < w; x += stepX) {
                    int c = bitmap.getPixel(x, y);
                    int lum = ((c >> 16 & 0xff) * 30 + (c >> 8 & 0xff) * 59 + (c & 0xff) * 11) / 100;
                    sum += lum;
                    sumSq += (long) lum * lum;
                    n++;
                    if (colors.size() < 64) colors.add(c & 0x00f0f0f0);
                }
            }
            if (n < 16) return true;
            double mean = sum / (double) n;
            double var = sumSq / (double) n - mean * mean;
            double std = var <= 0 ? 0 : Math.sqrt(var);
            Log.d(TAG, "bitmapHasContent std=" + (int) std + " colors=" + colors.size());
            return std >= 6.0 || colors.size() >= 6;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static Bitmap extractBitmapFromComposeSemantics(Object node) {
        if (node == null) return null;
        try {
            // 1. Direct inspection of node fields
            Bitmap direct = findBitmapInObject(node, 0, new HashSet<>());
            if (direct != null) return direct;

            // 2. SemanticsNode.layoutNode
            Object layoutNode = null;
            for (Method m : node.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().contains("Layout") || m.getReturnType().getSimpleName().contains("LayoutNode"))) {
                    try {
                        m.setAccessible(true);
                        layoutNode = m.invoke(node);
                        if (layoutNode != null) break;
                    } catch (Throwable ignored) {}
                }
            }
            if (layoutNode == null) {
                for (Field f : node.getClass().getDeclaredFields()) {
                    if (f.getType().getSimpleName().contains("LayoutNode")) {
                        try {
                            f.setAccessible(true);
                            layoutNode = f.get(node);
                            if (layoutNode != null) break;
                        } catch (Throwable ignored) {}
                    }
                }
            }

            if (layoutNode != null) {
                Bitmap fromLayout = findBitmapInLayoutNode(layoutNode);
                if (fromLayout != null) return fromLayout;
            }
        } catch (Throwable e) {
            Log.w(TAG, "extractBitmapFromComposeSemantics err: " + e);
        }
        return null;
    }

    private static Bitmap findBitmapInLayoutNode(Object layoutNode) {
        if (layoutNode == null) return null;
        try {
            for (Field f : layoutNode.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(layoutNode);
                if (val == null) continue;
                String typeName = val.getClass().getSimpleName();
                if (typeName.contains("NodeChain") || typeName.contains("Modifier")) {
                    Bitmap bmp = findBitmapInObject(val, 0, new HashSet<>());
                    if (bmp != null) return bmp;
                }
            }
            for (Method m : layoutNode.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getNodes") || m.getName().endsWith("nodes"))) {
                    m.setAccessible(true);
                    Object val = m.invoke(layoutNode);
                    if (val != null) {
                        Bitmap bmp = findBitmapInObject(val, 0, new HashSet<>());
                        if (bmp != null) return bmp;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Bitmap findBitmapInObject(Object obj, int depth, Set<Object> visited) {
        if (obj == null || depth > 5 || visited.contains(obj)) return null;
        visited.add(obj);

        Class<?> c = obj.getClass();
        String cName = c.getName();
        if (cName.startsWith("android.view.") || cName.startsWith("java.lang.")) return null;

        if (obj instanceof Bitmap) {
            Bitmap b = (Bitmap) obj;
            if (!b.isRecycled() && b.getWidth() > 100 && b.getHeight() > 100) return b;
        }
        if (obj instanceof android.graphics.drawable.BitmapDrawable) {
            Bitmap b = ((android.graphics.drawable.BitmapDrawable) obj).getBitmap();
            if (b != null && !b.isRecycled() && b.getWidth() > 100 && b.getHeight() > 100) return b;
        }

        if (cName.contains("ImageBitmap")) {
            for (Method m : c.getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == Bitmap.class) {
                    try {
                        m.setAccessible(true);
                        Bitmap b = (Bitmap) m.invoke(obj);
                        if (b != null && !b.isRecycled() && b.getWidth() > 100) return b;
                    } catch (Throwable ignored) {}
                }
            }
        }

        if (cName.contains("Success") || cName.contains("AsyncImage")) {
            for (Method m : c.getMethods()) {
                if (m.getParameterCount() == 0 && android.graphics.drawable.Drawable.class.isAssignableFrom(m.getReturnType())) {
                    try {
                        m.setAccessible(true);
                        Object d = m.invoke(obj);
                        if (d instanceof android.graphics.drawable.BitmapDrawable) {
                            Bitmap b = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
                            if (b != null && !b.isRecycled() && b.getWidth() > 100) return b;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }

        for (Class<?> cur = c; cur != null && cur != Object.class && !cur.getName().startsWith("android.view.View"); cur = cur.getSuperclass()) {
            for (Field f : cur.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                Class<?> ft = f.getType();
                if (ft.isPrimitive() || ft == String.class) continue;
                try {
                    f.setAccessible(true);
                    Object fVal = f.get(obj);
                    if (fVal == null) continue;
                    if (fVal instanceof Bitmap) {
                        Bitmap b = (Bitmap) fVal;
                        if (!b.isRecycled() && b.getWidth() > 100 && b.getHeight() > 100) return b;
                    }
                    if (fVal instanceof android.graphics.drawable.BitmapDrawable) {
                        Bitmap b = ((android.graphics.drawable.BitmapDrawable) fVal).getBitmap();
                        if (b != null && !b.isRecycled() && b.getWidth() > 100 && b.getHeight() > 100) return b;
                    }
                    String fvName = fVal.getClass().getName();
                    if (fvName.contains("Painter") || fvName.contains("ImageBitmap") || fvName.contains("AsyncImage") || fvName.contains("Success") || fvName.contains("Drawable") || fvName.contains("Modifier")) {
                        Bitmap nested = findBitmapInObject(fVal, depth + 1, visited);
                        if (nested != null) return nested;
                    }
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static void collectNativeViewsUnderPoint(View root, float rawX, float rawY, List<View> out) {
        if (root == null || root.getVisibility() != View.VISIBLE) return;
        int[] loc = new int[2];
        root.getLocationOnScreen(loc);
        int left = loc[0];
        int top = loc[1];
        int right = left + root.getWidth();
        int bottom = top + root.getHeight();
        if (rawX < left - 30 || rawX > right + 30 || rawY < top - 30 || rawY > bottom + 30) {
            return;
        }
        if (root instanceof ImageView || root instanceof TextView) {
            out.add(0, root);
        } else if (!isComposeView(root)) {
            out.add(root);
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                collectNativeViewsUnderPoint(vg.getChildAt(i), rawX, rawY, out);
            }
        }
    }

    private static Bundle extractHybridComposeContent(View composeView, float rawX, float rawY,
                                                     boolean wantText, boolean wantBitmap) {
        if (composeView == null) return null;
        View actual = resolveAndroidComposeView(composeView);
        View root = actual != null ? actual : composeView;

        List<View> nativeViews = new ArrayList<>();
        collectNativeViewsUnderPoint(root, rawX, rawY, nativeViews);

        for (View v : nativeViews) {
            if (isWebViewLike(v)) {
                Bundle web = webViewBundle(v);
                if (web != null && isBundleMeaningful(web)) {
                    Log.i(TAG, "hybrid compose hit WebView " + v.getClass().getName());
                    return web;
                }
            }
            if (wantBitmap) {
                Bundle file = extractFile(v);
                if (file != null && isBundleMeaningful(file)) {
                    Log.i(TAG, "hybrid compose hit file " + v.getClass().getName());
                    return file;
                }
                if (isAcceptableImageView(v) || v instanceof ImageView) {
                    Bitmap bmp = extractImageBitmap(v);
                    if (bmp != null && !bmp.isRecycled()) {
                        Bundle b = new Bundle();
                        b.putParcelable("viewBitmap", toSoftwareBitmap(bmp));
                        b.putInt("catchType", 2);
                        Log.i(TAG, "hybrid compose hit native ImageView " + bmp.getWidth() + "x" + bmp.getHeight());
                        return b;
                    }
                }
            }
            if (wantText) {
                Bundle custom = extractCustomText(v);
                if (custom != null && isBundleMeaningful(custom)) return custom;
                if (v instanceof TextView) {
                    Bundle tv = extractTextView((TextView) v);
                    if (tv != null && isBundleMeaningful(tv)) return tv;
                }
            }
        }
        return null;
    }

    private static Bundle extractCompose(View composeView, float rawX, float rawY,
                                         boolean wantText, boolean wantBitmap) {
        View actualComposeView = resolveAndroidComposeView(composeView);
        if (actualComposeView == null) {
            actualComposeView = composeView;
        }
        try {
            if (actualComposeView == null) {
                actualComposeView = composeView;
            }
            int[] loc = new int[2];
            actualComposeView.getLocationOnScreen(loc);
            float localX = rawX - loc[0];
            float localY = rawY - loc[1];
            int viewWidth = actualComposeView.getWidth();
            int viewHeight = actualComposeView.getHeight();
            if (localX < -36 || localX > viewWidth + 36 || localY < -36 || localY > viewHeight + 36) {
                return null;
            }

            Object owner = getComposeSemanticsOwner(actualComposeView);
            if (owner != null) {
                Object rootNode = getComposeUnmergedRoot(owner);
                if (rootNode != null) {
                    // Bounds-space calibration. Semantics bounds are usually in the
                    // hosting AndroidComposeView's local space, but for nested
                    // compositions (LazyColumn inside a Dialog / second ComposeView)
                    // they are root/window based instead. Testing both spaces
                    // independently made a node 446px away look like an "exact hit"
                    // while its distance said otherwise. Pick ONE space: whichever
                    // one puts the finger inside the composition root, then use it
                    // for containment, exactness and distance alike.
                    Rect rootBounds = getComposeNodeBounds(rootNode);
                    float touchX = localX;
                    float touchY = localY;
                    String space = "local";
                    if (rootBounds != null
                            && !rootBounds.contains((int) localX, (int) localY)
                            && rootBounds.contains((int) rawX, (int) rawY)) {
                        touchX = rawX;
                        touchY = rawY;
                        space = "raw";
                    }
                    Log.i(TAG, "compose xy raw=(" + (int) rawX + "," + (int) rawY + ")"
                            + " loc=(" + loc[0] + "," + loc[1] + ")"
                            + " local=(" + (int) localX + "," + (int) localY + ")"
                            + " touch=(" + (int) touchX + "," + (int) touchY + ") space=" + space
                            + " rootBounds=" + (rootBounds == null ? "null" : rootBounds.toShortString()));

                    List<ComposeSemanticsMatch> candidates = new ArrayList<>();
                    collectComposeCandidates(rootNode, 0, touchX, touchY, touchX, touchY, viewWidth, viewHeight, candidates);
                    Log.i(TAG, "compose owner=" + owner.getClass().getName()
                            + " root=" + rootNode.getClass().getName()
                            + " candidates=" + candidates.size());

                    ComposeSemanticsMatch best = pickBestComposeMatch(candidates, viewWidth, viewHeight);
                    if (best != null) {
                        Log.i(TAG, "compose pick " + best.describe()
                                + (best.hasText ? " text=«" + previewText(best.text) + "»" : ""));
                    }
                    for (int ci = 0; ci < candidates.size() && ci < 12; ci++) {
                        ComposeSemanticsMatch cm = candidates.get(ci);
                        Log.d(TAG, "  cand[" + ci + "] " + cm.describe()
                                + (cm.hasText ? " «" + previewText(cm.text) + "»" : ""));
                    }
                    if (best == null && wantBitmap) {
                        ComposeSemanticsMatch geom = pickBestEmptyImageCandidate(candidates, viewWidth, viewHeight);
                        if (geom != null && geom.bounds != null) {
                            Log.i(TAG, "compose empty image candidate bounds=" + geom.bounds
                                    + " depth=" + geom.depth + " area=" + geom.area);
                            Bundle geomResult = cropComposeImage(actualComposeView, geom.bounds, "semantics-empty");
                            if (geomResult != null && isBundleMeaningful(geomResult)) {
                                return geomResult;
                            }
                        }
                    }
                    if (best != null) {
                        if (wantText && !TextUtils.isEmpty(best.text) && isMeaningfulUserText(best.text)) {
                            Bundle b = new Bundle();
                            b.putString("viewContent", best.text);
                            b.putInt("catchType", 1);
                            Log.i(TAG, "compose hit text=" + best.text.length() + " depth=" + best.depth);
                            return b;
                        }
                        if (wantBitmap && (best.isImageLike || best.geometryImage) && best.bounds != null) {
                            Rect b = best.bounds;
                            int x = Math.max(0, b.left);
                            int y = Math.max(0, b.top);
                            int w = Math.min(viewWidth - x, b.width());
                            int h = Math.min(viewHeight - y, b.height());
                            if (w >= 32 && h >= 32) {
                                try {
                                    // 1. Try to find embedded native ImageView under best.bounds first!
                                    Bundle hybrid = extractHybridComposeContent(actualComposeView,
                                            loc[0] + x + w / 2f, loc[1] + y + h / 2f, false, true);
                                    if (hybrid != null && hybrid.getParcelable("viewBitmap") != null) {
                                        return hybrid;
                                    }

                                    // 2. Try to extract original lossless Bitmap directly from Compose Semantics / Painter / Coil!
                                    Bitmap directBmp = extractBitmapFromComposeSemantics(best.node);
                                    if (directBmp != null && !directBmp.isRecycled()) {
                                        Log.i(TAG, "compose hit direct lossless Bitmap " + directBmp.getWidth() + "x" + directBmp.getHeight());
                                        Bundle res = new Bundle();
                                        res.putParcelable("viewBitmap", toSoftwareBitmap(directBmp));
                                        res.putInt("catchType", 2);
                                        return res;
                                    }

                                    // 3. Try to find any native ImageView / PhotoView in the window hierarchy
                                    Rect nodeScreen = new Rect(loc[0] + x, loc[1] + y,
                                            loc[0] + x + w, loc[1] + y + h);
                                    Bitmap winBmp = findNativeImageViewBitmap(actualComposeView, rawX, rawY, nodeScreen);
                                    if (winBmp != null && !winBmp.isRecycled() && bitmapHasContent(winBmp)) {
                                        Log.i(TAG, "compose hit window ImageView Bitmap " + winBmp.getWidth() + "x" + winBmp.getHeight());
                                        Bundle res = new Bundle();
                                        res.putParcelable("viewBitmap", toSoftwareBitmap(winBmp));
                                        res.putInt("catchType", 2);
                                        return res;
                                    }
                                    if (winBmp != null) {
                                        Log.i(TAG, "compose window ImageView rejected (no content / outside node " + nodeScreen.toShortString() + ")");
                                    }

                                    // 4. Compose screenshot fallback with smart segmented trimming
                                    // Only screenshot fallback enforces aspect ratio to avoid capturing dividers / background bars
                                    float aspect = w / (float) Math.max(1, h);
                                    if (aspect >= 0.5f && aspect <= 2.0f) {
                                        Bitmap full = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
                                        actualComposeView.draw(new Canvas(full));
                                        Bitmap crop = Bitmap.createBitmap(full, x, y, w, h);
                                        full.recycle();
                                        Bitmap clean = trimLetterbox(crop);
                                        if (clean != null && !clean.isRecycled()) {
                                            float cleanAspect = clean.getWidth() / (float) Math.max(1, clean.getHeight());
                                            if (cleanAspect >= 0.5f && cleanAspect <= 2.0f
                                                    && bitmapHasContent(clean)) {
                                                Bundle res = new Bundle();
                                                res.putParcelable("viewBitmap", toSoftwareBitmap(clean));
                                                res.putInt("catchType", 2);
                                                Log.i(TAG, "compose hit image (screenshot fallback cropped) " + clean.getWidth() + "x" + clean.getHeight() + " depth=" + best.depth);
                                                return res;
                                            } else {
                                                Log.i(TAG, "compose cropped bitmap rejected (aspect=" + cleanAspect
                                                        + " empty=" + !bitmapHasContent(clean) + ")");
                                            }
                                        }
                                    } else {
                                        Log.i(TAG, "compose screenshot candidate rejected by aspect ratio: " + aspect + " (" + w + "x" + h + ")");
                                    }
                                } catch (Throwable drawErr) {
                                    Log.w(TAG, "compose draw bitmap failed: " + drawErr);
                                }
                            }
                        }
                    }

                    // ColorOS OplusComposeViewDragProcessor fallback:
                    // When the finger touches a chat bubble / card container's padding or a row node
                    // whose text lives in child SemanticsNodes, aggregate text from the deepest container row's subtree!
                    if (wantText && !candidates.isEmpty()) {
                        String rowText = collectTextFromCandidateSubtrees(candidates, viewWidth, viewHeight);
                        if (!TextUtils.isEmpty(rowText) && isMeaningfulUserText(rowText)) {
                            Bundle b = new Bundle();
                            b.putString("viewContent", rowText);
                            b.putInt("catchType", 1);
                            Log.i(TAG, "compose hit row subtree text=" + rowText.length());
                            return b;
                        }
                    }
                    // Finger in a gap between rows: take the single text node
                    // closest to it instead of the whole visible list.
                    if (wantText) {
                        String nearest = nearestComposeText(rootNode, touchX, touchY, 140);
                        if (!TextUtils.isEmpty(nearest) && isMeaningfulUserText(nearest)) {
                            Bundle b = new Bundle();
                            b.putString("viewContent", nearest);
                            b.putInt("catchType", 1);
                            Log.i(TAG, "compose hit nearest text=" + nearest.length()
                                    + " «" + previewText(nearest) + "»");
                            return b;
                        }
                    }
                }
            } else {
                Log.i(TAG, "compose semanticsOwner miss on " + actualComposeView.getClass().getName());
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "extractCompose failed: " + throwable);
        }
        if (wantBitmap) {
            Bundle geom = extractComposeImageFallback(actualComposeView, rawX, rawY);
            if (geom != null && isBundleMeaningful(geom)) {
                return geom;
            }
        }
        if (wantText) {
            String a11y = accessibilityText(actualComposeView, new PointF(rawX, rawY));
            if (isMeaningfulUserText(a11y)) {
                Bundle b = new Bundle();
                b.putString("viewContent", a11y);
                b.putInt("catchType", 1);
                Log.i(TAG, "compose hit via accessibility: " + a11y.length() + " chars");
                return b;
            }
        }
        return null;
    }

    /**
     * Last-resort image crop for Compose viewers whose image is a pure Canvas /
     * Painter draw and therefore has no semantics text, ContentDescription or
     * Role.Image. The accessibility tree still carries the image layout bounds
     * (e.g. Bilibili's comment image viewer: 1220x915 inside a full-screen
     * overlay). Crop only that rectangle from the ComposeView; never capture
     * the whole screen.
     */
    private static Bundle extractComposeImageFallback(View composeView, float rawX, float rawY) {
        if (composeView == null || composeView.getWidth() < 80 || composeView.getHeight() < 80) {
            return null;
        }
        try {
            Context ctx = composeView.getContext();
            if (ctx == null || !"tv.danmaku.bili".equals(ctx.getPackageName())) {
                return null;
            }
            Rect screen = findAccessibilityImageBounds(composeView, rawX, rawY);
            if (screen == null) {
                Log.i(TAG, "compose image fallback: no accessibility bounds under touch");
                return null;
            }
            int[] loc = new int[2];
            composeView.getLocationOnScreen(loc);
            Rect local = new Rect(screen.left - loc[0], screen.top - loc[1],
                    screen.right - loc[0], screen.bottom - loc[1]);
            return cropComposeImage(composeView, local, "accessibility");
        } catch (Throwable throwable) {
            Log.w(TAG, "compose image fallback failed", throwable);
            return null;
        }
    }

    private static Bundle cropComposeImage(View composeView, Rect bounds, String source) {
        if (composeView == null || bounds == null) {
            return null;
        }
        try {
            int viewWidth = composeView.getWidth();
            int viewHeight = composeView.getHeight();
            int x = Math.max(0, bounds.left);
            int y = Math.max(0, bounds.top);
            int w = Math.min(viewWidth - x, bounds.width());
            int h = Math.min(viewHeight - y, bounds.height());
            if (w < 32 || h < 32) {
                return null;
            }
            Bitmap full = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
            composeView.draw(new Canvas(full));
            Bitmap crop = Bitmap.createBitmap(full, x, y, w, h);
            full.recycle();
            Bitmap clean = trimLetterbox(crop);
            if (clean == null || clean.isRecycled()) {
                return null;
            }
            float aspect = clean.getWidth() / (float) Math.max(1, clean.getHeight());
            if (aspect < 0.5f || aspect > 2.0f) {
                Log.i(TAG, "compose image crop rejected by aspect: " + aspect);
                clean.recycle();
                return null;
            }
            if (!bitmapHasContent(clean)) {
                // Cropping a flat region means we picked a background / empty
                // state panel, not a picture. Refuse instead of dragging it.
                Log.i(TAG, "compose image crop rejected: flat region source=" + source
                        + " bounds=" + bounds.toShortString());
                clean.recycle();
                return null;
            }
            Bundle result = new Bundle();
            result.putParcelable("viewBitmap", toSoftwareBitmap(clean));
            result.putInt("catchType", 2);
            Log.i(TAG, "compose image crop source=" + source + " bounds=" + bounds
                    + " crop=" + clean.getWidth() + "x" + clean.getHeight());
            return result;
        } catch (Throwable throwable) {
            Log.w(TAG, "cropComposeImage failed", throwable);
            return null;
        }
    }

    /**
     * Geometry-only image candidate, used when no semantics text / image-like
     * node was found under the finger.
     *
     * This used to accept any childless-looking big rectangle, which meant a
     * press in a gap cropped the whole comment list ("screenshot of the visible
     * comments"). Now it must be a real leaf, close to the finger, and of
     * picture-like size — a container is never an image.
     */
    private static ComposeSemanticsMatch pickBestEmptyImageCandidate(List<ComposeSemanticsMatch> candidates,
                                                                     int viewWidth, int viewHeight) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int viewArea = Math.max(1, viewWidth * viewHeight);
        ComposeSemanticsMatch best = null;
        for (ComposeSemanticsMatch m : candidates) {
            if (m == null || m.bounds == null || m.hasText || m.isImageLike) {
                continue;
            }
            if (m.childCount != 0) {
                continue;
            }
            if (m.distance > 60) {
                continue;
            }
            float aspect = m.bounds.width() / (float) Math.max(1, m.bounds.height());
            if (aspect < 0.5f || aspect > 2.0f) {
                continue;
            }
            if (m.area < viewArea * 0.02f || m.area > viewArea * 0.60f) {
                continue;
            }
            if (best == null || m.area < best.area
                    || (m.area == best.area && m.depth > best.depth)) {
                best = m;
            }
        }
        return best;
    }

    private static Rect findAccessibilityImageBounds(View composeView, float rawX, float rawY) {
        try {
            AccessibilityNodeInfo root = composeView.createAccessibilityNodeInfo();
            if (root == null) {
                return null;
            }
            Rect[] best = new Rect[]{null};
            int[] bestArea = new int[]{Integer.MAX_VALUE};
            int viewArea = Math.max(1, composeView.getWidth() * composeView.getHeight());
            collectAccessibilityImageBounds(root, 0, rawX, rawY, viewArea, best, bestArea);
            return best[0];
        } catch (Throwable throwable) {
            Log.w(TAG, "findAccessibilityImageBounds failed", throwable);
            return null;
        }
    }

    private static void collectAccessibilityImageBounds(AccessibilityNodeInfo node, int depth,
                                                        float rawX, float rawY, int viewArea,
                                                        Rect[] best, int[] bestArea) {
        if (node == null || depth > 30) {
            return;
        }
        try {
            int count = Math.min(node.getChildCount(), 64);
            for (int i = 0; i < count; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child == null) {
                    continue;
                }
                Rect rect = new Rect();
                child.getBoundsInScreen(rect);
                if (rect.width() < 150 || rect.height() < 150
                        || rawX < rect.left - 24 || rawX > rect.right + 24
                        || rawY < rect.top - 24 || rawY > rect.bottom + 24) {
                    collectAccessibilityImageBounds(child, depth + 1, rawX, rawY, viewArea, best, bestArea);
                    continue;
                }
                CharSequence text = child.getText();
                if (!isMeaningfulUserText(text)) {
                    text = child.getContentDescription();
                }
                float aspect = rect.width() / (float) Math.max(1, rect.height());
                int area = rect.width() * rect.height();
                boolean bigEnough = area >= viewArea * 0.10f
                        && rect.width() >= 150
                        && rect.height() >= 150;
                if (!isMeaningfulUserText(text) && bigEnough
                        && aspect >= 0.5f && aspect <= 2.0f && area < bestArea[0]) {
                    best[0] = new Rect(rect);
                    bestArea[0] = area;
                }
                collectAccessibilityImageBounds(child, depth + 1, rawX, rawY, viewArea, best, bestArea);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isPlausibleSemanticsNodeClass(Class<?> nc) {
        if (nc == null || nc.isPrimitive() || nc.isArray() || nc == Object.class || nc == String.class) {
            return false;
        }
        String name = nc.getName();
        if (name.startsWith("java.") || name.startsWith("android.") || name.startsWith("kotlin.")) {
            return false;
        }
        if (name.contains("SemanticsNode")) {
            return true;
        }
        boolean hasListReturn = false;
        boolean hasBoxReturn = false;
        try {
            for (Method m : nc.getDeclaredMethods()) {
                if (List.class.isAssignableFrom(m.getReturnType())) {
                    hasListReturn = true;
                }
                if (m.getParameterCount() == 0 && hasFourBoxFieldsClass(m.getReturnType())) {
                    hasBoxReturn = true;
                }
            }
        } catch (Throwable ignored) {
        }
        return hasListReturn && hasBoxReturn;
    }

    private static boolean hasFourBoxFieldsClass(Class<?> c) {
        if (c == null || c.isPrimitive() || c.isArray() || c == Object.class || c == String.class) {
            return false;
        }
        try {
            int floatCount = 0;
            for (Field f : c.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && (f.getType() == float.class || f.getType() == Float.class)) {
                    floatCount++;
                }
            }
            return floatCount == 4;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object getComposeSemanticsOwner(View view) {
        View target = resolveAndroidComposeView(view);
        if (target == null) {
            target = view;
        }
        try {
            Method m = findMethod(target.getClass(), "getSemanticsOwner");
            if (m != null) {
                m.setAccessible(true);
                Object res = m.invoke(target);
                if (res != null) return res;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Class<?> c = target.getClass(); c != null && c != ViewGroup.class && c != Object.class; c = c.getSuperclass()) {
                Field f = findFieldOrNull(c, "semanticsOwner");
                if (f != null) {
                    f.setAccessible(true);
                    Object res = f.get(target);
                    if (res != null) return res;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().contains("SemanticsOwner")) {
                    m.setAccessible(true);
                    Object res = m.invoke(target);
                    if (res != null) return res;
                }
            }
        } catch (Throwable ignored) {
        }
        // Full R8 obfuscation fallback: match field/method whose type has a 0-arg method returning SemanticsNode
        try {
            for (Class<?> c = target.getClass(); c != null && c != ViewGroup.class && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    Class<?> ft = f.getType();
                    if (ft.isPrimitive() || ft.isArray() || View.class.isAssignableFrom(ft)) continue;
                    String fn = ft.getName();
                    if (fn.startsWith("java.") || fn.startsWith("android.") || fn.startsWith("kotlin.")) continue;
                    for (Method m : ft.getDeclaredMethods()) {
                        if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())
                                && m.getParameterCount() == 0
                                && isPlausibleSemanticsNodeClass(m.getReturnType())) {
                            f.setAccessible(true);
                            Object cand = f.get(target);
                            if (cand != null) {
                                return cand;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getComposeUnmergedRoot(Object owner) {
        if (owner == null) {
            return null;
        }
        try {
            Method m = findMethod(owner.getClass(), "getUnmergedRootSemanticsNode");
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(owner);
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = findMethod(owner.getClass(), "getRootSemanticsNode");
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(owner);
            }
        } catch (Throwable ignored) {
        }
        // ProGuard fallback (e.g. Ldf9;->a()Laf9; or so5.a() -> po5):
        // Prefer the method returning a SemanticsNode whose mergingEnabled (boolean field) is false (unmerged root)
        Object firstNodeCandidate = null;
        try {
            for (Method m : owner.getClass().getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 0) {
                    continue;
                }
                Class<?> rt = m.getReturnType();
                if (rt.isPrimitive() || rt == void.class || rt == String.class) {
                    continue;
                }
                if (isPlausibleSemanticsNodeClass(rt) || "a".equals(m.getName())) {
                    try {
                        m.setAccessible(true);
                        Object res = m.invoke(owner);
                        if (res != null) {
                            if (firstNodeCandidate == null) {
                                firstNodeCandidate = res;
                            }
                            if (isUnmergedSemanticsNode(res)) {
                                return res;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return firstNodeCandidate;
    }

    private static boolean isUnmergedSemanticsNode(Object node) {
        if (node == null) return false;
        try {
            for (Field f : node.getClass().getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == boolean.class) {
                    f.setAccessible(true);
                    return !f.getBoolean(node);
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Rect getComposeNodeBounds(Object node) {
        if (node == null) {
            return null;
        }
        String[] preferredNames = new String[]{"getBoundsInRoot", "g", "getBoundsInWindow", "h", "m"};
        for (String name : preferredNames) {
            try {
                Method m = findMethod(node.getClass(), name);
                if (m != null && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    Object boundsObj = m.invoke(node);
                    Rect r = toRectFromFourBox(boundsObj);
                    if (r != null && (r.width() > 0 || r.height() > 0)) {
                        return r;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        Rect fallbackEmpty = null;
        try {
            for (Method m : node.getClass().getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        && m.getParameterCount() == 0
                        && !m.getReturnType().isPrimitive()
                        && m.getReturnType() != void.class
                        && m.getReturnType() != List.class) {
                    try {
                        m.setAccessible(true);
                        Object res = m.invoke(node);
                        if (res != null && hasFourBoxFields(res)) {
                            Rect r = toRectFromFourBox(res);
                            if (r != null) {
                                if (r.width() > 0 || r.height() > 0) {
                                    return r;
                                }
                                if (fallbackEmpty == null) {
                                    fallbackEmpty = r;
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return fallbackEmpty;
    }

    private static Rect toRectFromFourBox(Object boundsObj) {
        if (boundsObj == null) return null;
        try {
            Class<?> bc = boundsObj.getClass();
            Field leftF = findFieldIgnoreCase(bc, "left", "a");
            Field topF = findFieldIgnoreCase(bc, "top", "b");
            Field rightF = findFieldIgnoreCase(bc, "right", "c");
            Field bottomF = findFieldIgnoreCase(bc, "bottom", "d");
            if (leftF != null && topF != null && rightF != null && bottomF != null) {
                leftF.setAccessible(true);
                topF.setAccessible(true);
                rightF.setAccessible(true);
                bottomF.setAccessible(true);
                float l = ((Number) leftF.get(boundsObj)).floatValue();
                float t = ((Number) topF.get(boundsObj)).floatValue();
                float r = ((Number) rightF.get(boundsObj)).floatValue();
                float b = ((Number) bottomF.get(boundsObj)).floatValue();
                return new Rect(Math.round(l), Math.round(t), Math.round(r), Math.round(b));
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean hasFourBoxFields(Object obj) {
        if (obj == null) return false;
        int floatCount = 0;
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.getType() == float.class || f.getType() == Float.class || f.getType() == int.class) {
                floatCount++;
            }
        }
        return floatCount >= 4;
    }

    private static boolean boundsContainExactTouch(Rect bounds, float localX, float localY, float rawX, float rawY) {
        if (bounds == null) return false;
        if (bounds.contains((int) localX, (int) localY)) {
            return true;
        }
        if ((localX != rawX || localY != rawY) && bounds.contains((int) rawX, (int) rawY)) {
            return true;
        }
        return false;
    }

    private static boolean boundsContainTouch(Rect bounds, float localX, float localY, float rawX, float rawY) {
        if (bounds == null) return false;
        if (boundsContainExactTouch(bounds, localX, localY, rawX, rawY)) {
            return true;
        }
        // Tight 12px (~4dp) slop so adjacent buttons/lines are not mis-selected with an offset
        Rect slop = new Rect(bounds.left - 12, bounds.top - 12, bounds.right + 12, bounds.bottom + 12);
        if (slop.contains((int) localX, (int) localY)) {
            return true;
        }
        return (localX != rawX || localY != rawY) && slop.contains((int) rawX, (int) rawY);
    }

    private static Field findFieldIgnoreCase(Class<?> clazz, String... names) {
        for (Field f : clazz.getDeclaredFields()) {
            for (String n : names) {
                if (f.getName().equalsIgnoreCase(n)) return f;
            }
        }
        return null;
    }

    private static Object getComposeConfig(Object node) {
        if (node == null) {
            return null;
        }
        try {
            Method m = findMethod(node.getClass(), "getConfig");
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(node);
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = findMethod(node.getClass(), "getUnmergedConfig");
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(node);
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : node.getClass().getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    Object val = f.get(node);
                    if (val != null && (f.getType().getSimpleName().contains("SemanticsConfiguration") || hasMapOrEntries(val))) {
                        return val;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // ProGuard fallback (e.g. Laf9;->k()Lte9; or po5.k() -> jo5)
        try {
            for (Method m : node.getClass().getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        && m.getParameterCount() == 0
                        && !m.getReturnType().isPrimitive()
                        && m.getReturnType() != void.class
                        && m.getReturnType() != List.class
                        && m.getReturnType() != String.class) {
                    try {
                        m.setAccessible(true);
                        Object res = m.invoke(node);
                        if (res != null && hasMapOrEntries(res)) {
                            return res;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean hasMapOrEntries(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Map || obj instanceof Iterable) return true;
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getComposeChildren(Object node) {
        if (node == null) {
            return null;
        }
        try {
            Method m = findMethod(node.getClass(), "getChildren");
            if (m != null && !java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0) {
                m.setAccessible(true);
                Object res = m.invoke(node);
                if (res instanceof List && !((List<?>) res).isEmpty()) {
                    return (List<Object>) res;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : node.getClass().getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        && m.getParameterCount() == 0
                        && m.getName().startsWith("getChildren")) {
                    try {
                        m.setAccessible(true);
                        Object res = m.invoke(node);
                        if (res instanceof List && !((List<?>) res).isEmpty()) {
                            return (List<Object>) res;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // ProGuard fallback 1: s(ArrayList, boolean) -> unmergedChildren$ui_release (e.g. Laf9;->s(ArrayList, Z))
        // and i(boolean, boolean) -> getChildren$ui_release(includeReplacedSemantics=true, includeFakeNodes=false)
        List<Object> emptyFallback = null;
        for (Method m : node.getClass().getDeclaredMethods()) {
            try {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        || !List.class.isAssignableFrom(m.getReturnType())) {
                    continue;
                }
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length == 2 && pts[0] == boolean.class && pts[1] == boolean.class) {
                    m.setAccessible(true);
                    Object res = m.invoke(node, Boolean.TRUE, Boolean.FALSE);
                    if (res instanceof List) {
                        List<Object> list = (List<Object>) res;
                        if (!list.isEmpty()) {
                            return list;
                        }
                        emptyFallback = list;
                    }
                    Object res2 = m.invoke(node, Boolean.FALSE, Boolean.FALSE);
                    if (res2 instanceof List && !((List<?>) res2).isEmpty()) {
                        return (List<Object>) res2;
                    }
                } else if (pts.length == 2 && List.class.isAssignableFrom(pts[0]) && pts[1] == boolean.class) {
                    m.setAccessible(true);
                    ArrayList<Object> acc = new ArrayList<>();
                    Object res = m.invoke(node, acc, Boolean.FALSE);
                    if (res instanceof List && !((List<?>) res).isEmpty()) {
                        return (List<Object>) res;
                    }
                    if (!acc.isEmpty()) {
                        return acc;
                    }
                } else if (pts.length == 0) {
                    m.setAccessible(true);
                    Object res = m.invoke(node);
                    if (res instanceof List && !((List<?>) res).isEmpty()) {
                        return (List<Object>) res;
                    }
                } else if (pts.length == 1 && pts[0] == boolean.class) {
                    m.setAccessible(true);
                    Object res = m.invoke(node, Boolean.TRUE);
                    if (res instanceof List && !((List<?>) res).isEmpty()) {
                        return (List<Object>) res;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            for (Field f : node.getClass().getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object res = f.get(node);
                    if (res instanceof List && !((List<?>) res).isEmpty()) {
                        return (List<Object>) res;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return emptyFallback;
    }

    private static void collectComposeCandidates(Object node, int depth,
                                                 float localX, float localY, float rawX, float rawY,
                                                 int viewWidth, int viewHeight,
                                                 List<ComposeSemanticsMatch> candidates) {
        if (node == null || depth > 40) {
            return;
        }
        Rect bounds = getComposeNodeBounds(node);
        if (bounds == null || !boundsContainTouch(bounds, localX, localY, rawX, rawY)) {
            return;
        }
        boolean exactHit = boundsContainExactTouch(bounds, localX, localY, rawX, rawY);
        Object config = getComposeConfig(node);
        List<Object> children = getComposeChildren(node);
        ComposeSemanticsMatch match = buildComposeMatch(node, config, bounds, depth, exactHit, viewWidth, viewHeight);
        if (match != null) {
            match.childCount = children == null ? 0 : children.size();
            match.distance = rectDistance(bounds, localX, localY);
            candidates.add(match);
        }
        if (children != null) {
            for (Object child : children) {
                collectComposeCandidates(child, depth + 1, localX, localY, rawX, rawY, viewWidth, viewHeight, candidates);
            }
        }
    }

    private static boolean isIgnoredSemanticsPropertyKey(String keyName) {
        if (keyName == null) return true;
        return keyName.contains("EditableText")
                || keyName.contains("TextSubstitution")
                || keyName.contains("TextLayout")
                || keyName.contains("TestTag")
                || keyName.contains("LinkTestMarker")
                || keyName.contains("Action")
                || keyName.contains("Result")
                || keyName.startsWith("Set");
    }

    private static boolean isSemanticsTextKey(String keyName) {
        if (keyName == null || isIgnoredSemanticsPropertyKey(keyName)) return false;
        return keyName.equals("Text") || keyName.endsWith(".Text");
    }

    private static ComposeSemanticsMatch buildComposeMatch(Object node, Object config, Rect bounds, int depth,
                                                           boolean exactHit, int viewWidth, int viewHeight) {
        String[] textOut = new String[1];
        boolean[] imageOut = new boolean[1];
        extractFromSemanticsConfig(config, textOut, imageOut);

        // ColorOS OplusComposeViewDragProcessor.extractTextFromLayoutNodeAttached:
        // Also inspect SemanticsNode.layoutNode -> collapsedSemantics (Lkb6;->x()Lte9; / t: Lte9;)
        // and Modifier.Node chain (Ldha; TextAnnotatedStringNode / Lqla; TextStringSimpleNode)
        String layoutAttachedText = extractTextFromLayoutNodeAttached(node, config);
        if (!TextUtils.isEmpty(layoutAttachedText)) {
            textOut[0] = pickLongerText(textOut[0], layoutAttachedText);
        }

        // ColorOS rule: if node has valid text, it cannot be treated as an image
        boolean isImage = imageOut[0] && TextUtils.isEmpty(textOut[0]);

        return new ComposeSemanticsMatch(bounds, depth, textOut[0], isImage, false, exactHit, node);
    }

    private static void extractFromSemanticsConfig(Object config, String[] textOut, boolean[] imageOut) {
        if (config == null) return;
        String foundText = textOut[0];
        boolean isImage = imageOut[0];

        if (config instanceof Iterable) {
            try {
                for (Object item : (Iterable<?>) config) {
                    if (item instanceof Map.Entry) {
                        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) item;
                        String keyName = getSemanticsKeyName(entry.getKey());
                        Object value = entry.getValue();
                        if (keyName != null && !isIgnoredSemanticsPropertyKey(keyName)) {
                            if (isSemanticsTextKey(keyName)) {
                                String parsed = parseComposeTextValue(value);
                                if (!TextUtils.isEmpty(parsed)) {
                                    foundText = pickLongerText(foundText, parsed);
                                }
                            } else if (keyName.contains("ContentDescription")) {
                                String parsed = parseComposeTextValue(value);
                                if (!TextUtils.isEmpty(parsed) && foundText == null) {
                                    foundText = parsed;
                                }
                            } else if (keyName.contains("Role")) {
                                if (value != null && value.toString().contains("Image")) {
                                    isImage = true;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // Direct ScatterMap inspection (Lpd7; / c14: b=keys, c=values)
        if (TextUtils.isEmpty(foundText)) {
            for (Field f : config.getClass().getDeclaredFields()) {
                try {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    Object sub = f.get(config);
                    if (sub != null) {
                        Field keysField = null;
                        Field valsField = null;
                        for (Class<?> sc = sub.getClass(); sc != null && sc != Object.class; sc = sc.getSuperclass()) {
                            for (Field sf : sc.getDeclaredFields()) {
                                if (sf.getType() == Object[].class) {
                                    if (keysField == null) keysField = sf;
                                    else if (valsField == null) { valsField = sf; break; }
                                }
                            }
                            if (keysField != null && valsField != null) break;
                        }
                        if (keysField != null && valsField != null) {
                            keysField.setAccessible(true);
                            valsField.setAccessible(true);
                            Object[] keys = (Object[]) keysField.get(sub);
                            Object[] vals = (Object[]) valsField.get(sub);
                            if (keys != null && vals != null) {
                                int len = Math.min(keys.length, vals.length);
                                for (int i = 0; i < len; i++) {
                                    Object k = keys[i];
                                    Object v = vals[i];
                                    if (k == null || v == null) continue;
                                    String keyName = getSemanticsKeyName(k);
                                    if (keyName != null && !isIgnoredSemanticsPropertyKey(keyName)) {
                                        if (isSemanticsTextKey(keyName)) {
                                            String parsed = parseComposeTextValue(v);
                                            if (!TextUtils.isEmpty(parsed)) {
                                                foundText = pickLongerText(foundText, parsed);
                                            }
                                        } else if (keyName.contains("ContentDescription")) {
                                            String parsed = parseComposeTextValue(v);
                                            if (!TextUtils.isEmpty(parsed) && foundText == null) {
                                                foundText = parsed;
                                            }
                                        } else if (keyName.contains("Role")) {
                                            if (v.toString().contains("Image")) {
                                                isImage = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        if (TextUtils.isEmpty(foundText) && !isImage) {
            Map<?, ?> propsMap = findPropsMap(config);
            if (propsMap != null) {
                for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                    String keyName = getSemanticsKeyName(entry.getKey());
                    Object value = entry.getValue();
                    if (keyName != null && !isIgnoredSemanticsPropertyKey(keyName)) {
                        if (isSemanticsTextKey(keyName)) {
                            String parsed = parseComposeTextValue(value);
                            if (!TextUtils.isEmpty(parsed)) {
                                foundText = pickLongerText(foundText, parsed);
                            }
                        } else if (keyName.contains("ContentDescription")) {
                            String parsed = parseComposeTextValue(value);
                            if (!TextUtils.isEmpty(parsed) && foundText == null) {
                                foundText = parsed;
                            }
                        } else if (keyName.contains("Role")) {
                            if (value != null && value.toString().contains("Image")) {
                                isImage = true;
                            }
                        }
                    }
                }
            }
        }

        textOut[0] = foundText;
        imageOut[0] = isImage;
    }

    private static String extractTextFromModifierNode(Object modNode, int depth, Set<Object> visited) {
        if (modNode == null || depth > 8 || visited == null || visited.size() > 32 || !visited.add(modNode)) return null;
        String best = null;
        try {
            Class<?> mc = modNode.getClass();
            for (Field f : mc.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                Class<?> ft = f.getType();
                if (CharSequence.class.isAssignableFrom(ft)) {
                    f.setAccessible(true);
                    Object val = f.get(modNode);
                    if (val instanceof CharSequence) {
                        String s = val.toString().trim();
                        if (isMeaningfulUserText(s)) {
                            best = pickLongerText(best, s);
                        }
                    }
                }
            }
            // Check delegate chain (e.g. Lup3;->p:Lw97;) and next child in Modifier.Node chain (Lw97;->f:Lw97;)
            for (Class<?> c = mc; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    String fn = f.getName();
                    if ("f".equals(fn) || "child".equals(fn) || "p".equals(fn) || "delegate".equals(fn)) {
                        Class<?> ft = f.getType();
                        if (!ft.isPrimitive() && ft != String.class && ft != Object.class) {
                            f.setAccessible(true);
                            Object next = f.get(modNode);
                            if (next != null && next != modNode) {
                                String sub = extractTextFromModifierNode(next, depth + 1, visited);
                                if (!TextUtils.isEmpty(sub)) {
                                    best = pickLongerText(best, sub);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return best;
    }

    private static String extractTextFromLayoutNodeAttached(Object node, Object nodeConfig) {
        if (node == null) return null;
        String best = null;
        Class<?> configClass = nodeConfig != null ? nodeConfig.getClass() : null;

        try {
            for (Field nf : node.getClass().getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(nf.getModifiers())) continue;
                Class<?> ft = nf.getType();
                if (ft.isPrimitive() || ft == String.class || (configClass != null && ft == configClass)
                        || ft == node.getClass()) {
                    continue;
                }
                nf.setAccessible(true);
                Object fieldVal = nf.get(node);
                if (fieldVal == null) continue;

                // 1. Direct Modifier.Node on SemanticsNode (Laf9;->a: Lw97;)
                String modText = extractTextFromModifierNode(fieldVal, 0, new HashSet<>());
                if (!TextUtils.isEmpty(modText)) {
                    best = pickLongerText(best, modText);
                }

                // 2. Check fields of layoutNode (e.g. Lkb6;->t: Lte9; and Lkb6;->E: Lbi; NodeChain)
                for (Field lf : fieldVal.getClass().getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(lf.getModifiers())) continue;
                    if ((configClass != null && lf.getType() == configClass)
                            || lf.getType().getSimpleName().contains("SemanticsConfiguration")) {
                        lf.setAccessible(true);
                        Object collapsed = lf.get(fieldVal);
                        if (collapsed != null && collapsed != nodeConfig) {
                            String[] out = new String[1];
                            boolean[] img = new boolean[1];
                            extractFromSemanticsConfig(collapsed, out, img);
                            if (!TextUtils.isEmpty(out[0])) {
                                best = pickLongerText(best, out[0]);
                            }
                        }
                    } else if ("E".equals(lf.getName()) || lf.getName().contains("nodes")) {
                        lf.setAccessible(true);
                        Object nodeChain = lf.get(fieldVal);
                        if (nodeChain != null) {
                            for (Field cf : nodeChain.getClass().getDeclaredFields()) {
                                if (java.lang.reflect.Modifier.isStatic(cf.getModifiers())) continue;
                                if ("f".equals(cf.getName()) || "g".equals(cf.getName()) || "head".equals(cf.getName())) {
                                    cf.setAccessible(true);
                                    Object headMod = cf.get(nodeChain);
                                    if (headMod != null) {
                                        String chainText = extractTextFromModifierNode(headMod, 0, new HashSet<>());
                                        if (!TextUtils.isEmpty(chainText)) {
                                            best = pickLongerText(best, chainText);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 3. Check 0-arg methods of layoutNode returning SemanticsConfiguration (e.g. Lkb6;->x()Lte9;)
                for (Method lm : fieldVal.getClass().getDeclaredMethods()) {
                    if (!java.lang.reflect.Modifier.isStatic(lm.getModifiers())
                            && lm.getParameterCount() == 0
                            && ((configClass != null && lm.getReturnType() == configClass)
                            || lm.getReturnType().getSimpleName().contains("SemanticsConfiguration"))) {
                        lm.setAccessible(true);
                        Object collapsed = lm.invoke(fieldVal);
                        if (collapsed != null && collapsed != nodeConfig) {
                            String[] out = new String[1];
                            boolean[] img = new boolean[1];
                            extractFromSemanticsConfig(collapsed, out, img);
                            if (!TextUtils.isEmpty(out[0])) {
                                best = pickLongerText(best, out[0]);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return best;
    }

    private static String collectTextFromCandidateSubtrees(List<ComposeSemanticsMatch> candidates,
                                                           int viewWidth, int viewHeight) {
        if (candidates == null || candidates.isEmpty()) return null;
        int maxArea = (int) (viewWidth * viewHeight * 0.55f);
        // Walk from deepest candidate under the finger upwards, NEVER aggregating full-screen root/scaffold nodes!
        for (int i = candidates.size() - 1; i >= 0; i--) {
            ComposeSemanticsMatch cand = candidates.get(i);
            if (cand.node == null || cand.bounds == null) continue;
            if (cand.area <= 0 || cand.depth <= 0 || (maxArea > 0 && cand.area > maxArea)) continue;
            List<String> segments = new ArrayList<>();
            int[] visited = new int[]{0};
            collectSubtreeTextsInBounds(cand.node, cand.bounds, 0, visited, segments);
            // A single comment row / chat bubble holds a handful of text lines.
            // Twenty of them means we climbed to the list container and are about
            // to drag "every comment on screen" — refuse that and let the nearest
            // single text line be picked instead.
            if (segments.size() > 6) {
                Log.d(TAG, "compose row text refused: " + segments.size()
                        + " segments under " + cand.bounds.toShortString() + " depth=" + cand.depth);
                continue;
            }
            if (!segments.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String seg : segments) {
                    if (TextUtils.isEmpty(seg)) continue;
                    if (sb.indexOf(seg) >= 0) continue;
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(seg);
                }
                String combined = sb.toString().trim();
                if (isMeaningfulUserText(combined)) {
                    return combined;
                }
            }
        }
        return null;
    }

    private static void collectSubtreeTextsInBounds(Object node, Rect rowBounds, int depth,
                                                    int[] visited, List<String> out) {        if (node == null || depth > 24 || visited[0] > 128) return;
        visited[0]++;
        Rect nb = getComposeNodeBounds(node);
        if (nb != null && rowBounds != null && !Rect.intersects(nb, rowBounds)) {
            return;
        }
        Object config = getComposeConfig(node);
        String[] textOut = new String[1];
        boolean[] imgOut = new boolean[1];
        extractFromSemanticsConfig(config, textOut, imgOut);
        String attached = extractTextFromLayoutNodeAttached(node, config);
        String nodeText = pickLongerText(textOut[0], attached);
        if (!TextUtils.isEmpty(nodeText) && isMeaningfulUserText(nodeText)) {
            out.add(nodeText);
        }
        List<Object> children = getComposeChildren(node);
        if (children != null) {
            for (Object child : children) {
                collectSubtreeTextsInBounds(child, rowBounds, depth + 1, visited, out);
            }
        }
    }

    /**
     * Last text resort for Compose pages: the text node physically closest to
     * the finger, even when the finger sits in a gap between rows (such a point
     * is inside no text node, so the normal candidate walk finds nothing and we
     * used to drag the whole visible list instead).
     */
    private static String nearestComposeText(Object rootNode, float touchX, float touchY, int maxDistance) {
        if (rootNode == null) return null;
        Object[] best = new Object[]{null, null};   // {node, text}
        int[] bestRank = new int[]{Integer.MAX_VALUE};
        walkNearestText(rootNode, touchX, touchY, maxDistance, 0, new int[]{0}, best, bestRank);
        return best[1] instanceof String ? (String) best[1] : null;
    }

    private static void walkNearestText(Object node, float touchX, float touchY, int maxDistance,
                                        int depth, int[] visited, Object[] best, int[] bestRank) {
        if (node == null || depth > 24 || visited[0] > 400) return;
        visited[0]++;
        Rect bounds = getComposeNodeBounds(node);
        int distance = rectDistance(bounds, touchX, touchY);
        if (distance <= maxDistance) {
            Object config = getComposeConfig(node);
            String[] textOut = new String[1];
            boolean[] imgOut = new boolean[1];
            extractFromSemanticsConfig(config, textOut, imgOut);
            String attached = extractTextFromLayoutNodeAttached(node, config);
            String text = pickLongerText(textOut[0], attached);
            if (!TextUtils.isEmpty(text) && isMeaningfulUserText(text)) {
                int rank = distance - Math.min(depth, 12);
                if (rank < bestRank[0]) {
                    bestRank[0] = rank;
                    best[0] = node;
                    best[1] = text;
                }
            }
        }
        List<Object> children = getComposeChildren(node);
        if (children != null) {
            for (Object child : children) {
                walkNearestText(child, touchX, touchY, maxDistance, depth + 1, visited, best, bestRank);
            }
        }
    }

    private static String getSemanticsKeyName(Object key) {        if (key == null) return null;
        try {
            Method m = key.getClass().getMethod("getName");
            m.setAccessible(true);
            Object res = m.invoke(key);
            if (res instanceof String && !TextUtils.isEmpty((String) res)) return (String) res;
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : key.getClass().getDeclaredFields()) {
                if (f.getType() == String.class) {
                    f.setAccessible(true);
                    Object val = f.get(key);
                    if (val instanceof String && !TextUtils.isEmpty((String) val)) {
                        return (String) val;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return key.toString();
    }

    private static Map<?, ?> findPropsMap(Object config) {
        if (config == null) return null;
        try {
            for (Field f : config.getClass().getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object res = f.get(config);
                    if (res instanceof Map) return (Map<?, ?>) res;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String parseComposeTextValue(Object value) {
        if (value == null) return null;
        String className = value.getClass().getName();
        if (className.contains("AccessibilityAction")) return null;
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return null;
            if (list.size() == 1) {
                return parseComposeTextValue(list.get(0));
            }
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                String s = parseComposeTextValue(item);
                if (!TextUtils.isEmpty(s)) {
                    if (sb.indexOf(s) >= 0) continue;
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(s);
                }
            }
            return sb.length() > 0 ? sb.toString().trim() : null;
        }
        if (value instanceof CharSequence) {
            String s = value.toString().trim();
            return s.startsWith("AccessibilityAction(") ? null : s;
        }
        try {
            Method m = value.getClass().getMethod("getText");
            m.setAccessible(true);
            Object res = m.invoke(value);
            if (res instanceof CharSequence) {
                return res.toString().trim();
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : value.getClass().getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && CharSequence.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(value);
                    if (val instanceof CharSequence && !TextUtils.isEmpty((CharSequence) val)) {
                        return val.toString().trim();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String pickLongerText(String a, String b) {
        if (TextUtils.isEmpty(a)) return b;
        if (TextUtils.isEmpty(b)) return a;
        return a.length() >= b.length() ? a : b;
    }

    private static String previewText(String text) {
        if (text == null) return "";
        String flat = text.replace('\n', ' ').trim();
        return flat.length() <= 48 ? flat : flat.substring(0, 48) + "…";
    }

    /**
     * Picks the semantics node that owns the drag content.
     *
     * Order matters: text only wins when the finger is literally inside that
     * text node. A comment row is a Compose subtree whose label ("点踩",
     * "共3条回复") often sits inside the slop radius of the touch point, and
     * letting it win unconditionally is what turned "long press the picture"
     * into a 2-character text drag on Bilibili's comment section.
     */
    private static ComposeSemanticsMatch pickBestComposeMatch(List<ComposeSemanticsMatch> candidates,
                                                             int viewWidth, int viewHeight) {
        if (candidates == null || candidates.isEmpty()) return null;
        ComposeSemanticsMatch exactText = bestComposeText(candidates, true);
        if (exactText != null) return exactText;
        ComposeSemanticsMatch exactImage = bestComposeImage(candidates, true);
        if (exactImage != null) return exactImage;
        ComposeSemanticsMatch leafImage = bestComposeLeafImage(candidates, viewWidth, viewHeight);
        if (leafImage != null) {
            leafImage.geometryImage = true;
            return leafImage;
        }
        ComposeSemanticsMatch slopText = bestComposeText(candidates, false);
        if (slopText != null && slopText.distance <= MAX_TEXT_SLOP_PX) return slopText;
        return bestComposeImage(candidates, false);
    }

    /**
     * Merged semantics containers (a comment row with mergeDescendants) report
     * the first child's text, so they must lose against a real text leaf that is
     * only slightly farther away from the finger.
     */
    private static final int MERGED_NODE_PENALTY_PX = 48;

    /** A text node farther than this from the finger is not "the text I pressed". */
    private static final int MAX_TEXT_SLOP_PX = 120;

    private static ComposeSemanticsMatch bestComposeText(List<ComposeSemanticsMatch> candidates, boolean exactOnly) {
        ComposeSemanticsMatch best = null;
        int bestRank = Integer.MAX_VALUE;
        for (ComposeSemanticsMatch m : candidates) {
            if (!m.hasText || !isMeaningfulUserText(m.text)) continue;
            if (exactOnly && !m.exactHit) continue;
            int rank = m.distance + (m.childCount == 0 ? 0 : MERGED_NODE_PENALTY_PX);
            if (best == null || rank < bestRank
                    || (rank == bestRank && m.depth > best.depth)
                    || (rank == bestRank && m.depth == best.depth && m.area < best.area)) {
                best = m;
                bestRank = rank;
            }
        }
        return best;
    }

    private static ComposeSemanticsMatch bestComposeImage(List<ComposeSemanticsMatch> candidates, boolean exactOnly) {
        ComposeSemanticsMatch best = null;
        int bestRank = Integer.MAX_VALUE;
        for (ComposeSemanticsMatch m : candidates) {
            if (!m.isImageLike || m.bounds == null) continue;
            if (exactOnly && !m.exactHit) continue;
            int rank = m.distance + (m.childCount == 0 ? 0 : MERGED_NODE_PENALTY_PX / 2);
            if (best == null || rank < bestRank
                    || (rank == bestRank && m.area > best.area)) {
                best = m;
                bestRank = rank;
            }
        }
        return best;
    }

    /**
     * A picture drawn with a Compose Painter has no text, no ContentDescription
     * and no Role.Image — it only shows up as an empty leaf semantics node. Pick
     * the smallest such leaf that the finger is inside, big enough to be a real
     * image and small enough not to be a background.
     */
    private static ComposeSemanticsMatch bestComposeLeafImage(List<ComposeSemanticsMatch> candidates,
                                                             int viewWidth, int viewHeight) {
        int viewArea = Math.max(1, viewWidth * viewHeight);
        ComposeSemanticsMatch best = null;
        for (ComposeSemanticsMatch m : candidates) {
            if (m.bounds == null || m.hasText || m.isImageLike || m.childCount != 0) continue;
            if (!m.exactHit) continue;
            float aspect = m.bounds.width() / (float) Math.max(1, m.bounds.height());
            if (aspect < 0.5f || aspect > 2.0f) continue;
            if (m.area < viewArea * 0.02f || m.area > viewArea * 0.60f) continue;
            if (best == null || m.area < best.area
                    || (m.area == best.area && m.depth > best.depth)) {
                best = m;
            }
        }
        return best;
    }

    private static int compareDirectSemanticsContent(ComposeSemanticsMatch a, ComposeSemanticsMatch b) {
        if (a.hasText != b.hasText) {
            return a.hasText ? -1 : 1;
        }
        if (a.isImageLike != b.isImageLike) {
            return a.isImageLike ? -1 : 1;
        }
        return a.area - b.area;
    }

    private static final class ComposeSemanticsMatch {
        final Rect bounds;
        final int depth;
        final String text;
        final boolean hasText;
        final boolean isImageLike;
        final boolean isLink;
        final boolean exactHit;
        final int area;
        final Object node;
        /** Children under this node in the semantics tree; 0 means a leaf. */
        int childCount;
        /** Pixels from the touch point to this node's bounds; 0 when inside. */
        int distance;
        /** Set when this node was picked purely by geometry (empty leaf block). */
        boolean geometryImage;

        ComposeSemanticsMatch(Rect bounds, int depth, String text, boolean isImageLike, boolean isLink, boolean exactHit, Object node) {
            this.bounds = bounds;
            this.depth = depth;
            this.text = text;
            this.hasText = !TextUtils.isEmpty(text);
            this.isImageLike = isImageLike;
            this.isLink = isLink;
            this.exactHit = exactHit;
            this.area = bounds != null ? Math.max(0, bounds.width()) * Math.max(0, bounds.height()) : 0;
            this.node = node;
        }

        String describe() {
            return (bounds == null ? "null" : bounds.toShortString())
                    + (exactHit ? " exact" : " slop")
                    + (hasText ? " text=" + text.length() : "")
                    + (isImageLike ? " imageLike" : "")
                    + " leaf=" + (childCount == 0)
                    + " d=" + distance
                    + " depth=" + depth;
        }
    }

    /** Distance in pixels from a point (ComposeView-local) to a rect; 0 inside. */
    private static int rectDistance(Rect rect, float x, float y) {
        if (rect == null) return Integer.MAX_VALUE / 4;
        int dx = x < rect.left ? (int) (rect.left - x) : (x > rect.right ? (int) (x - rect.right) : 0);
        int dy = y < rect.top ? (int) (rect.top - y) : (y > rect.bottom ? (int) (y - rect.bottom) : 0);
        return (int) Math.sqrt((double) dx * dx + (double) dy * dy);
    }

    private static String accessibilityText(View view, PointF point) {
        try {
            AccessibilityNodeInfo root = view.createAccessibilityNodeInfo();
            if (root == null) {
                return null;
            }
            Candidate best = new Candidate();
            collectAccessibility(root, 0, point, best);
            return best.text;
        } catch (Throwable throwable) {
            Log.w(TAG, "accessibilityText failed", throwable);
            return null;
        }
    }

    private static void collectAccessibility(AccessibilityNodeInfo node, int depth, PointF point,
                                             Candidate best) {
        if (node == null || depth > 40) {
            return;
        }
        try {
            CharSequence text = node.getText();
            if (!isMeaningfulUserText(text)) {
                text = node.getContentDescription();
            }
            if (isMeaningfulUserText(text)) {
                Rect rect = new Rect();
                node.getBoundsInScreen(rect);
                boolean hit = point == null || (
                    point.x >= rect.left - 24 && point.x <= rect.right + 24
                    && point.y >= rect.top - 24 && point.y <= rect.bottom + 24
                );
                int area = Math.max(1, rect.width()) * Math.max(1, rect.height());
                if (hit && (best.text == null || area < best.area
                        || (area == best.area && depth > best.depth))) {
                    String value = text.toString();
                    if (value.length() > 20000) {
                        value = value.substring(0, 20000);
                    }
                    best.text = value;
                    best.area = area;
                    best.depth = depth;
                }
            }
            int count = Math.min(node.getChildCount(), 64);
            for (int i = 0; i < count; i++) {
                collectAccessibility(node.getChild(i), depth + 1, point, best);
            }
        } catch (Throwable ignored) {
        }
    }

    private static final class Candidate {
        String text;
        int area = Integer.MAX_VALUE;
        int depth = -1;
    }

    private static Bitmap scaleDown(Bitmap source, int maxDimension) {
        if (source == null || source.isRecycled()) {
            return null;
        }
        Bitmap.Config cfg = source.getConfig();
        if (cfg == Bitmap.Config.HARDWARE || cfg == Bitmap.Config.RGBA_F16) {
            Bitmap software = toSoftwareBitmap(source);
            if (software == null || software.getConfig() == Bitmap.Config.HARDWARE || software.getConfig() == Bitmap.Config.RGBA_F16) {
                return null;
            }
            source = software;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        int max = Math.max(width, height);
        if (max <= maxDimension) {
            return source;
        }
        float scale = maxDimension / (float) max;
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);
    }

    private static String describeBitmap(Bundle bundle) {
        try {
            Object value = bundle.getParcelable("viewBitmap");
            if (value instanceof Bitmap) {
                Bitmap bitmap = (Bitmap) value;
                return bitmap.getWidth() + "x" + bitmap.getHeight();
            }
        } catch (Throwable ignored) {
        }
        return "null";
    }

    /**
     * Called instead of ContentCatcherManager.onContentCatched(Content).
     * This is the direct-dispatch hook: no AES service, no Content Portal.
     */
    public static void deliverContent(Object content) {
        try {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Log.d(TAG, "deliverContent ignored (background thread)");
                return;
            }
            Context ctx = currentContext();
            ensureConfig(ctx);
            if (!sMasterSwitch || isBlacklisted(ctx) || (!sLandscapeTrigger && isLandscape(ctx))) {
                Log.d(TAG, "deliverContent ignored (disabled, blacklisted, or landscape)");
                return;
            }
            if (!sGestureActive || content == null
                    || sTriggeredGeneration == sGestureGeneration
                    || sDragStartedGeneration == sGestureGeneration
                    || sPendingPayload != null
                    || sGestureState.get() != STATE_IDLE
                    || sActiveDragView != null) {
                Log.d(TAG, "deliverContent ignored (gesture inactive or already triggered)");
                return;
            }
            long elapsed = SystemClock.uptimeMillis() - sDownAt;
            if (sDownAt > 0 && elapsed < (sLongPressDurationMs - 100)) {
                Log.w(TAG, "deliverContent arrived too early (" + elapsed + "ms < " + sLongPressDurationMs
                        + "ms), ignoring premature AES delivery");
                return;
            }
            Object resultMap = getField(content, "mResultMap");
            Object catchType = getField(content, "mCatchType");
            if (!Integer.valueOf(2).equals(catchType)) {
                Log.d(TAG, "deliverContent ignored (non-long-press catchType=" + catchType + ")");
                return;
            }
            int size = resultMap instanceof Map ? ((Map<?, ?>) resultMap).size() : -1;
            Log.i(TAG, "deliver content catchType=" + catchType + " mapSize=" + size);
            if (!(resultMap instanceof Map)) {
                return;
            }
            Map<?, ?> map = (Map<?, ?>) resultMap;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                String summary;
                if (value instanceof Bundle) {
                    Bundle bundle = (Bundle) value;
                    summary = "Bundle text=" + bundle.getString("viewContent")
                            + " catchType=" + bundle.getInt("catchType", 0)
                            + " bitmap=" + describeBitmap(bundle);
                } else {
                    summary = String.valueOf(value);
                }
                Log.i(TAG, "deliver item key=" + entry.getKey() + " value=" + summary);
            }
            Bundle payload = pickPayload(map);
            if (payload != null) {
                sTriggeredGeneration = sGestureGeneration;
                sMainHandler.removeCallbacks(sCustomLongPressRunnable);
                Object coordinate = map.get("coordinate");
                if (coordinate instanceof String) {
                    payload.putString("coordinate", (String) coordinate);
                }
                // AES has already completed its long-press and extraction
                // pipeline. Do NOT mark the gesture as moved: a stationary
                // long-press must still reach the app (Weibo multi-select
                // etc.). The system drag starts later, once the finger leaves
                // the drag circle, by ping().
                sDragOriginX = getTouchRawX();
                sDragOriginY = getTouchRawY();
                sPendingPayload = payload;
                sPendingContent = content;
                sPendingGeneration = sGestureGeneration;
                dismissTextSelectionUi(sLastTouchView);
                Log.i(TAG, "long press preview only; drag waits for real move");
                forwardToPortal(payload, content, false);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "deliverContent failed", throwable);
        }
    }

    private static Bundle pickPayload(Map<?, ?> map) {
        Bundle textPayload = null;
        Bundle bitmapPayload = null;
        Bundle urlPayload = null;
        for (Object value : map.values()) {
            if (!(value instanceof Bundle)) {
                continue;
            }
            Bundle bundle = (Bundle) value;
            if (bundle.getParcelable("filePfd") instanceof ParcelFileDescriptor) {
                return bundle;
            }
            String text = bundle.getString("viewContent");
            if (textPayload == null && isMeaningfulUserText(text)) {
                textPayload = bundle;
            }
            if (bitmapPayload == null && bundle.getParcelable("viewBitmap") instanceof Bitmap) {
                bitmapPayload = bundle;
            }
            if (urlPayload == null && (!TextUtils.isEmpty(bundle.getString("imageUrl"))
                    || !TextUtils.isEmpty(bundle.getString("imgUrl")))) {
                urlPayload = bundle;
            }
        }
        if (textPayload != null) return textPayload;
        if (bitmapPayload != null) return bitmapPayload;
        return urlPayload;
    }

    /**
     * Stream the original PFD into the portal's write-only staging provider.
     * No size limit, no Binder byte[] and no ParcelFileDescriptor in the
     * startService Intent, so the original file survives untouched.
     */
    /** Best-effort MIME sniffing for descriptors that arrive without a type. */
    private static String sniffMime(ParcelFileDescriptor pfd) {
        if (pfd == null) {
            return null;
        }
        String magicMime = sniffMimeByMagic(pfd);
        if (magicMime != null) {
            return magicMime;
        }
        try {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFileDescriptor(
                    pfd.getFileDescriptor(), null, options);
            if (!TextUtils.isEmpty(options.outMimeType)) {
                return options.outMimeType;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // ---- Animated image hand-off -------------------------------------------------
    // QQ ships its sticker GIFs with a byte-mangled signature: "GIF89a" is stored as
    // 47 48 46 39 39 60 ("GHF99`"), i.e. the bytes at index 1/3/5 are XORed with 1.
    // Naive sniffers report application/octet-stream, and every GIF decoder that
    // checks the magic refuses the file. Detect the pattern, advertise it as image/gif
    // and rewrite the six header bytes while staging so the target accepts it.
    private static byte[] readHead(ParcelFileDescriptor pfd, int count) {
        if (pfd == null) {
            return null;
        }
        try {
            byte[] buf = new byte[count];
            int read = android.system.Os.pread(pfd.getFileDescriptor(), buf, 0, count, 0L);
            return read >= 6 ? buf : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isMangledGif(byte[] head, int len) {
        if (head == null || len < 6) {
            return false;
        }
        return head[0] == 0x47                       // 'G'
                && head[1] == 0x48                   // 'H'  (== 'I' ^ 1)
                && head[2] == 0x46                   // 'F'
                && (head[3] == 0x38 || head[3] == 0x39)   // '8'/'9' ^ 1
                && head[5] == 0x60;                  // '`'  (== 'a' ^ 1)
    }

    private static void repairMangledGif(byte[] head, int len) {
        if (!isMangledGif(head, len)) {
            return;
        }
        head[1] = 0x49;                              // 'I'
        head[3] = (byte) (head[3] ^ 0x01);           // back to '8' or '9'
        head[5] = 0x61;                              // 'a'
    }

    private static String sniffMimeByMagic(ParcelFileDescriptor pfd) {
        byte[] head = readHead(pfd, 12);
        if (head == null) {
            return null;
        }
        if (head[0] == 0x47 && head[1] == 0x49 && head[2] == 0x46
                && head[3] == 0x38 && (head[4] == 0x37 || head[4] == 0x39)) {
            return "image/gif";                      // GIF87a / GIF89a
        }
        if (isMangledGif(head, 12)) {
            return "image/gif";                      // QQ obfuscated sticker GIF
        }
        if (head[0] == 0x52 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x46
                && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50) {
            return "image/webp";                     // RIFF....WEBP
        }
        return null;
    }

    /**
     * True when the descriptor starts with a signature we can forward untouched and
     * still call "animated": GIF87a/GIF89a, QQ's mangled GIF, or an animated WebP
     * (VP8X chunk carrying the ANIM flag). Static images return false so they keep the
     * old path.
     */
    private static boolean isAnimatedDescriptor(ParcelFileDescriptor pfd) {
        byte[] head = readHead(pfd, 24);
        if (head == null) {
            return false;
        }
        if (head[0] == 0x47 && head[1] == 0x49 && head[2] == 0x46) {
            return true;
        }
        if (isMangledGif(head, 24)) {
            return true;
        }
        return head[0] == 0x52 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x46
                && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50
                && head[12] == 0x56 && head[13] == 0x50 && head[14] == 0x38 && head[15] == 0x58
                && (head[20] & 0x02) != 0;           // VP8X flags: ANIMATION
    }

    // ---- Finding the animated original behind a view ------------------------------
    // QQ's gallery/sticker pages use com.tencent.image.URLImageView: the source file
    // lives inside a URLDrawable, so getDrawable() only ever yields the decoded bitmap
    // and the extractor reports file=null / uri=null / cache=null. Walk the view's
    // object graph (bounded) looking for a File or path that is really an animated
    // image, then hand it to the normal staging path.
    private static boolean isAnimatedHead(byte[] head) {
        if (head == null || head.length < 6) {
            return false;
        }
        if (head[0] == 0x47 && head[1] == 0x49 && head[2] == 0x46) {
            return true;                                   // GIF87a / GIF89a
        }
        if (isMangledGif(head, head.length)) {
            return true;                                   // QQ sticker GIF
        }
        return head.length >= 21
                && head[0] == 0x52 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x46
                && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50
                && head[12] == 0x56 && head[13] == 0x50 && head[14] == 0x38 && head[15] == 0x58
                && (head[20] & 0x02) != 0;                 // animated WebP
    }

    private static byte[] readHeadFile(java.io.File file) {
        java.io.FileInputStream in = null;
        try {
            in = new java.io.FileInputStream(file);
            byte[] buf = new byte[24];
            int read = 0;
            while (read < buf.length) {
                int n = in.read(buf, read, buf.length - read);
                if (n <= 0) {
                    break;
                }
                read += n;
            }
            return read >= 6 ? buf : null;
        } catch (Throwable ignored) {
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean isAnimatedFile(java.io.File file) {
        if (file == null) {
            return false;
        }
        try {
            return file.isFile() && file.length() > 32 && isAnimatedHead(readHeadFile(file));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static java.io.File fileFromString(String value) {
        if (TextUtils.isEmpty(value) || value.length() > 512) {
            return null;
        }
        try {
            String path = value;
            // URLDrawable.getURL() hands back "file:/storage/..." with a single slash,
            // while providers use "file:///storage/...". Normalise both.
            if (path.startsWith("file:")) {
                path = path.substring(5);
                if (path.startsWith("//")) {
                    path = path.substring(2);
                }
            }
            if (!path.startsWith("/")) {
                return null;
            }
            int query = path.indexOf('?');
            if (query > 0) {
                path = path.substring(0, query);
            }
            return new java.io.File(path);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Only probe where an animated original plausibly exists: QQ's URLDrawable-backed
     * views. Everything else keeps the old, cheap extraction path.
     */
    private static boolean shouldProbeAnimated(View view) {
        if (view == null) {
            return false;
        }
        try {
            if (view instanceof ImageView) {
                android.graphics.drawable.Drawable drawable = ((ImageView) view).getDrawable();
                if (drawable != null) {
                    String name = drawable.getClass().getName();
                    if (name.contains("tencent.image")) {
                        return true;
                    }
                    // Generic: any animated-image drawable is worth probing, whatever the
                    // app (Weibo's com.sina.gifdecoder.GifDrawable, Qzone, etc.).
                    String lower = name.toLowerCase(Locale.US);
                    if (lower.contains("gif") || lower.contains("anim")
                            || lower.contains("movie") || lower.contains("webp")
                            || lower.contains("apng")) {
                        return true;
                    }
                }
            }
            Context ctx = view.getContext();
            String pkg = ctx == null ? null : ctx.getPackageName();
            // WeChat's sticker view is a custom View, so probing by package is the only
            // reliable gate there.
            return "com.tencent.mobileqq".equals(pkg) || "com.tencent.mm".equals(pkg);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static java.io.File findAnimatedFile(View root, java.util.List<String> hashes) {
        if (root == null) {
            return null;
        }
        java.util.IdentityHashMap<Object, Boolean> seen = new java.util.IdentityHashMap<>();
        java.util.ArrayDeque<Object> queue = new java.util.ArrayDeque<>();
        java.util.ArrayDeque<Integer> depths = new java.util.ArrayDeque<>();
        StringBuilder diag = new StringBuilder();
        int[] paths = new int[]{0};
        queue.add(root);
        depths.add(0);
        int budget = 900;
        while (!queue.isEmpty() && budget-- > 0) {
            Object node = queue.poll();
            Integer depthBox = depths.poll();
            int depth = depthBox == null ? 0 : depthBox;
            if (node == null || seen.put(node, Boolean.TRUE) != null) {
                continue;
            }
            if (node instanceof java.io.File) {
                if (isAnimatedFile((java.io.File) node)) {
                    return (java.io.File) node;
                }
                continue;
            }
            if (node instanceof String) {
                String text = (String) node;
                if (text.length() <= 512) {
                    if (paths[0] < 6
                            && (text.startsWith("/") || text.startsWith("file:") || text.contains("://"))) {
                        paths[0]++;
                        diag.append(" [").append(text).append(']');
                    }
                    // Sticker views usually carry only the content hash, not a path.
                    if (hashes != null && hashes.size() < 8 && text.length() == 32 && isHexHash(text)) {
                        hashes.add(text);
                    }
                    java.io.File candidate = fileFromString(text);
                    if (isAnimatedFile(candidate)) {
                        Log.i(TAG, "findAnimatedFile hit via string: " + candidate);
                        return candidate;
                    }
                }
                continue;
            }
            if (node instanceof byte[]) {
                java.io.File fromBytes = bytesToTempFile(root.getContext(), (byte[]) node);
                if (fromBytes != null) {
                    Log.i(TAG, "findAnimatedFile hit via byte[] node");
                    return fromBytes;
                }
                continue;
            }
            if (node instanceof java.nio.ByteBuffer) {
                java.nio.ByteBuffer buffer = ((java.nio.ByteBuffer) node).duplicate();
                byte[] copy = new byte[Math.min(buffer.remaining(), 8 * 1024 * 1024)];
                buffer.get(copy);
                java.io.File fromBytes = bytesToTempFile(root.getContext(), copy);
                if (fromBytes != null) {
                    Log.i(TAG, "findAnimatedFile hit via ByteBuffer node");
                    return fromBytes;
                }
                continue;
            }
            if (node instanceof Uri) {
                java.io.File candidate = fileFromString(((Uri) node).toString());
                if (isAnimatedFile(candidate)) {
                    return candidate;
                }
                continue;
            }
            if (node instanceof java.net.URL) {
                java.io.File candidate = fileFromString(((java.net.URL) node).getPath());
                if (isAnimatedFile(candidate)) {
                    Log.i(TAG, "findAnimatedFile hit via URL: " + candidate);
                    return candidate;
                }
                continue;
            }
            if (depth >= 5) {
                continue;
            }
            Class<?> type = node.getClass();
            String typeName = type.getName();
            if (typeName.startsWith("java.") || typeName.startsWith("android.graphics.Bitmap")) {
                continue;
            }
            // URLDrawable keeps its source in mUrl / mParams rather than in a File.
            String[] accessors = {"getURL", "getUrl", "getUrlString", "getOriginUrl", "getSrcUrl",
                    "getSourceUri", "getUri", "getFilePath", "getLocalPath", "getImagePath",
                    "getPath", "getSrcPath", "getCacheKey", "getRequest", "getImageRequest",
                    "getParams", "getBytes", "getByteBuffer", "getSourceBytes", "getData",
                    "getSource", "getDrawable", "getState", "getConstantState", "getOrigin",
                    "getOriginal", "getTarget", "getContent", "getBuffer", "getGifBuffer"};
            for (String accessor : accessors) {
                try {
                    Object value = type.getMethod(accessor).invoke(node);
                    if (value != null) {
                        // Libra's GifDrawable (and most GIF wrappers) keep the encoded
                        // bytes; staging those is more robust than finding a file.
                        if (value instanceof byte[]) {
                            java.io.File fromBytes = bytesToTempFile(root.getContext(), (byte[]) value);
                            if (fromBytes != null) {
                                Log.i(TAG, "findAnimatedFile hit via " + accessor + " bytes");
                                return fromBytes;
                            }
                        }
                        // Glide's GifDrawable exposes the encoded GIF through getBuffer().
                        if (value instanceof java.nio.ByteBuffer) {
                            java.nio.ByteBuffer buffer = ((java.nio.ByteBuffer) value).duplicate();
                            buffer.position(0);
                            byte[] copy = new byte[Math.min(buffer.remaining(), 32 * 1024 * 1024)];
                            buffer.get(copy);
                            java.io.File fromBuffer = bytesToTempFile(root.getContext(), copy);
                            if (fromBuffer != null) {
                                Log.i(TAG, "findAnimatedFile hit via " + accessor + " buffer");
                                return fromBuffer;
                            }
                        }
                        // URLDrawable.getURL() returns java.net.URL, whose toString is
                        // "file:/storage/..." (single slash). Normalise before judging.
                        String asPath = null;
                        if (value instanceof java.net.URL) {
                            asPath = ((java.net.URL) value).getPath();
                        } else if (value instanceof String) {
                            asPath = (String) value;
                        }
                        if (asPath != null) {
                            java.io.File direct = fileFromString(asPath);
                            if (direct != null) {
                                boolean animated = isAnimatedFile(direct);
                                Log.i(TAG, "accessor " + accessor + " -> " + direct
                                        + " exists=" + direct.isFile() + " animated=" + animated);
                                if (animated) {
                                    return direct;
                                }
                            }
                        }
                        if (diag.length() < 400) {
                            diag.append(" {").append(accessor).append('=')
                                    .append(value.getClass().getSimpleName()).append(':')
                                    .append(String.valueOf(value)).append('}');
                        }
                        queue.add(value);
                        depths.add(depth + 1);
                    }
                } catch (Throwable ignored) {
                }
            }
            Class<?> cursor = type;
            int levels = 0;
            while (cursor != null && cursor != Object.class && levels++ < 6) {
                java.lang.reflect.Field[] fields;
                try {
                    fields = cursor.getDeclaredFields();
                } catch (Throwable throwable) {
                    break;
                }
                for (java.lang.reflect.Field field : fields) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(node);
                        if (value != null) {
                            queue.add(value);
                            depths.add(depth + 1);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                cursor = cursor.getSuperclass();
            }
        }
        Log.i(TAG, "findAnimatedFile: no hit, seen=" + seen.size() + " paths=" + diag
                + " hashes=" + (hashes == null ? "-" : hashes));
        return null;
    }

    /**
     * Called from preExtract() when the normal extractor produced only a bitmap.
     * Records the real animated file so forwardToPortal() can stage the original
     * bytes instead of a PNG of the first frame.
     */
    private static java.io.File bytesToTempFile(Context ctx, byte[] data) {
        if (ctx == null || data == null || data.length < 64 || !isAnimatedHead(data)) {
            return null;
        }
        try {
            java.io.File dir = new java.io.File(ctx.getCacheDir(), "hp_anim");
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return null;
            }
            // These copies can be tens of megabytes; keep only the newest few.
            java.io.File[] existing = dir.listFiles();
            if (existing != null && existing.length > 2) {
                java.util.Arrays.sort(existing, new java.util.Comparator<java.io.File>() {
                    @Override
                    public int compare(java.io.File a, java.io.File b) {
                        return Long.compare(b.lastModified(), a.lastModified());
                    }
                });
                for (int i = 2; i < existing.length; i++) {
                    try {
                        existing[i].delete();
                    } catch (Throwable ignored) {
                    }
                }
            }
            java.io.File out = new java.io.File(dir, "anim_" + SystemClock.uptimeMillis() + ".gif");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            try {
                fos.write(data);
            } finally {
                try {
                    fos.close();
                } catch (Throwable ignored) {
                }
            }
            Log.i(TAG, "bytesToTempFile " + out + " " + data.length + " bytes");
            return out;
        } catch (Throwable throwable) {
            Log.w(TAG, "bytesToTempFile failed", throwable);
            return null;
        }
    }

    /**
     * Diagnostic: dump the no-arg getters of the drawable so an unfamiliar image stack
     * (QQ's Libra) tells us which accessor to use instead of us guessing.
     */
    private static void logDrawableApi(View view) {
        try {
            if (!(view instanceof ImageView)) {
                return;
            }
            android.graphics.drawable.Drawable drawable = ((ImageView) view).getDrawable();
            if (drawable == null) {
                return;
            }
            StringBuilder builder = new StringBuilder();
            int count = 0;
            for (java.lang.reflect.Method method : drawable.getClass().getMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                String name = method.getName();
                if (!name.startsWith("get") && !name.startsWith("is")) {
                    continue;
                }
                builder.append(name).append(' ');
                if (++count >= 40) {
                    break;
                }
            }
            Log.i(TAG, "drawable api: " + drawable.getClass().getName() + " :: " + builder);
        } catch (Throwable ignored) {
        }
    }

    /** width/height/frameCount of the drawable, or null when it exposes none. */
    private static int[] drawableGeometry(android.graphics.drawable.Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        try {
            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            int frames = 0;
            try {
                Object value = drawable.getClass().getMethod("getFrameCount").invoke(drawable);
                if (value instanceof Integer) {
                    frames = (Integer) value;
                }
            } catch (Throwable ignored) {
            }
            return new int[]{w, h, frames};
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** GIF header width/height, or null when the file is not a GIF. */
    private static int[] gifGeometry(java.io.File file) {
        byte[] head = readHeadFile(file);
        if (head == null || head[0] != 0x47 || head[1] != 0x49 || head[2] != 0x46) {
            return null;
        }
        return new int[]{(head[6] & 0xFF) | ((head[7] & 0xFF) << 8),
                (head[8] & 0xFF) | ((head[9] & 0xFF) << 8)};
    }

    /** Counts graphic-control-extension blocks (0x21 0xF9 0x04) in a GIF. */
    private static int gifFrameCount(java.io.File file) {
        java.io.FileInputStream in = null;
        try {
            in = new java.io.FileInputStream(file);
            byte[] buf = new byte[65536];
            int read;
            int previous1 = -1;
            int previous2 = -1;
            int frames = 0;
            long total = 0;
            while ((read = in.read(buf)) > 0) {
                for (int i = 0; i < read; i++) {
                    int b = buf[i] & 0xFF;
                    if (previous2 == 0x21 && previous1 == 0xF9 && b == 0x04) {
                        frames++;
                    }
                    previous2 = previous1;
                    previous1 = b;
                }
                total += read;
                if (total > 32L * 1024 * 1024) {
                    break;
                }
            }
            return frames;
        } catch (Throwable ignored) {
            return 0;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * QQ's Libra decoder keeps only the decoded frames, so the encoded GIF is gone by the
     * time we see the view. The sticker pack directories survive on disk though, and the
     * pack currently on screen is the one whose .jtmp metadata was just rewritten. Match
     * inside that pack by geometry (intrinsic size + frame count) instead of by name.
     */
    private static java.io.File findQqStickerByGeometry(Context ctx, int[] geometry) {
        if (ctx == null || !"com.tencent.mobileqq".equals(ctx.getPackageName())) {
            return null;
        }
        java.io.File ext = ctx.getExternalFilesDir(null);
        java.io.File root = ext == null ? null : ext.getParentFile();
        java.io.File emo = root == null
                ? null : new java.io.File(root, "Tencent/MobileQQ/.emotionsm");
        java.io.File[] packs = emo == null ? null : emo.listFiles();
        if (packs == null) {
            return null;
        }
        long fresh = System.currentTimeMillis() - 30L * 60 * 1000;
        java.io.File best = null;
        long bestStamp = 0;
        int width = geometry == null ? 0 : geometry[0];
        int height = geometry == null ? 0 : geometry[1];
        int frames = geometry == null ? 0 : geometry[2];
        for (java.io.File pack : packs) {
            if (pack == null || !pack.isDirectory()) {
                continue;
            }
            java.io.File[] files = pack.listFiles();
            if (files == null) {
                continue;
            }
            long stamp = 0;
            for (java.io.File f : files) {
                if (f != null && f.getName().endsWith(".jtmp") && f.lastModified() > fresh) {
                    stamp = Math.max(stamp, f.lastModified());
                }
            }
            if (stamp == 0) {
                continue;
            }
            for (java.io.File f : files) {
                if (f == null || !f.isFile()) {
                    continue;
                }
                String name = f.getName();
                if (name.endsWith(".jtmp") || name.endsWith(".png")) {
                    continue;
                }
                int[] gif = gifGeometry(f);
                if (gif == null) {
                    continue;
                }
                if (width > 0 && height > 0 && (gif[0] != width || gif[1] != height)) {
                    continue;
                }
                if (frames > 0 && gifFrameCount(f) != frames) {
                    continue;
                }
                if (stamp > bestStamp) {
                    bestStamp = stamp;
                    best = f;
                }
            }
        }
        return best;
    }

    private static boolean isHexHash(String text) {
        if (text == null || text.length() != 32) {
            return false;
        }
        for (int i = 0; i < 32; i++) {
            char c = text.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sticker views often expose only the content hash, never a path. WeChat and QQ both
     * store stickers as <container>/<hash> with no extension, and we run inside the app
     * process, so those directories are readable here.
     */
    private static java.io.File resolveEmojiByHash(Context ctx, String hash) {
        if (ctx == null || !isHexHash(hash)) {
            return null;
        }
        long start = SystemClock.uptimeMillis();
        try {
            String pkg = ctx.getPackageName();
            if ("com.tencent.mm".equals(pkg)) {
                java.io.File base = ctx.getDataDir();
                java.io.File micro = base == null ? null : new java.io.File(base, "MicroMsg");
                java.io.File[] users = micro == null ? null : micro.listFiles();
                if (users != null) {
                    for (java.io.File user : users) {
                        if (user == null || !user.isDirectory()) {
                            continue;
                        }
                        java.io.File emoji = new java.io.File(user, "emoji");
                        if (!emoji.isDirectory()) {
                            continue;
                        }
                        java.io.File candidate = new java.io.File(emoji, hash);
                        if (isAnimatedFile(candidate)) {
                            return candidate;
                        }
                        candidate = new java.io.File(new java.io.File(emoji, "decrypt"), hash);
                        if (isAnimatedFile(candidate)) {
                            return candidate;
                        }
                    }
                }
            } else if ("com.tencent.mobileqq".equals(pkg)) {
                java.io.File ext = ctx.getExternalFilesDir(null);
                java.io.File root = ext == null ? null : ext.getParentFile();
                java.io.File emo = root == null
                        ? null : new java.io.File(root, "Tencent/MobileQQ/.emotionsm");
                java.io.File[] packs = emo == null ? null : emo.listFiles();
                if (packs != null) {
                    for (java.io.File pack : packs) {
                        if (pack == null || !pack.isDirectory()) {
                            continue;
                        }
                        java.io.File candidate = new java.io.File(pack, hash);
                        if (isAnimatedFile(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "resolveEmojiByHash failed", throwable);
        }
        Log.i(TAG, "resolveEmojiByHash miss " + hash
                + " in " + (SystemClock.uptimeMillis() - start) + "ms");
        return null;
    }

    private static void attachAnimatedSource(View view, Bundle result) {
        try {
            if (view == null || result == null) {
                return;
            }
            if (!TextUtils.isEmpty(result.getString("fileName"))) {
                return;
            }
            boolean probe = shouldProbeAnimated(view);
            String drawableName = "?";
            try {
                if (view instanceof ImageView) {
                    android.graphics.drawable.Drawable d = ((ImageView) view).getDrawable();
                    drawableName = d == null ? "null" : d.getClass().getName();
                }
            } catch (Throwable ignored) {
            }
            Log.i(TAG, "probe animated: view=" + view.getClass().getName()
                    + " drawable=" + drawableName + " probe=" + probe);
            if (!probe) {
                return;
            }
            java.util.ArrayList<String> hashes = new java.util.ArrayList<>();
            java.io.File file = findAnimatedFile(view, hashes);
            if (file == null && !hashes.isEmpty()) {
                Context ctx = view.getContext();
                for (String hash : hashes) {
                    file = resolveEmojiByHash(ctx, hash);
                    if (file != null) {
                        Log.i(TAG, "resolved sticker by hash " + hash + " -> " + file);
                        break;
                    }
                }
            }
            if (file == null) {
                Context ctx = view.getContext();
                android.graphics.drawable.Drawable drawable = view instanceof ImageView
                        ? ((ImageView) view).getDrawable() : null;
                int[] geometry = drawableGeometry(drawable);
                file = findQqStickerByGeometry(ctx, geometry);
                if (file != null) {
                    Log.i(TAG, "resolved sticker by geometry "
                            + (geometry == null ? "-"
                            : geometry[0] + "x" + geometry[1] + "/" + geometry[2] + "f")
                            + " -> " + file);
                }
            }
            if (file == null) {
                logDrawableApi(view);
                return;
            }
            result.putString("animatedSourcePath", file.getAbsolutePath());
            result.putString("mimeType", "image/gif");
            Log.i(TAG, "attachAnimatedSource " + file.getAbsolutePath()
                    + " " + file.length() + " bytes");
        } catch (Throwable throwable) {
            Log.w(TAG, "attachAnimatedSource failed", throwable);
        }
    }

    private static String copyPfdToPortal(Context context, ParcelFileDescriptor pfd, Bundle payload) {
        if (context == null || pfd == null) {
            return null;
        }
        String name = payload.getString("fileName");
        String mime = payload.getString("mimeType");
        if (TextUtils.isEmpty(mime) || "application/octet-stream".equals(mime)) {
            // Weibo's image cache hands over an extension-less name and no MIME
            // type. Sniff the descriptor so the staged file can still be typed.
            String sniffed = sniffMime(pfd);
            if (!TextUtils.isEmpty(sniffed)) {
                mime = sniffed;
            }
        }
        String suffix = ".bin";
        String lowerMime = mime == null ? "" : mime.toLowerCase(Locale.US);
        if (lowerMime.contains("png")) {
            suffix = ".png";
        } else if (lowerMime.contains("webp")) {
            suffix = ".webp";
        } else if (lowerMime.contains("jpeg") || lowerMime.contains("jpg")) {
            suffix = ".jpg";
        } else if (lowerMime.contains("gif")) {
            suffix = ".gif";
        } else if (lowerMime.contains("bmp")) {
            suffix = ".bmp";
        } else if (lowerMime.contains("heic")) {
            suffix = ".heic";
        } else if (lowerMime.contains("heif")) {
            suffix = ".heif";
        } else if (name != null && name.contains(".")) {
            suffix = name.substring(name.lastIndexOf('.'));
        }
        if (TextUtils.isEmpty(name)) {
            name = "drag_" + SystemClock.uptimeMillis() + suffix;
        } else if (name.lastIndexOf('.') <= 0) {
            // A staged file with no extension is untyped for every receiver that
            // derives the MIME type from the name - WeChat's chat drop handler only
            // accepts it when getType() contains "image"/"video"/"audio", and it
            // falls back to the file name. Weibo stages 64-hex-character names.
            name = name + suffix;
        }
        name = name.replaceAll("[/\\\\\\x00-\\x1f]", "_");
        Uri target = Uri.parse("content://com.github.leaf.hyperportal.files/incoming/" + Uri.encode(name));
        try {
            if (pfd.getFileDescriptor() != null && pfd.getFileDescriptor().valid()) {
                try {
                    android.system.Os.lseek(pfd.getFileDescriptor(), 0, android.system.OsConstants.SEEK_SET);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        try (OutputStream output = context.getContentResolver().openOutputStream(target, "w");
             InputStream input = new FileInputStream(pfd.getFileDescriptor())) {
            if (output == null) {
                return null;
            }
            byte[] buffer = new byte[65536];
            int count;
            long total = 0;
            boolean firstChunk = true;
            while ((count = input.read(buffer)) > 0) {
                if (firstChunk) {
                    firstChunk = false;
                    // QQ's sticker GIFs carry a mangled signature; no receiver accepts
                    // them until the six header bytes are restored.
                    if (isMangledGif(buffer, count)) {
                        repairMangledGif(buffer, count);
                        Log.i(TAG, "copyPfdToPortal repaired mangled GIF header for " + name);
                    }
                }
                output.write(buffer, 0, count);
                total += count;
                if (total > 128L * 1024 * 1024) {
                    Log.w(TAG, "copyPfdToPortal too large, aborting");
                    context.getContentResolver().delete(target, null, null);
                    return null;
                }
            }
            output.flush();
            if (total <= 0) {
                Log.w(TAG, "copyPfdToPortal read 0 bytes, deleting empty target and falling back");
                context.getContentResolver().delete(target, null, null);
                return null;
            }
            Log.i(TAG, "copyPfdToPortal " + name + " " + total + " bytes");
            return name;
        } catch (Throwable throwable) {
            Log.w(TAG, "copyPfdToPortal failed", throwable);
            try {
                context.getContentResolver().delete(target, null, null);
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    /** True when the URI is already served by this app's staging provider. */
    private static boolean isPortalUri(Uri uri) {
        return uri != null
                && "content".equals(uri.getScheme())
                && "com.github.leaf.hyperportal.files".equals(uri.getAuthority());
    }

    private static String queryDisplayName(Context context, Uri uri) {
        try (android.database.Cursor cursor = context.getContentResolver().query(uri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (!TextUtils.isEmpty(name)) {
                        return name;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Re-stages a third-party image URI into PortalFileProvider.
     *
     * A drag that carries somebody else's content:// URI is opaque to the receiver:
     * WeChat's drop handler (com.tencent.mm.pluginsdk.ui.tools.m7) queries for a
     * "_data" column, and a foreign provider either refuses the query or exposes
     * none, which ends in its "发送失败" toast. Streaming the bytes into our own
     * provider makes every drag we start carry a URI we can describe.
     */
    private static String copyUriToPortal(Context context, Uri uri, Bundle payload) {
        if (context == null || uri == null) {
            return null;
        }
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) {
                Log.w(TAG, "copyUriToPortal no fd for " + uri);
                return null;
            }
            Bundle meta = new Bundle(payload == null ? Bundle.EMPTY : payload);
            String mime = meta.getString("mimeType");
            if (TextUtils.isEmpty(mime)) {
                mime = context.getContentResolver().getType(uri);
            }
            if (TextUtils.isEmpty(mime)) {
                mime = mimeForUri(uri, null);
            }
            if (!TextUtils.isEmpty(mime)) {
                meta.putString("mimeType", mime);
            }
            if (TextUtils.isEmpty(meta.getString("fileName"))) {
                String displayName = queryDisplayName(context, uri);
                if (!TextUtils.isEmpty(displayName)) {
                    meta.putString("fileName", displayName);
                }
            }
            String staged = copyPfdToPortal(context, pfd, meta);
            Log.i(TAG, "copyUriToPortal " + uri + " -> " + staged + " mime=" + mime);
            return staged;
        } catch (Throwable throwable) {
            Log.w(TAG, "copyUriToPortal failed for " + uri, throwable);
            return null;
        }
    }

    private static String copyBitmapToPortal(Context context, Bitmap bitmap) {
        if (context == null || bitmap == null) return null;
        boolean hasAlpha = bitmap.hasAlpha();
        String ext = hasAlpha ? ".png" : ".jpg";
        String name = "drag_" + SystemClock.uptimeMillis() + ext;
        Uri target = Uri.parse("content://com.github.leaf.hyperportal.files/incoming/" + Uri.encode(name));
        try (OutputStream output = context.getContentResolver().openOutputStream(target, "w")) {
            if (output == null) return null;
            if (hasAlpha) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output);
            }
            output.flush();
            Log.i(TAG, "copyBitmapToPortal " + name + " " + bitmap.getWidth() + "x" + bitmap.getHeight());
            return name;
        } catch (Throwable t) {
            Log.w(TAG, "copyBitmapToPortal failed", t);
            return null;
        }
    }

    /**
     * Wakes or thaws the HyperPortal provider process via system_server (UID 1000)
     * and PortalReceiver when a direct ContentProvider write fails on the first try.
     * Never writes temporary drag bitmaps into MediaStore/Pictures, so triggering a
     * drag can never pollute the user's gallery.
     */
    private static boolean wakePortalForStaging(Context context) {
        if (context == null) {
            return false;
        }
        requestSystemWakePortal(context);
        try {
            Intent keepAlive = new Intent();
            keepAlive.setClassName(PORTAL_PACKAGE, PORTAL_SERVICE);
            keepAlive.putExtra("keepAlive", true);
            context.startService(keepAlive);
        } catch (Throwable ignored) {
        }
        Uri providerUri = Uri.parse("content://com.github.leaf.hyperportal.files");
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Thread.sleep(35);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                Bundle res = context.getContentResolver().call(providerUri, "getConfig", null, null);
                if (res != null) {
                    Log.i(TAG, "wakePortalForStaging succeeded on attempt " + (attempt + 1));
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /**
     * The staged file is the source of truth. Source MIME values such as image/gif
     * must not be reused when the bitmap fallback actually produced a PNG.
     */
    private static String mimeForStagedFile(String name, String fallback) {
        if (name != null) {
            String lower = name.toLowerCase(Locale.US);
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".bmp")) return "image/bmp";
            if (lower.endsWith(".heic")) return "image/heic";
            if (lower.endsWith(".heif")) return "image/heif";
            int dot = lower.lastIndexOf('.');
            if (dot >= 0) {
                String ext = lower.substring(dot + 1);
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                if (!TextUtils.isEmpty(mime)) return mime;
            }
        }
        if (TextUtils.isEmpty(fallback)) {
            return null;
        }
        return fallback;
    }

    private static String mimeForUri(Uri uri, String fallback) {
        if (uri != null) {
            String name = uri.getLastPathSegment();
            if (!TextUtils.isEmpty(name)) {
                String fromName = mimeForStagedFile(name, null);
                if (!TextUtils.isEmpty(fromName)) {
                    return fromName;
                }
            }
        }
        return mimeForStagedFile(null, fallback);
    }

    private static final class PfdBytes {
        byte[] data;
        String mime;
        String name;
    }

    /**
     * Android 14+ refuses to marshal ParcelFileDescriptor through
     * startService/startForegroundService Intents ("Not allowed to write file
     * descriptors here"). Read the original file and send a bounded byte[]
     * instead; the portal writes it into its own cache and shares the file.
     */
    private static PfdBytes encodePfd(ParcelFileDescriptor pfd, String name, String mime) {
        if (pfd == null) {
            return null;
        }
        try (InputStream input = new FileInputStream(pfd.getFileDescriptor())) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) > 0) {
                total += count;
                if (total > 8 * 1024 * 1024) {
                    return null;
                }
                output.write(buffer, 0, count);
            }
            byte[] data = output.toByteArray();
            String detected = detectImageMime(data);
            if (detected != null) {
                mime = detected;
            }
            if (data.length > 400 * 1024) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                byte[] compressed = compressJpeg(bitmap, 1600, 400 * 1024);
                if (compressed == null) {
                    return null;
                }
                data = compressed;
                mime = "image/jpeg";
            }
            PfdBytes result = new PfdBytes();
            result.data = data;
            result.mime = mime == null ? "application/octet-stream" : mime;
            result.name = name;
            return result;
        } catch (Throwable throwable) {
            Log.w(TAG, "encodePfd failed", throwable);
            return null;
        }
    }

    private static String detectImageMime(byte[] data) {
        if (data == null || data.length < 12) {
            return null;
        }
        if ((data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            return "image/png";
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
            return "image/gif";
        }
        if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static byte[] compressJpeg(Bitmap bitmap, int maxDimension, int maxBytes) {
        if (bitmap == null) {
            return null;
        }
        Bitmap scaled = scaleDown(bitmap, maxDimension);
        if (scaled == null) {
            return null;
        }
        for (int quality = 90; quality >= 40; quality -= 10) {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, output);
                byte[] data = output.toByteArray();
                if (data.length <= maxBytes) {
                    return data;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static int getPhysicalScreenHeight(Context context) {
        if (context != null) {
            try {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowMetrics metrics = wm.getMaximumWindowMetrics();
                        int h = metrics.getBounds().height();
                        if (h > 0) return h;
                    }
                    Display display = wm.getDefaultDisplay();
                    if (display != null) {
                        DisplayMetrics dm = new DisplayMetrics();
                        display.getRealMetrics(dm);
                        if (dm.heightPixels > 0) return dm.heightPixels;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            return Resources.getSystem().getDisplayMetrics().heightPixels;
        } catch (Throwable ignored) {
            return 2656;
        }
    }

    /**
     * @param prepareDrag true only for the call that may start the real system drag.
     *                    Preview-only calls must not stage images: otherwise a
     *                    stationary long-press creates an orphan copy in
     *                    Pictures/HyperPortal before the user ever drags.
     */
    private static void forwardToPortal(Bundle payload, Object content, boolean prepareDrag) {
        Context context = currentContext();
        if (context == null) {
            Log.w(TAG, "forward skipped: no context");
            return;
        }
        String text = payload.getString("viewContent");
        Object bitmap = payload.getParcelable("viewBitmap");
        Object filePfd = payload.getParcelable("filePfd");
        String source = tokenString(content);
        Intent intent = new Intent();
        intent.setClassName(PORTAL_PACKAGE, PORTAL_SERVICE);
        if (text != null) {
            intent.putExtra("viewContent", text);
        }
        String imageUrl = payload.getString("imageUrl");
        if (!TextUtils.isEmpty(imageUrl)) {
            intent.putExtra("imageUrl", imageUrl);
        }
        String coordinate = payload.getString("coordinate");
        if (coordinate != null) {
            int split = coordinate.indexOf('-');
            try {
                if (split > 0) {
                    intent.putExtra("touchX", Float.parseFloat(coordinate.substring(0, split)));
                    intent.putExtra("touchY", Float.parseFloat(coordinate.substring(split + 1)));
                }
            } catch (Throwable ignored) {
            }
        }
        Uri imageContentUri = null;
        String imageCacheName = payload.getString("stagedCacheName");
        String stagedUriStr = payload.getString("stagedImageUri");
        if (stagedUriStr != null) {
            imageContentUri = Uri.parse(stagedUriStr);
        }
        String imageMimeType = payload.getString("stagedImageMime");
        if (imageMimeType == null) {
            imageMimeType = payload.getString("mimeType");
        }
        String lowerSourceMime = imageMimeType == null
                ? "" : imageMimeType.toLowerCase(Locale.US);
        String sourceName = payload.getString("fileName");
        boolean sourceIsGif = lowerSourceMime.contains("gif")
                || (sourceName != null
                && sourceName.toLowerCase(Locale.US).endsWith(".gif"));

        Bitmap dragBitmap = null;
        if (bitmap instanceof Bitmap) {
            dragBitmap = toSoftwareBitmap((Bitmap) bitmap);
        }

        // An animated source must never be rasterised: staging a PNG of the first frame
        // is what turned a dragged GIF into a still picture. When the descriptor itself
        // is a real animated file (GIF87a/GIF89a, QQ's mangled sticker GIF, animated
        // WebP) we forward it byte-for-byte and keep the bitmap for the drag shadow only.
        boolean animatedOriginal = filePfd instanceof ParcelFileDescriptor
                && isAnimatedDescriptor((ParcelFileDescriptor) filePfd);
        if (animatedOriginal) {
            imageMimeType = "image/gif";
            Log.i(TAG, "animated source detected, forwarding original bytes"
                    + " (was mime=" + lowerSourceMime + ")");
        }
        boolean preferPngForGif = sourceIsGif && dragBitmap != null && !animatedOriginal;
        if (prepareDrag) {
            if (sDragStartedGeneration == sGestureGeneration) {
                Log.i(TAG, "forwardToPortal(prepareDrag=true) skipped: already started for generation "
                        + sGestureGeneration);
                return;
            }
            sDragStartedGeneration = sGestureGeneration;
            sMainHandler.removeCallbacks(sCustomLongPressRunnable);
            // A URI owned by the source app (Weibo's own provider, a photo-picker
            // grant, ...) is opaque to the drop target. Re-stage it so every drag
            // we start carries a URI this app can describe.
            if (imageCacheName == null && imageContentUri != null
                    && !isPortalUri(imageContentUri) && context != null) {
                String restaged = copyUriToPortal(context, imageContentUri, payload);
                if (restaged != null) {
                    imageCacheName = restaged;
                    imageContentUri = null;
                }
            }
            if (imageCacheName == null && imageContentUri == null
                    && filePfd instanceof ParcelFileDescriptor && !preferPngForGif) {
                imageCacheName = copyPfdToPortal(context, (ParcelFileDescriptor) filePfd, payload);
            }
            String animatedPath = payload.getString("animatedSourcePath");
            if (imageCacheName == null && imageContentUri == null
                    && !TextUtils.isEmpty(animatedPath)) {
                // The extractor only had a bitmap, but preExtract() located the real
                // animated file behind the view. Stage those bytes; the bitmap stays
                // as the drag shadow.
                ParcelFileDescriptor animatedPfd = null;
                try {
                    animatedPfd = ParcelFileDescriptor.open(
                            new java.io.File(animatedPath), ParcelFileDescriptor.MODE_READ_ONLY);
                    imageCacheName = copyPfdToPortal(context, animatedPfd, payload);
                    if (imageCacheName != null) {
                        imageMimeType = "image/gif";
                    }
                } catch (Throwable throwable) {
                    Log.w(TAG, "stage animated source failed: " + animatedPath, throwable);
                } finally {
                    try {
                        if (animatedPfd != null) {
                            animatedPfd.close();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (imageCacheName == null && imageContentUri == null && dragBitmap != null) {
                // Stage full-resolution bitmap into PortalFileProvider as lossless PNG
                imageCacheName = copyBitmapToPortal(context, dragBitmap);
                imageMimeType = "image/png";
            }
            if (imageCacheName == null && imageContentUri == null
                    && filePfd instanceof ParcelFileDescriptor) {
                // Staging a bitmap failed; preserve the original file rather than dropping it.
                imageCacheName = copyPfdToPortal(context, (ParcelFileDescriptor) filePfd, payload);
            }
            if (imageCacheName == null && imageContentUri == null
                    && (dragBitmap != null || filePfd instanceof ParcelFileDescriptor)) {
                // If PortalFileProvider was frozen or cold-stopped, ask system_server
                // (UID 1000) to wake it and retry staging once—never write temporary
                // transport files into MediaStore / Pictures!
                if (wakePortalForStaging(context)) {
                    if (dragBitmap != null && (! (filePfd instanceof ParcelFileDescriptor) || preferPngForGif)) {
                        imageCacheName = copyBitmapToPortal(context, dragBitmap);
                        if (imageCacheName != null) {
                            imageMimeType = "image/png";
                        }
                    }
                    if (imageCacheName == null && filePfd instanceof ParcelFileDescriptor) {
                        imageCacheName = copyPfdToPortal(context, (ParcelFileDescriptor) filePfd, payload);
                    }
                    if (imageCacheName == null && dragBitmap != null) {
                        imageCacheName = copyBitmapToPortal(context, dragBitmap);
                        if (imageCacheName != null) {
                            imageMimeType = "image/png";
                        }
                    }
                }
            }
            if (imageCacheName != null) {
                imageContentUri = Uri.parse("content://com.github.leaf.hyperportal.files/incoming/"
                        + Uri.encode(imageCacheName));
            }
        }
        if (imageCacheName != null) {
            if (imageContentUri == null) {
                imageContentUri = Uri.parse("content://com.github.leaf.hyperportal.files/incoming/"
                        + Uri.encode(imageCacheName));
            }
            payload.putString("stagedCacheName", imageCacheName);
            payload.putString("stagedImageUri", imageContentUri.toString());
            intent.putExtra("imageCacheName", imageCacheName);
            intent.putExtra("imageUri", imageContentUri.toString());
            intent.putExtra("imageFileName", payload.getString("fileName", imageCacheName));
            // The staged file extension is authoritative: a bitmap fallback is always
            // .png even when the source was image/gif, and a .bin file must never be
            // advertised as image/*.
            String normalizedMime = mimeForStagedFile(imageCacheName, imageMimeType);
            imageMimeType = TextUtils.isEmpty(normalizedMime) ? "application/octet-stream" : normalizedMime;
            payload.putString("stagedImageMime", imageMimeType);
            intent.putExtra("imageMimeType", imageMimeType);
            Log.i(TAG, "staged image name=" + imageCacheName
                    + " mime=" + imageMimeType
                    + " sourceMime=" + lowerSourceMime
                    + " bitmap=" + describeBitmap(payload));
        } else if (imageContentUri != null) {
            if (TextUtils.isEmpty(imageMimeType)) {
                imageMimeType = "image/png";
            }
            String imageFileName = payload.getString("stagedImageName");
            if (TextUtils.isEmpty(imageFileName)) {
                imageFileName = "dragshare_" + SystemClock.uptimeMillis() + ".png";
                payload.putString("stagedImageName", imageFileName);
            }
            intent.putExtra("imageUri", imageContentUri.toString());
            intent.putExtra("imageFileName", imageFileName);
            intent.putExtra("imageMimeType", imageMimeType);
            Log.i(TAG, "staged image fallback MediaStore uri=" + imageContentUri
                    + " reused=" + payload.getBoolean("stagedImageIsMediaStore", false));
        }
        if (dragBitmap != null) {
            // Preview thumbnail for UI and drag shadow (small so Intent parcel doesn't exceed 1MB)
            Bitmap preview = scaleDown(dragBitmap, 360);
            if (preview != null) {
                intent.putExtra("viewBitmap", preview);
            }
        }
        intent.putExtra("catchType", payload.getInt("catchType", 0));
        if (source != null) {
            intent.putExtra("sourcePkg", source);
        }
        Log.i(TAG, "forward -> portal text=" + (text == null ? "null" : text.length() + " chars")
                + " bitmap=" + describeBitmap(payload)
                + " file=" + (filePfd == null ? "null" : payload.getString("fileName"))
                + " cache=" + imageCacheName
                + " uri=" + imageContentUri
                + " prepareDrag=" + prepareDrag
                + " source=" + source);
        boolean dragStarted = prepareDrag
                && startSystemDrag(context, text, dragBitmap, imageContentUri, imageMimeType);
        intent.putExtra("dragActive", dragStarted);
        try {
            // The target app is foreground during long-press, so a plain
            // startService is allowed. Never use startForegroundService here:
            // PortalService cannot call startForeground() from this background
            // app context and crashes with ForegroundServiceStartNotAllowed.
            context.startService(intent);
        } catch (Throwable throwable) {
            Log.w(TAG, "forward startService failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(), throwable);
            boolean started = false;
            try {
                context.startService(intent);
                started = true;
                Log.i(TAG, "forward fallback startService ok");
            } catch (Throwable second) {
                Log.w(TAG, "forward fallback startService failed: "
                        + second.getClass().getName() + ": " + second.getMessage(), second);
            }
            if (!started) {
                try {
                    Intent broadcast = new Intent("com.github.leaf.hyperportal.SHOW");
                    broadcast.putExtras(intent);
                    Intent appBrd = new Intent(broadcast);
                    appBrd.setPackage(PORTAL_PACKAGE);
                    context.sendBroadcast(appBrd);
                    Intent sysUiBrd = new Intent(broadcast);
                    sysUiBrd.setPackage("com.android.systemui");
                    sysUiBrd.removeExtra("viewBitmap");
                    context.sendBroadcast(sysUiBrd);
                    started = true;
                    Log.i(TAG, "forward fallback broadcast sent");
                } catch (Throwable third) {
                    Log.w(TAG, "forward fallback broadcast failed", third);
                }
            }
        }
        // HyperOS may silently drop a cross-app startForegroundService() even
        // when it does not throw. Mirror the content through the exported
        // receiver with a small payload (preview bitmap / text only) so the
        // portal always gets a visible card.
        try {
            Intent mirror = new Intent("com.github.leaf.hyperportal.SHOW");
            mirror.putExtras(intent);
            mirror.removeExtra("filePfd");
            mirror.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            Intent appMirror = new Intent(mirror);
            appMirror.setPackage(PORTAL_PACKAGE);
            context.sendBroadcast(appMirror);
            Intent sysUiMirror = new Intent(mirror);
            sysUiMirror.setPackage("com.android.systemui");
            sysUiMirror.removeExtra("viewBitmap");
            context.sendBroadcast(sysUiMirror);
            Log.i(TAG, "forward receiver mirror sent");
        } catch (Throwable throwable) {
            Log.w(TAG, "forward receiver mirror failed", throwable);
        }
    }

    /**
     * Start Android's system drag at the long-press moment. The drag shadow
     * follows the same finger without lifting, which is exactly ColorOS's
     * "long press and drag in one gesture" behaviour.
     */
    private static void cancelShadowMorphAnimator() {
        ValueAnimator anim = sShadowMorphAnimator;
        sShadowMorphAnimator = null;
        if (anim != null) {
            try {
                anim.cancel();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void animateShadowMorph(View view, boolean toSquare) {
        cancelShadowMorphAnimator();
        float start = sShadowMorphProgress;
        float target = toSquare ? 1.0f : 0.0f;
        if (Math.abs(start - target) < 0.001f) {
            return;
        }
        sActiveShadowSquare = toSquare;
        ValueAnimator anim = ValueAnimator.ofFloat(start, target);
        anim.setDuration(180);
        anim.setInterpolator(new DecelerateInterpolator());
        final long[] lastFrame = new long[]{0};
        anim.addUpdateListener(animation -> {
            long now = SystemClock.uptimeMillis();
            float val = (Float) animation.getAnimatedValue();
            sShadowMorphProgress = val;
            if (now - lastFrame[0] < 14 && val < 0.99f && val > 0.01f) {
                return;
            }
            lastFrame[0] = now;
            try {
                if (android.os.Build.VERSION.SDK_INT >= 24 && view != null && view.isAttachedToWindow()) {
                    view.updateDragShadow(new PreviewShadow(sActiveDragText,
                            sActiveDragBitmap, sActiveDragDensity, sActiveDragSourceWidth,
                            sActiveDragScreenWidth, sActiveDragScreenHeight, sShadowMorphProgress, sShadowDim));
                }
            } catch (Throwable ignored) {
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (sShadowMorphAnimator == animation) {
                    sShadowMorphAnimator = null;
                    sShadowMorphProgress = target;
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= 24 && view != null && view.isAttachedToWindow()) {
                            view.updateDragShadow(new PreviewShadow(sActiveDragText,
                                    sActiveDragBitmap, sActiveDragDensity, sActiveDragSourceWidth,
                                    sActiveDragScreenWidth, sActiveDragScreenHeight, target, sShadowDim));
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
        sShadowMorphAnimator = anim;
        anim.start();
    }

    private static void updateSystemShadow(boolean square) {
        View view = sActiveDragView;
        if (view == null || !view.isAttachedToWindow() || sActiveShadowSquare == square) {
            return;
        }
        sActiveShadowSquare = square;
        Log.i(TAG, "shadow update called square=" + square);
        Runnable action = () -> animateShadowMorph(view, square);
        if (Looper.myLooper() != Looper.getMainLooper()) {
            sMainHandler.post(action);
        } else {
            action.run();
        }
    }

    /**
     * Dims the drag shadow while the finger is back inside the drag circle,
     * i.e. while releasing would cancel the drag. Only called from the source
     * window's ACTION_DRAG_LOCATION, which by definition means the pointer is
     * over that window.
     */
    private static void refreshShadowDim(View view, float screenX, float screenY) {
        if (view == null || sDragOriginX < 0f) {
            return;
        }
        float density = view.getResources().getDisplayMetrics().density;
        float radius = Math.max(8f, sTouchSlopDp * density);
        boolean dim = Math.hypot(screenX - sDragOriginX, screenY - sDragOriginY) < radius;
        if (dim == sShadowDim) {
            return;
        }
        sShadowDim = dim;
        try {
            View dragView = sActiveDragView != null ? sActiveDragView : view;
            if (android.os.Build.VERSION.SDK_INT >= 24 && dragView.isAttachedToWindow()) {
                dragView.updateDragShadow(new PreviewShadow(sActiveDragText, sActiveDragBitmap,
                        sActiveDragDensity, sActiveDragSourceWidth, sActiveDragScreenWidth,
                        sActiveDragScreenHeight, sShadowMorphProgress, dim));
            }
        } catch (Throwable ignored) {
        }
        Log.i(TAG, "shadow dim=" + dim);
    }

    private static int sLastNotifiedState = -1;
    private static float sLastNotifiedX = -1f;
    private static float sLastNotifiedY = -1f;
    private static long sLastNotifyTime = 0;

    private static void notifyPortalDragState(Context context, int state, float x, float y) {
        if (context == null) return;
        long now = SystemClock.uptimeMillis();
        boolean isRight = "right".equals(sTriggerPosition);
        if (state != 0) {
            if (state == 2 && sLastNotifiedState == 2) {
                return;
            }
            if (state == 1 && sLastNotifiedState == 1) {
                float dist = (float) Math.hypot(x - sLastNotifiedX, y - sLastNotifiedY);
                if (now - sLastNotifyTime < 12 || dist < 2f) {
                    return;
                }
            }
        }
        sLastNotifiedState = state;
        sLastNotifiedX = x;
        sLastNotifiedY = y;
        sLastNotifyTime = now;
        try {
            Intent intent = new Intent("com.github.leaf.hyperportal.ACTION_DRAG_STATE");
            intent.putExtra("state", state);
            intent.putExtra("x", x);
            intent.putExtra("y", y);
            intent.putExtra("position", sTriggerPosition);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

            Intent appIntent = new Intent(intent);
            appIntent.setPackage(PORTAL_PACKAGE);
            context.sendBroadcast(appIntent);

            Intent sysUiIntent = new Intent(intent);
            sysUiIntent.setPackage("com.android.systemui");
            context.sendBroadcast(sysUiIntent);

            Log.i(TAG, "notified portal drag state=" + state + " x=" + x + " y=" + y + " pos=" + sTriggerPosition);
        } catch (Throwable t) {
            Log.w(TAG, "notifyPortalDragState failed", t);
        }
    }

    private static void notifyPortalDragState(Context context, int state, float y) {
        notifyPortalDragState(context, state, -1f, y);
    }

    /**
     * Ships the drag circle to system_server with the ClipData: a drop inside
     * this circle, over the window the drag started from, is cancelled
     * (see PortalSystemModule.onReportDropWindowLock). Coordinates are screen
     * pixels, same space as Bridge's raw touch coordinates.
     */
    private static void attachDragCircle(ClipDescription description, float density) {
        if (description == null) {
            return;
        }
        try {
            float cx = sDragOriginX >= 0f ? sDragOriginX : sDownRawX;
            float cy = sDragOriginY >= 0f ? sDragOriginY : sDownRawY;
            if (cx <= 0f && cy <= 0f) {
                return;
            }
            float radiusPx = Math.max(8f, sTouchSlopDp * density);
            android.os.PersistableBundle extras = new android.os.PersistableBundle();
            extras.putDouble("hp_circle_x", cx);
            extras.putDouble("hp_circle_y", cy);
            extras.putDouble("hp_circle_r", radiusPx);
            description.setExtras(extras);
        } catch (Throwable ignored) {
        }
    }

    private static boolean startSystemDrag(String text, Bitmap bitmap) {
        return startSystemDrag(null, text, bitmap, null, null);
    }

    private static boolean startSystemDrag(Context context, String text, Bitmap bitmap,
                                           Uri imageUri, String imageMime) {
        try {
            sMainHandler.removeCallbacks(sCustomLongPressRunnable);
            if (isInternalDragLabel(text)) {
                text = null;
            }
            View view = sLastTouchView;
            if (view == null || !view.isAttachedToWindow()) {
                Log.i(TAG, "system drag skipped: no attached view");
                return false;
            }
            Log.i(TAG, "startSystemDrag enter target=" + view.getClass().getName()
                    + " roots=" + getGlobalRootViews().size());
            if (context == null) {
                context = view.getContext();
            }
            ensureConfig(context);
            if (!sMasterSwitch || isBlacklisted(context) || (!sLandscapeTrigger && isLandscape(context))) {
                return false;
            }
            if (SystemClock.uptimeMillis() - sLastTouchAt > Math.max(3000, sLongPressDurationMs + 2000)) {
                Log.i(TAG, "system drag skipped: stale long press");
                return false;
            }
            if (TextUtils.isEmpty(text) && bitmap == null && imageUri == null) {
                return false;
            }
            if (!sMovedSinceDown) {
                Log.i(TAG, "system drag skipped: stationary long press");
                return false;
            }
            View target = view;
            sGestureState.set(STATE_DRAGGING);
            cancelAppLongPress(target);

            if (imageUri == null && bitmap != null && context != null) {
                String name = copyBitmapToPortal(context, bitmap);
                if (name == null && wakePortalForStaging(context)) {
                    name = copyBitmapToPortal(context, bitmap);
                }
                if (name != null) {
                    imageUri = Uri.parse("content://com.github.leaf.hyperportal.files/incoming/" + Uri.encode(name));
                    imageMime = "image/png";
                }
            }

            ClipData clip;
            if (imageUri != null) {
                if (context != null) {
                    try {
                        context.getContentResolver().call(
                                Uri.parse("content://com.github.leaf.hyperportal.files"),
                                "grantDragPermissions",
                                imageUri.toString(),
                                null
                        );
                    } catch (Throwable ignored) {
                    }
                }
                String mime = mimeForUri(imageUri, imageMime);
                if (TextUtils.isEmpty(mime)) {
                    mime = "application/octet-stream";
                }
                List<String> mimeList = new ArrayList<>();
                mimeList.add(mime);
                if (mime.startsWith("image/")) {
                    mimeList.add("image/*");
                } else {
                    mimeList.add("*/*");
                }
                mimeList.add(ClipDescription.MIMETYPE_TEXT_URILIST);
                mimeList.add("application/x-dragshare");
                String clipLabel = !TextUtils.isEmpty(text) ? text : "图片";
                ClipDescription description = new ClipDescription(clipLabel, mimeList.toArray(new String[0]));
                attachDragCircle(description, view.getResources().getDisplayMetrics().density);
                // Never expose the internal fallback label as clipboard text.
                // The URI is the payload; a null text item prevents "drag_file"
                // from being pasted into target apps.
                ClipData.Item item = new ClipData.Item(
                        TextUtils.isEmpty(text) ? null : text, null, null, imageUri);
                clip = new ClipData(description, item);
                Log.i(TAG, "startSystemDrag: dragging URI " + imageUri + " mime=" + mime + " label=" + clipLabel);
            } else {
                ClipDescription description = new ClipDescription("content",
                        new String[]{"text/plain", "application/x-dragshare"});
                attachDragCircle(description, view.getResources().getDisplayMetrics().density);
                ClipData.Item item = new ClipData.Item(TextUtils.isEmpty(text) ? "" : text);
                clip = new ClipData(description, item);
                Log.i(TAG, "startSystemDrag: dragging text length=" + (text == null ? 0 : text.length()));
            }

            DisplayMetrics metrics = view.getResources().getDisplayMetrics();
            int screenW = metrics.widthPixels;
            int screenH = getPhysicalScreenHeight(view.getContext());
            View.DragShadowBuilder shadow = new PreviewShadow(text, bitmap, metrics.density,
                    target.getWidth(), screenW, screenH);
            try {
                View root = target.getRootView();
                root.setOnDragListener(new View.OnDragListener() {
                    private float lastNotifiedMotionX = -1f;
                    private float lastNotifiedMotionY = -1f;

                    @Override
                    public boolean onDrag(View v, DragEvent event) {
                        if (event == null) return false;
                        int action = event.getAction();
                        if (action == DragEvent.ACTION_DRAG_STARTED) {
                            sDragStartY = -1f;
                            sDragStartX = -1f;
                            suppressChildDragTargets(v, event);
                            dismissTextSelectionUi(v);
                            notifyPortalDragState(v.getContext(), 1, 0f, 0f);
                            return true;
                        }
                        if (action == DragEvent.ACTION_DRAG_LOCATION) {
                            if (event.getX() == 0f && event.getY() == 0f) {
                                return true;
                            }
                            int[] loc = new int[2];
                            v.getLocationOnScreen(loc);
                            float screenX = loc[0] + event.getX();
                            float screenY = loc[1] + event.getY();
                            refreshShadowDim(v, screenX, screenY);
                            if (sDragStartY < 0f) {
                                sDragStartY = sDownRawY > 0 ? sDownRawY : screenY;
                            }
                            if (sDragStartX < 0f) {
                                sDragStartX = sDownRawX > 0 ? sDownRawX : screenX;
                            }
                            boolean isRight = "right".equals(sTriggerPosition);
                            float enterFactor = sTriggerThreshold > 0f ? sTriggerThreshold : 0.76f;
                            float exitFactor = Math.max(0.40f, enterFactor - 0.06f);

                            boolean inHotZone;
                            if (isRight) {
                                int enterThreshold = (int) (screenW * enterFactor);
                                int exitThreshold = (int) (screenW * exitFactor);
                                if (sDragStartX >= enterThreshold) {
                                    float rightwardRequired = dp(v.getContext(), 35);
                                    if (sActiveShadowSquare) {
                                        inHotZone = (screenX >= sDragStartX + dp(v.getContext(), 10)) && (screenX >= exitThreshold);
                                    } else {
                                        inHotZone = (screenX >= sDragStartX + rightwardRequired) && (screenX >= enterThreshold);
                                    }
                                } else {
                                    inHotZone = sActiveShadowSquare
                                            ? (screenX >= exitThreshold)
                                            : (screenX >= enterThreshold);
                                }
                            } else {
                                int enterThreshold = (int) (screenH * enterFactor);
                                int exitThreshold = (int) (screenH * exitFactor);
                                if (sDragStartY >= enterThreshold) {
                                    float downwardRequired = dp(v.getContext(), 35);
                                    if (sActiveShadowSquare) {
                                        inHotZone = (screenY >= sDragStartY + dp(v.getContext(), 10)) && (screenY >= exitThreshold);
                                    } else {
                                        inHotZone = (screenY >= sDragStartY + downwardRequired) && (screenY >= enterThreshold);
                                    }
                                } else {
                                    inHotZone = sActiveShadowSquare
                                            ? (screenY >= exitThreshold)
                                            : (screenY >= enterThreshold);
                                }
                            }
                            if (inHotZone != sActiveShadowSquare) {
                                Log.i(TAG, "onDrag hotZone=" + inHotZone + " screenX=" + screenX + " screenY=" + screenY + " pos=" + sTriggerPosition);
                                updateSystemShadow(inHotZone);
                                notifyPortalDragState(v.getContext(), inHotZone ? 2 : 1, screenX, screenY);
                                lastNotifiedMotionX = screenX;
                                lastNotifiedMotionY = screenY;
                            } else if (!inHotZone) {
                                float dist = (float) Math.hypot(screenX - lastNotifiedMotionX, screenY - lastNotifiedMotionY);
                                if (dist > 2.0f) {
                                    lastNotifiedMotionX = screenX;
                                    lastNotifiedMotionY = screenY;
                                    notifyPortalDragState(v.getContext(), 1, screenX, screenY);
                                }
                            }
                            return true;
                        }
                        if (action == DragEvent.ACTION_DROP) {
                            sDragStartY = -1f;
                            sDragStartX = -1f;
                            cancelShadowMorphAnimator();
                            return false;
                        }
                        if (action == DragEvent.ACTION_DRAG_ENDED) {
                            sDragStartY = -1f;
                            sDragStartX = -1f;
                            dismissTextSelectionUi(v);
                            cancelShadowMorphAnimator();
                            sShadowMorphProgress = 0f;
                            sShadowDim = false;
                            sActiveShadowSquare = false;
                            notifyPortalDragState(v.getContext(), 0, 0f, 0f);
                            sActiveDragView = null;
                            sActiveDragText = null;
                            sActiveDragBitmap = null;
                            sGestureState.set(STATE_IDLE);
                            return true;
                        }
                        return false;
                    }
                });
            } catch (Throwable ignored) {
            }
            sActiveDragView = target;
            sActiveDragText = text;
            sActiveDragBitmap = bitmap;
            sActiveDragDensity = metrics.density;
            sActiveDragSourceWidth = target.getWidth();
            sActiveDragScreenWidth = screenW;
            sActiveDragScreenHeight = screenH;
            sActiveShadowSquare = false;
            sShadowMorphProgress = 0f;
            sShadowDim = false;
            cancelShadowMorphAnimator();
            boolean started = target.startDragAndDrop(clip, shadow, null,
                    View.DRAG_FLAG_GLOBAL | View.DRAG_FLAG_GLOBAL_URI_READ | View.DRAG_FLAG_GLOBAL_URI_WRITE);
            // ColorOS parity: Editor.startDragAndDrop() calls
            // stopTextActionMode() immediately after TextView.startDragAndDrop().
            // Do this even when start failed: cancelAppLongPress() has already
            // interrupted WeChat, and its selection handles would otherwise stay
            // in a separate overlay window until the process is killed.
            dismissTextSelectionUi(target);
            clearWebViewSelection(target);
            final View cleanupRoot = target;
            sMainHandler.postDelayed(() -> {
                dismissTextSelectionUi(cleanupRoot);
                clearWebViewSelection(cleanupRoot);
            }, 120);
            sMainHandler.postDelayed(() -> {
                dismissTextSelectionUi(cleanupRoot);
                clearWebViewSelection(cleanupRoot);
            }, 350);
            if (!started) {
                sActiveDragView = null;
                sActiveDragText = null;
                sActiveDragBitmap = null;
                sGestureState.set(STATE_IDLE);
            }
            Log.i(TAG, "system drag started=" + started + " target=" + target.getClass().getName()
                    + " text=" + (text == null ? "null" : text.length()));
            return started;
        } catch (Throwable throwable) {
            Log.w(TAG, "startSystemDrag failed", throwable);
            return false;
        }
    }

    private static Field sViewPrivateFlags2Field;
    private static Field sVgChildrenInterestedField;
    private static Field sDragEventActionField;
    private static Method sDragEventObtainMethod;
    private static boolean sDragFieldsResolved;

    /**
     * WebView.onDragEvent() (and EditText.onDragEvent()) returns true for
     * ACTION_DRAG_STARTED, which sets PFLAG2_DRAG_CAN_ACCEPT (0x1) in
     * View.mPrivateFlags2 and adds the view chain to ViewGroup.mChildrenInterestedInDrag.
     * ViewGroup.dispatchDragEvent() then routes ACTION_DRAG_LOCATION and ACTION_DROP
     * to the WebView instead of the root DecorView's OnDragListener, and
     * WebView.onDragEvent(ACTION_DROP) returns true — telling WMS the drop was
     * consumed and skipping the native createCancelAnimationLocked fly-back
     * animation (Rule 3).
     *
     * Clearing PFLAG2_DRAG_CAN_ACCEPT and mChildrenInterestedInDrag on the
     * source window's descendants right after ACTION_DRAG_STARTED (when
     * ViewGroup.dispatchDragEvent has just finished notifyChildOfDragStart and
     * invokes super.dispatchDragEvent on the root DecorView) ensures all drag
     * location and drop events in the source window are handled exclusively by
     * the root DecorView listener, which returns false on ACTION_DROP so the
     * shadow always flies back smoothly.
     */
    private static void suppressChildDragTargets(View root, DragEvent startEvent) {
        if (!(root instanceof ViewGroup)) {
            return;
        }
        try {
            if (!sDragFieldsResolved) {
                sDragFieldsResolved = true;
                try {
                    Field pf2 = View.class.getDeclaredField("mPrivateFlags2");
                    pf2.setAccessible(true);
                    sViewPrivateFlags2Field = pf2;
                } catch (Throwable ignored) {
                }
                try {
                    Field cid = ViewGroup.class.getDeclaredField("mChildrenInterestedInDrag");
                    cid.setAccessible(true);
                    sVgChildrenInterestedField = cid;
                } catch (Throwable ignored) {
                }
                try {
                    Field act = DragEvent.class.getDeclaredField("mAction");
                    act.setAccessible(true);
                    sDragEventActionField = act;
                    Method obt = DragEvent.class.getDeclaredMethod("obtain", DragEvent.class);
                    obt.setAccessible(true);
                    sDragEventObtainMethod = obt;
                } catch (Throwable ignored) {
                }
            }
            ViewGroup rootGroup = (ViewGroup) root;
            // 1. Construct a synthetic ACTION_DRAG_ENDED event via public Parcelable API
            //    (with reflective fallback) and dispatch it to rootGroup's direct children.
            //    ViewGroup.dispatchDragEvent(ACTION_DRAG_ENDED) and View.callDragEventHandler()
            //    natively clear mChildrenInterestedInDrag and mPrivateFlags2 (&= ~0x3, i.e.
            //    canAcceptDrag() -> false) on every child and descendant (including WebView),
            //    without needing hidden-API access.
            DragEvent ended = obtainDragEndedEvent(startEvent);
            if (ended != null) {
                for (int i = 0; i < rootGroup.getChildCount(); i++) {
                    View child = rootGroup.getChildAt(i);
                    if (child != null) {
                        try {
                            child.dispatchDragEvent(ended);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
            // 2. Also clear mChildrenInterestedInDrag and mPrivateFlags2 directly across
            //    the view hierarchy (including rootGroup itself) when reflection is permitted.
            clearDragAcceptRecursive(rootGroup, 0);
        } catch (Throwable throwable) {
            Log.w(TAG, "suppressChildDragTargets failed", throwable);
        }
    }

    private static DragEvent obtainDragEndedEvent(DragEvent startEvent) {
        if (startEvent != null) {
            android.os.Parcel parcel = null;
            try {
                parcel = android.os.Parcel.obtain();
                startEvent.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);
                int firstInt = parcel.readInt();
                if (firstInt == startEvent.getAction()) {
                    parcel.setDataPosition(0);
                    parcel.writeInt(DragEvent.ACTION_DRAG_ENDED);
                    parcel.setDataPosition(0);
                    DragEvent ev = DragEvent.CREATOR.createFromParcel(parcel);
                    if (ev != null && ev.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                        return ev;
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (parcel != null) {
                    try {
                        parcel.recycle();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        if (startEvent != null && sDragEventObtainMethod != null && sDragEventActionField != null) {
            try {
                DragEvent ev = (DragEvent) sDragEventObtainMethod.invoke(null, startEvent);
                if (ev != null) {
                    sDragEventActionField.setInt(ev, DragEvent.ACTION_DRAG_ENDED);
                    return ev;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void clearDragAcceptRecursive(ViewGroup group, int depth) {
        if (group == null || depth > 32) {
            return;
        }
        if (sVgChildrenInterestedField != null) {
            try {
                Object set = sVgChildrenInterestedField.get(group);
                if (set instanceof java.util.Collection) {
                    ((java.util.Collection<?>) set).clear();
                }
            } catch (Throwable ignored) {
            }
        }
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child == null) {
                continue;
            }
            if (sViewPrivateFlags2Field != null) {
                try {
                    int flags2 = sViewPrivateFlags2Field.getInt(child);
                    if ((flags2 & 0x3) != 0) {
                        sViewPrivateFlags2Field.setInt(child, flags2 & ~0x3);
                    }
                } catch (Throwable ignored) {
                }
            }
            if (child instanceof ViewGroup) {
                clearDragAcceptRecursive((ViewGroup) child, depth + 1);
            }
        }
    }

    private static boolean hasExtractableContentAt(View root, float rawX, float rawY) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return false;
        }
        try {
            int[] location = new int[2];
            root.getLocationOnScreen(location);
            if (rawX < location[0] || rawX > location[0] + root.getWidth()
                    || rawY < location[1] || rawY > location[1] + root.getHeight()) {
                return false;
            }
            if (root instanceof TextView && !TextUtils.isEmpty(((TextView) root).getText())) {
                return true;
            }
            if (root instanceof ImageView && ((ImageView) root).getDrawable() != null) {
                return true;
            }
            if (isWebViewLike(root) || isComposeView(root)) {
                return true;
            }
            String name = root.getClass().getName();
            if (name.contains("Compose") || name.contains("WebView")) {
                return true;
            }
            if (!TextUtils.isEmpty(root.getContentDescription())) {
                return true;
            }
            if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                for (int i = group.getChildCount() - 1; i >= 0; i--) {
                    if (hasExtractableContentAt(group.getChildAt(i), rawX, rawY)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean hasExtractableContent(View view) {
        if (view == null) {
            return false;
        }
        try {
            if (view instanceof TextView && !TextUtils.isEmpty(((TextView) view).getText())) {
                return true;
            }
            if (view instanceof ImageView && ((ImageView) view).getDrawable() != null) {
                return true;
            }
            if (isWebViewLike(view) || isComposeView(view)) {
                return true;
            }
            String name = view.getClass().getName();
            if (name.contains("Compose") || name.contains("WebView")) {
                return true;
            }
            if (!TextUtils.isEmpty(view.getContentDescription())) {
                return true;
            }
            Object tag = view.getTag();
            if (tag instanceof String || tag instanceof Uri || tag instanceof File) {
                return true;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    if (hasExtractableContent(group.getChildAt(i))) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isPassthroughOverlay(View v) {
        if (v == null) return false;
        String name = v.getClass().getName();
        if (name.contains("CanvasProxyView")
                || name.contains("Slidr")
                || name.contains("SwipeBack")
                || name.contains("SlideBack")
                || name.contains("TouchProxy")
                || name.contains("GestureOverlay")
                || name.contains("TouchInterceptor")
                || name.contains("FloatBall")
                || name.contains("FloatView")
                || name.contains("FloatIndicator")
                || name.contains("TalkRoom")
                || name.contains("MultiTask")) {
            return true;
        }
        try {
            CharSequence desc = v.getContentDescription();
            if (desc != null && "浮窗".equals(desc.toString().trim())) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        if (!(v instanceof ViewGroup) && !isComposeView(v) && !isWebViewLike(v)
                && !(v instanceof TextView) && !(v instanceof ImageView) && !isTextLikeView(v)) {
            Drawable bg = v.getBackground();
            if (bg == null || bg.getOpacity() == PixelFormat.TRANSPARENT) {
                if (!v.isClickable() && !v.isLongClickable()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static View findDeepTouchView(View root, float rawX, float rawY) {
        return findDeepTouchView(root, rawX, rawY, false);
    }

    private static View findDeepTouchView(View root, float rawX, float rawY, boolean skipCompose) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return null;
        }
        if (isPassthroughOverlay(root)) {
            return null;
        }
        // Compose's AndroidViewsHandler is full-screen/transparent when no
        // AndroidView child is under the finger. Returning the handler itself
        // would hide the real AndroidComposeView and make extractCompose() fail
        // with "compose semanticsOwner miss". Return null when it has no native
        // child so the caller can continue with the actual Compose root.
        if (isAndroidViewsHandler(root)) {
            return findDeepChildInGroup((ViewGroup) root, rawX, rawY, skipCompose);
        }
        try {
            if (isWebViewLike(root)) {
                return root;
            }
            if (isComposeView(root)) {
                if (root instanceof ViewGroup) {
                    View deep = findDeepChildInGroup((ViewGroup) root, rawX, rawY, skipCompose);
                    if (deep != null && deep != root && (!skipCompose || !isComposeView(deep))) {
                        return deep;
                    }
                }
                return skipCompose ? null : root;
            }
            if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                View deep = findDeepChildInGroup(group, rawX, rawY, skipCompose);
                if (deep != null) {
                    return deep;
                }
                if (skipCompose) {
                    return null;
                }
            }
            return root;
        } catch (Throwable ignored) {
            return root;
        }
    }

    private static View findDeepChildInGroup(ViewGroup group, float rawX, float rawY, boolean skipCompose) {
        int count = group.getChildCount();
        int[] location = new int[2];
        for (int i = count - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (isPassthroughOverlay(child)) {
                continue;
            }
            if (skipCompose && isComposeView(child)) {
                if (child instanceof ViewGroup && ((ViewGroup) child).getChildCount() > 0) {
                    View deepChild = findDeepChildInGroup((ViewGroup) child, rawX, rawY, true);
                    if (deepChild != null && !isComposeView(deepChild)) {
                        return deepChild;
                    }
                }
                continue;
            }
            child.getLocationOnScreen(location);
            int left = location[0];
            int top = location[1];
            int right = left + child.getWidth();
            int bottom = top + child.getHeight();
            if (rawX >= left - 5 && rawX <= right + 5 && rawY >= top - 5 && rawY <= bottom + 5) {
                View deep = findDeepTouchView(child, rawX, rawY, skipCompose);
                if (deep != null) {
                    return deep;
                }
            }
        }
        return null;
    }

    private static void cancelAppLongPress(View target) {
        if (sCancelSent) {
            return;
        }
        sCancelSent = true;
        try {
            target.cancelLongPress();
        } catch (Throwable ignored) {
        }
        try {
            View decor = sLastTouchView != null ? sLastTouchView.getRootView() : null;
            if (decor != null && sLastTouchPoint != null) {
                long now = SystemClock.uptimeMillis();
                MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL,
                        sLastTouchPoint.x, sLastTouchPoint.y, 0);
                decor.dispatchTouchEvent(cancel);
                cancel.recycle();
                Log.i(TAG, "sent ACTION_CANCEL to decor");
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "cancelAppLongPress failed", throwable);
        }
    }

    /**
     * Android's own text drag path (Editor.startDragAndDrop) finishes the text
     * action mode right after the drag starts.  We start the drag ourselves from
     * an arbitrary view, so apps such as WeChat keep their selection handles in
     * a separate overlay window forever.  Mirror that cleanup here.
     */
    private static void dismissTextSelectionUi(View rootOrTarget) {
        if (rootOrTarget == null) {
            return;
        }
        try {
            List<View> roots = new ArrayList<>();
            View root = rootOrTarget.getRootView();
            if (root != null && !roots.contains(root)) {
                roots.add(root);
            }
            if (!roots.contains(rootOrTarget)) {
                roots.add(rootOrTarget);
            }
            // Selection handles can live in a PopupWindow owned by another
            // ViewRootImpl.  Walk all roots in this process, not just the app's
            // main decor view.
            for (View globalRoot : getGlobalRootViews()) {
                if (globalRoot != null && !roots.contains(globalRoot)) {
                    roots.add(globalRoot);
                }
            }
            List<TextView> candidates = new ArrayList<>();
            for (View candidateRoot : roots) {
                collectSelectedTextViews(candidateRoot, candidates, 0);
            }
            for (TextView textView : candidates) {
                dismissTextSelectionUi(textView);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "dismissTextSelectionUi failed", throwable);
        }
    }

    private static List<View> getGlobalRootViews() {
        List<View> roots = new ArrayList<>();
        try {
            Class<?> globalClass = Class.forName("android.view.WindowManagerGlobal");
            Object global = globalClass.getMethod("getInstance").invoke(null);
            Field viewsField = globalClass.getDeclaredField("mViews");
            viewsField.setAccessible(true);
            Object value = viewsField.get(global);
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof View) {
                        roots.add((View) item);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return roots;
    }

    private static void collectSelectedTextViews(View view, List<TextView> out, int depth) {
        if (view == null || depth > 80 || out.size() >= 24) {
            return;
        }
        try {
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                boolean selected;
                try {
                    selected = textView.hasSelection();
                } catch (Throwable ignored) {
                    selected = false;
                }
                if (selected || textView.isFocused() || hasTextActionMode(textView)) {
                    if (!out.contains(textView)) {
                        out.add(textView);
                    }
                }
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    collectSelectedTextViews(group.getChildAt(i), out, depth + 1);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean hasTextActionMode(TextView textView) {
        try {
            Object editor = getField(textView, "mEditor");
            return editor != null && getField(editor, "mTextActionMode") != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean stopEditorTextActionMode(TextView textView) {
        try {
            Object editor = getField(textView, "mEditor");
            return editor != null && invokeNoArg(editor, "stopTextActionMode");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hideEditorHandles(TextView textView) {
        try {
            Object editor = getField(textView, "mEditor");
            if (editor == null) {
                return false;
            }
            boolean hidden = false;
            Object selectionController = getField(editor, "mSelectionModifierCursorController");
            if (selectionController == null) {
                selectionController = getField(editor, "mSelectionController");
            }
            if (selectionController != null) {
                hidden = invokeNoArg(selectionController, "hide");
            }
            try {
                Object insertionController = getField(editor, "mInsertionController");
                if (insertionController != null) {
                    hidden = invokeNoArg(insertionController, "hide") || hidden;
                }
            } catch (Throwable ignored) {
            }
            return hidden;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean finishTextActionMode(TextView textView) {
        try {
            Object editor = getField(textView, "mEditor");
            Object mode = editor == null ? null : getField(editor, "mTextActionMode");
            if (mode instanceof ActionMode) {
                ((ActionMode) mode).finish();
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void dismissTextSelectionUi(TextView textView) {
        if (textView == null) {
            return;
        }
        boolean hadSelection = false;
        boolean hadActionMode = false;
        try {
            hadSelection = textView.hasSelection();
            hadActionMode = hasTextActionMode(textView);
        } catch (Throwable ignored) {
        }
        // Hide both the selection handles and the insertion (cursor) handle.
        // A focused input can own a cursor handle even when hasSelection() is
        // false, and that is exactly the state left behind by some app editors.
        boolean handlesHidden = hideEditorHandles(textView);
        if (!hadSelection && !hadActionMode) {
            return;
        }
        boolean stopped = invokeNoArg(textView, "stopTextActionMode");
        if (!stopped) {
            stopped = stopEditorTextActionMode(textView);
        }
        if (!stopped) {
            stopped = finishTextActionMode(textView);
        }
        handlesHidden = hideEditorHandles(textView) || handlesHidden;
        if (!stopped && !handlesHidden) {
            // Public fallback: Editor.onFocusChanged(false) hides the cursor and
            // span controllers and finishes the text action mode.
            try {
                textView.clearFocus();
            } catch (Throwable ignored) {
            }
        }
        boolean stillSelected = false;
        try {
            stillSelected = textView.hasSelection();
        } catch (Throwable ignored) {
        }
        if (hadSelection && stillSelected) {
            try {
                textView.performAccessibilityAction(
                        AccessibilityNodeInfo.ACTION_CLEAR_SELECTION, null);
            } catch (Throwable ignored) {
            }
            try {
                CharSequence text = textView.getText();
                if (text instanceof Spannable) {
                    Selection.removeSelection((Spannable) text);
                }
            } catch (Throwable ignored) {
            }
        }
        Log.i(TAG, "text selection ui dismissed selection=" + hadSelection
                + " actionMode=" + hadActionMode + " stopped=" + stopped
                + " handlesHidden=" + handlesHidden
                + " view=" + textView.getClass().getName());
    }

    private static boolean invokeNoArg(Object target, String name) {
        if (target == null) {
            return false;
        }
        try {
            Method method = findMethod(target.getClass(), name);
            if (method == null) {
                return false;
            }
            method.setAccessible(true);
            method.invoke(target);
            return true;
        } catch (Throwable throwable) {
            Log.w(TAG, "invokeNoArg " + name + " failed: " + throwable.getClass().getName());
            return false;
        }
    }

    private static void showTiltEffect(View decor) {
        try {
            if (!(decor instanceof ViewGroup)) {
                return;
            }
            hideTiltEffect();
            ViewGroup decorGroup = (ViewGroup) decor;
            if (decorGroup.getChildCount() == 0) {
                return;
            }
            View root = decorGroup.getChildAt(0);
            Context context = decor.getContext();
            int barHeight = dp(context, 104);
            View dim = new View(context);
            dim.setBackgroundColor(0x66000000);
            dim.setAlpha(0f);
            decorGroup.addView(dim, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            HorizontalScrollView bar = buildBridgeBar(context);
            bar.setAlpha(0f);
            bar.setTranslationY(barHeight);
            bar.setElevation(dp(context, 12));
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, barHeight, Gravity.BOTTOM);
            params.bottomMargin = dp(context, 52);
            decorGroup.addView(bar, 1, params);
            sTiltDecor = decor;
            sTiltBar = bar;
            sTiltDim = dim;
            sTiltRoot = root;
            sTiltBarHeight = barHeight;
            sTiltProgress = 0f;

            root.setPivotX(root.getWidth() / 2f);
            root.setPivotY(root.getHeight());
            root.setCameraDistance(dp(context, 12000));
            root.setClipToOutline(true);
            root.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline outline) {
                    outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(context, 18));
                }
            });
            decorGroup.setOnDragListener((v, event) -> handleTiltDrag(context, decorGroup, bar, event));
            sMainHandler.removeCallbacks(sTiltTimeout);
            sMainHandler.postDelayed(sTiltTimeout, 15000);
            Log.i(TAG, "tilt prepared, bar=" + barHeight);
        } catch (Throwable throwable) {
            Log.w(TAG, "showTiltEffect failed", throwable);
            hideTiltEffect();
        }
    }

    /**
     * Applies the same task-zoom transform to every window root in this app
     * process, including PopupWindow / Dialog roots. ColorOS does this at the
     * system Task surface level; without system permission this is the closest
     * app-process equivalent.
     */
    private static void transformOtherRoots(View decor, float progress) {
        try {
            Class<?> globalClass = Class.forName("android.view.WindowManagerGlobal");
            Object global = globalClass.getMethod("getInstance").invoke(null);
            Field viewsField = globalClass.getDeclaredField("mViews");
            viewsField.setAccessible(true);
            Object value = viewsField.get(global);
            if (!(value instanceof List)) {
                return;
            }
            Context context = decor.getContext();
            int screenWidth = decor.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getPhysicalScreenHeight(context);
            for (Object item : (List<?>) value) {
                if (!(item instanceof View)) {
                    continue;
                }
                View view = (View) item;
                if (view == decor || view == sTiltBar || view == sTiltDim) {
                    continue;
                }
                try {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    view.setPivotX(screenWidth / 2f - location[0]);
                    view.setPivotY(screenHeight - location[1]);
                    view.setCameraDistance(dp(context, 12000));
                    view.setScaleX(1f - 0.08f * progress);
                    view.setScaleY(1f - 0.08f * progress);
                    view.setTranslationY(-dp(context, 36) * progress);
                    view.setRotationX(4f * progress);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable throwable) {
            Log.d(TAG, "transformOtherRoots unavailable: " + throwable);
        }
    }

    private static void updateTiltProgress(float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        sTiltProgress = p;
        try {
            if (sTiltRoot != null) {
                sTiltRoot.setScaleX(1f - 0.08f * p);
                sTiltRoot.setScaleY(1f - 0.08f * p);
                sTiltRoot.setTranslationY(-dp(sTiltRoot.getContext(), 36) * p);
                sTiltRoot.setRotationX(4f * p);
            }
            if (sTiltDim != null) {
                sTiltDim.setAlpha(0.45f * p);
            }
            if (sTiltBar != null) {
                sTiltBar.setAlpha(p);
                sTiltBar.setTranslationY(sTiltBarHeight * (1f - p));
            }
            if (sTiltDecor != null) {
                transformOtherRoots(sTiltDecor, p);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "updateTiltProgress failed", throwable);
        }
    }

    private static void updateTiltFromDrag(View decor, float rawY) {
        int screenHeight = getPhysicalScreenHeight(decor.getContext());
        float start = screenHeight * 0.62f;
        float end = screenHeight * 0.92f;
        updateTiltProgress((rawY - start) / (end - start));
    }

    private static boolean handleTiltDrag(Context context, ViewGroup decor, View bar, DragEvent event) {
        try {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                case DragEvent.ACTION_DRAG_ENTERED: {
                    int[] decorLocation = new int[2];
                    decor.getLocationOnScreen(decorLocation);
                    float currentY = decorLocation[1] + event.getY();
                    updateTiltFromDrag(decor, currentY);
                    int screenH = getPhysicalScreenHeight(decor.getContext());
                    float density = decor.getResources().getDisplayMetrics().density;
                    int enterThreshold = Math.min((int) (screenH * 0.82f),
                            screenH - Math.round(160f * density));
                    int exitThreshold = (int) (screenH * 0.77f);
                    boolean inHotZone = sActiveShadowSquare
                            ? (currentY >= exitThreshold)
                            : (currentY >= enterThreshold);
                    updateSystemShadow(inHotZone);
                    return true;
                }
                case DragEvent.ACTION_DRAG_EXITED: {
                    updateSystemShadow(false);
                    return true;
                }
                case DragEvent.ACTION_DROP: {
                    cancelShadowMorphAnimator();
                    String text = null;
                    ClipData clip = event.getClipData();
                    if (clip != null && clip.getItemCount() > 0) {
                        text = clip.getItemAt(0).coerceToText(context).toString();
                    }
                    int[] decorLocation = new int[2];
                    decor.getLocationOnScreen(decorLocation);
                    float rawX = decorLocation[0] + event.getX();
                    float rawY = decorLocation[1] + event.getY();
                    updateTiltFromDrag(decor, rawY);
                    BridgeTarget target = findBridgeTarget(bar, rawX, rawY);
                    if (target != null) {
                        performBridgeTarget(context, target, text);
                    }
                    return true;
                }
                case DragEvent.ACTION_DRAG_ENDED:
                    updateSystemShadow(false);
                    hideTiltEffect();
                    return true;
                default:
                    return true;
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "handleTiltDrag failed", throwable);
            return true;
        }
    }

    private static BridgeTarget findBridgeTarget(View bar, float rawX, float rawY) {
        try {
            if (!(bar instanceof HorizontalScrollView) || ((ViewGroup) bar).getChildCount() == 0) {
                return null;
            }
            ViewGroup row = (ViewGroup) ((ViewGroup) bar).getChildAt(0);
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                int[] location = new int[2];
                child.getLocationOnScreen(location);
                if (rawX >= location[0] && rawX <= location[0] + child.getWidth()
                        && rawY >= location[1] && rawY <= location[1] + child.getHeight()) {
                    Object tag = child.getTag();
                    return tag instanceof BridgeTarget ? (BridgeTarget) tag : null;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void performBridgeTarget(Context context, BridgeTarget target, String text) {
        try {
            if (target.kind == BridgeTarget.KIND_SAVE) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("portal", text == null ? "" : text));
                Log.i(TAG, "bridge target save text=" + (text == null ? "null" : text.length()));
                return;
            }
            if (target.kind == BridgeTarget.KIND_SEARCH) {
                Intent search = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.bing.com/search?q=" + Uri.encode(text == null ? "" : text)));
                search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(search);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
            if (!TextUtils.isEmpty(target.packageName)) {
                intent.setPackage(target.packageName);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "bridge target share pkg=" + target.packageName);
        } catch (Throwable throwable) {
            Log.w(TAG, "performBridgeTarget failed", throwable);
        }
    }

    private static HorizontalScrollView buildBridgeBar(Context context) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xF21C1D20);
        background.setCornerRadius(dp(context, 20));
        scroll.setBackground(background);
        scroll.setPadding(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 6));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (BridgeTarget target : buildBridgeTargets(context)) {
            row.addView(buildBridgeItem(context, target), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        scroll.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private static View buildBridgeItem(Context context, BridgeTarget target) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
        item.setTag(target);

        FrameLayout iconBox = new FrameLayout(context);
        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setColor(Color.rgb(245, 246, 248));
        iconBackground.setCornerRadius(dp(context, 22));
        iconBox.setBackground(iconBackground);
        ImageView icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setImageDrawable(target.icon);
        iconBox.addView(icon, new FrameLayout.LayoutParams(dp(context, 28), dp(context, 28), Gravity.CENTER));
        item.addView(iconBox, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));

        TextView label = new TextView(context);
        label.setText(target.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                dp(context, 56), ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(context, 3);
        item.addView(label, labelParams);
        return item;
    }

    private static List<BridgeTarget> buildBridgeTargets(Context context) {
        List<BridgeTarget> targets = new ArrayList<>();
        try {
            targets.add(new BridgeTarget("中转站",
                    context.getPackageManager().getApplicationIcon(context.getPackageName()),
                    "", BridgeTarget.KIND_SAVE));
        } catch (Throwable ignored) {
        }
        try {
            PackageManager pm = context.getPackageManager();
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            List<ResolveInfo> resolved = pm.queryIntentActivities(send, 0);
            java.util.Set<String> seen = new java.util.HashSet<>();
            seen.add(context.getPackageName());
            int added = 0;
            for (ResolveInfo info : resolved) {
                if (info.activityInfo == null) {
                    continue;
                }
                String pkg = info.activityInfo.packageName;
                if (TextUtils.isEmpty(pkg) || !seen.add(pkg)) {
                    continue;
                }
                try {
                    targets.add(new BridgeTarget(info.loadLabel(pm).toString(), info.loadIcon(pm), pkg,
                            BridgeTarget.KIND_SHARE));
                    added++;
                } catch (Throwable ignored) {
                }
                if (added >= 6) {
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
        targets.add(new BridgeTarget("搜索地址", context.getDrawable(android.R.drawable.ic_menu_search),
                "", BridgeTarget.KIND_SEARCH));
        return targets;
    }

    private static void hideTiltEffect() {
        final View root = sTiltRoot;
        final View decor = sTiltDecor;
        final View bar = sTiltBar;
        final View dim = sTiltDim;
        sTiltRoot = null;
        sTiltDecor = null;
        sTiltBar = null;
        sTiltDim = null;
        sTiltProgress = 0f;
        sMainHandler.removeCallbacks(sTiltTimeout);
        if (decor != null) {
            transformOtherRoots(decor, 0f);
        }
        if (decor instanceof ViewGroup) {
            try {
                ((ViewGroup) decor).setOnDragListener(null);
            } catch (Throwable ignored) {
            }
        }
        if (root != null) {
            try {
                root.animate().cancel();
                root.setScaleX(1f);
                root.setScaleY(1f);
                root.setTranslationY(0f);
                root.setRotationX(0f);
                root.setClipToOutline(false);
            } catch (Throwable ignored) {
            }
        }
        removeBridgeBar(decor, bar, dim);
    }

    private static void removeBridgeBar(View decor, View bar, View dim) {
        if (decor instanceof ViewGroup) {
            try {
                if (bar != null) {
                    ((ViewGroup) decor).removeView(bar);
                }
                if (dim != null) {
                    ((ViewGroup) decor).removeView(dim);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class BridgeTarget {
        static final int KIND_SHARE = 0;
        static final int KIND_SAVE = 1;
        static final int KIND_SEARCH = 2;

        final String label;
        final Drawable icon;
        final String packageName;
        final int kind;

        BridgeTarget(String label, Drawable icon, String packageName, int kind) {
            this.label = label;
            this.icon = icon;
            this.packageName = packageName;
            this.kind = kind;
        }
    }

    private static final class PreviewShadow extends View.DragShadowBuilder {
        private final String text;
        private final Bitmap bitmap;
        private final float density;
        private final int sourceWidth;
        private final int screenWidth;
        private final int screenHeight;
        private final float morphProgress;
        /** Dimmed while the finger is back inside the drag circle: releasing now cancels. */
        private final boolean dim;

        private int width;
        private int height;
        private int touchX;
        private int touchY;
        private StaticLayout layout;

        PreviewShadow(String text, Bitmap bitmap, float density, int sourceWidth, int screenWidth, int screenHeight) {
            this(text, bitmap, density, sourceWidth, screenWidth, screenHeight, 0f);
        }

        PreviewShadow(String text, Bitmap bitmap, float density, int sourceWidth, int screenWidth, int screenHeight, boolean square) {
            this(text, bitmap, density, sourceWidth, screenWidth, screenHeight, square ? 1f : 0f);
        }

        PreviewShadow(String text, Bitmap bitmap, float density, int sourceWidth, int screenWidth, int screenHeight, float morphProgress) {
            this(text, bitmap, density, sourceWidth, screenWidth, screenHeight, morphProgress, false);
        }

        PreviewShadow(String text, Bitmap bitmap, float density, int sourceWidth, int screenWidth, int screenHeight, float morphProgress, boolean dim) {
            this.text = text;
            this.bitmap = bitmap;
            this.density = density > 0 ? density : 3.0f;
            this.sourceWidth = sourceWidth;
            this.screenWidth = screenWidth > 0 ? screenWidth : 1080;
            this.screenHeight = screenHeight > 0 ? screenHeight : 2400;
            this.morphProgress = Math.max(0f, Math.min(1f, morphProgress));
            this.dim = dim;

            initMetrics();
        }

        private int dp(float value) {
            return Math.round(value * density);
        }

        static float getImageScale(float areaPx, float density) {
            float d2 = density * density;
            if (areaPx <= 14400f * d2) {
                return 1.10f;
            } else if (areaPx <= 40000f * d2) {
                return 0.85f;
            } else if (areaPx <= 129600f * d2) {
                return 0.60f;
            } else if (areaPx <= 230400f * d2) {
                return 0.40f;
            } else {
                return 0.30f;
            }
        }

        private void initMetrics() {
            if (bitmap != null) {
                float areaPx = (float) bitmap.getWidth() * (float) bitmap.getHeight();
                float scale = getImageScale(areaPx, density);
                int scaledW = Math.round(bitmap.getWidth() * scale);
                int scaledH = Math.round(bitmap.getHeight() * scale);
                // Only scale up if both dimensions are genuinely tiny (< 60dp)
                if (scaledW < dp(60) && scaledH < dp(60)) {
                    float minScale = Math.max((float) dp(60) / bitmap.getWidth(), (float) dp(60) / bitmap.getHeight());
                    scale = Math.max(scale, minScale);
                    scaledW = Math.round(bitmap.getWidth() * scale);
                    scaledH = Math.round(bitmap.getHeight() * scale);
                }
                // Cap maximum card width at 75% screen width
                int maxAllowedW = (int) (screenWidth * 0.75f);
                if (scaledW > maxAllowedW) {
                    float down = (float) maxAllowedW / scaledW;
                    scaledW = maxAllowedW;
                    scaledH = Math.round(scaledH * down);
                }
                width = scaledW;
                height = scaledH;
                touchX = width / 2;
                touchY = height / 2;
                return;
            }

            if (!TextUtils.isEmpty(text)) {
                TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                float fontSize = 12.27f * density;
                textPaint.setTextSize(fontSize);
                textPaint.setColor(Color.BLACK);

                float textMeasuredWidth = textPaint.measureText(text);
                int maxAllowedWidth = (int) (screenWidth * 0.8f);
                int baseWidth = (int) textMeasuredWidth;
                if (sourceWidth > 0) {
                    baseWidth = Math.min(baseWidth, sourceWidth);
                }
                baseWidth = Math.min(baseWidth, maxAllowedWidth);
                int shadowContentWidth = Math.min((int) (baseWidth * 1.05f), maxAllowedWidth);
                if (shadowContentWidth < dp(60)) {
                    shadowContentWidth = Math.min(dp(220), maxAllowedWidth);
                }

                float lineHeight = 17.43f * density;
                float lineSpacingMult = (lineHeight - 4f) / fontSize;
                if (lineSpacingMult <= 0) {
                    lineSpacingMult = 1.5f;
                }

                try {
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, shadowContentWidth)
                                .setLineSpacing(0f, lineSpacingMult)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .build();
                    } else {
                        layout = new StaticLayout(text, textPaint, shadowContentWidth,
                                Layout.Alignment.ALIGN_NORMAL, lineSpacingMult, 0f, true);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "StaticLayout creation failed", t);
                }

                int padding = dp(12);
                int layoutH = layout != null ? layout.getHeight() : dp(40);
                int maxAllowedHeight = (int) (screenHeight * 0.8f);

                width = Math.max(dp(76), Math.min(shadowContentWidth + padding * 2, screenWidth));
                height = Math.max(dp(76), Math.min(layoutH + padding * 2, maxAllowedHeight));

                touchX = width / 2;
                touchY = height / 2;
                return;
            }

            width = dp(120);
            height = dp(76);
            touchX = width / 2;
            touchY = height / 2;
        }

        @Override
        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            outShadowSize.set(width, height);
            outShadowTouchPoint.set(touchX, touchY);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            int restore = -1;
            if (dim) {
                restore = canvas.saveLayerAlpha(new RectF(0f, 0f, width, height), Math.round(255f * 0.45f));
            }
            try {
                drawShadowContent(canvas);
            } finally {
                if (restore >= 0) {
                    canvas.restoreToCount(restore);
                }
            }
        }

        private void drawShadowContent(Canvas canvas) {
            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
            float p = morphProgress;
            int targetBox = dp(76);
            float curW = width + (targetBox - width) * p;
            float curH = height + (targetBox - height) * p;
            float boxLeft = touchX - curW / 2f;
            float boxTop = touchY - curH / 2f;
            RectF boxRect = new RectF(boxLeft, boxTop, boxLeft + curW, boxTop + curH);
            float radius = dp(12) + (dp(16) - dp(12)) * p;

            if (bitmap != null) {
                Path clip = new Path();
                clip.addRoundRect(boxRect, radius, radius, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(clip);
                float scale = Math.max(curW / bitmap.getWidth(), curH / bitmap.getHeight());
                float dx = boxRect.left + (curW - bitmap.getWidth() * scale) * 0.5f;
                float dy = boxRect.top + (curH - bitmap.getHeight() * scale) * 0.5f;
                canvas.translate(dx, dy);
                canvas.scale(scale, scale);
                canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
                canvas.restore();
                return;
            }

            Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
            background.setColor(Color.WHITE);
            canvas.drawRoundRect(boxRect, radius, radius, background);

            if (!TextUtils.isEmpty(text)) {
                if (p <= 0.01f && layout != null) {
                    int padding = dp(12);
                    canvas.save();
                    canvas.translate(padding, padding);
                    layout.draw(canvas);
                    canvas.restore();
                    return;
                }
                float pad = dp(12) + (dp(6) - dp(12)) * p;
                float fontSize = (12.27f + (11.0f - 12.27f) * p) * density;
                float fullSpacing = ((17.43f * density) - 4f) / (12.27f * density);
                if (fullSpacing <= 0f) fullSpacing = 1.1f;
                float spacingMult = fullSpacing + (1.0f - fullSpacing) * p;

                TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                tp.setTextSize(fontSize);
                tp.setColor(Color.BLACK);

                int innerW = Math.max(dp(36), Math.round(curW - pad * 2f));
                int innerH = Math.max(dp(24), Math.round(curH - pad * 2f));
                float lineH = Math.max(fontSize, fontSize * spacingMult);
                int maxLines = Math.max(1, Math.round(innerH / lineH));
                if (p >= 0.85f) {
                    maxLines = Math.min(maxLines, 4);
                }
                String sub = text.length() > 300 ? text.substring(0, 300) : text;
                StaticLayout dynLayout = null;
                try {
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        dynLayout = StaticLayout.Builder.obtain(sub, 0, sub.length(), tp, innerW)
                                .setLineSpacing(0f, spacingMult)
                                .setMaxLines(maxLines)
                                .setEllipsize(TextUtils.TruncateAt.END)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .build();
                    } else {
                        dynLayout = new StaticLayout(sub, tp, innerW,
                                Layout.Alignment.ALIGN_NORMAL, spacingMult, 0f, true);
                    }
                } catch (Throwable ignored) {
                }

                if (dynLayout != null) {
                    Path clip = new Path();
                    clip.addRoundRect(boxRect, radius, radius, Path.Direction.CW);
                    canvas.save();
                    canvas.clipPath(clip);
                    float centeredTop = boxRect.top + (curH - dynLayout.getHeight()) / 2f;
                    float normalTop = boxRect.top + pad;
                    float drawTop = normalTop + (Math.max(boxRect.top + pad, centeredTop) - normalTop) * p;
                    canvas.translate(boxRect.left + pad, drawTop);
                    dynLayout.draw(canvas);
                    canvas.restore();
                }
            }
        }
    }

    private static String tokenString(Object content) {
        try {
            Object token = getField(content, "mInjectorToken");
            if (token != null) {
                Object pkg = token.getClass().getMethod("getPkgName").invoke(token);
                if (pkg instanceof String) {
                    return (String) pkg;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Context currentContext() {
        WeakReference<Context> reference = sContextRef;
        Context context = reference == null ? null : reference.get();
        if (context != null) {
            return context;
        }
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object application = activityThread.getMethod("currentApplication").invoke(null);
            if (application instanceof Context) {
                rememberContext((Context) application);
                return (Context) application;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void rememberContext(Context context) {
        if (context != null) {
            sContextRef = new WeakReference<>(context);
        }
    }

    private static Object getField(Object target, String name) throws Exception {
        return findField(target.getClass(), name).get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        findField(target.getClass(), name).set(target, value);
    }

    private static Field findField(Class<?> type, String name) throws Exception {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Field field = cursor.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " in " + type.getName());
    }

    private static Method findMethod(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null) {
            for (Method m : cursor.getDeclaredMethods()) {
                if (name.equals(m.getName())) {
                    return m;
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    private static Field findFieldOrNull(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }
}














