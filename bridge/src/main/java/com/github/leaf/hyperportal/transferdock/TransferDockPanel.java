// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.leaf.hyperportal.R;

import java.io.File;
import java.util.List;

/**
 * 100% ColorOS TransferDock Panel Drawer (Right side sliding drawer).
 *
 * Replicates ColorOS Smart Sidebar TransferDock specifications:
 * - Panel width: 146dp (scene_panel_width)
 * - Card width: 122dp (transfer_dock_item_width), height: 84dp (transfer_dock_item_height)
 * - Card corner radius: 16dp (shape_transfer_dock_item_normal_bg)
 * - Title bar height: 40dp (transfer_dock_title_height)
 * - Ease animation: PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)
 * - Full drag-out support: long-press cards to start global drag-and-drop into any app!
 */
public class TransferDockPanel extends FrameLayout {

    public interface OnPanelDismissListener {
        void onPanelDismiss();
    }

    private static final PathInterpolator sEaseInterpolator =
            new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);

    private final float mDensity;
    private View mBackdropView;
    private LinearLayout mDrawerLayout;
    private RelativeLayout mHeaderBar;
    private TextView mTitleView;
    private ImageView mCloseBtn;
    private TextView mClearBtn;
    private FrameLayout mContentContainer;
    private LinearLayout mEmptyView;
    private ScrollView mScrollView;
    private LinearLayout mCardContainer;

    private OnPanelDismissListener mDismissListener;
    private boolean mIsDismissing = false;

    public TransferDockPanel(@NonNull Context context) {
        super(context);
        mDensity = context.getResources().getDisplayMetrics().density;
        init();
    }

    public TransferDockPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDensity = context.getResources().getDisplayMetrics().density;
        init();
    }

    private int dp(float value) {
        return (int) (value * mDensity + 0.5f);
    }

    private void init() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenH = dm.heightPixels;

        // 1. Semi-transparent backdrop for dismissing when tapping outside
        mBackdropView = new View(getContext());
        mBackdropView.setBackgroundColor(Color.parseColor("#4D000000"));
        mBackdropView.setOnClickListener(v -> dismissWithAnim());
        addView(mBackdropView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 2. Right-docked drawer panel (width 146dp)
        int panelHeight = Math.min((int) (screenH * 0.72f), dp(560));
        mDrawerLayout = new LinearLayout(getContext());
        mDrawerLayout.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable drawerBg = new GradientDrawable();
        drawerBg.setColor(Color.parseColor("#F21C1C1E"));
        drawerBg.setCornerRadii(new float[]{
                dp(22), dp(22), // top-left
                0, 0,           // top-right
                0, 0,           // bottom-right
                dp(22), dp(22)  // bottom-left
        });
        drawerBg.setStroke(dp(0.8f), Color.parseColor("#26FFFFFF"));
        mDrawerLayout.setBackground(drawerBg);
        mDrawerLayout.setElevation(dp(16));

        LayoutParams drawerLp = new LayoutParams(dp(146), panelHeight);
        drawerLp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        addView(mDrawerLayout, drawerLp);

        // 3. Header Bar (40dp)
        mHeaderBar = new RelativeLayout(getContext());
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
        mDrawerLayout.addView(mHeaderBar, headerLp);

        // Close button on left
        mCloseBtn = new ImageView(getContext());
        try {
            mCloseBtn.setImageResource(R.drawable.ic_transfer_dock_close);
        } catch (Throwable ignored) {
            mCloseBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
        mCloseBtn.setColorFilter(Color.parseColor("#B0FFFFFF"));
        mCloseBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        RelativeLayout.LayoutParams closeLp = new RelativeLayout.LayoutParams(dp(28), dp(28));
        closeLp.addRule(RelativeLayout.ALIGN_PARENT_START);
        closeLp.addRule(RelativeLayout.CENTER_VERTICAL);
        closeLp.leftMargin = dp(8);
        mCloseBtn.setOnClickListener(v -> dismissWithAnim());
        mHeaderBar.addView(mCloseBtn, closeLp);

        // Title in center
        mTitleView = new TextView(getContext());
        mTitleView.setText("中转站");
        mTitleView.setTextColor(Color.WHITE);
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        RelativeLayout.LayoutParams titleLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        titleLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mHeaderBar.addView(mTitleView, titleLp);

        // Clear button on right
        mClearBtn = new TextView(getContext());
        mClearBtn.setText("清空");
        mClearBtn.setTextColor(Color.parseColor("#80FFFFFF"));
        mClearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        mClearBtn.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams clearLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, dp(28));
        clearLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        clearLp.addRule(RelativeLayout.CENTER_VERTICAL);
        clearLp.rightMargin = dp(8);
        mClearBtn.setPadding(dp(6), dp(4), dp(6), dp(4));
        mClearBtn.setOnClickListener(v -> clearTransferDock());
        mHeaderBar.addView(mClearBtn, clearLp);

        // Divider
        View divider = new View(getContext());
        divider.setBackgroundColor(Color.parseColor("#15FFFFFF"));
        mDrawerLayout.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(0.6f)));

        // 4. Content Area
        mContentContainer = new FrameLayout(getContext());
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        mDrawerLayout.addView(mContentContainer, contentLp);

        // Empty state view
        mEmptyView = new LinearLayout(getContext());
        mEmptyView.setOrientation(LinearLayout.VERTICAL);
        mEmptyView.setGravity(Gravity.CENTER);
        mEmptyView.setPadding(dp(12), dp(20), dp(12), dp(20));

        ImageView emptyIcon = new ImageView(getContext());
        try {
            emptyIcon.setImageResource(R.drawable.ic_file_dock);
        } catch (Throwable ignored) {
            emptyIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        }
        emptyIcon.setColorFilter(Color.parseColor("#40FFFFFF"));
        mEmptyView.addView(emptyIcon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        TextView emptyTitle = new TextView(getContext());
        emptyTitle.setText("暂无内容");
        emptyTitle.setTextColor(Color.parseColor("#75FFFFFF"));
        emptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        emptyTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyTitleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyTitleLp.topMargin = dp(8);
        mEmptyView.addView(emptyTitle, emptyTitleLp);

        TextView emptyHint = new TextView(getContext());
        emptyHint.setText("拖入图文随时存");
        emptyHint.setTextColor(Color.parseColor("#45FFFFFF"));
        emptyHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        emptyHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyHintLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyHintLp.topMargin = dp(3);
        mEmptyView.addView(emptyHint, emptyHintLp);

        mContentContainer.addView(mEmptyView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Scrollable Card List
        mScrollView = new ScrollView(getContext());
        mScrollView.setVerticalScrollBarEnabled(false);
        mScrollView.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
        mScrollView.setPadding(dp(12), dp(8), dp(12), dp(8));
        mScrollView.setClipToPadding(false);

        mCardContainer = new LinearLayout(getContext());
        mCardContainer.setOrientation(LinearLayout.VERTICAL);
        mCardContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        mScrollView.addView(mCardContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        mContentContainer.addView(mScrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Restore drawer alpha when external drag finishes
        setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                mDrawerLayout.animate().alpha(1.0f).setDuration(200).start();
            }
            return false;
        });

        reloadItems();
    }

    public void setOnDismissListener(OnPanelDismissListener listener) {
        this.mDismissListener = listener;
    }

    public void reloadItems() {
        TransferDockManager mgr = TransferDockManager.getInstance(getContext());
        List<TransferDockItem> list = mgr.getItems();
        mCardContainer.removeAllViews();

        if (list.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mScrollView.setVisibility(View.GONE);
            mClearBtn.setVisibility(View.INVISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mScrollView.setVisibility(View.VISIBLE);
            mClearBtn.setVisibility(View.VISIBLE);

            for (TransferDockItem item : list) {
                View card = createCardView(item);
                mCardContainer.addView(card);
            }
        }
    }

    private View createCardView(TransferDockItem item) {
        FrameLayout card = new FrameLayout(getContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(dp(122), dp(84));
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#323235"));
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(0.6f), Color.parseColor("#1FFFFFFF"));
        card.setBackground(cardBg);
        card.setClipToOutline(true);

        if (item.getType() == TransferDockItem.TYPE_IMAGE && item.getThumbnailPath() != null) {
            // Image card
            ImageView thumbView = new ImageView(getContext());
            thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bmp = BitmapFactory.decodeFile(item.getThumbnailPath());
            if (bmp != null) {
                thumbView.setImageBitmap(bmp);
            }
            card.addView(thumbView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            // Bottom gradient overlay with detail
            View gradientOverlay = new View(getContext());
            GradientDrawable overlayGrad = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{Color.parseColor("#B3000000"), Color.TRANSPARENT});
            gradientOverlay.setBackground(overlayGrad);
            FrameLayout.LayoutParams gradLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(26));
            gradLp.gravity = Gravity.BOTTOM;
            card.addView(gradientOverlay, gradLp);

            TextView detailView = new TextView(getContext());
            detailView.setText(item.getDetail());
            detailView.setTextColor(Color.parseColor("#E0FFFFFF"));
            detailView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            detailView.setSingleLine(true);
            detailView.setEllipsize(TextUtils.TruncateAt.END);
            detailView.setPadding(dp(6), 0, dp(6), dp(4));
            FrameLayout.LayoutParams detailLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            detailLp.gravity = Gravity.BOTTOM;
            card.addView(detailView, detailLp);
        } else {
            // Text or file card
            LinearLayout textLayout = new LinearLayout(getContext());
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setPadding(dp(9), dp(8), dp(9), dp(6));

            TextView titleView = new TextView(getContext());
            titleView.setText(item.getTitle());
            titleView.setTextColor(Color.WHITE);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            titleView.setMaxLines(3);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setLineSpacing(dp(1.5f), 1.0f);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            textLayout.addView(titleView, titleLp);

            TextView timeView = new TextView(getContext());
            timeView.setText(item.getDetail());
            timeView.setTextColor(Color.parseColor("#70FFFFFF"));
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            timeView.setSingleLine(true);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textLayout.addView(timeView, timeLp);

            card.addView(textLayout, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        // Long press -> Start drag-and-drop out!
        card.setOnLongClickListener(v -> {
            startDragOut(card, item);
            return true;
        });

        // Click -> Quick options
        card.setOnClickListener(v -> showItemOptions(item));

        return card;
    }

    private void startDragOut(View cardView, TransferDockItem item) {
        cardView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        ClipData clipData;
        if (item.getType() == TransferDockItem.TYPE_IMAGE) {
            Uri uri = TransferDockManager.getInstance(getContext()).exportItemUri(item);
            if (uri != null) {
                clipData = ClipData.newUri(getContext().getContentResolver(), "image", uri);
            } else if (!TextUtils.isEmpty(item.getText())) {
                clipData = ClipData.newPlainText("TransferDock", item.getText());
            } else {
                clipData = ClipData.newPlainText("TransferDock", item.getTitle());
            }
        } else {
            String text = item.getText();
            if (TextUtils.isEmpty(text)) text = item.getTitle();
            clipData = ClipData.newPlainText("TransferDock", text);
        }

        View.DragShadowBuilder shadow = new View.DragShadowBuilder(cardView) {
            @Override
            public void onProvideShadowMetrics(android.graphics.Point outShadowSize,
                                              android.graphics.Point outShadowTouchPoint) {
                outShadowSize.set(cardView.getWidth(), cardView.getHeight());
                outShadowTouchPoint.set(cardView.getWidth() / 2, cardView.getHeight() / 2);
            }
        };

        // Temporarily fade out the drawer during drag so the target app is visible
        mDrawerLayout.animate().alpha(0.08f).setDuration(150).start();

        int flags = View.DRAG_FLAG_GLOBAL | View.DRAG_FLAG_GLOBAL_URI_READ;
        cardView.startDragAndDrop(clipData, shadow, item, flags);
    }

    private void showItemOptions(TransferDockItem item) {
        // Quick popup options
        String[] options = new String[]{"复制", "分享", "删除"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle(item.getTitle());
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { // 复制
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (item.getType() == TransferDockItem.TYPE_IMAGE) {
                    Uri uri = TransferDockManager.getInstance(getContext()).exportItemUri(item);
                    if (uri != null) {
                        cm.setPrimaryClip(ClipData.newUri(getContext().getContentResolver(), "image", uri));
                        Toast.makeText(getContext(), "图片已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String t = item.getText() != null ? item.getText() : item.getTitle();
                    cm.setPrimaryClip(ClipData.newPlainText("TransferDock", t));
                    Toast.makeText(getContext(), "文本已复制到剪贴板", Toast.LENGTH_SHORT).show();
                }
            } else if (which == 1) { // 分享
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (item.getType() == TransferDockItem.TYPE_IMAGE) {
                    Uri uri = TransferDockManager.getInstance(getContext()).exportItemUri(item);
                    if (uri != null) {
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                } else {
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, item.getText() != null ? item.getText() : item.getTitle());
                }
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent chooser = Intent.createChooser(shareIntent, "分享到");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(chooser);
                dismissWithAnim();
            } else if (which == 2) { // 删除
                TransferDockManager.getInstance(getContext()).removeItem(item.getId());
                reloadItems();
                Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }
        dialog.show();
    }

    private void clearTransferDock() {
        TransferDockManager.getInstance(getContext()).clearAll();
        reloadItems();
        Toast.makeText(getContext(), "中转站已清空", Toast.LENGTH_SHORT).show();
    }

    public void showWithAnim() {
        mIsDismissing = false;
        mDrawerLayout.setTranslationX(dp(146));
        mBackdropView.setAlpha(0f);
        setVisibility(View.VISIBLE);

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(300);
        anim.setInterpolator(sEaseInterpolator);
        anim.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            mDrawerLayout.setTranslationX(dp(146) * (1f - frac));
            mBackdropView.setAlpha(frac);
        });
        anim.start();
    }

    public void dismissWithAnim() {
        if (mIsDismissing) return;
        mIsDismissing = true;

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(250);
        anim.setInterpolator(sEaseInterpolator);
        anim.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            mDrawerLayout.setTranslationX(dp(146) * frac);
            mBackdropView.setAlpha(1f - frac);
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(View.GONE);
                if (mDismissListener != null) {
                    mDismissListener.onPanelDismiss();
                }
            }
        });
        anim.start();
    }
}
