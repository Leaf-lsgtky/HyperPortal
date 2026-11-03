// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
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

import java.util.List;

/**
 * ColorOS 17 TransferDock panel (right side drawer).
 *
 * Reference: com.oplus.smartsidebar 17.9.12 / TransferDockPanel.kt and
 * res/layout/layout_transfer_panel (y-.xml):
 * - panel width 146dp, panel height 578dp
 * - title bar 40dp with 8dp top margin; 14sp title
 * - card 122dp x 84dp, 17dp radius, 8dp bottom margin
 * - clear footer 72dp, 114dp x 40dp pill
 *
 * The view itself is hosted in a bounded WindowManager window by
 * {@link TransferDockController}.  That lets the compositor blur only the
 * rectangle behind the panel instead of blurring the whole screen.
 */
public class TransferDockPanel extends FrameLayout {

    private static final String TAG = "TransferDockPanel";

    public interface OnPanelDismissListener {
        void onPanelDismiss();
    }

    public interface OnThemeChangedListener {
        void onThemeChanged(boolean night);
    }

    private static final PathInterpolator sEaseInterpolator =
            new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);

    private static final int PANEL_WIDTH_DP = 146;
    private static final int PANEL_HEIGHT_DP = 578;
    private static final int HEADER_HEIGHT_DP = 40;
    private static final int FOOTER_HEIGHT_DP = 72;
    private static final int CARD_WIDTH_DP = 122;
    private static final int CARD_HEIGHT_DP = 84;
    private static final int CARD_RADIUS_DP = 17;
    private static final int PANEL_RADIUS_DP = 20;

    private final float mDensity;
    private final int mPanelHeightPx;

    private LinearLayout mDrawerLayout;
    private RelativeLayout mHeaderBar;
    private TextView mTitleView;
    private ImageView mBackBtn;
    private ImageView mMoreBtn;
    private FrameLayout mContentContainer;
    private LinearLayout mEmptyView;
    private ImageView mEmptyIcon;
    private TextView mEmptyTitle;
    private TextView mEmptyHint;
    private ScrollView mScrollView;
    private LinearLayout mCardContainer;
    private FrameLayout mFooter;
    private TextView mClearBtn;

    private ValueAnimator mAnim;
    private OnPanelDismissListener mDismissListener;
    private OnThemeChangedListener mThemeListener;
    private TransferDockController mController;
    private TransferDockTheme mTheme;
    private boolean mBlurAvailable;
    private boolean mIsDismissing = false;
    private boolean mLastNotifiedNight;

    public TransferDockPanel(@NonNull Context context) {
        this(context, null);
    }

    public TransferDockPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDensity = context.getResources().getDisplayMetrics().density;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mPanelHeightPx = Math.min(dp(PANEL_HEIGHT_DP), Math.max(dp(360), dm.heightPixels - dp(96)));
        mTheme = TransferDockTheme.resolve(context, false);
        mLastNotifiedNight = mTheme.night;
        init();
    }

    public void setController(TransferDockController controller) {
        this.mController = controller;
    }

    public void setOnDismissListener(OnPanelDismissListener listener) {
        this.mDismissListener = listener;
    }

    public void setOnThemeChangedListener(OnThemeChangedListener listener) {
        this.mThemeListener = listener;
    }

    public int getPanelHeightPx() {
        return mPanelHeightPx;
    }

    public int getPanelWidthPx() {
        return dp(PANEL_WIDTH_DP);
    }

    private int dp(float value) {
        return (int) (value * mDensity + 0.5f);
    }

    private void init() {
        setClipChildren(false);
        setBackgroundColor(Color.TRANSPARENT);

        // 1. The panel itself.  The window is exactly this size, so the
        // background blur applied by TransferDockController is local.
        mDrawerLayout = new LinearLayout(getContext());
        mDrawerLayout.setOrientation(LinearLayout.VERTICAL);
        mDrawerLayout.setClipToOutline(true);
        mDrawerLayout.setBackground(buildPanelBackground());
        addView(mDrawerLayout, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 2. Header: back / centered title / overflow, matching layout_transfer_panel.
        mHeaderBar = new RelativeLayout(getContext());
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(HEADER_HEIGHT_DP));
        headerLp.topMargin = dp(8);
        mDrawerLayout.addView(mHeaderBar, headerLp);

        mBackBtn = new ImageView(getContext());
        mBackBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mBackBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        mBackBtn.setContentDescription("关闭");
        mBackBtn.setOnClickListener(v -> dismissWithAnim());
        RelativeLayout.LayoutParams backLp = new RelativeLayout.LayoutParams(dp(40), dp(40));
        backLp.addRule(RelativeLayout.ALIGN_PARENT_START);
        backLp.addRule(RelativeLayout.CENTER_VERTICAL);
        backLp.leftMargin = dp(4);
        mHeaderBar.addView(mBackBtn, backLp);

        mTitleView = new TextView(getContext());
        mTitleView.setText("中转站");
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mTitleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mTitleView.setGravity(Gravity.CENTER);
        mTitleView.setSingleLine(true);
        mTitleView.setIncludeFontPadding(false);
        RelativeLayout.LayoutParams titleLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        titleLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mHeaderBar.addView(mTitleView, titleLp);

        mMoreBtn = new ImageView(getContext());
        mMoreBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mMoreBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        mMoreBtn.setContentDescription("更多");
        mMoreBtn.setOnClickListener(v -> showOverflowMenu());
        RelativeLayout.LayoutParams moreLp = new RelativeLayout.LayoutParams(dp(40), dp(40));
        moreLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        moreLp.addRule(RelativeLayout.CENTER_VERTICAL);
        moreLp.rightMargin = dp(4);
        mHeaderBar.addView(mMoreBtn, moreLp);

        // 3. Scrollable list.
        mContentContainer = new FrameLayout(getContext());
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        mDrawerLayout.addView(mContentContainer, contentLp);

        mEmptyView = new LinearLayout(getContext());
        mEmptyView.setOrientation(LinearLayout.VERTICAL);
        mEmptyView.setGravity(Gravity.CENTER);
        mEmptyView.setPadding(dp(12), dp(20), dp(12), dp(20));

        mEmptyIcon = new ImageView(getContext());
        mEmptyIcon.setImageDrawable(TransferDockRes.getFileDockIcon(getContext()));
        mEmptyView.addView(mEmptyIcon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        mEmptyTitle = new TextView(getContext());
        mEmptyTitle.setText("暂无内容");
        mEmptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mEmptyTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyTitleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyTitleLp.topMargin = dp(8);
        mEmptyView.addView(mEmptyTitle, emptyTitleLp);

        mEmptyHint = new TextView(getContext());
        mEmptyHint.setText("拖入图文随时存");
        mEmptyHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        mEmptyHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyHintLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyHintLp.topMargin = dp(3);
        mEmptyView.addView(mEmptyHint, emptyHintLp);

        mContentContainer.addView(mEmptyView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mScrollView = new ScrollView(getContext());
        mScrollView.setVerticalScrollBarEnabled(false);
        mScrollView.setOverScrollMode(OVER_SCROLL_NEVER);
        mScrollView.setClipToPadding(false);
        mScrollView.setPadding(dp(8), dp(6), dp(8), dp(6));

        mCardContainer = new LinearLayout(getContext());
        mCardContainer.setOrientation(LinearLayout.VERTICAL);
        mCardContainer.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        mScrollView.addView(mCardContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        mContentContainer.addView(mScrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 4. Bottom clear button: 72dp footer, 114dp x 40dp pill.
        mFooter = new FrameLayout(getContext());
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(FOOTER_HEIGHT_DP));
        mDrawerLayout.addView(mFooter, footerLp);

        mClearBtn = new TextView(getContext());
        mClearBtn.setText("清空");
        mClearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mClearBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mClearBtn.setGravity(Gravity.CENTER);
        mClearBtn.setIncludeFontPadding(false);
        mClearBtn.setOnClickListener(v -> clearTransferDock());
        FrameLayout.LayoutParams clearLp = new FrameLayout.LayoutParams(dp(114), dp(40));
        clearLp.gravity = Gravity.CENTER;
        mFooter.addView(mClearBtn, clearLp);

        View footerDivider = new View(getContext());
        FrameLayout.LayoutParams dividerLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(0.5f)));
        dividerLp.gravity = Gravity.TOP;
        mFooter.addView(footerDivider, dividerLp);
        mFooter.setTag(footerDivider);

        // 5. Drag listener: MUST return true on ACTION_DRAG_STARTED to receive ACTION_DRAG_ENDED.
        setOnDragListener((v, event) -> {
            int action = event.getAction();
            if (action == DragEvent.ACTION_DRAG_STARTED) {
                return true;
            }
            if (action == DragEvent.ACTION_DRAG_ENDED) {
                boolean success = event.getResult();
                Log.i(TAG, "ACTION_DRAG_ENDED received in TransferDockPanel, success=" + success);
                if (mController != null) {
                    mController.onPanelDragEnded(success);
                } else {
                    restoreFromDrag();
                }
                return true;
            }
            return false;
        });

        applyTheme();
        reloadItems();
    }

    private Drawable buildPanelBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(mTheme.panelFill);
        bg.setCornerRadius(dp(PANEL_RADIUS_DP));
        bg.setStroke(Math.max(1, dp(0.6f)), mTheme.panelStroke);
        return bg;
    }

    private Drawable buildCardBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(mTheme.cardBackground);
        bg.setCornerRadius(dp(CARD_RADIUS_DP));
        bg.setStroke(Math.max(1, dp(0.5f)), mTheme.cardStroke);
        return bg;
    }

    private Drawable buildCardOutline() {
        GradientDrawable outline = new GradientDrawable();
        outline.setColor(Color.TRANSPARENT);
        outline.setCornerRadius(dp(CARD_RADIUS_DP));
        outline.setStroke(Math.max(1, dp(0.6f)), mTheme.cardStroke);
        return outline;
    }

    private Drawable buildIconBackground() {
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(mTheme.pressedOverlay);
        pressed.setCornerRadius(dp(20));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, new GradientDrawable());
        return states;
    }

    private Drawable buildFooterBackground() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(mTheme.footerBackground);
        shape.setCornerRadius(dp(20));
        shape.setStroke(Math.max(1, dp(0.5f)), mTheme.footerStroke);
        return new RippleDrawable(ColorStateList.valueOf(mTheme.pressedOverlay), shape, null);
    }

    private void applyTheme() {
        mTheme = TransferDockTheme.resolve(getContext(), mBlurAvailable);

        mDrawerLayout.setBackground(buildPanelBackground());
        mTitleView.setTextColor(mTheme.primaryText);
        mBackBtn.setImageDrawable(TransferDockRes.getBackIcon(
                getContext(), mDensity, mTheme.primaryText));
        mMoreBtn.setImageDrawable(TransferDockRes.getMoreIcon(
                getContext(), mDensity, mTheme.primaryText));
        mBackBtn.setBackground(buildIconBackground());
        mMoreBtn.setBackground(buildIconBackground());

        mEmptyIcon.setColorFilter(mTheme.emptyIcon);
        mEmptyTitle.setTextColor(mTheme.secondaryText);
        mEmptyHint.setTextColor(mTheme.secondaryText);
        mEmptyHint.setAlpha(0.72f);

        mClearBtn.setTextColor(mTheme.footerText);
        mClearBtn.setBackground(buildFooterBackground());

        Object tag = mFooter.getTag();
        if (tag instanceof View) {
            ((View) tag).setBackgroundColor(mTheme.divider);
        }

        if (mThemeListener != null && mLastNotifiedNight != mTheme.night) {
            mLastNotifiedNight = mTheme.night;
            mThemeListener.onThemeChanged(mTheme.night);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyTheme();
        reloadItems();
    }

    /**
     * Called by the controller when the compositor blur state changes.
     * The panel keeps a readable translucent fallback when blur is disabled.
     */
    /**
     * Re-resolve the system night mode.  A detached View does not receive
     * onConfigurationChanged(), so the controller calls this before every open.
     */
    public void refreshTheme() {
        applyTheme();
        reloadItems();
    }

    public void setBackdropBlurAvailable(boolean available) {
        if (mBlurAvailable == available) {
            return;
        }
        mBlurAvailable = available;
        applyTheme();
        reloadItems();
    }

    public void restoreFromDrag() {
        mDrawerLayout.animate().cancel();
        mDrawerLayout.animate().alpha(1.0f).setDuration(180).start();
    }

    public void reloadItems() {
        TransferDockManager mgr = TransferDockManager.getInstance(getContext());
        List<TransferDockItem> list = mgr.getItems();
        mCardContainer.removeAllViews();

        boolean empty = list.isEmpty();
        mEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        mScrollView.setVisibility(empty ? View.GONE : View.VISIBLE);
        mFooter.setVisibility(empty ? View.GONE : View.VISIBLE);
        mMoreBtn.setEnabled(!empty);
        mMoreBtn.setAlpha(empty ? 0.42f : 1.0f);

        if (empty) {
            return;
        }

        for (TransferDockItem item : list) {
            mCardContainer.addView(createCardView(item));
        }
        mScrollView.scrollTo(0, 0);
    }

    private View createCardView(TransferDockItem item) {
        FrameLayout card = new FrameLayout(getContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                dp(CARD_WIDTH_DP), dp(CARD_HEIGHT_DP));
        cardLp.gravity = Gravity.CENTER_HORIZONTAL;
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);
        card.setBackground(buildCardBackground());
        card.setForeground(buildCardOutline());
        card.setClipToOutline(true);
        card.setContentDescription(item.getTitle());

        if (item.getType() == TransferDockItem.TYPE_IMAGE && item.getThumbnailPath() != null) {
            ImageView thumbView = new ImageView(getContext());
            thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bmp = BitmapFactory.decodeFile(item.getThumbnailPath());
            if (bmp != null) {
                thumbView.setImageBitmap(bmp);
            }
            card.addView(thumbView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        } else {
            LinearLayout textLayout = new LinearLayout(getContext());
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setPadding(dp(10), dp(9), dp(10), dp(8));

            TextView titleView = new TextView(getContext());
            titleView.setText(item.getTitle());
            titleView.setTextColor(mTheme.cardText);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            titleView.setMaxLines(3);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setLineSpacing(dp(1.5f), 1.0f);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            textLayout.addView(titleView, titleLp);

            TextView timeView = new TextView(getContext());
            timeView.setText(item.getDetail());
            timeView.setTextColor(mTheme.cardSecondaryText);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            timeView.setSingleLine(true);
            timeView.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textLayout.addView(timeView, timeLp);

            card.addView(textLayout, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        card.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().cancel();
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            } else if (action == android.view.MotionEvent.ACTION_UP
                    || action == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().cancel();
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }
            return false;
        });

        card.setOnLongClickListener(v -> {
            v.animate().cancel();
            v.setScaleX(1.0f);
            v.setScaleY(1.0f);
            startDragOut(card, item);
            return true;
        });

        card.setOnClickListener(v -> showItemOptions(item));
        return card;
    }

    private void startDragOut(View cardView, TransferDockItem item) {
        cardView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        ClipData clipData;
        if (item.getType() == TransferDockItem.TYPE_IMAGE) {
            Uri uri = TransferDockManager.getInstance(getContext()).exportItemUri(item);
            if (uri != null) {
                String[] mimeTypes = new String[]{"image/png", "image/*", "text/plain"};
                ClipDescription desc = new ClipDescription("TransferDock", mimeTypes);
                ClipData.Item clipItem = new ClipData.Item(item.getTitle(), null, uri);
                clipData = new ClipData(desc, clipItem);
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

            @Override
            public void onDrawShadow(Canvas canvas) {
                float prevAlpha = cardView.getAlpha();
                cardView.setAlpha(1.0f);
                super.onDrawShadow(canvas);
                cardView.setAlpha(prevAlpha);
            }
        };

        mDrawerLayout.animate().cancel();
        mDrawerLayout.setAlpha(0.12f);

        if (mController != null) {
            mController.onPanelDragStarted();
        }

        int flags = View.DRAG_FLAG_GLOBAL | View.DRAG_FLAG_GLOBAL_URI_READ;
        boolean started = cardView.startDragAndDrop(clipData, shadow, item, flags);
        if (!started) {
            if (mController != null) {
                mController.onPanelDragEnded(false);
            } else {
                restoreFromDrag();
            }
        }
    }

    private void copyItem(TransferDockItem item) {
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (item.getType() == TransferDockItem.TYPE_IMAGE) {
            Uri uri = TransferDockManager.getInstance(getContext()).exportItemUri(item);
            if (uri != null) {
                cm.setPrimaryClip(ClipData.newUri(getContext().getContentResolver(), "image", uri));
                Toast.makeText(getContext(), "图片已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String t = item.getText() != null ? item.getText() : item.getTitle();
        cm.setPrimaryClip(ClipData.newPlainText("TransferDock", t));
        Toast.makeText(getContext(), "文本已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private void showItemOptions(TransferDockItem item) {
        try {
            String[] options = new String[]{"复制", "分享", "删除"};
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
            builder.setTitle(item.getTitle());
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    copyItem(item);
                } else if (which == 1) {
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
                        shareIntent.putExtra(Intent.EXTRA_TEXT,
                                item.getText() != null ? item.getText() : item.getTitle());
                    }
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Intent chooser = Intent.createChooser(shareIntent, "分享到");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(chooser);
                    dismissWithAnim();
                } else if (which == 2) {
                    TransferDockManager.getInstance(getContext()).removeItem(item.getId());
                    reloadItems();
                    Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("取消", null);
            android.app.AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(resolveDialogWindowType());
            }
            dialog.show();
        } catch (Throwable t) {
            Log.w(TAG, "showItemOptions dialog failed, fallback to copy", t);
            copyItem(item);
        }
    }

    private void showOverflowMenu() {
        if (TransferDockManager.getInstance(getContext()).getItemCount() == 0) {
            return;
        }
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
            builder.setTitle("中转站");
            builder.setItems(new String[]{"清空中转站"}, (dialog, which) -> clearTransferDock());
            builder.setNegativeButton("取消", null);
            android.app.AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(resolveDialogWindowType());
            }
            dialog.show();
        } catch (Throwable t) {
            Log.w(TAG, "showOverflowMenu failed", t);
            clearTransferDock();
        }
    }

    private int resolveDialogWindowType() {
        if ("com.android.systemui".equals(getContext().getPackageName())) {
            return 2024; // TYPE_NAVIGATION_BAR_PANEL
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void clearTransferDock() {
        TransferDockManager.getInstance(getContext()).clearAll();
        reloadItems();
        Toast.makeText(getContext(), "中转站已清空", Toast.LENGTH_SHORT).show();
        if (mController != null) {
            mController.hideFloatBar();
        }
    }

    public void showWithAnim() {
        mIsDismissing = false;
        if (mAnim != null) {
            mAnim.removeAllListeners();
            mAnim.cancel();
        }
        mDrawerLayout.animate().cancel();
        mDrawerLayout.setAlpha(1.0f);
        mDrawerLayout.setTranslationX(0f);
        mDrawerLayout.setPivotX(getPanelWidthPx());
        mDrawerLayout.setPivotY(mPanelHeightPx / 2f);
        mDrawerLayout.setScaleX(0.94f);
        mDrawerLayout.setScaleY(0.97f);
        setVisibility(View.VISIBLE);

        // COUI reveal is a spring-like growth anchored to the right edge.
        mAnim = ValueAnimator.ofFloat(0f, 1f);
        mAnim.setDuration(280);
        mAnim.setInterpolator(sEaseInterpolator);
        mAnim.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            float scaleX = 0.94f + 0.06f * frac;
            float scaleY = 0.97f + 0.03f * frac;
            mDrawerLayout.setScaleX(scaleX);
            mDrawerLayout.setScaleY(scaleY);
        });
        mAnim.start();
    }

    public void dismissWithAnim() {
        if (mIsDismissing) return;
        mIsDismissing = true;

        if (mAnim != null) {
            mAnim.removeAllListeners();
            mAnim.cancel();
        }
        mDrawerLayout.animate().cancel();
        final float startScaleX = mDrawerLayout.getScaleX();
        final float startScaleY = mDrawerLayout.getScaleY();
        final float startAlpha = mDrawerLayout.getAlpha();
        mAnim = ValueAnimator.ofFloat(0f, 1f);
        mAnim.setDuration(200);
        mAnim.setInterpolator(sEaseInterpolator);
        mAnim.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            mDrawerLayout.setScaleX(startScaleX - 0.06f * frac);
            mDrawerLayout.setScaleY(startScaleY - 0.03f * frac);
            mDrawerLayout.setAlpha(startAlpha * (1.0f - 0.35f * frac));
        });
        mAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(View.GONE);
                if (mDismissListener != null) {
                    mDismissListener.onPanelDismiss();
                }
            }
        });
        mAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnim != null) {
            mAnim.removeAllListeners();
            mAnim.cancel();
            mAnim = null;
        }
        mDrawerLayout.animate().cancel();
        super.onDetachedFromWindow();
    }
}
