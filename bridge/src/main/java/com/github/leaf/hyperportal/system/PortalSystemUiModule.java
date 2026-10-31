package com.github.leaf.hyperportal.system;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
    private static final int TYPE_NAVIGATION_BAR_PANEL = 2024;

    private static volatile XposedInterface sXposed;
    private static volatile Context sSysUiContext;
    private static volatile SystemUiOverlay sOverlay;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    public static void initSystemUi(XposedInterface xposed, XposedModuleInterface.PackageLoadedParam param) {
        sXposed = xposed;
        try {
            ClassLoader cl = param.getDefaultClassLoader();
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

    private static void initOverlay(Context context) {
        if (context == null || sOverlay != null) {
            return;
        }
        try {
            sOverlay = new SystemUiOverlay(context);
            sOverlay.registerReceivers();
            Log.i(TAG, "SystemUiOverlay attached and listening for drag events!");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to instantiate SystemUiOverlay", t);
        }
    }

    private static final class SystemUiOverlay {
        private final Context mContext;
        private final WindowManager mWindowManager;
        private FrameLayout mContainer;
        private WindowManager.LayoutParams mParams;
        private GlowView mGlowView;
        private TextView mPromptText;
        private HorizontalScrollView mAppRow;
        private final List<AppTarget> mTargets = new ArrayList<>();
        private boolean mShowing = false;

        // Current dragged content payload
        private String mText;
        private Bitmap mBitmap;
        private Uri mImageUri;

        public SystemUiOverlay(Context context) {
            this.mContext = context;
            this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            buildTargets();
            buildViewHierarchy();
        }

        private int dp(float value) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                    mContext.getResources().getDisplayMetrics());
        }

        private void buildTargets() {
            mTargets.clear();
            PackageManager pm = mContext.getPackageManager();

            // Default quick target: Clipboard
            mTargets.add(new AppTarget("复制文本", null, (ctx, text, bmp, uri) -> {
                if (!TextUtils.isEmpty(text)) {
                    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("HyperPortal", text));
                    toast("已复制到剪贴板");
                }
            }));

            // Common apps
            String[] candidatePkgs = {
                    "com.tencent.mm",              // WeChat
                    "com.tencent.mobileqq",         // QQ
                    "com.sina.weibo",               // Weibo
                    "com.coolapk.market",           // Coolapk
                    "org.telegram.messenger",       // Telegram
                    "com.miui.gallery",             // Xiaomi Album
                    "com.miui.notes",               // Xiaomi Notes
                    "com.android.fileexplorer"      // Xiaomi File Manager
            };

            for (String pkg : candidatePkgs) {
                try {
                    Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) {
                        CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
                        Drawable icon = pm.getApplicationIcon(pkg);
                        mTargets.add(new AppTarget(label.toString(), icon, (ctx, text, bmp, uri) -> {
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setPackage(pkg);
                            share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (uri != null) {
                                share.setType("image/*");
                                share.putExtra(Intent.EXTRA_STREAM, uri);
                                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } else if (!TextUtils.isEmpty(text)) {
                                share.setType("text/plain");
                                share.putExtra(Intent.EXTRA_TEXT, text);
                            }
                            try {
                                ctx.startActivity(share);
                                toast("已发送至 " + label);
                            } catch (Throwable t) {
                                ctx.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            }
                        }));
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        private void buildViewHierarchy() {
            mContainer = new FrameLayout(mContext);
            mContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);

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

            // 2. Prompt Text ("拖拽至此处触发内容传送门")
            TextView prompt = new TextView(mContext);
            prompt.setText("拖拽至此处触发内容传送门");
            prompt.setTextColor(Color.parseColor("#CCFFFFFF"));
            prompt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            prompt.setGravity(Gravity.CENTER);
            prompt.setShadowLayer(dp(4), 0, dp(1), Color.parseColor("#80000000"));
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setColor(Color.parseColor("#26000000"));
            pillBg.setCornerRadius(dp(16));
            prompt.setBackground(pillBg);
            prompt.setPadding(dp(16), dp(8), dp(16), dp(8));
            FrameLayout.LayoutParams promptParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            promptParams.bottomMargin = navBarHeight + dp(24);
            mContainer.addView(prompt, promptParams);
            mPromptText = prompt;

            // 3. Application Bar (initially hidden in Phase 1)
            HorizontalScrollView scroll = buildAppRow();
            scroll.setVisibility(View.GONE);
            scroll.setTranslationY(dp(60));
            scroll.setAlpha(0f);
            FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            scrollParams.bottomMargin = navBarHeight + dp(20);
            scrollParams.leftMargin = dp(16);
            scrollParams.rightMargin = dp(16);
            mContainer.addView(scroll, scrollParams);
            mAppRow = scroll;

            // 4. Drag and Drop Listener on Container
            mContainer.setOnDragListener((v, event) -> {
                int action = event.getAction();
                if (action == DragEvent.ACTION_DRAG_STARTED) {
                    return isMasterEnabled();
                } else if (action == DragEvent.ACTION_DROP) {
                    // Check if dropped onto one of our AppTargets
                    float dropX = event.getX();
                    float dropY = event.getY();
                    AppTarget hit = findHitTarget(dropX, dropY);
                    if (hit != null) {
                        try {
                            hit.action.onDrop(mContext, mText, mBitmap, mImageUri);
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
                    handleDragStateChanged(0, 0f);
                    return true;
                }
                return true;
            });

            // Layout Params for System Window
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    dp(380),
                    TYPE_NAVIGATION_BAR_PANEL, // SystemUI native window
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT);
            mParams.gravity = Gravity.BOTTOM;
        }

        private HorizontalScrollView buildAppRow() {
            HorizontalScrollView scroll = new HorizontalScrollView(mContext);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

            LinearLayout row = new LinearLayout(mContext);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Color.parseColor("#E6202024"));
            cardBg.setCornerRadius(dp(28));
            cardBg.setStroke(dp(1), Color.parseColor("#26FFFFFF"));
            row.setBackground(cardBg);

            for (AppTarget target : mTargets) {
                LinearLayout item = new LinearLayout(mContext);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(dp(12), dp(8), dp(12), dp(8));
                item.setTag(target);

                ImageView iconView = new ImageView(mContext);
                if (target.icon != null) {
                    iconView.setImageDrawable(target.icon);
                } else {
                    GradientDrawable iconBg = new GradientDrawable();
                    iconBg.setColor(Color.parseColor("#33FFFFFF"));
                    iconBg.setCornerRadius(dp(16));
                    iconView.setBackground(iconBg);
                }
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
                item.addView(iconView, iconParams);

                TextView nameView = new TextView(mContext);
                nameView.setText(target.name);
                nameView.setTextColor(Color.WHITE);
                nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                nameView.setGravity(Gravity.CENTER);
                nameView.setSingleLine(true);
                nameView.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                textParams.topMargin = dp(4);
                item.addView(nameView, textParams);

                row.addView(item);
            }
            scroll.addView(row);
            return scroll;
        }

        private AppTarget findHitTarget(float dropX, float dropY) {
            if (mAppRow == null || mAppRow.getVisibility() != View.VISIBLE) {
                return null;
            }
            int[] rowLoc = new int[2];
            mAppRow.getLocationOnScreen(rowLoc);
            int[] containerLoc = new int[2];
            mContainer.getLocationOnScreen(containerLoc);

            float rowX = dropX - (rowLoc[0] - containerLoc[0]);
            float rowY = dropY - (rowLoc[1] - containerLoc[1]);

            if (rowX < 0 || rowX > mAppRow.getWidth() || rowY < 0 || rowY > mAppRow.getHeight()) {
                return null;
            }

            ViewGroup row = (ViewGroup) mAppRow.getChildAt(0);
            if (row == null) {
                return null;
            }

            float scrollX = mAppRow.getScrollX();
            float contentX = rowX + scrollX;

            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (contentX >= child.getLeft() && contentX <= child.getRight()
                        && rowY >= child.getTop() && rowY <= child.getBottom()) {
                    Object tag = child.getTag();
                    if (tag instanceof AppTarget) {
                        return (AppTarget) tag;
                    }
                }
            }
            return null;
        }

        private void toast(String msg) {
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }

        public void registerReceivers() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_DRAG_STATE);
            filter.addAction(ACTION_SHOW);
            if (Build.VERSION.SDK_INT >= 33) {
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        handleIntent(intent);
                    }
                }, filter, Context.RECEIVER_EXPORTED);
            } else {
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        handleIntent(intent);
                    }
                }, filter);
            }
        }

        private boolean isMasterEnabled() {
            try {
                Uri uri = Uri.parse("content://com.github.leaf.hyperportal.files");
                Bundle res = mContext.getContentResolver().call(uri, "getConfig", null, null);
                if (res != null) {
                    return res.getBoolean("master_switch", true);
                }
            } catch (Throwable ignored) {
            }
            return true;
        }

        private void handleIntent(Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (ACTION_SHOW.equals(action)) {
                mText = intent.getStringExtra("viewContent");
                Object bmp = intent.getParcelableExtra("viewBitmap");
                if (bmp instanceof Bitmap) {
                    mBitmap = (Bitmap) bmp;
                }
                String uriStr = intent.getStringExtra("imageUri");
                if (uriStr != null) {
                    mImageUri = Uri.parse(uriStr);
                }
            } else if (ACTION_DRAG_STATE.equals(action)) {
                int state = intent.getIntExtra("state", 0);
                float y = intent.getFloatExtra("y", 0f);
                if (state > 0 && !isMasterEnabled()) {
                    handleDragStateChanged(0, 0f);
                    return;
                }
                handleDragStateChanged(state, y);
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
            if (Looper.myLooper() != Looper.getMainLooper()) {
                sHandler.post(() -> handleDragStateChanged(state, y));
                return;
            }

            if (state == 0) {
                // Drag ended / cancelled
                if (mGlowView != null) {
                    mGlowView.setVisibility(View.GONE);
                }
                if (mPromptText != null) {
                    mPromptText.setVisibility(View.GONE);
                }
                if (mAppRow != null) {
                    mAppRow.setVisibility(View.GONE);
                }
                hideOverlay();
                mText = null;
                mBitmap = null;
                mImageUri = null;
                return;
            }

            if (state == 1) {
                // Phase 1: Glow view active, prompt text shown
                showOverlay();
                setTouchable(false);
                int screenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
                float progress = Math.max(0f, Math.min(1.0f, (y - screenHeight * 0.15f) / (screenHeight * 0.67f)));
                if (mGlowView != null) {
                    mGlowView.setPhase1Progress(progress);
                    mGlowView.setVisibility(View.VISIBLE);
                }
                if (mPromptText != null) {
                    mPromptText.setVisibility(View.VISIBLE);
                    mPromptText.setAlpha(1.0f);
                }
                if (mAppRow != null && mAppRow.getVisibility() == View.VISIBLE) {
                    mAppRow.animate()
                            .translationY(dp(60))
                            .alpha(0f)
                            .setDuration(160)
                            .withEndAction(() -> {
                                if (mAppRow != null) mAppRow.setVisibility(View.GONE);
                            })
                            .start();
                }
            } else if (state == 2) {
                // Phase 2: Crossed 76% threshold -> Expand shelf & enable touch
                showOverlay();
                setTouchable(true);
                if (mGlowView != null) {
                    mGlowView.setPhase2();
                    mGlowView.setVisibility(View.VISIBLE);
                }
                if (mPromptText != null && mPromptText.getVisibility() == View.VISIBLE) {
                    mPromptText.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction(() -> {
                                if (mPromptText != null) mPromptText.setVisibility(View.GONE);
                            })
                            .start();
                }
                if (mAppRow != null) {
                    if (mAppRow.getVisibility() != View.VISIBLE) {
                        mAppRow.setVisibility(View.VISIBLE);
                        mAppRow.setTranslationY(dp(60));
                        mAppRow.setAlpha(0f);
                        try {
                            mAppRow.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                        } catch (Throwable ignored) {
                        }
                    }
                    mAppRow.animate()
                            .translationY(0f)
                            .alpha(1.0f)
                            .setDuration(220)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        }
    }

    private static final class AppTarget {
        final String name;
        final Drawable icon;
        final DropAction action;

        AppTarget(String name, Drawable icon, DropAction action) {
            this.name = name;
            this.icon = icon;
            this.action = action;
        }
    }

    private interface DropAction {
        void onDrop(Context context, String text, Bitmap bitmap, Uri imageUri) throws Throwable;
    }

    /**
     * ColorOS Aurora Bottom Glow View.
     */
    private static final class GlowView extends View {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RuntimeShader mShader;
        private Bitmap mNoiseBmp;

        private float mTargetCenterY = -4.134f;
        private float mCurrentCenterY = -4.134f;
        private float mTargetStart = 4.0f;
        private float mCurrentStart = 4.0f;
        private float mTargetSpace = 0.18f;
        private float mCurrentSpace = 0.18f;
        private float mTargetAlpha = 0.0f;
        private float mCurrentAlpha = 0.0f;

        public GlowView(Context context) {
            super(context);
            initShader(context);
        }

        private void initShader(Context context) {
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    // Load oppo_glow.png directly from module APK
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
                                + "    return float2(p.x * c - p.y * s, p.x * s + p.y * c);\n"
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
                        BitmapShader bmpShader = new BitmapShader(mNoiseBmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
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

        public void setPhase1Progress(float progress) {
            float p = Math.max(0f, Math.min(1.0f, progress));
            mTargetCenterY = -2.5f + 1.34f * p;
            mTargetStart = 2.5f - 1.5f * p;
            mTargetSpace = 0.30f + 0.46f * p;
            mTargetAlpha = 0.50f + 0.45f * p;
            invalidate();
        }

        public void setPhase2() {
            mTargetCenterY = -1.16f;
            mTargetStart = 1.0f;
            mTargetSpace = 0.76f;
            mTargetAlpha = 0.95f;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            mCurrentCenterY += (mTargetCenterY - mCurrentCenterY) * 0.18f;
            mCurrentStart += (mTargetStart - mCurrentStart) * 0.18f;
            mCurrentSpace += (mTargetSpace - mCurrentSpace) * 0.18f;
            mCurrentAlpha += (mTargetAlpha - mCurrentAlpha) * 0.18f;

            if (mShader != null) {
                float time = (SystemClock.uptimeMillis() % 60000) / 1000.0f;
                mShader.setFloatUniform("u_time", time);
                mShader.setFloatUniform("u_resolution", (float) w, (float) h);
                mShader.setFloatUniform("_HaloCenter", 0.5f, mCurrentCenterY);
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
