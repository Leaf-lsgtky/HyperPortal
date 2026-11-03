// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

/**
 * Manages the presentation, lifecycle, and WindowManager integration
 * of ColorOS TransferDock Capsule, FloatBar Handle, and Drawer Panel.
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

    public TransferDockController(Context context) {
        this.mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
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

    /**
     * Show the ColorOS TransferDock floating capsule on the right screen edge.
     */
    public void showCapsule(final TransferDockItem item) {
        mMainHandler.post(() -> {
            try {
                hideFloatBar();

                if (mPanelAttached && mPanelView != null) {
                    // Drawer already open, simply refresh contents
                    mPanelView.reloadItems();
                    return;
                }

                if (mCapsuleView == null) {
                    mCapsuleView = new TransferDockCapsuleView(mContext);
                    mCapsuleView.setListeners(
                            () -> { // On capsule click
                                dismissCapsule();
                                openDrawer();
                            },
                            () -> { // On capsule dismiss
                                dismissCapsule();
                            }
                    );
                }

                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                int windowType = resolveWindowType();

                if (mCapsuleParams == null) {
                    mCapsuleParams = new WindowManager.LayoutParams(
                            dp(166), dp(172),
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
                    mCapsuleParams.gravity = Gravity.TOP | Gravity.END;
                    mCapsuleParams.x = 0;
                    mCapsuleParams.y = (int) (dm.heightPixels * 0.35f);
                }

                if (!mCapsuleAttached) {
                    mWindowManager.addView(mCapsuleView, mCapsuleParams);
                    mCapsuleAttached = true;
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
                    mFloatBarView.setOnHandleClickListener(() -> {
                        openDrawer();
                    });
                }

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
                    mFloatBarParams.y = (int) (dm.heightPixels * 0.38f);
                }

                if (!mFloatBarAttached) {
                    mWindowManager.addView(mFloatBarView, mFloatBarParams);
                    mFloatBarAttached = true;
                }
                mFloatBarView.setVisibility(android.view.View.VISIBLE);
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
                    mPanelView.setController(this);
                    mPanelView.setOnDismissListener(() -> dismissDrawer());
                } else {
                    mPanelView.setController(this);
                }

                int windowType = resolveWindowType();

                if (mPanelParams == null) {
                    mPanelParams = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            windowType,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                    | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                            PixelFormat.TRANSLUCENT
                    );
                    if (Build.VERSION.SDK_INT >= 28) {
                        mPanelParams.layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                    }
                    mPanelParams.gravity = Gravity.FILL;
                } else {
                    mPanelParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                    mPanelParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                    mPanelParams.gravity = Gravity.FILL;
                }

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
     * Makes the window non-touchable and shrinks to 146dp so underlying apps receive the drop!
     */
    public void onPanelDragStarted() {
        mMainHandler.post(() -> {
            if (mPanelAttached && mPanelView != null && mPanelParams != null) {
                try {
                    mPanelParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                    mPanelParams.width = dp(146);
                    mPanelParams.gravity = Gravity.TOP | Gravity.END;
                    mWindowManager.updateViewLayout(mPanelView, mPanelParams);
                    Log.i(TAG, "onPanelDragStarted: window set to NOT_TOUCHABLE, width=146dp");
                } catch (Throwable t) {
                    Log.w(TAG, "onPanelDragStarted failed", t);
                }
            }
        });
    }

    /**
     * Called when the drag ends (either dropped into an app or cancelled).
     */
    public void onPanelDragEnded(boolean success) {
        mMainHandler.post(() -> {
            Log.i(TAG, "onPanelDragEnded success=" + success);
            if (success) {
                // Drop consumed by target app! Dismiss drawer and return to float bar
                dismissDrawer();
                android.widget.Toast.makeText(mContext, "已拖出发送", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // Drag cancelled or dropped nowhere: restore full screen window and restore drawer
                if (mPanelAttached && mPanelView != null && mPanelParams != null) {
                    try {
                        mPanelParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                        mPanelParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                        mPanelParams.gravity = Gravity.FILL;
                        mWindowManager.updateViewLayout(mPanelView, mPanelParams);
                    } catch (Throwable t) {
                        Log.w(TAG, "restore window failed", t);
                    }
                    mPanelView.restoreFromDrag();
                }
            }
        });
    }

    /**
     * Dismiss the TransferDock drawer panel.
     */
    public void dismissDrawer() {
        mMainHandler.post(() -> {
            if (mPanelAttached && mPanelView != null) {
                try {
                    mWindowManager.removeView(mPanelView);
                } catch (Throwable ignored) {
                }
                mPanelAttached = false;
            }
            if (TransferDockManager.getInstance(mContext).getItemCount() > 0) {
                showFloatBar();
            } else {
                hideFloatBar();
            }
        });
    }
}
