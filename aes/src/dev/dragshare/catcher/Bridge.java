package dev.dragshare.catcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
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
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
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
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Minimal Plan-B bridge for the AES 5.1.7 PoC.
 *
 * All methods are called from smali patches inside classes.dex, so this class
 * must stay dependency-free (Android SDK only) and must never throw.
 */
public final class Bridge {

    private static final String TAG = "DragSharePoC";
    private static final String PORTAL_PACKAGE = "com.github.leaf.hyperportal";
    private static final String PORTAL_SERVICE = PORTAL_PACKAGE + ".PortalService";
    private static volatile WeakReference<Context> sContextRef;
    private static volatile PointF sLastTouchPoint;
    private static volatile View sLastTouchView;
    private static volatile Activity sLastActivity;
    private static volatile long sLastTouchAt;

    private static final int CUSTOM_LONG_PRESS_TIMEOUT_MS = 400; // Oppo ColorOS standard
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
    private static volatile boolean sActiveShadowSquare;
    private static volatile boolean sShadowReceiverRegistered;
    private static final BroadcastReceiver sShadowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !"com.dragshare.bridge.SHADOW_STATE".equals(intent.getAction())) {
                return;
            }
            int state = intent.getIntExtra("state", 0);
            updateSystemShadow(state >= 2);
        }
    };
    private static volatile long sDownAt;
    private static volatile int sGestureGeneration;
    private static volatile int sPendingGeneration;
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

    static {
        try {
            Log.i(TAG, "Bridge loaded v36 pid=" + android.os.Process.myPid()
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
            ensureShadowReceiver(activity != null ? activity.getApplicationContext() : currentContext());
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
                    if (view != null) {
                        sLastTouchView = view;
                    }
                    sDownRawX = rawX;
                    sDownRawY = rawY;
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
                    sActiveDragView = null;
                    sActiveShadowSquare = false;
                    int generation = sGestureGeneration + 1;
                    sGestureGeneration = generation;
                    sPendingGeneration = generation;
                    // AES' own u/a touch listener owns the long-press timer and
                    // runs the extraction on its established pipeline. Do not
                    // start a second 400ms engine here: that duplicate pass was
                    // the main source of jank on simple pages.
                    sMainHandler.removeCallbacks(sCustomLongPressRunnable);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    sLastMoveRawX = rawX;
                    sLastMoveRawY = rawY;
                    float dx = rawX - sDownRawX;
                    float dy = rawY - sDownRawY;
                    float dist = (float) Math.hypot(dx, dy);

                    // ColorOS touch slop: allow natural finger micro-tremors (up to 100px).
                    // Only cancel if user actually dragged far away (> 100px).
                    if (dist > 100f) {
                        sMovedSinceDown = true;
                        if (sGestureState.get() == STATE_IDLE) {
                            sMainHandler.removeCallbacks(sCustomLongPressRunnable);
                        }
                    }

                    if (sActiveDragView != null) {
                        int height = sActiveDragView.getResources()
                                .getDisplayMetrics().heightPixels;
                        updateSystemShadow(rawY >= height * 0.82f);
                    }

                    if (sGestureState.get() == STATE_DRAGGING) {
                        // Custom long-press has already triggered and forwarded content
                    } else if (sMovedSinceDown && sPendingPayload != null) {
                        Bundle pending = sPendingPayload;
                        Object pendingContent = sPendingContent;
                        sPendingPayload = null;
                        sPendingContent = null;
                        Log.i(TAG, "pending drag starts on move");
                        forwardToPortal(pending, pendingContent);
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    sMainHandler.removeCallbacks(sCustomLongPressRunnable);
                    sGestureActive = false;
                    sActiveDragView = null;
                    sActiveDragText = null;
                    sActiveDragBitmap = null;
                    if (action == MotionEvent.ACTION_CANCEL && sGestureState.get() == STATE_DRAGGING) {
                        // Our own cancelAppLongPress() injects an ACTION_CANCEL to
                        // the decor. Do not let that synthetic event tear down the
                        // drag state that was just established.
                        Log.d(TAG, "ignore synthetic cancel while dragging");
                        return;
                    }
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
            if (!sGestureActive || sPendingGeneration != sGestureGeneration) {
                Log.d(TAG, "customLongPress skipped (stale gesture)");
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
            Log.i(TAG, "customLongPress triggered at " + sDownRawX + "," + sDownRawY
                    + " after " + (startedAt - sDownAt) + "ms");

            View touchTarget = findDeepTouchView(root, sDownRawX, sDownRawY);
            if (touchTarget == null) {
                touchTarget = root;
            }

            // If AES' own long-press pipeline already extracted this gesture,
            // reuse its result instead of running the whole tree scan twice.
            Bundle content = sPendingPayload;
            if (content != null && isBundleMeaningful(content)) {
                sPendingPayload = null;
                sPendingContent = null;
                Log.i(TAG, "customLongPress reuses AES pipeline payload");
            } else {
                sPendingPayload = null;
                sPendingContent = null;
                content = preExtract(touchTarget, 3, false);
            }

            // USER EXPLICIT CONSTRAINT: "不要全量兜底截屏"
            if (content == null || !isBundleMeaningful(content)) {
                Log.i(TAG, "customLongPress: no real content found, aborting drag (no fallback screenshot)"
                        + " took=" + (SystemClock.uptimeMillis() - startedAt) + "ms");
                sGestureState.set(STATE_PROCESSED);
                return;
            }

            Log.i(TAG, "customLongPress: hit real content! type=" + content.getInt("catchType", 0)
                    + " text=" + content.getString("viewContent")
                    + " bitmap=" + describeBitmap(content)
                    + " took=" + (SystemClock.uptimeMillis() - startedAt) + "ms");
            sGestureState.set(STATE_DRAGGING);

            try {
                touchTarget.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } catch (Throwable ignored) {
            }

            content.putString("coordinate", ((int) sDownRawX) + "-" + ((int) sDownRawY));
            sMovedSinceDown = true;
            sLastTouchAt = SystemClock.uptimeMillis();
            if (touchTarget.isAttachedToWindow()) {
                sLastTouchView = touchTarget;
            }
            forwardToPortal(content, null);
        } catch (Throwable throwable) {
            Log.w(TAG, "onCustomLongPressTriggered failed", throwable);
            sGestureState.set(STATE_PROCESSED);
        }
    }

    /** Called at the beginning of Interceptor.l(PointF, PointF, View). */
    public static void recordTouch(PointF first, PointF second, View view) {
        try {
            if (sGestureState.get() != STATE_IDLE) {
                Log.d(TAG, "recordTouch ignored (custom engine active)");
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
            if (name.contains("AndroidComposeView") || name.contains("compose.ui.platform")
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

    private static boolean isWebViewLikeRaw(View view) {
        if (view instanceof WebView) {
            return true;
        }
        String name = view.getClass().getName();
        if (!name.contains("WebView") || name.contains("WebViewClient")) {
            return false;
        }
        try {
            for (Method method : view.getClass().getMethods()) {
                if ("evaluateJavascript".equals(method.getName())
                        && method.getParameterTypes().length == 2) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
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
        if (isWebViewLike(root)) {
            int[] location = new int[2];
            root.getLocationOnScreen(location);
            if (rawX >= location[0] && rawX < location[0] + root.getWidth()
                    && rawY >= location[1] && rawY < location[1] + root.getHeight()) {
                return root;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View found = findWebViewAt(group.getChildAt(i), rawX, rawY);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void prefetchWebView(View webView, float rawX, float rawY) {
        try {
            long now = SystemClock.uptimeMillis();
            WebCache previous = sWebCache.get(webView);
            if (previous != null && now - previous.timestamp < 300) {
                return;
            }
            float density = webView.getResources().getDisplayMetrics().density;
            int[] location = new int[2];
            webView.getLocationOnScreen(location);
            float cssX = (rawX - location[0]) / density;
            float cssY = (rawY - location[1]) / density;
            String js = String.format(Locale.US,
                    "(function(){try{var e=document.elementFromPoint(%.2f,%.2f);"
                            + "if(!e)return JSON.stringify({t:null,s:null});"
                            + "var t=(e.innerText||e.textContent||'').trim();"
                            + "var s=e.src||'';"
                            + "if(!s){var bg=getComputedStyle(e).backgroundImage||'';"
                            + "var m=/url\\([\"']?(.*?)[\"']?\\)/.exec(bg);if(m)s=m[1];}"
                            + "return JSON.stringify({t:t.substring(0,20000),s:s});"
                            + "}catch(x){return JSON.stringify({t:null,s:null,e:String(x)});}})()",
                    cssX, cssY);
            ValueCallback<String> callback = new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    handleWebResult(webView, value, cssX, cssY);
                }
            };
            if (webView instanceof WebView) {
                ((WebView) webView).evaluateJavascript(js, callback);
            } else {
                for (Method method : webView.getClass().getMethods()) {
                    if ("evaluateJavascript".equals(method.getName())
                            && method.getParameterTypes().length == 2) {
                        method.setAccessible(true);
                        Class<?> callbackType = method.getParameterTypes()[1];
                        Object proxy = Proxy.newProxyInstance(
                                callbackType.getClassLoader(),
                                new Class<?>[]{callbackType},
                                (proxyObject, proxyMethod, args) -> {
                                    if ("onReceiveValue".equals(proxyMethod.getName())
                                            && args != null && args.length > 0) {
                                        handleWebResult(webView,
                                                args[0] == null ? null : String.valueOf(args[0]),
                                                cssX, cssY);
                                    }
                                    return null;
                                });
                        method.invoke(webView, js, proxy);
                        return;
                    }
                }
            }
        } catch (Throwable throwable) {
            Log.d(TAG, "prefetchWebView failed: " + throwable);
        }
    }

    private static void handleWebResult(View webView, String value, float cssX, float cssY) {
        try {
            String raw = value;
            if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
                raw = new JSONArray("[" + raw + "]").getString(0);
            }
            JSONObject json = new JSONObject(raw);
            WebCache cache = new WebCache();
            cache.text = json.optString("t", null);
            String srcUrl = json.optString("s", null);
            if (srcUrl != null && srcUrl.startsWith("//")) {
                srcUrl = "https:" + srcUrl;
            }
            if (srcUrl != null && srcUrl.contains("&quot;")) {
                srcUrl = srcUrl.replace("&quot;", "");
            }
            cache.src = srcUrl;
            cache.cssX = cssX;
            cache.cssY = cssY;
            cache.timestamp = SystemClock.uptimeMillis();
            sWebCache.put(webView, cache);
            Log.i(TAG, "webview prefetch text=" + (cache.text == null ? 0 : cache.text.length())
                    + " src=" + cache.src);
        } catch (Throwable throwable) {
            Log.d(TAG, "webview callback failed: " + throwable);
        }
    }

    private static Bundle webViewBundle(View view) {
        WebCache cache = sWebCache.get(view);
        if (cache == null || SystemClock.uptimeMillis() - cache.timestamp > 5000) {
            return null;
        }
        if (!TextUtils.isEmpty(cache.text)) {
            Bundle bundle = new Bundle();
            bundle.putString("viewContent", cache.text);
            bundle.putInt("catchType", 1);
            return bundle;
        }
        if (!TextUtils.isEmpty(cache.src)) {
            Bundle bundle = new Bundle();
            bundle.putString("imgUrl", cache.src);
            bundle.putInt("catchType", 2);
            return bundle;
        }
        return null;
    }

    private static final class WebCache {
        String text;
        String src;
        float cssX;
        float cssY;
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
            if (!isPlausibleContentHost(view)) {
                return null;
            }
            Bundle result = extractContentColorOS(view, flags);
            if (result != null && isBundleMeaningful(result)) {
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
                        bundle.putParcelable("viewBitmap", scaleDown(preview, 360));
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
            if (directPfd != null && uri == null && file == null) {
                long directSize = -1;
                try {
                    directSize = directPfd.getStatSize();
                } catch (Throwable ignored) {
                }
                if (directSize <= 0) {
                    // A stale/closed PFD from a custom image loader must not
                    // shadow the Bitmap fallback.
                    return null;
                }
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
            long statSize = -1;
            try {
                statSize = pfd.getStatSize();
            } catch (Throwable ignored) {
            }
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
            String scheme = ((Uri) value).getScheme();
            return "content".equals(scheme) || "file".equals(scheme);
        }
        if (value instanceof File) {
            return true;
        }
        if (value instanceof ParcelFileDescriptor) {
            try {
                return ((ParcelFileDescriptor) value).getFileDescriptor() != null;
            } catch (Throwable ignored) {
                return false;
            }
        }
        if (value instanceof String) {
            String string = (String) value;
            return string.length() < 4096
                    && (string.startsWith("content://") || string.startsWith("file://")
                    || (string.startsWith("/") && string.indexOf('\n') < 0));
        }
        return false;
    }

    private static ParcelFileDescriptor openPfd(Context context, Uri uri, File file) {
        try {
            if (file != null) {
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            }
            if (uri == null || context == null) {
                return null;
            }
            if ("file".equals(uri.getScheme())) {
                return ParcelFileDescriptor.open(new File(uri.getPath()),
                        ParcelFileDescriptor.MODE_READ_ONLY);
            }
            return context.getContentResolver().openFileDescriptor(uri, "r");
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
        String text = bundle.getString("viewContent");
        if (!TextUtils.isEmpty(text)) {
            return isMeaningfulUserText(text);
        }
        if (bundle.getParcelable("viewBitmap") != null) {
            return true;
        }
        if (bundle.getParcelable("filePfd") != null) {
            return true;
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
        if (!name.contains("ChatMessageCell") && !name.contains("BackupImageView")) {
            return null;
        }
        try {
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
                    b.putParcelable("viewBitmap", scaleDown(bmp, 1080));
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
            Bundle compose = extractCompose(target, rawX, rawY, wantText, wantBitmap);
            if (compose != null && isBundleMeaningful(compose)) {
                return compose;
            }
        }
        if (isWebViewLike(target)) {
            Bundle web = webViewBundle(target);
            if (web != null && isBundleMeaningful(web)) {
                return web;
            }
        }

        // 4. Hierarchy search starting from target: custom text views,
        // TextViews and ImageViews.
        View candidate = target;
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

    private static boolean isMeaningfulUserText(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        String s = text.toString().trim();
        if (s.isEmpty()) {
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
                || lower.equals("语音消息") || lower.equals("已发送") || lower.equals("发送中")) {
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
        if (view == null || view instanceof TextView || !isTextLikeView(view)) {
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
                return scaleDown(toSoftwareBitmap(tg), 1080);
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
                    return scaleDown(toSoftwareBitmap(source), 1080);
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
            if (bitmap.getConfig() == Bitmap.Config.HARDWARE) {
                return bitmap.copy(Bitmap.Config.ARGB_8888, false);
            }
        } catch (Throwable ignored) {
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
        if (name.contains("AndroidComposeView") || findMethod(view.getClass(), "getSemanticsOwner") != null) {
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
            if (localX < 0 || localX > viewWidth || localY < 0 || localY > viewHeight) {
                return null;
            }

            Object owner = getComposeSemanticsOwner(actualComposeView);
            if (owner != null) {
                Object rootNode = getComposeUnmergedRoot(owner);
                if (rootNode != null) {
                    List<ComposeSemanticsMatch> candidates = new ArrayList<>();
                    collectComposeCandidates(rootNode, 0, localX, localY, rawX, rawY, viewWidth, viewHeight, candidates);
                    Log.i(TAG, "compose candidates count=" + candidates.size());

                    ComposeSemanticsMatch best = pickBestComposeMatch(candidates, viewWidth, viewHeight);
                    if (best != null) {
                        if (wantText && !TextUtils.isEmpty(best.text) && isMeaningfulUserText(best.text)) {
                            Bundle b = new Bundle();
                            b.putString("viewContent", best.text);
                            b.putInt("catchType", 1);
                            Log.i(TAG, "compose hit text=" + best.text.length() + " depth=" + best.depth);
                            return b;
                        }
                        if (wantBitmap && best.isImageLike && best.bounds != null) {
                            Rect b = best.bounds;
                            int x = Math.max(0, b.left);
                            int y = Math.max(0, b.top);
                            int w = Math.min(viewWidth - x, b.width());
                            int h = Math.min(viewHeight - y, b.height());
                            if (w >= 32 && h >= 32) {
                                try {
                                    Bitmap full = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
                                    actualComposeView.draw(new Canvas(full));
                                    Bitmap crop = Bitmap.createBitmap(full, x, y, w, h);
                                    full.recycle();
                                    Bitmap scaled = scaleDown(crop, 1080);
                                    Bundle res = new Bundle();
                                    res.putParcelable("viewBitmap", scaled);
                                    res.putInt("catchType", 2);
                                    Log.i(TAG, "compose hit image " + w + "x" + h + " depth=" + best.depth);
                                    return res;
                                } catch (Throwable drawErr) {
                                    Log.w(TAG, "compose draw bitmap failed: " + drawErr);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "extractCompose failed: " + throwable);
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

    private static Object getComposeSemanticsOwner(View view) {
        View target = resolveAndroidComposeView(view);
        if (target == null) {
            target = view;
        }
        try {
            Method m = findMethod(target.getClass(), "getSemanticsOwner");
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(view);
            }
        } catch (Throwable ignored) {
        }
        try {
            Field f = findFieldOrNull(target.getClass(), "semanticsOwner");
            if (f != null) {
                f.setAccessible(true);
                return f.get(target);
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().contains("SemanticsOwner")) {
                    m.setAccessible(true);
                    return m.invoke(target);
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
        try {
            for (Method m : owner.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().contains("SemanticsNode")) {
                    m.setAccessible(true);
                    return m.invoke(owner);
                }
            }
        } catch (Throwable ignored) {
        }
        // ProGuard fallback (e.g. so5.a() -> po5):
        try {
            for (Method m : owner.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()
                        && m.getReturnType() != void.class && m.getReturnType() != String.class) {
                    m.setAccessible(true);
                    Object res = m.invoke(owner);
                    if (res != null) {
                        return res;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Rect getComposeNodeBounds(Object node) {
        if (node == null) {
            return null;
        }
        Object boundsObj = null;
        try {
            Method m = findMethod(node.getClass(), "getBoundsInRoot");
            if (m != null) {
                m.setAccessible(true);
                boundsObj = m.invoke(node);
            }
        } catch (Throwable ignored) {
        }
        if (boundsObj == null) {
            try {
                for (Method m : node.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getName().toLowerCase(Locale.US).contains("boundsinroot")) {
                        m.setAccessible(true);
                        boundsObj = m.invoke(node);
                        if (boundsObj != null) break;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        // ProGuard fallback: prefer 'h' (boundsInRoot in Compose po5)
        if (boundsObj == null) {
            try {
                for (Method m : node.getClass().getDeclaredMethods()) {
                    if ("h".equals(m.getName()) && m.getParameterCount() == 0) {
                        m.setAccessible(true);
                        Object res = m.invoke(node);
                        if (res != null && hasFourBoxFields(res)) {
                            boundsObj = res;
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (boundsObj == null) {
            try {
                for (Method m : node.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()
                            && m.getReturnType() != void.class && m.getReturnType() != List.class
                            && m.getReturnType() != boolean.class) {
                        m.setAccessible(true);
                        Object res = m.invoke(node);
                        if (res != null && hasFourBoxFields(res)) {
                            boundsObj = res;
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (boundsObj == null) {
            return null;
        }
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

    private static boolean boundsContainTouch(Rect bounds, float localX, float localY, float rawX, float rawY) {
        if (bounds == null) return false;
        // Check local coordinates (if bounds is boundsInRoot)
        if (bounds.contains((int) localX, (int) localY)) {
            return true;
        }
        // Check window coordinates (if bounds is boundsInWindow)
        if (bounds.contains((int) rawX, (int) rawY)) {
            return true;
        }
        // Finger touch slop (36px margin for natural finger placement)
        Rect slop = new Rect(bounds.left - 36, bounds.top - 36, bounds.right + 36, bounds.bottom + 36);
        return slop.contains((int) localX, (int) localY) || slop.contains((int) rawX, (int) rawY);
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
                if (f.getType().getSimpleName().contains("SemanticsConfiguration")) {
                    f.setAccessible(true);
                    return f.get(node);
                }
            }
        } catch (Throwable ignored) {
        }
        // ProGuard fallback (e.g. po5.k() -> jo5)
        try {
            for (Method m : node.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()
                        && m.getReturnType() != void.class && m.getReturnType() != List.class
                        && m.getReturnType() != String.class && m.getReturnType() != boolean.class) {
                    m.setAccessible(true);
                    Object res = m.invoke(node);
                    if (res != null && hasMapOrEntries(res)) {
                        return res;
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
            if (m != null) {
                m.setAccessible(true);
                Object res = m.invoke(node);
                if (res instanceof List) {
                    return (List<Object>) res;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : node.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().startsWith("getChildren")) {
                    m.setAccessible(true);
                    Object res = m.invoke(node);
                    if (res instanceof List) {
                        return (List<Object>) res;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // ProGuard fallback (e.g. po5.i(false, false) -> List or any method returning List)
        try {
            for (Method m : node.getClass().getDeclaredMethods()) {
                if (List.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    Object[] args = new Object[m.getParameterCount()];
                    for (int i = 0; i < args.length; i++) {
                        Class<?> pt = m.getParameterTypes()[i];
                        if (pt == boolean.class) args[i] = Boolean.FALSE;
                        else if (pt == int.class) args[i] = 0;
                        else args[i] = null;
                    }
                    Object res = m.invoke(node, args);
                    if (res instanceof List) {
                        return (List<Object>) res;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : node.getClass().getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object res = f.get(node);
                    if (res instanceof List) {
                        return (List<Object>) res;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
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
        Object config = getComposeConfig(node);
        if (config != null) {
            ComposeSemanticsMatch match = buildComposeMatch(node, config, bounds, depth, viewWidth, viewHeight);
            if (match != null && !match.isLink) {
                candidates.add(match);
            }
        }
        List<Object> children = getComposeChildren(node);
        if (children != null) {
            for (Object child : children) {
                collectComposeCandidates(child, depth + 1, localX, localY, rawX, rawY, viewWidth, viewHeight, candidates);
            }
        }
    }

    private static boolean isSemanticsTextKey(String keyName) {
        if (keyName == null) return false;
        if (keyName.contains("Action") || keyName.contains("Result") || keyName.startsWith("Set")) {
            return false;
        }
        return keyName.equals("Text") || keyName.equals("EditableText")
                || keyName.endsWith(".Text") || keyName.endsWith(".EditableText");
    }

    private static ComposeSemanticsMatch buildComposeMatch(Object node, Object config, Rect bounds, int depth,
                                                           int viewWidth, int viewHeight) {
        String foundText = null;
        boolean isImage = false;
        boolean isLink = false;

        if (config instanceof Iterable) {
            for (Object item : (Iterable<?>) config) {
                if (item instanceof Map.Entry) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) item;
                    String keyName = getSemanticsKeyName(entry.getKey());
                    Object value = entry.getValue();
                    if (keyName != null) {
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
                        } else if (keyName.contains("TestTag") || keyName.contains("Link")) {
                            isLink = true;
                        }
                    }
                }
            }
        }

        // Direct ScatterMap inspection (c14: b=keys, c=values) if foundText is still empty
        if (TextUtils.isEmpty(foundText) && config != null) {
            for (Field f : config.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object sub = f.get(config);
                    if (sub != null) {
                        Field keysField = null;
                        Field valsField = null;
                        for (Field sf : sub.getClass().getDeclaredFields()) {
                            if (sf.getType() == Object[].class) {
                                if (keysField == null) keysField = sf;
                                else if (valsField == null) { valsField = sf; break; }
                            }
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
                                    if (keyName != null) {
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
                                            if (v != null && v.toString().contains("Image")) {
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
                    if (keyName != null) {
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

        // Note: Do NOT guess isImage from arbitrary container aspect ratios.
        // Doing so misclassifies text container layouts as images and crashes with hardware bitmap draw errors.

        return new ComposeSemanticsMatch(bounds, depth, foundText, isImage, isLink, node);
    }

    private static String getSemanticsKeyName(Object key) {
        if (key == null) return null;
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
        if (value.getClass().getName().contains("AccessibilityAction") || value.toString().startsWith("AccessibilityAction(")) return null;
        if (value instanceof List) {
            String longest = null;
            for (Object item : (List<?>) value) {
                String s = parseComposeTextValue(item);
                longest = pickLongerText(longest, s);
            }
            return longest;
        }
        if (value instanceof CharSequence) {
            return value.toString().trim();
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
                if (CharSequence.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(value);
                    if (val instanceof CharSequence && !TextUtils.isEmpty((CharSequence) val)) {
                        return val.toString().trim();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return value.toString().trim();
    }

    private static String pickLongerText(String a, String b) {
        if (TextUtils.isEmpty(a)) return b;
        if (TextUtils.isEmpty(b)) return a;
        return a.length() >= b.length() ? a : b;
    }

    private static ComposeSemanticsMatch pickBestComposeMatch(List<ComposeSemanticsMatch> candidates,
                                                             int viewWidth, int viewHeight) {
        if (candidates == null || candidates.isEmpty()) return null;
        // Priority 1: Pick candidates with real meaningful text (prefer deepest / most specific)
        ComposeSemanticsMatch bestText = null;
        for (ComposeSemanticsMatch m : candidates) {
            if (m.hasText && isMeaningfulUserText(m.text)) {
                if (bestText == null || m.depth > bestText.depth
                        || (m.depth == bestText.depth && m.area < bestText.area)) {
                    bestText = m;
                }
            }
        }
        if (bestText != null) {
            return bestText;
        }
        // Priority 2: Image-like candidates
        for (int i = candidates.size() - 1; i >= 0; i--) {
            ComposeSemanticsMatch m = candidates.get(i);
            if (m.isImageLike) {
                return m;
            }
        }
        return null;
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
        final int area;
        final Object node;

        ComposeSemanticsMatch(Rect bounds, int depth, String text, boolean isImageLike, boolean isLink, Object node) {
            this.bounds = bounds;
            this.depth = depth;
            this.text = text;
            this.hasText = !TextUtils.isEmpty(text);
            this.isImageLike = isImageLike;
            this.isLink = isLink;
            this.area = bounds != null ? Math.max(0, bounds.width()) * Math.max(0, bounds.height()) : 0;
            this.node = node;
        }
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
        if (source.getConfig() == Bitmap.Config.HARDWARE) {
            Bitmap software = toSoftwareBitmap(source);
            if (software == null || software.getConfig() == Bitmap.Config.HARDWARE) {
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
            if (content == null || sGestureState.get() == STATE_DRAGGING) {
                Log.d(TAG, "deliverContent ignored (custom engine already dragging)");
                return;
            }
            Object resultMap = getField(content, "mResultMap");
            Object catchType = getField(content, "mCatchType");
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
                Object coordinate = map.get("coordinate");
                if (coordinate instanceof String) {
                    payload.putString("coordinate", (String) coordinate);
                }
                // AES has already completed its long-press and extraction
                // pipeline. Show the preview immediately, but do NOT mark the
                // gesture as moved: a stationary long-press must still reach
                // the app (Weibo multi-select etc.). The actual system drag is
                // started later by ping() when the finger really moves.
                sPendingPayload = payload;
                sPendingContent = content;
                sPendingGeneration = sGestureGeneration;
                Log.i(TAG, "long press preview only; drag waits for real move");
                forwardToPortal(payload, content);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "deliverContent failed", throwable);
        }
    }

    private static Bundle pickPayload(Map<?, ?> map) {
        Bundle bitmapPayload = null;
        for (Object value : map.values()) {
            if (!(value instanceof Bundle)) {
                continue;
            }
            Bundle bundle = (Bundle) value;
            String text = bundle.getString("viewContent");
            if (isMeaningfulUserText(text)) {
                return bundle;
            }
            if (bundle.getParcelable("filePfd") instanceof ParcelFileDescriptor) {
                return bundle;
            }
            if (bitmapPayload == null && bundle.getParcelable("viewBitmap") instanceof Bitmap) {
                bitmapPayload = bundle;
            }
        }
        return bitmapPayload;
    }

    /**
     * Stream the original PFD into the portal's write-only staging provider.
     * No size limit, no Binder byte[] and no ParcelFileDescriptor in the
     * startService Intent, so the original file survives untouched.
     */
    private static String copyPfdToPortal(Context context, ParcelFileDescriptor pfd, Bundle payload) {
        if (context == null || pfd == null) {
            return null;
        }
        String name = payload.getString("fileName");
        String mime = payload.getString("mimeType");
        String suffix = ".bin";
        if (mime != null && mime.contains("png")) {
            suffix = ".png";
        } else if (mime != null && mime.contains("webp")) {
            suffix = ".webp";
        } else if (mime != null && (mime.contains("jpeg") || mime.contains("jpg"))) {
            suffix = ".jpg";
        } else if (name != null && name.contains(".")) {
            suffix = name.substring(name.lastIndexOf('.'));
        }
        if (TextUtils.isEmpty(name)) {
            name = "drag_" + SystemClock.uptimeMillis() + suffix;
        }
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        Uri target = Uri.parse("content://com.github.leaf.hyperportal.files/incoming/" + Uri.encode(name));
        try (OutputStream output = context.getContentResolver().openOutputStream(target, "w");
             InputStream input = new FileInputStream(pfd.getFileDescriptor())) {
            if (output == null) {
                return null;
            }
            byte[] buffer = new byte[65536];
            int count;
            long total = 0;
            while ((count = input.read(buffer)) > 0) {
                output.write(buffer, 0, count);
                total += count;
                if (total > 128L * 1024 * 1024) {
                    Log.w(TAG, "copyPfdToPortal too large, aborting");
                    return null;
                }
            }
            output.flush();
            Log.i(TAG, "copyPfdToPortal " + name + " " + total + " bytes");
            return name;
        } catch (Throwable throwable) {
            Log.w(TAG, "copyPfdToPortal failed", throwable);
            return null;
        }
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

    private static void forwardToPortal(Bundle payload, Object content) {
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
        String imageCacheName = null;
        int previewHeight = dp(context, 76);
        int previewWidth;
        if (bitmap instanceof Bitmap) {
            // ColorOS image preview is a 76dp square; text preview keeps the
            // source width until it reaches the bottom hot zone.
            previewWidth = dp(context, 76);
        } else {
            int maxWidth = dp(context, 150);
            int viewWidth = sLastTouchView != null ? sLastTouchView.getWidth() : 0;
            previewWidth = viewWidth > 0
                    ? Math.min(Math.max(viewWidth, dp(context, 76)), maxWidth)
                    : maxWidth;
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            previewWidth = Math.min(previewWidth, screenWidth - dp(context, 32));
        }
        intent.putExtra("previewWidth", previewWidth);
        intent.putExtra("previewHeight", previewHeight);
        if (filePfd instanceof ParcelFileDescriptor) {
            imageCacheName = copyPfdToPortal(context, (ParcelFileDescriptor) filePfd, payload);
        }
        Bitmap dragBitmap = null;
        if (bitmap instanceof Bitmap) {
            dragBitmap = toSoftwareBitmap((Bitmap) bitmap);
        }
        if (dragBitmap != null) {
            // With the original file staged in the portal cache we only need a
            // tiny drag preview. Without a file, use the normal 360px bitmap.
            Bitmap preview = scaleDown(dragBitmap, imageCacheName != null ? 128 : 360);
            if (preview != null) {
                intent.putExtra("viewBitmap", preview);
            }
        }
        if (imageCacheName != null) {
            intent.putExtra("imageCacheName", imageCacheName);
            intent.putExtra("imageFileName", payload.getString("fileName"));
            String mime = payload.getString("mimeType");
            if (TextUtils.isEmpty(mime)) {
                mime = URLConnection.guessContentTypeFromName(imageCacheName);
            }
            intent.putExtra("imageMimeType", TextUtils.isEmpty(mime) ? "image/*" : mime);
        }
        intent.putExtra("catchType", payload.getInt("catchType", 0));
        if (source != null) {
            intent.putExtra("sourcePkg", source);
        }
        Log.i(TAG, "forward -> portal text=" + (text == null ? "null" : text.length() + " chars")
                + " bitmap=" + describeBitmap(payload)
                + " file=" + (filePfd == null ? "null" : payload.getString("fileName"))
                + " cache=" + imageCacheName
                + " source=" + source);
        boolean dragStarted = startSystemDrag(text, dragBitmap);
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
                    broadcast.setPackage(PORTAL_PACKAGE);
                    broadcast.putExtras(intent);
                    context.sendBroadcast(broadcast);
                    started = true;
                    Log.i(TAG, "forward fallback broadcast sent");
                } catch (Throwable third) {
                    Log.w(TAG, "forward fallback broadcast failed", third);
                }
            }
            if (!started) {
                try {
                    Intent activity = new Intent(intent);
                    activity.setClassName(PORTAL_PACKAGE, PORTAL_PACKAGE + ".MainActivity");
                    activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(activity);
                } catch (Throwable fourth) {
                    Log.w(TAG, "forward fallback activity failed", fourth);
                }
            }
        }
        // HyperOS may silently drop a cross-app startForegroundService() even
        // when it does not throw. Mirror the content through the exported
        // receiver with a small payload (preview bitmap / text only) so the
        // portal always gets a visible card.
        try {
            Intent mirror = new Intent("com.github.leaf.hyperportal.SHOW");
            mirror.setPackage(PORTAL_PACKAGE);
            mirror.putExtras(intent);
            // Keep imageBytes in the mirror: if HyperOS dropped the direct
            // startForegroundService, this broadcast is the only path that can
            // still deliver the original image. PFDs cannot cross a broadcast.
            mirror.removeExtra("filePfd");
            mirror.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(mirror);
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
    private static void updateSystemShadow(boolean square) {
        View view = sActiveDragView;
        if (view == null || !view.isAttachedToWindow() || sActiveShadowSquare == square) {
            return;
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                view.updateDragShadow(new PreviewShadow(sActiveDragText,
                        sActiveDragBitmap, sActiveDragDensity, square, sActiveDragSourceWidth));
                sActiveShadowSquare = square;
                Log.i(TAG, "shadow morph square=" + square);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "updateDragShadow failed", throwable);
        }
    }

    private static boolean startSystemDrag(String text, Bitmap bitmap) {
        try {
            View view = sLastTouchView;
            if (view == null || !view.isAttachedToWindow()) {
                Log.i(TAG, "system drag skipped: no attached view");
                return false;
            }
            if (SystemClock.uptimeMillis() - sLastTouchAt > 1500) {
                Log.i(TAG, "system drag skipped: stale long press");
                return false;
            }
            if (TextUtils.isEmpty(text) && bitmap == null) {
                return false;
            }
            if (!sMovedSinceDown) {
                Log.i(TAG, "system drag skipped: stationary long press");
                return false;
            }
            View target = view;
            cancelAppLongPress(target);
            ClipDescription description = new ClipDescription("content",
                    new String[]{"text/plain", "application/x-dragshare"});
            ClipData clip = new ClipData(description,
                    new ClipData.Item(TextUtils.isEmpty(text) ? "image" : text));
            DisplayMetrics metrics = view.getResources().getDisplayMetrics();
            View.DragShadowBuilder shadow = new PreviewShadow(text, bitmap, metrics.density,
                    false, target.getWidth());
            try {
                View root = target.getRootView();
                root.setOnDragListener((v, event) -> {
                    if (event != null && event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                        int screenHeight = v.getResources().getDisplayMetrics().heightPixels;
                        updateSystemShadow(event.getY() >= screenHeight * 0.82f);
                    }
                    return true;
                });
            } catch (Throwable ignored) {
            }
            // Task zoom / dim / bottom app bar are handled by the LSPosed
            // system_server module. The app-process Bridge must not scale the
            // DecorView; it only starts the system drag.
            boolean started = target.startDragAndDrop(clip, shadow, null,
                    View.DRAG_FLAG_GLOBAL | View.DRAG_FLAG_GLOBAL_URI_READ | View.DRAG_FLAG_GLOBAL_URI_WRITE);
            if (started) {
                sActiveDragView = target;
                sActiveDragText = text;
                sActiveDragBitmap = bitmap;
                sActiveDragDensity = metrics.density;
                sActiveShadowSquare = false;
            }
            Log.i(TAG, "system drag started=" + started + " target=" + target.getClass().getName()
                    + " text=" + (text == null ? "null" : text.length()));
            return started;
        } catch (Throwable throwable) {
            Log.w(TAG, "startSystemDrag failed", throwable);
            return false;
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
            if (root instanceof WebView) {
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
            if (view instanceof WebView) {
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

    private static View findDeepTouchView(View root, float rawX, float rawY) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return null;
        }
        if (isComposeView(root) || isWebViewLike(root)) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                int[] location = new int[2];
                child.getLocationOnScreen(location);
                if (rawX >= location[0] && rawX <= location[0] + child.getWidth()
                        && rawY >= location[1] && rawY <= location[1] + child.getHeight()) {
                    View deep = findDeepTouchView(child, rawX, rawY);
                    if (deep != null) {
                        return deep;
                    }
                }
            }
        }
        return root;
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
            int screenHeight = decor.getResources().getDisplayMetrics().heightPixels;
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
        int screenHeight = decor.getResources().getDisplayMetrics().heightPixels;
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
                    updateTiltFromDrag(decor, decorLocation[1] + event.getY());
                    return true;
                }
                case DragEvent.ACTION_DROP: {
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
        private final boolean square;
        private final int width;
        private final int height;

        PreviewShadow(String text, Bitmap bitmap, float density, boolean square, int sourceWidth) {
            this.text = text;
            this.bitmap = bitmap;
            this.density = density;
            this.square = square;
            // ColorOS DragViewHelper: text keeps the source view width and
            // 76dp height; images keep their decoded bitmap dimensions. At
            // the bottom hot zone both collapse to 76x76.
            if (square) {
                this.width = dp(76);
                this.height = dp(76);
            } else if (bitmap != null) {
                this.width = bitmap.getWidth();
                this.height = bitmap.getHeight();
            } else {
                this.width = sourceWidth > 0 ? sourceWidth : dp(260);
                this.height = dp(76);
            }
        }

        private int dp(float value) {
            return Math.round(value * density);
        }

        @Override
        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            outShadowSize.set(width, height);
            outShadowTouchPoint.set(width / 2, height / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            if (bitmap != null) {
                float radius = dp(8);
                Path clip = new Path();
                clip.addRoundRect(new RectF(0, 0, width, height), radius, radius,
                        Path.Direction.CW);
                canvas.save();
                canvas.clipPath(clip);
                float scale = Math.max(width / (float) bitmap.getWidth(),
                        height / (float) bitmap.getHeight());
                int srcW = Math.min(bitmap.getWidth(), Math.round(width / scale));
                int srcH = Math.min(bitmap.getHeight(), Math.round(height / scale));
                int srcX = (bitmap.getWidth() - srcW) / 2;
                int srcY = (bitmap.getHeight() - srcH) / 2;
                canvas.drawBitmap(bitmap,
                        new Rect(srcX, srcY, srcX + srcW, srcY + srcH),
                        new Rect(0, 0, width, height),
                        new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
                canvas.restore();
                return;
            }

            Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
            background.setColor(Color.WHITE);
            float radius = dp(10);
            canvas.drawRoundRect(0, 0, width, height, radius, radius, background);

            if (TextUtils.isEmpty(text)) {
                return;
            }
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(dp(11));
            String content = text.replace('\n', ' ');
            if (content.length() > 40) {
                content = content.substring(0, 40);
            }
            int pad = dp(6);
            List<String> lines = wrapText(content, textPaint, width - 2 * pad, 4);
            float lineHeight = dp(14);
            float y = (height - lines.size() * lineHeight) / 2f + textPaint.getTextSize();
            for (String line : lines) {
                canvas.drawText(line, pad, y, textPaint);
                y += lineHeight;
            }
        }
    }

    private static List<String> wrapText(String text, Paint paint, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        int start = 0;
        while (start < text.length() && lines.size() < maxLines) {
            int count = paint.breakText(text, start, text.length(), true, maxWidth, null);
            if (count <= 0) {
                break;
            }
            int end = start + count;
            int space = text.lastIndexOf(' ', end);
            if (space > start) {
                end = space;
            }
            String line = text.substring(start, end).trim();
            if (lines.size() == maxLines - 1 && end < text.length()) {
                line = line + "…";
            }
            lines.add(line);
            start = end;
            while (start < text.length() && text.charAt(start) == ' ') {
                start++;
            }
        }
        return lines;
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














