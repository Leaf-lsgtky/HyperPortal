// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.leaf.hyperportal.R;

import java.io.File;

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

    private ImageView mBgView;
    private RelativeLayout mContentView;
    private TextView mTitleView;
    private ImageView mIconView;

    private RelativeLayout mPreviewLayout;
    private TransferDockStackView mStackView;
    private TextView mBadgeView;

    private ValueAnimator mSlideAnimator;
    private ValueAnimator mPreviewAnimator;

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
        initView();
    }

    public TransferDockCapsuleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDensity = context.getResources().getDisplayMetrics().density;
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        initView();
    }

    private int dp(float value) {
        return (int) (value * mDensity + 0.5f);
    }

    private void initView() {
        setLayoutParams(new LayoutParams(dp(166), dp(172)));

        // 1. Background image (ColorOS recept_panel_right_bg)
        mBgView = new ImageView(getContext());
        mBgView.setScaleType(ImageView.ScaleType.FIT_XY);
        try {
            mBgView.setImageResource(R.drawable.recept_panel_right_bg);
        } catch (Throwable ignored) {
            // Fallback if resource not present
            mBgView.setBackgroundColor(Color.parseColor("#E6202022"));
        }
        LayoutParams bgLp = new LayoutParams(dp(166), dp(172));
        addView(mBgView, bgLp);

        // 2. Content container
        mContentView = new RelativeLayout(getContext());
        LayoutParams contentLp = new LayoutParams(dp(146), dp(112));
        contentLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        contentLp.leftMargin = dp(20);
        addView(mContentView, contentLp);

        // 3. Initial title & icon (ColorOS layout_transfer_dock_panel_right)
        mTitleView = new TextView(getContext());
        mTitleView.setId(View.generateViewId());
        mTitleView.setText("中转站");
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mTitleView.setTextColor(Color.WHITE);
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
        try {
            mIconView.setImageResource(R.drawable.ic_file_dock);
        } catch (Throwable ignored) {
        }
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
        try {
            mBadgeView.setBackgroundResource(R.drawable.shape_transfer_count_group_bg);
        } catch (Throwable ignored) {
            mBadgeView.setBackgroundColor(Color.parseColor("#3696FE"));
        }
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
            cardText.setTextColor(Color.WHITE);
            cardText.setMaxLines(3);
            cardText.setEllipsize(TextUtils.TruncateAt.END);
            cardText.setPadding(dp(8), dp(8), dp(8), dp(8));
            cardText.setBackgroundColor(Color.parseColor("#383838"));
            mStackView.setTopCustomView(cardText, totalCount);
        }

        mBadgeView.setText("+1");

        // Slide in from right (350ms, ease)
        setTranslationX(dp(166));
        setVisibility(View.VISIBLE);

        if (mSlideAnimator != null && mSlideAnimator.isRunning()) {
            mSlideAnimator.cancel();
        }
        mSlideAnimator = ValueAnimator.ofFloat(dp(166), 0f);
        mSlideAnimator.setDuration(350);
        mSlideAnimator.setInterpolator(sEaseInterpolator);
        mSlideAnimator.addUpdateListener(animation -> {
            setTranslationX((Float) animation.getAnimatedValue());
        });
        mSlideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Play preview scale animation (200ms)
                playPreviewAnim();
                // Schedule auto-dismiss after 4000ms
                mHandler.postDelayed(mAutoDismissRunnable, 4000);
            }
        });
        mSlideAnimator.start();
    }

    private void playPreviewAnim() {
        mPreviewLayout.setVisibility(View.VISIBLE);
        mPreviewLayout.setScaleX(0.9f);
        mPreviewLayout.setScaleY(0.9f);
        mPreviewLayout.setAlpha(0f);

        if (mPreviewAnimator != null && mPreviewAnimator.isRunning()) {
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
        if (mSlideAnimator != null && mSlideAnimator.isRunning()) {
            mSlideAnimator.cancel();
        }
        mSlideAnimator = ValueAnimator.ofFloat(getTranslationX(), dp(166));
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
                    dismissWithAnim();
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
