// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.AttachedSurfaceControl;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Samples the compositor luminance behind the edge handle.
 *
 * This mirrors the MIUI security-center sidebar behavior:
 * RegionSamplingImageView -> RegionSamplingHelper -> CompositionSamplingListener.
 * The hidden platform listener is used through reflection so the module can
 * still build against the public SDK.
 */
final class TransferDockRegionSampler {

    interface OnLumaChanged {
        void onLuma(float luma);
    }

    private static final String TAG = "TransferDockRegionSampler";
    private static final long RETRY_MS = 180L;
    private static final int MAX_RETRIES = 6;

    private final View mAnchor;
    private final float mDensity;
    private final OnLumaChanged mCallback;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Rect mSamplingRect = new Rect();

    private Object mListener;
    private Method mRegisterMethod;
    private Method mUnregisterMethod;
    private Consumer<Float> mConsumer;
    private boolean mRegistered;
    private boolean mStarted;
    private int mRetries;
    private boolean mLoggedFailure;

    TransferDockRegionSampler(View anchor, float density, OnLumaChanged callback) {
        mAnchor = anchor;
        mDensity = density;
        mCallback = callback;
    }

    void start() {
        if (mStarted) return;
        mStarted = true;
        mRetries = 0;
        mHandler.post(this::tryRegister);
    }

    void updateBounds() {
        if (mStarted && !mRegistered) {
            mHandler.post(this::tryRegister);
        }
    }

    void stop() {
        mStarted = false;
        mHandler.removeCallbacksAndMessages(null);
        if (mRegistered && mUnregisterMethod != null && mListener != null) {
            try {
                mUnregisterMethod.invoke(null, mListener);
            } catch (Throwable t) {
                Log.w(TAG, "unregister sampling failed", t);
            }
        }
        mRegistered = false;
        mListener = null;
        mRegisterMethod = null;
        mUnregisterMethod = null;
        mConsumer = null;
    }

    private void tryRegister() {
        if (!mStarted || mRegistered) return;
        if (mAnchor.getWidth() <= 0 || mAnchor.getHeight() <= 0
                || !mAnchor.isAttachedToWindow()) {
            retry();
            return;
        }

        try {
            updateSamplingRect();
            SurfaceControl stopLayer = findStopLayer();
            if (stopLayer == null || !stopLayer.isValid()) {
                retry();
                return;
            }

            // MIUI provides a concrete Consumer-based subclass in SystemUI.
            // The AOSP CompositionSamplingListener itself is abstract on this
            // build, so using this plugin class is the supported path.
            Class<?> listenerClass = Class.forName(
                    "com.miui.systemui.plugin.view.PluginCompositionSamplingListener");
            if (mListener == null) {
                Class<?> consumerClass = Consumer.class;
                Constructor<?> constructor = listenerClass.getDeclaredConstructor(
                        Executor.class, consumerClass);
                constructor.setAccessible(true);
                mConsumer = value -> {
                    if (value == null) return;
                    float luma = value;
                    if (luma >= 0f) {
                        mCallback.onLuma(luma);
                    }
                };
                Executor executor = mAnchor.getContext().getMainExecutor();
                mListener = constructor.newInstance(executor, mConsumer);
                mRegisterMethod = listenerClass.getDeclaredMethod(
                        "register", listenerClass, int.class, SurfaceControl.class, Rect.class);
                mRegisterMethod.setAccessible(true);
                mUnregisterMethod = listenerClass.getDeclaredMethod(
                        "unregister", listenerClass);
                mUnregisterMethod.setAccessible(true);
            }

            mRegisterMethod.invoke(null, mListener, Display.DEFAULT_DISPLAY, stopLayer, mSamplingRect);
            mRegistered = true;
            Log.i(TAG, "region sampling registered rect=" + mSamplingRect);
        } catch (Throwable t) {
            if (!mLoggedFailure) {
                mLoggedFailure = true;
                Log.w(TAG, "region sampling unavailable", t);
            }
            retry();
        }
    }

    private void retry() {
        if (!mStarted || mRetries >= MAX_RETRIES) return;
        mRetries++;
        mHandler.postDelayed(this::tryRegister, RETRY_MS);
    }

    private void updateSamplingRect() {
        int[] location = new int[2];
        mAnchor.getLocationOnScreen(location);
        int left = location[0] - dp(24);
        int top = location[1] - dp(20);
        int right = location[0] + mAnchor.getWidth() + dp(8);
        int bottom = location[1] + mAnchor.getHeight() + dp(20);

        DisplayMetrics dm = mAnchor.getResources().getDisplayMetrics();
        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(dm.widthPixels, right);
        bottom = Math.min(dm.heightPixels, bottom);
        mSamplingRect.set(left, top, right, bottom);
    }

    private SurfaceControl findStopLayer() {
        try {
            AttachedSurfaceControl root = mAnchor.getRootSurfaceControl();
            if (root != null) {
                Method getSurfaceControl = root.getClass().getDeclaredMethod("getSurfaceControl");
                getSurfaceControl.setAccessible(true);
                Object surface = getSurfaceControl.invoke(root);
                if (surface instanceof SurfaceControl) {
                    return (SurfaceControl) surface;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            // Match the MIUI security-center helper: View.getViewRootImpl().getSurfaceControl()
            Method getViewRootImpl = View.class.getDeclaredMethod("getViewRootImpl");
            getViewRootImpl.setAccessible(true);
            Object viewRoot = getViewRootImpl.invoke(mAnchor);
            if (viewRoot != null) {
                Method getSurfaceControl = viewRoot.getClass().getDeclaredMethod("getSurfaceControl");
                getSurfaceControl.setAccessible(true);
                Object surface = getSurfaceControl.invoke(viewRoot);
                if (surface instanceof SurfaceControl) {
                    return (SurfaceControl) surface;
                }
            }
        } catch (Throwable t) {
            if (!mLoggedFailure) {
                Log.w(TAG, "surface control lookup failed", t);
            }
        }
        return null;
    }

    private int dp(float value) {
        return (int) (value * mDensity + 0.5f);
    }
}
