// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.Toast;

import java.util.function.Consumer;

/**
 * Manages the presentation, lifecycle, and WindowManager integration
 * of ColorOS TransferDock Capsule, FloatBar Handle, and Drawer Panel.
 *
 * The drawer is split into two windows:
 * - a full-screen dim/touch scrim, and
 * - a bounded 146dp x 578dp panel window with compositor background blur.
 *
 * Keeping the panel window bounded is important: Window.setBackgroundBlurRadius
 * (and WindowManager.LayoutParams.setBlurBehindRadius) blurs behind the whole
 * window.  A full-screen panel window would therefore blur the whole screen.
 */
public class TransferDockController {

    private static final String TAG = "TransferDockController";

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private TransferDockCapsuleView mCapsuleView;
    private WindowManager.LayoutParams mCapsuleParams;
    private boolean mCapsuleAttached = false;

    private TransferDockFloatBarView mFloatBarView;
    private WindowManager.LayoutParams mFloatBarParams;
    private boolean mFloatBarAttached = false;

    private TransferDockPanel mPanelView;
    private WindowManager.LayoutParams mPanelParams;
    private boolean mPanelAttached = false;
    private ValueAnimator mPanelSlideAnim;
    private int mPanelRestX;
    private int mPanelDragOutX;

    private View mScrimView;
    private WindowManager.LayoutParams mScrimParams;
    private boolean mScrimAttached = false;

    private boolean mBlurAvailable;
    private Consumer<Boolean> mBlurEnabledListener;
    private boolean mBlurListenerRegistered = false;

    public TransferDockController(Context context) {
        this.mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mBlurAvailable = queryBlurAvailable();
        registerBlurListener();
    }

    private int dp(float value) {
        return (int) (value * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }

    private int resolveWindowType() {
        if ("com.android.systemui".equals(mContext.getPackageName())) {
            return 2024; // TYPE_NAVIGATION_BAR_PANEL (SystemUI root window privilege)
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private boolean queryBlurAvailable() {
        if (Build.VERSION.SDK_INT < 31) {
            return false;
        }
        try {
            return mWindowManager.isCrossWindowBlurEnabled();
        } catch (Throwable t) {
            Log.w(TAG, "isCrossWindowBlurEnabled failed", t);
            return false;
        }
    }

    private void registerBlurListener() {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }
        mBlurEnabledListener = enabled -> mMainHandler.post(() -> {
            mBlurAvailable = Boolean.TRUE.equals(enabled);
            if (mPanelAttached && mPanelView != null) {
                mPanelView.setBackdropBlurAvailable(mBlurAvailable);
            }
        });
        try {
            mWindowManager.addCrossWindowBlurEnabledListener(mBlurEnabledListener);
            mBlurListenerRegistered = true;
        } catch (Throwable t) {
            Log.w(TAG, "addCrossWindowBlurEnabledListener failed", t);
        }
    }

    private void clearNativePanelBlur(WindowManager.LayoutParams params) {
        if (params == null || Build.VERSION.SDK_INT < 31) {
            return;
        }
        try {
            // Window-level blur is full-display on this HyperOS build.  The
            // panel uses BackgroundBlurDrawable instead for a local region.
            params.setBlurBehindRadius(0);
            params.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        } catch (Throwable t) {
            Log.w(TAG, "clear native blur failed", t);
        }
    }

    /**
     * Show the ColorOS TransferDock floating capsule on the right screen edge.
     */
    public void showCapsule(final TransferDockItem item) {
        mMainHandler.post(() -> {
            try {
                hideFloatBar();

                if (mPanelAttached && mPanelView != null) {
                    mPanelView.reloadItems();
                    return;
                }

                if (mCapsuleView == null) {
                    mCapsuleView = new TransferDockCapsuleView(mContext);
                    mCapsuleView.setListeners(
                            () -> {
                                dismissCapsule();
                                openDrawer();
                            },
                            () -> dismissCapsule()
                    );
                }
                mCapsuleView.refreshTheme();

                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                int windowType = resolveWindowType();

                if (mCapsuleParams == null) {
                    mCapsuleParams = new WindowManager.LayoutParams(
                            dp(146), dp(112),
                            windowType,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                            PixelFormat.TRANSLUCENT
                    );
                    if (Build.VERSION.SDK_INT >= 28) {
                        mCapsuleParams.layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                    }
                }

                // Keep the +1 capsule centered on the collapsed FloatBar
                // handle (both use the 25% vertical anchor).
                int handleTop = (int) (dm.heightPixels * 0.25f);
                mCapsuleParams.gravity = Gravity.TOP | Gravity.END;
                // The dock panel sits 8dp off the right edge; the capsule
                // must use the same inset instead of being flush to the edge.
                mCapsuleParams.x = dp(8);
                mCapsuleParams.y = handleTop + (dp(68) - dp(112)) / 2;

                if (!mCapsuleAttached) {
                    mWindowManager.addView(mCapsuleView, mCapsuleParams);
                    mCapsuleAttached = true;
                } else {
                    mWindowManager.updateViewLayout(mCapsuleView, mCapsuleParams);
                }

                int totalCount = TransferDockManager.getInstance(mContext).getItemCount();
                mCapsuleView.showSavedItem(item, totalCount);
                Log.i(TAG, "TransferDock capsule displayed: " + item.getTitle());
            } catch (Throwable t) {
                Log.w(TAG, "showCapsule failed", t);
            }
        });
    }

    /**
     * Dismiss the capsule window. If items remain, show the ColorOS FloatBar edge handle.
     */
    public void dismissCapsule() {
        mMainHandler.post(() -> {
            if (mCapsuleAttached && mCapsuleView != null) {
                try {
                    mWindowManager.removeView(mCapsuleView);
                } catch (Throwable ignored) {
                }
                mCapsuleAttached = false;
            }
            if (!mPanelAttached && TransferDockManager.getInstance(mContext).getItemCount() > 0) {
                showFloatBar();
            }
        });
    }

    /**
     * Show ColorOS Smart Sidebar FloatBar (Edge Handle) on the right edge.
     */
    public void showFloatBar() {
        mMainHandler.post(() -> {
            try {
                if (mPanelAttached || mCapsuleAttached) {
                    return;
                }
                int count = TransferDockManager.getInstance(mContext).getItemCount();
                if (count <= 0) {
                    hideFloatBar();
                    return;
                }

                if (mFloatBarView == null) {
                    mFloatBarView = new TransferDockFloatBarView(mContext);
                    mFloatBarView.setOnHandleClickListener(this::openDrawer);
                }
                mFloatBarView.refreshTheme();

                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                int windowType = resolveWindowType();

                if (mFloatBarParams == null) {
                    mFloatBarParams = new WindowManager.LayoutParams(
                            dp(20), dp(68),
                            windowType,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                            PixelFormat.TRANSLUCENT
                    );
                    if (Build.VERSION.SDK_INT >= 28) {
                        mFloatBarParams.layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                    }
                    mFloatBarParams.gravity = Gravity.TOP | Gravity.END;
                    mFloatBarParams.x = 0;
                    mFloatBarParams.y = (int) (dm.heightPixels * 0.25f);
                }

                if (!mFloatBarAttached) {
                    mWindowManager.addView(mFloatBarView, mFloatBarParams);
                    mFloatBarAttached = true;
                }
                mFloatBarView.setVisibility(View.VISIBLE);
                Log.i(TAG, "TransferDock FloatBar handle displayed");
            } catch (Throwable t) {
                Log.w(TAG, "showFloatBar failed", t);
            }
        });
    }

    /**
     * Hide ColorOS FloatBar edge handle.
     */
    public void hideFloatBar() {
        mMainHandler.post(() -> {
            if (mFloatBarAttached && mFloatBarView != null) {
                try {
                    mWindowManager.removeView(mFloatBarView);
                } catch (Throwable ignored) {
                }
                mFloatBarAttached = false;
            }
        });
    }

    private void ensureScrim() {
        if (mScrimView == null) {
            mScrimView = new View(mContext);
            mScrimView.setOnClickListener(v -> dismissDrawer());
        }
        mScrimView.setBackgroundColor(TransferDockTheme.scrimColor(mContext));
        // A drag can leave the scrim GONE + NOT_TOUCHABLE; every reopen must
        // restore both flags, otherwise the dim layer silently disappears.
        mScrimView.setVisibility(View.VISIBLE);

        if (mScrimParams == null) {
            mScrimParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    resolveWindowType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
            );
            if (Build.VERSION.SDK_INT >= 28) {
                mScrimParams.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            mScrimParams.gravity = Gravity.FILL;
        }
        mScrimParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        if (mScrimAttached) {
            try {
                mWindowManager.updateViewLayout(mScrimView, mScrimParams);
            } catch (Throwable t) {
                Log.w(TAG, "refresh scrim layout failed", t);
            }
        }

        if (!mScrimAttached) {
            mScrimView.setAlpha(0f);
            mWindowManager.addView(mScrimView, mScrimParams);
            mScrimAttached = true;
            mScrimView.animate().alpha(1f).setDuration(260).start();
        } else {
            mScrimView.setVisibility(View.VISIBLE);
            mScrimView.animate().alpha(1f).setDuration(160).start();
        }
    }

    /**
     * Open the TransferDock drawer panel.
     */
    public void openDrawer() {
        mMainHandler.post(() -> {
            try {
                dismissCapsule();
                hideFloatBar();

                if (mPanelView == null) {
                    mPanelView = new TransferDockPanel(mContext);
                    mPanelView.setOnDismissListener(this::removePanelWindows);
                }
                mPanelView.setController(this);
                mPanelView.setOnThemeChangedListener(night -> {
                    if (mScrimView != null) {
                        mScrimView.setBackgroundColor(TransferDockTheme.scrimColor(night));
                    }
                });
                mPanelView.refreshTheme();

                if (!mPanelAttached) {
                    ensureScrim();
                } else if (mScrimView != null) {
                    mScrimView.setVisibility(View.VISIBLE);
                    mScrimView.setAlpha(1f);
                    mScrimView.setBackgroundColor(TransferDockTheme.scrimColor(mContext));
                    if (mScrimParams != null) {
                        mScrimParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                        try {
                            mWindowManager.updateViewLayout(mScrimView, mScrimParams);
                        } catch (Throwable t) {
                            Log.w(TAG, "restore scrim layout failed", t);
                        }
                    }
                }

                mPanelView.setBackdropBlurAvailable(mBlurAvailable);

                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                int panelWidth = mPanelView.getPanelWidthPx();
                int panelHeight = mPanelView.getPanelHeightPx();
                int panelTop = Math.max(dp(24), (dm.heightPixels - panelHeight) / 2);

                int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        // The panel slides mostly off the right edge while a
                        // card is being dragged; allow that negative x offset.
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

                if (mPanelParams == null) {
                    mPanelParams = new WindowManager.LayoutParams(
                            panelWidth, panelHeight,
                            resolveWindowType(),
                            flags,
                            PixelFormat.TRANSLUCENT
                    );
                    if (Build.VERSION.SDK_INT >= 28) {
                        mPanelParams.layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                    }
                } else {
                    mPanelParams.width = panelWidth;
                    mPanelParams.height = panelHeight;
                    mPanelParams.flags = flags;
                }
                mPanelParams.gravity = Gravity.TOP | Gravity.END;
                mPanelRestX = dp(8);
                // Leave a 36dp grab strip on screen: dragging the payload back
                // onto it cancels the send and lets the panel return.
                mPanelDragOutX = mPanelRestX - Math.max(0, panelWidth - dp(36));
                mPanelParams.x = mPanelRestX;
                mPanelParams.y = panelTop;
                clearNativePanelBlur(mPanelParams);

                if (!mPanelAttached) {
                    mWindowManager.addView(mPanelView, mPanelParams);
                    mPanelAttached = true;
                } else {
                    mWindowManager.updateViewLayout(mPanelView, mPanelParams);
                }

                mPanelView.reloadItems();
                mPanelView.showWithAnim();
                Log.i(TAG, "TransferDock drawer panel opened");
            } catch (Throwable t) {
                Log.w(TAG, "openDrawer failed", t);
            }
        });
    }

    /**
     * Called when user starts dragging a card out of the drawer.
     * Both the panel and the scrim become non-touchable so the target app
     * below can receive the drop.
     */
    /**
     * The payload is back over the transfer dock: expand the panel so the
     * user can see where it will land.  The drag itself is not interrupted;
     * moving out again slides the panel aside.
     */
    public void onPanelDragEntered() {
        mMainHandler.post(() -> animatePanelTo(mPanelRestX, 260));
    }

    /**
     * The payload left the transfer dock again: slide the panel aside so it
     * stops covering app targets.  Called repeatedly while the drag crosses
     * the panel edge.
     */
    public void onPanelDragExited() {
        mMainHandler.post(() -> animatePanelTo(mPanelDragOutX, 220));
    }

    public void onPanelDragStarted() {
        notifyTransferDockDragState(true);
        mMainHandler.post(() -> {
            if (mPanelAttached && mPanelView != null && mPanelParams != null) {
                try {
                    // Keep the panel touchable so its own drag listener can
                    // detect the payload coming back.  It is moved almost
                    // entirely off-screen, so it does not cover app targets.
                    mPanelParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                    mWindowManager.updateViewLayout(mPanelView, mPanelParams);
                    animatePanelTo(mPanelDragOutX, 240);
                } catch (Throwable t) {
                    Log.w(TAG, "onPanelDragStarted panel failed", t);
                }
            }
            if (mScrimAttached && mScrimView != null && mScrimParams != null) {
                try {
                    mScrimView.setVisibility(View.GONE);
                    mScrimParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                    mWindowManager.updateViewLayout(mScrimView, mScrimParams);
                } catch (Throwable t) {
                    Log.w(TAG, "onPanelDragStarted scrim failed", t);
                }
            }
            Log.i(TAG, "onPanelDragStarted: panel moved aside, scrim hidden");
        });
    }

    /**
     * Called when the drag ends (either dropped into an app or cancelled).
     */
    public void onPanelDragEnded(boolean success) {
        notifyTransferDockDragState(false);
        mMainHandler.post(() -> {
            Log.i(TAG, "onPanelDragEnded success=" + success);
            if (success) {
                // The drag hid the scrim.  Restore it before the exit
                // animation; otherwise the next open reuses a GONE,
                // non-touchable scrim and the dim layer is missing.
                if (mScrimAttached && mScrimView != null && mScrimParams != null) {
                    try {
                        mScrimParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                        mScrimView.setVisibility(View.VISIBLE);
                        mScrimView.setAlpha(1f);
                        mWindowManager.updateViewLayout(mScrimView, mScrimParams);
                    } catch (Throwable t) {
                        Log.w(TAG, "restore scrim after drag failed", t);
                    }
                }
                dismissDrawer();
                Toast.makeText(mContext, "已拖出发送", Toast.LENGTH_SHORT).show();
            } else {
                if (mPanelAttached && mPanelView != null && mPanelParams != null) {
                    try {
                        mPanelParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                        mWindowManager.updateViewLayout(mPanelView, mPanelParams);
                    } catch (Throwable t) {
                        Log.w(TAG, "restore panel failed", t);
                    }
                    mPanelView.restoreFromDrag();
                    animatePanelTo(mPanelRestX, 300);
                }
                if (mScrimAttached && mScrimView != null && mScrimParams != null) {
                    try {
                        mScrimParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                        mWindowManager.updateViewLayout(mScrimView, mScrimParams);
                        mScrimView.setVisibility(View.VISIBLE);
                        mScrimView.setAlpha(1f);
                    } catch (Throwable t) {
                        Log.w(TAG, "restore scrim failed", t);
                    }
                }
            }
        });
    }

    private void animatePanelTo(int targetX, long duration) {
        if (!mPanelAttached || mPanelView == null || mPanelParams == null) {
            return;
        }
        if (mPanelSlideAnim != null) {
            mPanelSlideAnim.cancel();
            mPanelSlideAnim = null;
        }
        final int startX = mPanelParams.x;
        if (startX == targetX) {
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(startX, targetX);
        animator.setDuration(duration);
        animator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        animator.addUpdateListener(animation -> {
            if (!mPanelAttached || mPanelParams == null) {
                return;
            }
            mPanelParams.x = (Integer) animation.getAnimatedValue();
            try {
                mWindowManager.updateViewLayout(mPanelView, mPanelParams);
            } catch (Throwable t) {
                Log.w(TAG, "panel slide update failed", t);
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (mPanelSlideAnim == animation) {
                    mPanelSlideAnim = null;
                }
            }
        });
        mPanelSlideAnim = animator;
        animator.start();
    }

    /**
     * Start the panel exit animation.  The panel's dismiss callback removes
     * both windows once the slide-out has finished.
     */
    public void dismissDrawer() {
        mMainHandler.post(() -> {
            if (mPanelAttached && mPanelView != null) {
                if (mScrimView != null) {
                    mScrimView.animate().alpha(0f).setDuration(240).start();
                }
                mPanelView.dismissWithAnim();
            } else {
                removePanelWindows();
            }
        });
    }

    private void notifyTransferDockDragState(boolean dragging) {
        TransferDockDragGuard.setDragging(dragging);
        try {
            Intent intent = new Intent(TransferDockDragGuard.ACTION_STATE);
            intent.setPackage("com.android.systemui");
            intent.putExtra("state", dragging ? 1 : 0);
            mContext.sendBroadcast(intent);
        } catch (Throwable t) {
            Log.w(TAG, "notify transfer dock drag state failed", t);
        }
    }

    private void removePanelWindows() {
        notifyTransferDockDragState(false);
        if (mPanelSlideAnim != null) {
            mPanelSlideAnim.cancel();
            mPanelSlideAnim = null;
        }
        if (mPanelAttached && mPanelView != null) {
            try {
                mWindowManager.removeView(mPanelView);
            } catch (Throwable ignored) {
            }
            mPanelAttached = false;
        }
        if (mScrimAttached && mScrimView != null) {
            try {
                mWindowManager.removeView(mScrimView);
            } catch (Throwable ignored) {
            }
            mScrimAttached = false;
        }
        if (mScrimView != null && mScrimParams != null) {
            mScrimParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            mScrimView.setVisibility(View.VISIBLE);
            mScrimView.setAlpha(1f);
        }
        if (TransferDockManager.getInstance(mContext).getItemCount() > 0) {
            showFloatBar();
        } else {
            hideFloatBar();
        }
    }
}
