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
 * of ColorOS TransferDock Capsule and Drawer Panel.
 */
public class TransferDockController {

    private static final String TAG = "TransferDockController";

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private TransferDockCapsuleView mCapsuleView;
    private WindowManager.LayoutParams mCapsuleParams;
    private boolean mCapsuleAttached = false;

    private TransferDockPanel mPanelView;
    private WindowManager.LayoutParams mPanelParams;
    private boolean mPanelAttached = false;

    public TransferDockController(Context context) {
        this.mContext = context.getApplicationContext();
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    private int dp(float value) {
        return (int) (value * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Show the ColorOS TransferDock floating capsule on the right screen edge.
     */
    public void showCapsule(final TransferDockItem item) {
        mMainHandler.post(() -> {
            try {
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
                int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE;

                if (mCapsuleParams == null) {
                    mCapsuleParams = new WindowManager.LayoutParams(
                            dp(166), dp(172),
                            windowType,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                            PixelFormat.TRANSLUCENT
                    );
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
     * Dismiss the capsule window.
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
        });
    }

    /**
     * Open the TransferDock drawer panel.
     */
    public void openDrawer() {
        mMainHandler.post(() -> {
            try {
                dismissCapsule();

                if (mPanelView == null) {
                    mPanelView = new TransferDockPanel(mContext);
                    mPanelView.setOnDismissListener(() -> dismissDrawer());
                }

                int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE;

                if (mPanelParams == null) {
                    mPanelParams = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            windowType,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                            PixelFormat.TRANSLUCENT
                    );
                    mPanelParams.gravity = Gravity.TOP | Gravity.START;
                }

                if (!mPanelAttached) {
                    mWindowManager.addView(mPanelView, mPanelParams);
                    mPanelAttached = true;
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
        });
    }
}
