package com.github.leaf.hyperportal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ColorOS-Content-Portal floating panel & bottom drag target bar.
 *
 * Direct drag (dragActive=true):
 * - Displays the ColorOS aurora glow at the bottom edge.
 * - Upper screen drag: only glow effect, intensity ramps with distance.
 * - Bottom threshold reached: application bar expands and task tilts in 3D.
 * - Drop on target app: direct share.
 *
 * Tap / Stationary long-press (dragActive=false):
 * - Shows the floating action menu (转发, 收藏, 引用, 翻译, etc.).
 */
public class PortalService extends Service {

    private static final String TAG = "DragSharePortal";
    private static final String CHANNEL = "dragshare_portal";
    private static final int NOTIFICATION_ID = 0x4453;
    public static final String ACTION_DRAG_STATE = "com.github.leaf.hyperportal.DRAG_STATE_CHANGED";

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private static volatile PortalService sInstance;

    public static PortalService getInstance() {
        return sInstance;
    }

    private WindowManager mWindowManager;

    // Bottom container: holds GlowView and mAppRow
    private FrameLayout mBottomContainer;
    private GlowView mGlowView;
    private HorizontalScrollView mAppRow;
    private TextView mPromptText;
    private WindowManager.LayoutParams mBottomParams;


    private String mText;
    private Bitmap mBitmap;
    private String mSource;
    private int mCatchType;
    private float mTouchX;
    private float mTouchY;
    private boolean mDragActive;

    // ColorOS-style drag preview: white rounded card, 76dp high, text reflows
    // from its original width into a 76x76 square at the bottom hot zone.
    private FrameLayout mDragPreview;
    private TextView mDragPreviewText;
    private ImageView mDragPreviewImage;
    private WindowManager.LayoutParams mDragPreviewParams;
    private int mDragPreviewWidth;
    private int mDragPreviewHeight;
    private boolean mDragPreviewBottom;
    private boolean mDragPreviewRaised;
    private int mCurrentDragState = 0;
    private int mLastNotifiedShadowState = -1;

    private ParcelFileDescriptor mFilePfd;
    private String mFileName;
    private String mMimeType;
    private long mFileSize = -1;
    private volatile File mLocalFile;
    private volatile String mImageUrl;

    private final List<AppTarget> mTargets = new ArrayList<>();

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            int state = intent.getIntExtra("state", 0);
            float y = intent.getFloatExtra("y", 0f);
            handleDragStateChanged(state, y);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createChannel();
        // Must be called for both startService() and startForegroundService():
        // the system drag-state path still uses startForegroundService, and
        // Android kills the process if startForeground() is never called.
        // For the content path this may throw ForegroundServiceStartNotAllowed,
        // which ensureForeground() catches and ignores.
        ensureForeground();
        buildTargets();

        IntentFilter filter = new IntentFilter(ACTION_DRAG_STATE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(mStateReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mStateReceiver, filter);
        }
    }

    private boolean isContentStart(Intent intent) {
        if (intent == null) {
            return false;
        }
        return intent.hasExtra("viewContent")
                || intent.hasExtra("viewBitmap")
                || intent.hasExtra("imageCacheName")
                || intent.hasExtra("imageBytes")
                || intent.hasExtra("filePfd");
    }

    private void ensureForeground() {
        try {
            startForeground(NOTIFICATION_ID, buildNotification());
        } catch (Throwable throwable) {
            Log.w(TAG, "startForeground unavailable: " + throwable);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        if (intent.hasExtra("state")) {
            // Compatibility with the old system_server module still in memory:
            // it may call startForegroundService(). Try to satisfy its 5s
            // deadline; a denial is caught and is harmless for startService().
            ensureForeground();
            int state = intent.getIntExtra("state", 0);
            float y = intent.getFloatExtra("y", 0f);
            handleDragStateChanged(state, y);
            return START_STICKY;
        }
        if (intent.getBooleanExtra("keepAlive", false)
                && intent.getStringExtra("viewContent") == null
                && intent.getParcelableExtra("viewBitmap") == null
                && intent.getParcelableExtra("filePfd") == null
                && intent.getByteArrayExtra("imageBytes") == null) {
            return START_STICKY;
        }

        mText = intent.getStringExtra("viewContent");
        mSource = intent.getStringExtra("sourcePkg");
        mCatchType = intent.getIntExtra("catchType", 0);
        mTouchX = intent.getFloatExtra("touchX", -1f);
        mTouchY = intent.getFloatExtra("touchY", -1f);
        mDragActive = intent.getBooleanExtra("dragActive", false);
        mDragPreviewWidth = intent.getIntExtra("previewWidth", 0);
        mDragPreviewHeight = intent.getIntExtra("previewHeight", 0);
        mFileName = intent.getStringExtra("imageFileName");
        if (mFileName == null) {
            mFileName = intent.getStringExtra("fileName");
        }
        mMimeType = intent.getStringExtra("imageMimeType");
        if (mMimeType == null) {
            mMimeType = intent.getStringExtra("mimeType");
        }
        mFileSize = intent.getLongExtra("fileSize", -1);
        mBitmap = null;
        mFilePfd = null;
        mLocalFile = null;
        mImageUrl = intent.getStringExtra("imageUrl");
        String imageCacheName = intent.getStringExtra("imageCacheName");
        if (imageCacheName != null && !imageCacheName.isEmpty()) {
            try {
                File incoming = new File(getCacheDir(), PortalFileProvider.INCOMING);
                File cached = new File(incoming, imageCacheName);
                if (cached.isFile() && cached.length() > 0) {
                    mLocalFile = cached;
                    mFileSize = cached.length();
                }
            } catch (Throwable throwable) {
                Log.w(TAG, "read imageCacheName failed", throwable);
            }
        }
        byte[] imageBytes = null;
        try {
            imageBytes = intent.getByteArrayExtra("imageBytes");
        } catch (Throwable ignored) {
        }
        try {
            Object bitmap = intent.getParcelableExtra("viewBitmap");
            if (bitmap instanceof Bitmap) {
                mBitmap = (Bitmap) bitmap;
            }
        } catch (Throwable ignored) {
        }
        if (imageBytes != null && imageBytes.length > 0) {
            mLocalFile = writeImageBytes(imageBytes);
            if (mBitmap == null) {
                try {
                    mBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                } catch (Throwable throwable) {
                    Log.w(TAG, "decode imageBytes failed", throwable);
                }
            }
            mFileSize = imageBytes.length;
        }
        try {
            Object pfd = intent.getParcelableExtra("filePfd");
            if (pfd instanceof ParcelFileDescriptor) {
                mFilePfd = (ParcelFileDescriptor) pfd;
                mLocalFile = copyPfd(mFilePfd);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "read filePfd failed", throwable);
        }
        if (mLocalFile == null && !TextUtils.isEmpty(mImageUrl)) {
            downloadImageAsync(mImageUrl);
        }

        Log.i(TAG, "onStartCommand text=" + (mText == null ? "null" : mText.length() + " chars")
                + " bitmap=" + (mBitmap == null ? "null" : mBitmap.getWidth() + "x" + mBitmap.getHeight())
                + " file=" + mFileName
                + " bytes=" + (imageBytes == null ? 0 : imageBytes.length)
                + " local=" + (mLocalFile == null ? "null" : mLocalFile.getName())
                + " dragActive=" + mDragActive
                + " source=" + mSource
                + " overlay=" + Settings.canDrawOverlays(this));

        if (TextUtils.isEmpty(mText) && mBitmap == null && mLocalFile == null) {
            Log.w(TAG, "empty payload, refusing to show a blank portal");
            removeBottomPortal();
            return START_NOT_STICKY;
        }

        if (Settings.canDrawOverlays(this)) {
            if (mDragActive) {
                // Direct drag: show the bottom glow & app row. The old
                // forward/favorite quick-action popup has been removed.
                showBottomPortal();
            }
        } else {
            launchMain(intent);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        try {
            unregisterReceiver(mStateReceiver);
        } catch (Throwable ignored) {
        }
        removeBottomPortal();
        hideDragPreview();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "Content Portal", NotificationManager.IMPORTANCE_MIN);
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        return builder.setContentTitle("内容传送门")
                .setContentText("传送门服务运行中")
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setOngoing(true)
                .build();
    }

    private void notifyBridgeShadowState(int state, float y) {
        if (state == mLastNotifiedShadowState && state != 0) {
            return;
        }
        mLastNotifiedShadowState = state;
        try {
            if (!TextUtils.isEmpty(mSource)) {
                Intent pkgIntent = new Intent("com.dragshare.bridge.SHADOW_STATE");
                pkgIntent.setPackage(mSource);
                pkgIntent.putExtra("state", state);
                pkgIntent.putExtra("y", y);
                pkgIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                sendBroadcast(pkgIntent);
            }
            Intent allIntent = new Intent("com.dragshare.bridge.SHADOW_STATE");
            allIntent.putExtra("state", state);
            allIntent.putExtra("y", y);
            allIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            sendBroadcast(allIntent);
        } catch (Throwable ignored) {
        }
    }

    private void setBottomPortalTouchable(boolean touchable) {
        if (mBottomContainer == null || mBottomParams == null) {
            return;
        }
        int oldFlags = mBottomParams.flags;
        if (touchable) {
            mBottomParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            mBottomParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        if (mBottomParams.flags != oldFlags) {
            try {
                mWindowManager.updateViewLayout(mBottomContainer, mBottomParams);
            } catch (Throwable ignored) {
            }
        }
    }

    private void handleDragStateChanged(int state, float y) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mMainHandler.post(() -> handleDragStateChanged(state, y));
            return;
        }
        if (state == 1 && mCurrentDragState == 2 && y <= 0f) {
            // Ignore late/synthetic y=0 state=1 broadcasts while already in hot zone
            return;
        }
        Log.i(TAG, "handleDragStateChanged state=" + state + " y=" + y);
        mCurrentDragState = state;
        if (state == 0) {
            notifyBridgeShadowState(0, y);
            mLastNotifiedShadowState = -1;
            // Drag closed: hide bottom portal smoothly
            removeBottomPortal();
            return;
        }
        if (state == 1) {
            // Drag active in upper screen: glow prompt only, app row collapsed
            showBottomPortal();
            setBottomPortalTouchable(false);
            notifyBridgeShadowState(1, y);
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            // Phase 1 progress (0.0 to 1.0 as finger moves from 15% to 80% screen height)
            float progress = Math.max(0f, Math.min(1.0f, (y - screenHeight * 0.15f) / (screenHeight * 0.67f)));
            if (mGlowView != null) {
                mGlowView.setPhase1Progress(progress);
                mGlowView.setVisibility(View.VISIBLE);
            }
            if (mPromptText != null) {
                mPromptText.setVisibility(View.VISIBLE);
                mPromptText.animate().alpha(1.0f).setDuration(120).start();
            }
            if (mAppRow != null && mAppRow.getVisibility() == View.VISIBLE) {
                mAppRow.animate()
                        .translationY(dp(60))
                        .alpha(0f)
                        .setDuration(160)
                        .withEndAction(() -> {
                            if (mAppRow != null) {
                                mAppRow.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
        } else if (state == 2) {
            // Bottom threshold reached: 3D tilt active, expand app row
            showBottomPortal();
            setBottomPortalTouchable(true);
            notifyBridgeShadowState(2, y);
            if (mGlowView != null) {
                mGlowView.setPhase2();
                mGlowView.setVisibility(View.VISIBLE);
            }
            if (mPromptText != null && mPromptText.getVisibility() == View.VISIBLE) {
                mPromptText.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            if (mPromptText != null) {
                                mPromptText.setVisibility(View.GONE);
                            }
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

    private void showDragPreview() {
        if (mDragPreview != null) {
            if (mBottomContainer != null) {
                raiseDragPreview();
            }
            updateDragPreviewPosition();
            return;
        }
        if (TextUtils.isEmpty(mText) && mBitmap == null) {
            return;
        }
        try {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int square = dp(76);
            if (mDragPreviewWidth <= 0) {
                mDragPreviewWidth = mBitmap != null
                        ? square
                        : Math.min(dp(220), screenWidth - dp(32));
            }
            if (mDragPreviewHeight <= 0) {
                mDragPreviewHeight = square;
            }
            FrameLayout card = new FrameLayout(this);
            card.setClipToOutline(true);

            if (mBitmap != null) {
                // ColorOS image preview is the bitmap itself with an 8dp
                // rounded clip, not a white padded card.
                card.setBackground(rounded(Color.TRANSPARENT, 8));
                card.setPadding(0, 0, 0, 0);
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageBitmap(mBitmap);
                card.addView(image, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                mDragPreviewImage = image;
            } else {
                card.setBackground(rounded(Color.WHITE, 10));
                card.setPadding(dp(6), dp(6), dp(6), dp(6));
                TextView text = new TextView(this);
                text.setTextColor(Color.BLACK);
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                text.setGravity(Gravity.CENTER);
                text.setMaxLines(4);
                text.setEllipsize(TextUtils.TruncateAt.END);
                text.setBreakStrategy(0);
                text.setHyphenationFrequency(2);
                String content = mText == null ? "" : mText.replace('\n', ' ');
                if (content.length() > 40) {
                    content = content.substring(0, 40);
                }
                text.setText(content);
                card.addView(text, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                mDragPreviewText = text;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    mDragPreviewWidth,
                    mDragPreviewHeight,
                    Build.VERSION.SDK_INT >= 26
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            mWindowManager.addView(card, params);
            mDragPreview = card;
            mDragPreviewParams = params;
            mDragPreviewBottom = false;
            mDragPreviewRaised = mBottomContainer != null;
            updateDragPreviewPosition();
            Log.i(TAG, "drag preview added " + mDragPreviewWidth + "x" + mDragPreviewHeight);
        } catch (Throwable throwable) {
            Log.w(TAG, "showDragPreview failed", throwable);
            mDragPreview = null;
            mDragPreviewParams = null;
        }
    }

    private void updateDragPreviewPosition() {
        if (mDragPreview == null || mDragPreviewParams == null) {
            return;
        }
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float x = mTouchX >= 0 ? mTouchX : screenWidth / 2f;
        float y = mTouchY >= 0 ? mTouchY : screenHeight / 2f;
        mDragPreviewParams.x = clamp((int) x - mDragPreviewParams.width / 2,
                0, Math.max(0, screenWidth - mDragPreviewParams.width));
        mDragPreviewParams.y = clamp((int) y - mDragPreviewParams.height / 2,
                0, Math.max(0, screenHeight - mDragPreviewParams.height));
        try {
            mWindowManager.updateViewLayout(mDragPreview, mDragPreviewParams);
        } catch (Throwable ignored) {
        }
    }

    private void morphDragPreview(boolean square) {
        if (mDragPreview == null || mDragPreviewParams == null
                || square == mDragPreviewBottom) {
            return;
        }
        mDragPreviewBottom = square;
        int targetWidth = square ? dp(76) : mDragPreviewWidth;
        int targetHeight = dp(76);
        mDragPreviewParams.width = targetWidth;
        mDragPreviewParams.height = targetHeight;
        ViewGroup.LayoutParams layoutParams = mDragPreview.getLayoutParams();
        layoutParams.width = targetWidth;
        layoutParams.height = targetHeight;
        mDragPreview.setLayoutParams(layoutParams);
        if (mDragPreviewText != null) {
            mDragPreviewText.requestLayout();
        }
        try {
            mWindowManager.updateViewLayout(mDragPreview, mDragPreviewParams);
        } catch (Throwable ignored) {
        }
        updateDragPreviewPosition();
        Log.i(TAG, "drag preview morph square=" + square + " " + targetWidth + "x" + targetHeight);
    }

    private void setDragPreviewScale(float scale) {
        if (mDragPreview != null) {
            mDragPreview.setScaleX(scale);
            mDragPreview.setScaleY(scale);
        }
    }

    private void raiseDragPreview() {
        if (mDragPreview == null || mDragPreviewParams == null || mDragPreviewRaised) {
            return;
        }
        mDragPreviewRaised = true;
        try {
            // Same window type as the bottom portal; re-add once so the
            // preview stays above the glow/app row.
            mWindowManager.removeView(mDragPreview);
            mWindowManager.addView(mDragPreview, mDragPreviewParams);
            updateDragPreviewPosition();
        } catch (Throwable throwable) {
            Log.w(TAG, "raiseDragPreview failed", throwable);
        }
    }

    private void hideDragPreview() {
        if (mDragPreview != null) {
            try {
                mWindowManager.removeView(mDragPreview);
            } catch (Throwable ignored) {
            }
            mDragPreview = null;
            mDragPreviewParams = null;
            mDragPreviewText = null;
            mDragPreviewImage = null;
            mDragPreviewRaised = false;
        }
    }

    private void showBottomPortal() {
        if (mBottomContainer != null) {
            return;
        }
        try {
            if (mTargets.isEmpty()) {
                buildTargets();
            }
            FrameLayout container = new FrameLayout(this);
            container.setFitsSystemWindows(false);
            container.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            int navBarHeight = 0;
            try {
                int navId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                if (navId > 0) {
                    navBarHeight = getResources().getDimensionPixelSize(navId);
                }
            } catch (Throwable ignored) {
            }
            if (navBarHeight <= 0) {
                navBarHeight = dp(20);
            }

            // 1. ColorOS Aurora Glow View (height spans full dp(380), reaches bottom edge of physical screen e2e)
            mGlowView = new GlowView(this);
            FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.BOTTOM);
            container.addView(mGlowView, glowParams);

            // 2. Prompt Text ("拖拽至此处触发内容传送门")
            TextView prompt = new TextView(this);
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
            container.addView(prompt, promptParams);
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
            container.addView(scroll, scrollParams);
            mAppRow = scroll;

            container.setOnDragListener((v, event) -> {
                int[] location = new int[2];
                container.getLocationOnScreen(location);
                float rawX = location[0] + event.getX();
                float rawY = location[1] + event.getY();
                int screenH = getResources().getDisplayMetrics().heightPixels;
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                    case DragEvent.ACTION_DRAG_ENTERED:
                        // IMPORTANT: event.getX()/getY() are ALWAYS 0.0f on ACTION_DRAG_ENTERED!
                        // Never use rawY on ACTION_DRAG_ENTERED, otherwise rawY == location[1] (1421.0)
                        // which is < 0.77 * screenH and immediately collapses the app bar after 40ms!
                        return true;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        if (event.getX() == 0f && event.getY() == 0f) {
                            return true;
                        }
                        float enterThreshold = screenH * 0.76f;
                        float exitThreshold = screenH * 0.70f;
                        boolean inHotZone = (mCurrentDragState == 2)
                                ? (rawY >= exitThreshold)
                                : (rawY >= enterThreshold);
                        if (inHotZone) {
                            updateTargetHighlight(rawX, rawY);
                            notifyBridgeShadowState(2, rawY);
                            mCurrentDragState = 2;
                        } else {
                            handleDragStateChanged(1, rawY);
                        }
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        updateTargetHighlight(-1f, -1f);
                        return true;
                    case DragEvent.ACTION_DROP:
                        String dropped = null;
                        ClipData clip = event.getClipData();
                        if (clip != null && clip.getItemCount() > 0) {
                            dropped = clip.getItemAt(0).coerceToText(this).toString();
                        }
                        if (!TextUtils.isEmpty(dropped)) {
                            mText = dropped;
                        }
                        AppTarget target = findTargetAt(rawX, rawY);
                        notifyBridgeShadowState(0, 0f);
                        mCurrentDragState = 0;
                        mLastNotifiedShadowState = -1;
                        removeBottomPortal();
                        if (target != null) {
                            performTarget(target);
                            return true;
                        }
                        return false;
                    case DragEvent.ACTION_DRAG_ENDED:
                        notifyBridgeShadowState(0, 0f);
                        mCurrentDragState = 0;
                        mLastNotifiedShadowState = -1;
                        mMainHandler.postDelayed(this::removeBottomPortal, 250);
                        return true;
                }
                return false;
            });

            int height = dp(380);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    height,
                    Build.VERSION.SDK_INT >= 26
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                    PixelFormat.TRANSLUCENT);
            if (Build.VERSION.SDK_INT >= 28) {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            params.gravity = Gravity.BOTTOM | Gravity.START;
            params.x = 0;
            params.y = 0;

            mWindowManager.addView(container, params);
            mBottomContainer = container;
            mBottomParams = params;
            Log.i(TAG, "bottom portal overlay added (height=" + height + ")");
        } catch (Throwable throwable) {
            Log.w(TAG, "showBottomPortal failed", throwable);
        }
    }

    private void removeBottomPortal() {
        if (mBottomContainer != null) {
            try {
                mWindowManager.removeView(mBottomContainer);
            } catch (Throwable ignored) {
            }
            mBottomContainer = null;
            mBottomParams = null;
            mGlowView = null;
            mAppRow = null;
            mPromptText = null;
        }
    }

    private HorizontalScrollView buildAppRow() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setBackground(rounded(Color.argb(220, 28, 29, 32), 22));
        scroll.setPadding(dp(10), dp(8), dp(10), dp(8));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (AppTarget target : mTargets) {
            row.addView(buildAppItem(target), new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        scroll.addView(row, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private View buildAppItem(AppTarget target) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(8), dp(4), dp(8), dp(4));
        item.setTag(target);

        FrameLayout iconBox = new FrameLayout(this);
        iconBox.setBackground(rounded(Color.rgb(245, 246, 248), 22));
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setImageDrawable(target.icon);
        iconBox.addView(icon, new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        item.addView(iconBox, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView label = new TextView(this);
        label.setText(target.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(3);
        item.addView(label, labelParams);
        return item;
    }

    private void updateTargetHighlight(float rawX, float rawY) {
        if (mAppRow == null) {
            return;
        }
        try {
            ViewGroup row = (ViewGroup) mAppRow.getChildAt(0);
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                int[] location = new int[2];
                child.getLocationOnScreen(location);
                boolean hit = rawX >= location[0] && rawX <= location[0] + child.getWidth()
                        && rawY >= location[1] && rawY <= location[1] + child.getHeight();
                child.setAlpha(hit ? 1f : 0.75f);
                child.setScaleX(hit ? 1.15f : 1f);
                child.setScaleY(hit ? 1.15f : 1f);
            }
        } catch (Throwable ignored) {
        }
    }

    private AppTarget findTargetAt(float rawX, float rawY) {
        if (mAppRow == null) {
            return null;
        }
        try {
            ViewGroup row = (ViewGroup) mAppRow.getChildAt(0);
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                int[] location = new int[2];
                child.getLocationOnScreen(location);
                if (rawX >= location[0] && rawX <= location[0] + child.getWidth()
                        && rawY >= location[1] && rawY <= location[1] + child.getHeight()) {
                    return (AppTarget) child.getTag();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void performTarget(AppTarget target) {
        if (target == null) {
            return;
        }
        Log.i(TAG, "performTarget " + target.label + " pkg=" + target.packageName + " kind=" + target.kind);
        hideDragPreview();
        if (target.kind == AppTarget.KIND_SAVE) {
            saveToPortal();
            return;
        }
        if (target.kind == AppTarget.KIND_SEARCH) {
            searchText();
            return;
        }
        Intent intent = buildShareIntent(target.packageName);
        if (intent == null) {
            // A blank image drag used to leave the bottom portal and the
            // system task-zoom state alive forever. Always tear both down.
            toast("没有可分享的内容");
            removeBottomPortal();
            return;
        }
        try {
            startActivity(intent);
            removeBottomPortal();
        } catch (Throwable throwable) {
            Log.w(TAG, "performTarget failed", throwable);
            toast("无法打开 " + target.label);
        }
    }

    private Intent buildShareIntent(String packageName) {
        Intent intent;
        if (mLocalFile != null) {
            Uri uri = ensureFileUri();
            if (uri == null) {
                return null;
            }
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType(TextUtils.isEmpty(mMimeType) ? "*/*" : mMimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setClipData(ClipData.newUri(getContentResolver(), "file", uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (mBitmap != null) {
            Uri uri = ensureImageUri();
            if (uri == null) {
                return null;
            }
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setClipData(ClipData.newUri(getContentResolver(), "image", uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (!TextUtils.isEmpty(mText)) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, mText);
        } else {
            return null;
        }
        if (!TextUtils.isEmpty(packageName)) {
            intent.setPackage(packageName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void buildTargets() {
        mTargets.clear();
        try {
            mTargets.add(new AppTarget("中转站", getPackageManager().getApplicationIcon(getPackageName()),
                    getPackageName(), AppTarget.KIND_SAVE));
        } catch (Throwable ignored) {
        }

        PackageManager pm = getPackageManager();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        List<ResolveInfo> resolved = pm.queryIntentActivities(send, 0);
        Set<String> seen = new HashSet<>();
        seen.add(getPackageName());
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
                Drawable icon = info.loadIcon(pm);
                CharSequence label = info.loadLabel(pm);
                mTargets.add(new AppTarget(label == null ? pkg : label.toString(), icon, pkg, AppTarget.KIND_SHARE));
                added++;
            } catch (Throwable ignored) {
            }
            if (added >= 6) {
                break;
            }
        }

        mTargets.add(new AppTarget("搜索", getDrawable(android.R.drawable.ic_menu_search),
                "", AppTarget.KIND_SEARCH));
    }

    private void saveToPortal() {
        if (!TextUtils.isEmpty(mText)) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("portal", mText));
            toast("已存入中转站");
        } else if (mLocalFile != null) {
            Uri uri = ensureFileUri();
            toast(uri == null ? "保存失败" : "已存入中转站");
        } else if (mBitmap != null) {
            Uri uri = ensureImageUri();
            toast(uri == null ? "保存失败" : "已存入中转站");
        }
        removeBottomPortal();
    }

    private void shareText() {
        if (TextUtils.isEmpty(mText) && mBitmap == null && mLocalFile == null) {
            toast("没有可分享的内容");
            return;
        }
        Intent intent = buildShareIntent(null);
        if (intent == null) {
            return;
        }
        startActivity(Intent.createChooser(intent, "分享内容").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void quoteText() {
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("quote", "> " + mText.replace("\n", "\n> ")));
        toast("已复制引用");
    }

    private void translateText() {
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://translate.google.com/?text=" + Uri.encode(mText)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Throwable throwable) {
            toast("无法打开翻译");
        }
    }

    private void searchText() {
        if (TextUtils.isEmpty(mText)) {
            toast("没有可搜索的文字");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.bing.com/search?q=" + Uri.encode(mText)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Throwable throwable) {
            toast("无法搜索");
        }
    }

    private void launchMain(Intent source) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtras(source);
        startActivity(intent);
        stopSelf();
    }

    private void downloadImageAsync(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (url.startsWith("/")) {
            File local = new File(url);
            if (local.isFile()) {
                mLocalFile = local;
                mFileSize = local.length();
            }
            return;
        }
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36");
                connection.connect();
                String suffix = url.contains(".png") ? ".png"
                        : (url.contains(".webp") ? ".webp" : ".jpg");
                File target = File.createTempFile("dragshare_url_", suffix, incomingDir());
                try (InputStream input = connection.getInputStream();
                     OutputStream output = new FileOutputStream(target)) {
                    byte[] buffer = new byte[65536];
                    int count;
                    while ((count = input.read(buffer)) > 0) {
                        output.write(buffer, 0, count);
                    }
                }
                if (target.length() > 0) {
                    mLocalFile = target;
                    mFileSize = target.length();
                    Log.i(TAG, "downloadImageAsync " + target.getName()
                            + " " + target.length() + " bytes");
                }
            } catch (Throwable throwable) {
                Log.w(TAG, "downloadImageAsync failed", throwable);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, "dragshare-image-download").start();
    }

    private File incomingDir() {
        File dir = new File(getCacheDir(), PortalFileProvider.INCOMING);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File writeImageBytes(byte[] bytes) {
        try {
            String suffix = ".bin";
            if (mFileName != null && mFileName.contains(".")) {
                suffix = mFileName.substring(mFileName.lastIndexOf('.'));
            } else if (mMimeType != null && mMimeType.contains("png")) {
                suffix = ".png";
            } else if (mMimeType != null && mMimeType.contains("webp")) {
                suffix = ".webp";
            } else if (mMimeType != null && mMimeType.startsWith("image/")) {
                suffix = ".jpg";
            }
            File target = File.createTempFile("dragshare_", suffix, incomingDir());
            try (OutputStream output = new FileOutputStream(target)) {
                output.write(bytes);
            }
            Log.i(TAG, "writeImageBytes " + target.getName() + " " + bytes.length + " bytes");
            return target;
        } catch (Throwable throwable) {
            Log.w(TAG, "writeImageBytes failed", throwable);
            return null;
        }
    }

    private File copyPfd(ParcelFileDescriptor pfd) {
        try {
            String suffix = mFileName != null && mFileName.contains(".")
                    ? mFileName.substring(mFileName.lastIndexOf('.')) : ".bin";
            File target = File.createTempFile("dragshare_", suffix, incomingDir());
            try (InputStream input = new FileInputStream(pfd.getFileDescriptor());
                 OutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) > 0) {
                    output.write(buffer, 0, count);
                }
            }
            return target;
        } catch (Throwable throwable) {
            Log.w(TAG, "copyPfd failed", throwable);
            return null;
        }
    }

    private Uri ensureFileUri() {
        if (mLocalFile == null) {
            return null;
        }
        // Serve straight from the portal's own cache through PortalFileProvider.
        // No MediaStore copy and no extra READ_MEDIA_IMAGES permission needed;
        // the share Intent grants a temporary read URI permission.
        return Uri.parse("content://com.github.leaf.hyperportal.files/incoming/"
                + Uri.encode(mLocalFile.getName()));
    }

    private Uri ensureImageUri() {
        if (mBitmap == null) {
            return null;
        }
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,
                "DragShare_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DragShare");
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            return null;
        }
        try (OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null || !mBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                return null;
            }
            return uri;
        } catch (Throwable throwable) {
            Log.w(TAG, "ensureImageUri failed", throwable);
            return null;
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * ColorOS Aurora Bottom Glow View.
     * Uses Android 13+ RuntimeShader with ColorOS extracted noise texture and SkSL shader,
     * with an elegant RadialGradient fallback.
     */
    private static final class GlowView extends View {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RuntimeShader mShader;
        private Bitmap mNoiseBmp;
        private long mStartTime;

        // Dynamic uniform values for smooth animation & state transitions
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
                    InputStream is = context.getAssets().open("oppo_glow.png");
                    mNoiseBmp = BitmapFactory.decodeStream(is);
                    is.close();
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
                        mStartTime = System.currentTimeMillis();
                        mPaint.setShader(mShader);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "RuntimeShader init failed, using radial fallback", t);
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
            if (w <= 0 || h <= 0) {
                return;
            }

            float damping = 0.20f;
            mCurrentCenterY += (mTargetCenterY - mCurrentCenterY) * damping;
            mCurrentStart += (mTargetStart - mCurrentStart) * damping;
            mCurrentSpace += (mTargetSpace - mCurrentSpace) * damping;
            mCurrentAlpha += (mTargetAlpha - mCurrentAlpha) * damping;

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
                int alpha = (int) (240 * mCurrentAlpha);
                if (alpha > 0) {
                    RadialGradient gradient = new RadialGradient(
                            w * 0.5f, h * 1.05f, w * 0.85f,
                            new int[]{
                                    Color.argb(alpha, 0x5C, 0xFD, 0xAC),
                                    Color.argb((int) (alpha * 0.85f), 0x18, 0xD4, 0xF2),
                                    Color.argb((int) (alpha * 0.65f), 0xA8, 0x9B, 0xF5),
                                    Color.argb((int) (alpha * 0.40f), 0xFC, 0xAF, 0x48),
                                    Color.TRANSPARENT
                            },
                            new float[]{0.0f, 0.25f, 0.55f, 0.80f, 1.0f},
                            Shader.TileMode.CLAMP
                    );
                    mPaint.setShader(gradient);
                    canvas.drawRect(0, 0, w, h, mPaint);
                }
            }
        }
    }

    private static final class AppTarget {
        static final int KIND_SHARE = 0;
        static final int KIND_SAVE = 1;
        static final int KIND_SEARCH = 2;

        final String label;
        final Drawable icon;
        final String packageName;
        final int kind;

        AppTarget(String label, Drawable icon, String packageName, int kind) {
            this.label = label;
            this.icon = icon;
            this.packageName = packageName;
            this.kind = kind;
        }
    }
}
