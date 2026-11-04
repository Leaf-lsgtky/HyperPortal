// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;

/**
 * 100% ColorOS TransferDock Capsule View (Right edge floating window).
 * Exactly replicates ColorOS 15 DragReceptionViewHelper specifications:
 * - Enter/Exit translation: 350ms, PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)
 * - Preview scale/alpha: 200ms (0.9f -> 1.0f), PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)
 * - Auto-dismiss: 4000ms
 * - Stack preview & "+1" badge
 */
public class TransferDockCapsuleView extends FrameLayout {

    public interface OnCapsuleClickListener {
        void onCapsuleClick();
    }

    public interface OnCapsuleDismissListener {
        void onCapsuleDismiss();
    }

    // ColorOS COUI ease interpolator: PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)
    private static final PathInterpolator sEaseInterpolator =
            new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final float mDensity;

    private View mBgView;
    private Drawable mLocalBlurDrawable;
    private Method mBlurSetRadius;
    private Method mBlurSetCorner;
    private Method mBlurSetColor;
    private RelativeLayout mContentView;
    private TextView mTitleView;
    private ImageView mIconView;
    private boolean mNight;

    private RelativeLayout mPreviewLayout;
    private TransferDockStackView mStackView;
    private TextView mBadgeView;

    private ValueAnimator mSlideAnimator;
    private ValueAnimator mPreviewAnimator;
    private boolean mHasBeenShown;

    private OnCapsuleClickListener mClickListener;
    private OnCapsuleDismissListener mDismissListener;

    private float mTouchDownX;
    private float mTouchDownY;
    private boolean mIsDragging = false;
    private final int mTouchSlop;

    private final Runnable mAutoDismissRunnable = new Runnable() {
        @Override
        public void run() {
            dismissWithAnim();
        }
    };

    public TransferDockCapsuleView(@NonNull Context context) {
        super(context);
        mDensity = context.getResources().getDisplayMetrics().density;
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mNight = TransferDockTheme.isNight(context);
        initView();
    }

    public TransferDockCapsuleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDensity = context.getResources().getDisplayMetrics().density;
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mNight = TransferDockTheme.isNight(context);
        initView();
    }

    private int dp(float value) {
        return (int) (value * mDensity + 0.5f);
    }

    private void applyCapsuleBackground() {
        if (mBgView == null) {
            return;
        }
        // Match the panel: a translucent tint over a local
        // BackgroundBlurDrawable.  When blur is unavailable, keep the surface
        // readable with the more opaque fallback.
        int alpha = mLocalBlurDrawable != null ? 0xB4 : 0xF2;
        GradientDrawable tint = new GradientDrawable();
        tint.setColor(mNight
                ? Color.argb(alpha, 28, 28, 30)
                : Color.argb(alpha, 242, 242, 242));
        tint.setCornerRadius(dp(20));
        if (mLocalBlurDrawable != null) {
            mBgView.setBackground(new LayerDrawable(
                    new Drawable[]{mLocalBlurDrawable, tint}));
        } else {
            mBgView.setBackground(tint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBgView.post(this::installLocalBlur);
    }

    private void installLocalBlur() {
        if (mLocalBlurDrawable != null || !isAttachedToWindow()) {
            return;
        }
        try {
            Method getViewRootImpl = View.class.getDeclaredMethod("getViewRootImpl");
            getViewRootImpl.setAccessible(true);
            Object viewRoot = getViewRootImpl.invoke(mBgView);
            if (viewRoot == null) {
                throw new IllegalStateException("ViewRootImpl is null");
            }
            Method create = viewRoot.getClass().getMethod("createBackgroundBlurDrawable");
            Object drawable = create.invoke(viewRoot);
            if (!(drawable instanceof Drawable)) {
                throw new IllegalStateException("no BackgroundBlurDrawable");
            }
            mLocalBlurDrawable = (Drawable) drawable;
            mBlurSetRadius = drawable.getClass().getMethod("setBlurRadius", int.class);
            mBlurSetCorner = drawable.getClass().getMethod("setCornerRadius", float.class);
            mBlurSetColor = drawable.getClass().getMethod("setColor", int.class);
            mBlurSetRadius.invoke(mLocalBlurDrawable, dp(28));
            mBlurSetCorner.invoke(mLocalBlurDrawable, (float) dp(20));
            mBlurSetColor.invoke(mLocalBlurDrawable, Color.TRANSPARENT);
            mLocalBlurDrawable.invalidateSelf();
            applyCapsuleBackground();
            android.util.Log.i("TransferDockCapsule", "local BackgroundBlurDrawable installed");
        } catch (Throwable t) {
            mLocalBlurDrawable = null;
            mBlurSetRadius = null;
            mBlurSetCorner = null;
            mBlurSetColor = null;
            applyCapsuleBackground();
            android.util.Log.w("TransferDockCapsule", "local blur unavailable", t);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mLocalBlurDrawable = null;
        mBlurSetRadius = null;
        mBlurSetCorner = null;
        mBlurSetColor = null;
        super.onDetachedFromWindow();
    }

    public void refreshTheme() {
        boolean night = TransferDockTheme.isNight(getContext());
        if (night != mNight) {
            mNight = night;
            applyCapsuleBackground();
            mTitleView.setTextColor(mNight ? Color.WHITE : Color.argb(230, 0, 0, 0));
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshTheme();
    }

    private void initView() {
        // Same width as the transfer dock panel; compact card height so the
        // +1 capsule does not look like a floating page.
        setLayoutParams(new LayoutParams(dp(146), dp(112)));

        // Background layer first, exactly like the panel's mDrawerLayout.
        mBgView = new View(getContext());
        mBgView.setClipToOutline(true);
        LayoutParams bgLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mBgView, bgLp);
        applyCapsuleBackground();

        // Content container
        mContentView = new RelativeLayout(getContext());
        mContentView.setPadding(dp(10), dp(8), dp(10), dp(8));
        LayoutParams contentLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        contentLp.gravity = Gravity.CENTER;
        addView(mContentView, contentLp);

        // 3. Initial title & icon (ColorOS layout_transfer_dock_panel_right)
        mTitleView = new TextView(getContext());
        mTitleView.setId(View.generateViewId());
        mTitleView.setText("中转站");
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mTitleView.setTextColor(mNight ? Color.WHITE : Color.argb(230, 0, 0, 0));
        mTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        mTitleView.setGravity(Gravity.CENTER);

        RelativeLayout.LayoutParams titleLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        titleLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        titleLp.topMargin = dp(14);
        mContentView.addView(mTitleView, titleLp);

        mIconView = new ImageView(getContext());
        mIconView.setId(View.generateViewId());
        mIconView.setImageDrawable(TransferDockRes.getFileDockIcon(getContext()));
        RelativeLayout.LayoutParams iconLp = new RelativeLayout.LayoutParams(dp(42), dp(42));
        iconLp.addRule(RelativeLayout.BELOW, mTitleView.getId());
        iconLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        iconLp.topMargin = dp(8);
        mContentView.addView(mIconView, iconLp);

        // 4. Preview Layout (ColorOS layout_reception_preview)
        mPreviewLayout = new RelativeLayout(getContext());
        mPreviewLayout.setVisibility(View.GONE);
        RelativeLayout.LayoutParams prevLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        mContentView.addView(mPreviewLayout, prevLp);

        // Stack view
        mStackView = new TransferDockStackView(getContext());
        mStackView.setId(View.generateViewId());
        RelativeLayout.LayoutParams stackLp = new RelativeLayout.LayoutParams(dp(72), dp(72));
        stackLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mPreviewLayout.addView(mStackView, stackLp);

        // Badge (+1)
        mBadgeView = new TextView(getContext());
        mBadgeView.setBackground(TransferDockRes.getBadgeBg(getContext(), mDensity));
        mBadgeView.setText("+1");
        mBadgeView.setTextColor(Color.WHITE);
        mBadgeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mBadgeView.setTypeface(Typeface.DEFAULT_BOLD);
        mBadgeView.setPadding(dp(7), dp(2), dp(7), dp(2));
        mBadgeView.setGravity(Gravity.CENTER);

        RelativeLayout.LayoutParams badgeLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        badgeLp.addRule(RelativeLayout.ALIGN_TOP, mStackView.getId());
        badgeLp.addRule(RelativeLayout.ALIGN_END, mStackView.getId());
        badgeLp.topMargin = -dp(6);
        badgeLp.rightMargin = -dp(6);
        mPreviewLayout.addView(mBadgeView, badgeLp);
    }

    public void setListeners(OnCapsuleClickListener clickListener, OnCapsuleDismissListener dismissListener) {
        this.mClickListener = clickListener;
        this.mDismissListener = dismissListener;
    }

    /**
     * Show capsule and animate the saved item.
     */
    public void showSavedItem(TransferDockItem item, int totalCount) {
        mHandler.removeCallbacks(mAutoDismissRunnable);

        // Setup preview content
        if (item.getType() == TransferDockItem.TYPE_IMAGE && item.getThumbnailPath() != null) {
            Bitmap bmp = BitmapFactory.decodeFile(item.getThumbnailPath());
            if (bmp != null) {
                mStackView.setTopBitmap(bmp, totalCount);
            }
        } else {
            // Text card inside stack
            TextView cardText = new TextView(getContext());
            cardText.setText(item.getTitle());
            cardText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            cardText.setTextColor(mNight ? Color.WHITE : Color.argb(230, 0, 0, 0));
            cardText.setMaxLines(3);
            cardText.setEllipsize(TextUtils.TruncateAt.END);
            cardText.setPadding(dp(8), dp(8), dp(8), dp(8));
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(mNight ? Color.parseColor("#383838") : Color.WHITE);
            cardBg.setCornerRadius(dp(11));
            cardText.setBackground(cardBg);
            mStackView.setTopCustomView(cardText, totalCount);
        }

        mBadgeView.setText("+1");

        // A newly attached View is already VISIBLE at translation 0, so the
        // old visibility check skipped the entrance slide on the first item.
        boolean alreadyShowing = mHasBeenShown
                && getVisibility() == View.VISIBLE
                && getTranslationX() < dp(20);

        if (alreadyShowing) {
            // If already on screen, immediately play preview pop animation
            playPreviewAnim();
            mHandler.postDelayed(mAutoDismissRunnable, 4000);
        } else {
            // Slide in from right (350ms, ease)
            mHasBeenShown = true;
            setTranslationX(dp(146));
            setVisibility(View.VISIBLE);
            mTitleView.setAlpha(1f);
            mIconView.setAlpha(1f);
            mPreviewLayout.setVisibility(View.GONE);

            if (mSlideAnimator != null) {
                mSlideAnimator.removeAllListeners();
                mSlideAnimator.cancel();
            }
            mSlideAnimator = ValueAnimator.ofFloat(dp(146), 0f);
            mSlideAnimator.setDuration(350);
            mSlideAnimator.setInterpolator(sEaseInterpolator);
            mSlideAnimator.addUpdateListener(animation -> {
                setTranslationX((Float) animation.getAnimatedValue());
            });
            mSlideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    playPreviewAnim();
                    mHandler.postDelayed(mAutoDismissRunnable, 4000);
                }
            });
            mSlideAnimator.start();
        }
    }

    private void playPreviewAnim() {
        mPreviewLayout.setVisibility(View.VISIBLE);
        mPreviewLayout.setScaleX(0.9f);
        mPreviewLayout.setScaleY(0.9f);
        mPreviewLayout.setAlpha(0f);

        if (mPreviewAnimator != null) {
            mPreviewAnimator.removeAllListeners();
            mPreviewAnimator.cancel();
        }
        mPreviewAnimator = ValueAnimator.ofFloat(0f, 1f);
        mPreviewAnimator.setDuration(200);
        mPreviewAnimator.setInterpolator(sEaseInterpolator);
        mPreviewAnimator.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            float scale = 0.9f + 0.1f * frac;
            mPreviewLayout.setScaleX(scale);
            mPreviewLayout.setScaleY(scale);
            mPreviewLayout.setAlpha(frac);

            // Cross-fade out title and icon
            mTitleView.setAlpha(1f - frac);
            mIconView.setAlpha(1f - frac);
        });
        mPreviewAnimator.start();
    }

    public void dismissWithAnim() {
        mHandler.removeCallbacks(mAutoDismissRunnable);
        if (mSlideAnimator != null) {
            mSlideAnimator.removeAllListeners();
            mSlideAnimator.cancel();
        }
        mSlideAnimator = ValueAnimator.ofFloat(getTranslationX(), dp(146));
        mSlideAnimator.setDuration(350);
        mSlideAnimator.setInterpolator(sEaseInterpolator);
        mSlideAnimator.addUpdateListener(animation -> {
            setTranslationX((Float) animation.getAnimatedValue());
        });
        mSlideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(View.GONE);
                if (mDismissListener != null) {
                    mDismissListener.onCapsuleDismiss();
                }
            }
        });
        mSlideAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getRawX();
                mTouchDownY = event.getRawY();
                mIsDragging = false;
                mHandler.removeCallbacks(mAutoDismissRunnable);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - mTouchDownX;
                float dy = event.getRawY() - mTouchDownY;
                if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
                    mIsDragging = true;
                    // Allow dragging to the right to dismiss
                    if (dx > 0) {
                        setTranslationX(dx);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!mIsDragging) {
                    // Click -> Open Drawer Panel
                    if (mClickListener != null) {
                        mClickListener.onCapsuleClick();
                    }
                } else {
                    float currentTransX = getTranslationX();
                    if (currentTransX > dp(50)) {
                        // Swiped out
                        dismissWithAnim();
                    } else {
                        // Spring back
                        ValueAnimator backAnim = ValueAnimator.ofFloat(currentTransX, 0f);
                        backAnim.setDuration(200);
                        backAnim.setInterpolator(sEaseInterpolator);
                        backAnim.addUpdateListener(a -> setTranslationX((Float) a.getAnimatedValue()));
                        backAnim.start();
                        mHandler.postDelayed(mAutoDismissRunnable, 4000);
                    }
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                mHandler.postDelayed(mAutoDismissRunnable, 4000);
                return true;
        }
        return super.onTouchEvent(event);
    }
}
