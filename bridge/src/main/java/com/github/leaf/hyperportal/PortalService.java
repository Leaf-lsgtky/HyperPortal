package com.github.leaf.hyperportal;

import com.github.leaf.hyperportal.data.HyperPortalPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.github.leaf.hyperportal.view.ColorOSDockBar;

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
    private ColorOSDockBar mDockBar;
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
    private float mStartDragY = -1f;

    private ParcelFileDescriptor mFilePfd;
    private String mFileName;
    private String mMimeType;
    private long mFileSize = -1;
    private volatile File mLocalFile;
    private volatile String mImageUrl;

    private final List<AppTarget> mTargets = new ArrayList<>();
    private float mTriggerThreshold = 0.76f;
    private String mTriggerPosition = "bottom";
    private float mStartDragX = -1f;

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            int state = intent.getIntExtra("state", 0);
            float x = intent.getFloatExtra("x", -1f);
            float y = intent.getFloatExtra("y", 0f);
            String pos = intent.getStringExtra("position");
            if (pos != null) {
                mTriggerPosition = pos;
            }
            handleDragStateChanged(state, x, y);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        PortalFileProvider.pruneCacheAsync(this, null);
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
        filter.addAction("com.github.leaf.hyperportal.ACTION_DRAG_STATE");
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
        if (PortalShareContract.ACTION_DISPATCH_IMAGE_SHARE.equals(intent.getAction())) {
            dispatchImageShare(intent);
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
                    try {
                        Uri incomingUri = Uri.parse("content://com.github.leaf.hyperportal.files/incoming/" + Uri.encode(imageCacheName));
                        String[] targets = new String[]{
                                "com.miui.notes", "com.tencent.mm", "com.tencent.mobileqq",
                                "com.sina.weibo", "org.telegram.messenger", "com.coolapk.market",
                                "com.android.fileexplorer", "com.zhihu.android", "com.xingin.xhs"
                        };
                        for (String t : targets) {
                            try {
                                grantUriPermission(t, incomingUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Throwable ignored) {
                            }
                        }
                    } catch (Throwable ignored) {
                    }
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

        if (PortalFileProvider.isSystemUiActive()) {
            // SystemUI module is handling the portal overlay directly.
            // PortalService is only keeping state/staged files, do not show duplicate overlay.
            return START_NOT_STICKY;
        }

        if (Settings.canDrawOverlays(this)) {
            if (mDragActive) {
                // Direct drag: show the bottom glow & app row. The old
                // forward/favorite quick-action popup has been removed.
                showBottomPortal();
            }
        } else {
            Log.w(TAG, "overlay permission not granted and SystemUI inactive; skip showing local overlay");
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

    private boolean isLandscape() {
        try {
            return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        } catch (Throwable t) {
            return false;
        }
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

    private void handleDragStateChanged(int state, float y) {
        handleDragStateChanged(state, -1f, y);
    }

    private void handleDragStateChanged(int state, float x, float y) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mMainHandler.post(() -> handleDragStateChanged(state, x, y));
            return;
        }
        if (PortalFileProvider.isSystemUiActive()) {
            // SystemUI module is handling the portal overlay directly at 120Hz VSYNC.
            // Avoid duplicate overlays and duplicate haptics.
            return;
        }
        if (state == 1 && mCurrentDragState == 2 && y <= 0f && x <= 0f) {
            // Ignore late/synthetic y=0 state=1 broadcasts while already in hot zone
            return;
        }
        HyperPortalPreferences.Config cfg = HyperPortalPreferences.INSTANCE.getConfig(this);
        mTriggerThreshold = cfg.getTriggerThreshold() / 100.0f;
        mTriggerPosition = cfg.getTriggerPosition();
        if (!cfg.getMasterSwitch() || (!cfg.getLandscapeTrigger() && isLandscape())) {
            removeBottomPortal();
            notifyBridgeShadowState(0, 0f);
            mCurrentDragState = 0;
            return;
        }
        if (mDockBar != null) {
            mDockBar.setEdgeScrollMarginDp(cfg.getEdgeScrollMargin());
        }
        Log.i(TAG, "handleDragStateChanged state=" + state + " x=" + x + " y=" + y + " pos=" + mTriggerPosition);
        mCurrentDragState = state;
        if (state == 0) {
            notifyBridgeShadowState(0, y);
            mLastNotifiedShadowState = -1;
            // Drag closed: hide bottom portal smoothly
            removeBottomPortal();
            return;
        }
        if (state == 1) {
            // Drag active outside hot zone: glow prompt only, app row collapsed
            showBottomPortal();
            setBottomPortalTouchable(false);
            boolean isRight = "right".equals(mTriggerPosition);
            notifyBridgeShadowState(1, isRight ? x : y);
            float coord = isRight ? x : y;
            int screenDim = isRight ? getResources().getDisplayMetrics().widthPixels
                                    : getPhysicalScreenHeight(this);
            // Phase 1 progress (0.0 to 1.0 as finger moves from 15% to trigger threshold)
            float enterFactor = mTriggerThreshold > 0f ? mTriggerThreshold : 0.76f;
            float glowSpan = Math.max(0.2f, enterFactor - 0.15f);
            float progress = Math.max(0f, Math.min(1.0f, (coord - screenDim * 0.15f) / (screenDim * glowSpan)));
            if (mGlowView != null) {
                mGlowView.setPhase1Progress(progress, isRight);
                mGlowView.setVisibility(View.VISIBLE);
            }
            if (mDockBar != null && mDockBar.getVisibility() == View.VISIBLE) {
                mDockBar.stopAutoScroll();
                mDockBar.animate()
                        .translationY(dp(60))
                        .alpha(0f)
                        .setDuration(160)
                        .withEndAction(() -> {
                            if (mDockBar != null) {
                                mDockBar.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
        } else if (state == 2) {
            // Hot zone reached: 3D tilt active, expand app row
            showBottomPortal();
            setBottomPortalTouchable(true);
            boolean isRight = "right".equals(mTriggerPosition);
            notifyBridgeShadowState(2, isRight ? x : y);
            if (mGlowView != null) {
                mGlowView.setPhase2(isRight);
                mGlowView.setVisibility(View.VISIBLE);
            }
            if (mDockBar != null) {
                if (mDockBar.getVisibility() != View.VISIBLE) {
                    mDockBar.setVisibility(View.VISIBLE);
                    mDockBar.setTranslationY(dp(60));
                    mDockBar.setAlpha(0f);
                    mDockBar.startEnterAnimation();
                }
                mDockBar.animate()
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

            // 2. ColorOS Dock Bar (initially hidden in Phase 1)
            mDockBar = new ColorOSDockBar(this);
            mDockBar.setVisibility(View.GONE);
            mDockBar.setTranslationY(dp(60));
            mDockBar.setAlpha(0f);
            List<ColorOSDockBar.DockItem> dockItems = new ArrayList<>();
            for (AppTarget target : mTargets) {
                dockItems.add(new ColorOSDockBar.DockItem(target.packageName, target.label, target.icon, target));
            }
            mDockBar.setItems(dockItems);
            FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(153), Gravity.BOTTOM);
            dockParams.bottomMargin = Math.max(0, navBarHeight + dp(20) - 50);
            container.addView(mDockBar, dockParams);

            container.setOnDragListener((v, event) -> {
                int[] location = new int[2];
                container.getLocationOnScreen(location);
                float rawX = location[0] + event.getX();
                float rawY = location[1] + event.getY();
                int screenH = getPhysicalScreenHeight(this);
                int screenW = getResources().getDisplayMetrics().widthPixels;
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        mStartDragY = -1f;
                        mStartDragX = -1f;
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        if (event.getX() == 0f && event.getY() == 0f) {
                            return true;
                        }
                        if (mStartDragY < 0f) {
                            mStartDragY = rawY;
                        }
                        if (mStartDragX < 0f) {
                            mStartDragX = rawX;
                        }
                        boolean isRight = "right".equals(mTriggerPosition);
                        float enterFactor = mTriggerThreshold > 0f ? mTriggerThreshold : 0.76f;
                        float exitFactor = Math.max(0.40f, enterFactor - 0.06f);
                        boolean inHotZone;
                        if (isRight) {
                            float enterThreshold = screenW * enterFactor;
                            float exitThreshold = screenW * exitFactor;
                            if (mStartDragX >= enterThreshold) {
                                float rightwardRequired = dp(35);
                                if (mCurrentDragState == 2) {
                                    inHotZone = (rawX >= mStartDragX + dp(10)) && (rawX >= exitThreshold);
                                } else {
                                    inHotZone = (rawX >= mStartDragX + rightwardRequired) && (rawX >= enterThreshold);
                                }
                            } else {
                                inHotZone = (mCurrentDragState == 2)
                                        ? (rawX >= exitThreshold)
                                        : (rawX >= enterThreshold);
                            }
                        } else {
                            float enterThreshold = screenH * enterFactor;
                            float exitThreshold = screenH * exitFactor;
                            if (mStartDragY >= enterThreshold) {
                                float downwardRequired = dp(35);
                                if (mCurrentDragState == 2) {
                                    inHotZone = (rawY >= mStartDragY + dp(10)) && (rawY >= exitThreshold);
                                } else {
                                    inHotZone = (rawY >= mStartDragY + downwardRequired) && (rawY >= enterThreshold);
                                }
                            } else {
                                inHotZone = (mCurrentDragState == 2)
                                        ? (rawY >= exitThreshold)
                                        : (rawY >= enterThreshold);
                            }
                        }
                        if (inHotZone) {
                            if (mDockBar != null) {
                                mDockBar.onDragLocation(rawX, rawY);
                            }
                            notifyBridgeShadowState(2, isRight ? rawX : rawY);
                            mCurrentDragState = 2;
                        } else {
                            handleDragStateChanged(1, rawX, rawY);
                        }
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        if (mDockBar != null) {
                            mDockBar.onDragExited();
                        }
                        return true;
                    case DragEvent.ACTION_DROP:
                        mStartDragY = -1f;
                        mStartDragX = -1f;
                        String dropped = null;
                        ClipData clip = event.getClipData();
                        if (clip != null && clip.getItemCount() > 0) {
                            dropped = clip.getItemAt(0).coerceToText(this).toString();
                        }
                        if (!TextUtils.isEmpty(dropped)) {
                            mText = dropped;
                        }
                        ColorOSDockBar.DockItem hit = mDockBar != null ? mDockBar.findHitTarget(rawX, rawY) : null;
                        AppTarget target = (hit != null && hit.tag instanceof AppTarget) ? (AppTarget) hit.tag : null;
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
                        mStartDragY = -1f;
                        mStartDragX = -1f;
                        if (mDockBar != null) {
                            mDockBar.resetMagnification(false);
                        }
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
            if (mDockBar != null) {
                mDockBar.stopAutoScroll();
                mDockBar.resetMagnification(false);
            }
            mBottomContainer = null;
            mBottomParams = null;
            mGlowView = null;
            mDockBar = null;
        }
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
        if (target.kind == AppTarget.KIND_COPY) {
            if (mLocalFile != null || mBitmap != null) {
                Uri uri = ensureImageUri();
                if (uri == null) uri = ensureFileUri();
                if (uri != null) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newUri(getContentResolver(), "image", uri));
                    toast("图片已复制到剪贴板");
                }
            } else if (!TextUtils.isEmpty(mText)) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("HyperPortal", mText));
                toast("文本已复制到剪贴板");
            }
            removeBottomPortal();
            return;
        }
        if (target.kind == AppTarget.KIND_SEARCH) {
            searchText();
            return;
        }
        Intent intent = buildShareIntent(target.packageName, target.activityName);
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
        return buildShareIntent(packageName, "");
    }

    private Intent buildShareIntent(String packageName, String activityName) {
        Intent intent;
        if (mLocalFile != null) {
            Uri uri = ensureFileUri();
            if (uri == null) {
                return null;
            }
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType(resolveMimeType(uri, mMimeType));
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
            if (!TextUtils.isEmpty(activityName)) {
                intent.setComponent(new ComponentName(packageName, activityName));
            }
            Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream != null) {
                try {
                    grantUriPermission(packageName, stream, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Throwable throwable) {
                    Log.w(TAG, "grant share uri failed target=" + packageName
                            + " uri=" + stream, throwable);
                }
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Dispatches an image share from the HyperPortal UID. The SystemUI module cannot
     * reliably grant a PortalFileProvider URI to a third-party app, so it asks this
     * service to perform the ACTION_SEND instead.
     */
    private void dispatchImageShare(Intent intent) {
        String target = intent.getStringExtra(PortalShareContract.EXTRA_TARGET_PACKAGE);
        String targetActivity = intent.getStringExtra(PortalShareContract.EXTRA_TARGET_ACTIVITY);
        String uriString = intent.getStringExtra(PortalShareContract.EXTRA_IMAGE_URI);
        String mime = intent.getStringExtra(PortalShareContract.EXTRA_IMAGE_MIME);
        String text = intent.getStringExtra(PortalShareContract.EXTRA_SHARE_TEXT);
        if (TextUtils.isEmpty(target)) {
            Log.w(TAG, "dispatchImageShare missing target");
            return;
        }
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setPackage(target);
            if (!TextUtils.isEmpty(targetActivity)) {
                share.setComponent(new ComponentName(target, targetActivity));
            }
            share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!TextUtils.isEmpty(uriString)) {
                Uri uri = Uri.parse(uriString);
                String resolved = resolveMimeType(uri, mime);
                share.setType(resolved);
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.setClipData(ClipData.newUri(getContentResolver(), "image", uri));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    grantUriPermission(target, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Throwable throwable) {
                    Log.w(TAG, "dispatchImageShare grant failed target=" + target
                            + " uri=" + uri, throwable);
                }
                Log.i(TAG, "dispatchImageShare target=" + target
                        + " uri=" + uri + " mime=" + resolved);
            } else if (!TextUtils.isEmpty(text)) {
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, text);
                Log.i(TAG, "dispatchTextShare target=" + target
                        + " chars=" + text.length());
            } else {
                Log.w(TAG, "dispatchImageShare empty payload target=" + target);
                return;
            }
            startActivity(share);
        } catch (Throwable throwable) {
            Log.w(TAG, "dispatchImageShare failed target=" + target
                    + " uri=" + uriString, throwable);
            toast("无法分享图片");
        }
    }

    private String resolveMimeType(Uri uri, String fallback) {
        try {
            String type = getContentResolver().getType(uri);
            if (!TextUtils.isEmpty(type)) {
                return type;
            }
        } catch (Throwable ignored) {
        }
        return TextUtils.isEmpty(fallback) ? "image/png" : fallback;
    }

    private void refreshTargets() {
        buildTargets();
        if (mDockBar != null) {
            List<ColorOSDockBar.DockItem> dockItems = new ArrayList<>();
            for (AppTarget target : mTargets) {
                dockItems.add(new ColorOSDockBar.DockItem(target.packageName, target.label, target.icon, target));
            }
            mDockBar.setItems(dockItems);
        }
    }

    private void buildTargets() {
        mTargets.clear();
        boolean isImage = (mBitmap != null || mLocalFile != null || mFilePfd != null);
        try {
            mTargets.add(new AppTarget(isImage ? "存入中转站" : "中转站",
                    getPackageManager().getApplicationIcon(getPackageName()),
                    getPackageName(), AppTarget.KIND_SAVE));
        } catch (Throwable ignored) {
        }

        mTargets.add(new AppTarget(isImage ? "复制图片" : "复制文本", null, "copy", AppTarget.KIND_COPY));

        PackageManager pm = getPackageManager();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(isImage ? "image/*" : "text/plain");
        List<ResolveInfo> resolved = pm.queryIntentActivities(send, 0);
        Set<String> seen = new HashSet<>();
        seen.add(getPackageName());
        int maxTargets = HyperPortalPreferences.INSTANCE.getConfig(this).getTargetLimit();
        int added = 0;
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null) {
                continue;
            }
            String pkg = info.activityInfo.packageName;
            String activity = info.activityInfo.name;
            String key = pkg + "/" + activity;
            if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(activity)
                    || pkg.equals(getPackageName()) || !seen.add(key)) {
                continue;
            }
            try {
                Drawable icon = info.loadIcon(pm);
                CharSequence label = info.loadLabel(pm);
                mTargets.add(new AppTarget(label == null ? pkg : label.toString(),
                        icon, pkg, activity, AppTarget.KIND_SHARE));
                added++;
            } catch (Throwable ignored) {
            }
            if (added >= maxTargets) {
                break;
            }
        }

        if (!isImage) {
            mTargets.add(new AppTarget("搜索", getDrawable(android.R.drawable.ic_menu_search),
                    "", AppTarget.KIND_SEARCH));
        }
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
        PortalFileProvider.pruneCacheAsync(this, null);
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
            setPhase1Progress(progress, false);
        }

        public void setPhase1Progress(float progress, boolean isRight) {
            mIsRight = isRight;
            float p = Math.max(0f, Math.min(1.0f, progress));
            if (isRight) {
                int w = getWidth() > 0 ? getWidth() : 1080;
                int h = getHeight() > 0 ? getHeight() : 2400;
                float aspect = h / (float) w;
                mTargetCenterX = 2.5f - 1.34f * p;
                mTargetCenterY = aspect * 0.5f;
            } else {
                mTargetCenterX = 0.5f;
                mTargetCenterY = -2.5f + 1.34f * p;
            }
            mTargetStart = 2.5f - 1.5f * p;
            mTargetSpace = 0.30f + 0.46f * p;
            mTargetAlpha = 0.50f + 0.45f * p;
            invalidate();
        }

        public void setPhase2() {
            setPhase2(false);
        }

        public void setPhase2(boolean isRight) {
            mIsRight = isRight;
            if (isRight) {
                int w = getWidth() > 0 ? getWidth() : 1080;
                int h = getHeight() > 0 ? getHeight() : 2400;
                float aspect = h / (float) w;
                mTargetCenterX = 1.16f;
                mTargetCenterY = aspect * 0.5f;
            } else {
                mTargetCenterX = 0.5f;
                mTargetCenterY = -1.16f;
            }
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
            mCurrentCenterX += (mTargetCenterX - mCurrentCenterX) * damping;
            mCurrentCenterY += (mTargetCenterY - mCurrentCenterY) * damping;
            mCurrentStart += (mTargetStart - mCurrentStart) * damping;
            mCurrentSpace += (mTargetSpace - mCurrentSpace) * damping;
            mCurrentAlpha += (mTargetAlpha - mCurrentAlpha) * damping;

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
                int alpha = (int) (240 * mCurrentAlpha);
                if (alpha > 0) {
                    float cx = mIsRight ? (w * 0.95f) : (w * 0.5f);
                    float cy = mIsRight ? (h * 0.5f) : (h * 1.05f);
                    RadialGradient gradient = new RadialGradient(
                            cx, cy, Math.max(w, h) * 0.85f,
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
        static final int KIND_COPY = 3;

        final String label;
        final Drawable icon;
        final String packageName;
        final String activityName;
        final int kind;

        AppTarget(String label, Drawable icon, String packageName, int kind) {
            this(label, icon, packageName, "", kind);
        }

        AppTarget(String label, Drawable icon, String packageName,
                  String activityName, int kind) {
            this.label = label;
            this.icon = icon;
            this.packageName = packageName;
            this.activityName = activityName;
            this.kind = kind;
        }
    }
}
