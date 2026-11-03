package com.github.leaf.hyperportal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.leaf.hyperportal.target.BuiltInIconDrawable;

import java.util.ArrayList;
import java.util.List;

/**
 * ColorOS 15 Content Portal Dock Bar (Bottom Shelf).
 *
 * Implements 100% decompiled specification from ColorOS Content Portal (17.4.2):
 * - Container height: 153dp (main_app_panel_max_height)
 * - Container padding: 10dp start / 10dp end (dp_10)
 * - Transparent background (NO card plate, directly over 3D tilt void)
 * - Horizontal centering when items fit on screen (CenterLinearLayoutManager parity)
 * - Single item: 138dp height (main_app_layout_height), 60dp step (50dp text + 2*5dp padding)
 * - Icon: 44dp x 44dp, 8dp bottom margin, anchored pivot at bottom center (pivotX=w/2, pivotY=h)
 * - Text: 50dp width, 10dp textSize, #FFF0F0F0, 50% black shadow (#80000000, dy=1dp, radius=2dp), 2 lines
 * - Fisheye magnification: scale = 1.23 - delta*0.13 +- offset*0.13 (max 1.23f)
 * - Upward wave lift: lift = 11.987dp * PathInterpolator(0.3f, 0f, 0.7f, 1f)
 * - Edge auto-scroll: 86dp zoomMargin, 36dp quickMargin, 300ms hover delay, 1.85 / 2.5 ms/px speed
 */
public class ColorOSDockBar extends FrameLayout {

    public interface OnDockItemDropListener {
        void onDockItemDrop(DockItem item);
    }

    public static class DockItem {
        public final String id;
        public final String label;
        public final Drawable icon;
        public final Object tag;

        public DockItem(String id, String label, Drawable icon, Object tag) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            this.tag = tag;
        }
    }

    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;

    private static final int ID_ICON = 0x7f09005d;
    private static final int ID_TEXT = 0x7f090061;

    // ColorOS PathInterpolator(0.3f, 0.0f, 0.7f, 1.0f) from Lv5/k;-><clinit>
    private static final PathInterpolator sPathInterpolator =
            new PathInterpolator(0.3f, 0.0f, 0.7f, 1.0f);

    private final float mDensity;
    private int mOrientation = ORIENTATION_HORIZONTAL;
    private HorizontalScrollView mScrollView;
    private ScrollView mVerticalScrollView;
    private LinearLayout mRow;
    private final List<DockItem> mItems = new ArrayList<>();
    private OnDockItemDropListener mDropListener;

    // Auto-scroll state (from Lv5/m;->D and Lv5/e;)
    private int mZoomMarginDp = 86; // Default 86dp, configurable via settings
    private int mScrollDirection = 0; // -1 for left/up, 1 for right/down, 0 for none
    private boolean mScrollFast = false;
    private long mHoverStartTime = 0L;
    private boolean mAutoScrollRunning = false;
    private long mLastScrollTickTime = 0L;
    private float mLastRawX = -1f;
    private float mLastRawY = -1f;

    // Fisheye bloom animation state (prevents abrupt jumping when dock opens)
    private float mExpansionProgress = 1.0f;
    private android.animation.ValueAnimator mEnterAnimator;
    private float mLastTouchX = -1f;
    private float mLastTouchY = -1f;

    private final Runnable mAutoScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mAutoScrollRunning || mScrollDirection == 0) {
                mAutoScrollRunning = false;
                return;
            }
            long now = SystemClock.uptimeMillis();
            if (now - mHoverStartTime >= 50L) { // 50ms hover delay
                long dt = Math.min(32, Math.max(8, now - mLastScrollTickTime));
                mLastScrollTickTime = now;
                float speedDpPerSec = mScrollFast ? 420f : 200f;
                int step = Math.max(1, Math.round(dp(speedDpPerSec * dt / 1000f)));

                if (mOrientation == ORIENTATION_VERTICAL) {
                    if (mVerticalScrollView != null && mRow != null) {
                        int currentScroll = mVerticalScrollView.getScrollY();
                        int maxScroll = Math.max(0, mRow.getHeight() - mVerticalScrollView.getHeight());
                        if (mScrollDirection < 0) {
                            if (currentScroll <= 0) {
                                stopAutoScroll();
                                return;
                            }
                            int actualStep = Math.min(step, currentScroll);
                            mVerticalScrollView.scrollBy(0, -actualStep);
                        } else if (mScrollDirection > 0) {
                            if (currentScroll >= maxScroll) {
                                stopAutoScroll();
                                return;
                            }
                            int actualStep = Math.min(step, maxScroll - currentScroll);
                            mVerticalScrollView.scrollBy(0, actualStep);
                        }
                    }
                } else {
                    if (mScrollView != null && mRow != null) {
                        int currentScroll = mScrollView.getScrollX();
                        int maxScroll = Math.max(0, mRow.getWidth() - mScrollView.getWidth());
                        if (mScrollDirection < 0) {
                            if (currentScroll <= 0) {
                                stopAutoScroll();
                                return;
                            }
                            int actualStep = Math.min(step, currentScroll);
                            mScrollView.scrollBy(-actualStep, 0);
                        } else if (mScrollDirection > 0) {
                            if (currentScroll >= maxScroll) {
                                stopAutoScroll();
                                return;
                            }
                            int actualStep = Math.min(step, maxScroll - currentScroll);
                            mScrollView.scrollBy(actualStep, 0);
                        }
                    }
                }
            } else {
                mLastScrollTickTime = now;
            }
            if (mAutoScrollRunning) {
                postOnAnimation(this);
            }
        }
    };

    public ColorOSDockBar(@NonNull Context context) {
        this(context, null);
    }

    public ColorOSDockBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDensity = context.getResources().getDisplayMetrics().density;
        initView();
    }

    private int dp(float dpValue) {
        return (int) (dpValue * mDensity + 0.5f);
    }

    public void setOrientation(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            removeAllViews();
            initView();
            setItems(new ArrayList<>(mItems));
        }
    }

    public int getOrientation() {
        return mOrientation;
    }

    private void initView() {
        // Entire dock bar has transparent background (no card, no pill plate)
        setBackground(null);
        setClipChildren(false);
        setClipToPadding(false);

        ViewGroup.LayoutParams existingLp = getLayoutParams();
        if (existingLp instanceof ViewGroup.MarginLayoutParams) {
            existingLp.width = mOrientation == ORIENTATION_VERTICAL ? dp(88) : ViewGroup.LayoutParams.MATCH_PARENT;
            existingLp.height = mOrientation == ORIENTATION_VERTICAL ? ViewGroup.LayoutParams.MATCH_PARENT : dp(153);
            setLayoutParams(existingLp);
        } else {
            setLayoutParams(new FrameLayout.LayoutParams(
                    mOrientation == ORIENTATION_VERTICAL ? dp(88) : FrameLayout.LayoutParams.MATCH_PARENT,
                    mOrientation == ORIENTATION_VERTICAL ? FrameLayout.LayoutParams.MATCH_PARENT : dp(153)));
        }

        if (mOrientation == ORIENTATION_VERTICAL) {
            mVerticalScrollView = new ScrollView(getContext()) {
                @Override
                protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                    super.onSizeChanged(w, h, oldw, oldh);
                    if (h > 0 && mRow != null) {
                        int topBlank = (int) (h * 0.20f);
                        int bottomBlank = (int) (h * 0.20f);
                        if (mRow.getPaddingTop() != topBlank || mRow.getPaddingBottom() != bottomBlank) {
                            mRow.setPadding(0, topBlank, 0, bottomBlank);
                        }
                    }
                }

                @Override
                protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                    super.onScrollChanged(l, t, oldl, oldt);
                    updateFisheyeDuringScroll();
                }
            };
            mVerticalScrollView.setVerticalScrollBarEnabled(false);
            mVerticalScrollView.setOverScrollMode(OVER_SCROLL_NEVER);
            mVerticalScrollView.setFillViewport(false);
            mVerticalScrollView.setClipChildren(false);
            mVerticalScrollView.setClipToPadding(false);

            FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                    dp(88), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END | Gravity.TOP);
            addView(mVerticalScrollView, scrollParams);

            int screenH = getContext().getResources().getDisplayMetrics().heightPixels;
            int topBlank = (int) (screenH * 0.20f);
            int bottomBlank = (int) (screenH * 0.20f);

            mRow = new LinearLayout(getContext());
            mRow.setOrientation(LinearLayout.VERTICAL);
            mRow.setGravity(Gravity.CENTER_HORIZONTAL);
            mRow.setPadding(0, topBlank, 0, bottomBlank);
            mRow.setClipChildren(false);
            mRow.setClipToPadding(false);

            FrameLayout.LayoutParams rowParams = new FrameLayout.LayoutParams(
                    dp(88), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            mVerticalScrollView.addView(mRow, rowParams);
            mScrollView = null;
        } else {
            mScrollView = new HorizontalScrollView(getContext()) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    if (mRow != null) {
                        int scrollW = getMeasuredWidth();
                        int rowW = mRow.getMeasuredWidth();
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mRow.getLayoutParams();
                        int targetGravity = (rowW < scrollW)
                                ? (Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM)
                                : (Gravity.START | Gravity.BOTTOM);
                        if (lp.gravity != targetGravity) {
                            lp.gravity = targetGravity;
                            mRow.setLayoutParams(lp);
                            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                        }
                    }
                }

                @Override
                protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                    super.onScrollChanged(l, t, oldl, oldt);
                    updateFisheyeDuringScroll();
                }
            };
            mScrollView.setHorizontalScrollBarEnabled(false);
            mScrollView.setOverScrollMode(OVER_SCROLL_NEVER);
            mScrollView.setFillViewport(false); // CRITICAL: Do NOT force row to viewport width!
            mScrollView.setClipChildren(false);
            mScrollView.setClipToPadding(false);

            FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(153), Gravity.BOTTOM);
            addView(mScrollView, scrollParams);

            mRow = new LinearLayout(getContext());
            mRow.setOrientation(LinearLayout.HORIZONTAL);
            mRow.setGravity(Gravity.BOTTOM);
            mRow.setPadding(dp(10), 0, dp(10), 0); // paddingStart=10dp, paddingEnd=10dp
            mRow.setClipChildren(false);
            mRow.setClipToPadding(false);

            FrameLayout.LayoutParams rowParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, dp(153), Gravity.START | Gravity.BOTTOM);
            mScrollView.addView(mRow, rowParams);
            mVerticalScrollView = null;
        }
    }

    public void setOnDockItemDropListener(OnDockItemDropListener listener) {
        this.mDropListener = listener;
    }

    public void setEdgeScrollMarginDp(int marginDp) {
        if (marginDp >= 30 && marginDp <= 200) {
            this.mZoomMarginDp = marginDp;
        }
    }

    public void setItems(List<DockItem> items) {
        mItems.clear();
        mRow.removeAllViews();
        if (items != null) {
            mItems.addAll(items);
            for (DockItem item : mItems) {
                mRow.addView(createItemView(item));
            }
        }
        if (mScrollView != null) {
            mScrollView.scrollTo(0, 0);
        }
        if (mVerticalScrollView != null) {
            mVerticalScrollView.scrollTo(0, 0);
        }
        stopAutoScroll();
    }

    private View createItemView(DockItem item) {
        if (mOrientation == ORIENTATION_VERTICAL) {
            // Vertical dock item: width = 88dp, height = 74dp
            FrameLayout itemLayout = new FrameLayout(getContext());
            itemLayout.setTag(item);
            itemLayout.setClipChildren(false);
            itemLayout.setClipToPadding(false);
            itemLayout.setPadding(0, dp(6), 0, dp(6));

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    dp(88), dp(72));
            itemLayout.setLayoutParams(itemParams);

            LinearLayout col = new LinearLayout(getContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            col.setClipChildren(false);
            col.setClipToPadding(false);

            FrameLayout.LayoutParams colParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            itemLayout.addView(col, colParams);

            ImageView iconView = new ImageView(getContext());
            iconView.setId(ID_ICON);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconView.setImageDrawable(resolveIcon(item));

            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            iconParams.bottomMargin = dp(4);
            iconParams.gravity = Gravity.CENTER_HORIZONTAL;
            col.addView(iconView, iconParams);

            TextView labelView = new TextView(getContext());
            labelView.setId(ID_TEXT);
            labelView.setText(item.label);
            labelView.setTextColor(Color.parseColor("#FFF0F0F0"));
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            labelView.setShadowLayer(dp(2), 0, dp(1), Color.parseColor("#80000000"));
            labelView.setMaxLines(1);
            labelView.setGravity(Gravity.CENTER_HORIZONTAL);
            labelView.setEllipsize(TextUtils.TruncateAt.END);

            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    dp(72), ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams.gravity = Gravity.CENTER_HORIZONTAL;
            col.addView(labelView, textParams);

            return itemLayout;
        }

        // Horizontal dock item: height = 138dp (main_app_layout_height), width = wrap_content
        FrameLayout itemLayout = new FrameLayout(getContext());
        itemLayout.setTag(item);
        itemLayout.setClipChildren(false);
        itemLayout.setClipToPadding(false);
        itemLayout.setPadding(dp(5), 0, dp(5), 0); // paddingStart=5dp, paddingEnd=5dp

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(138));
        itemLayout.setLayoutParams(itemParams);

        // Vertical column inside itemLayout to align icon and text from bottom up
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        col.setClipChildren(false);
        col.setClipToPadding(false);

        FrameLayout.LayoutParams colParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        itemLayout.addView(col, colParams);

        // 1. ImageView: 44dp x 44dp, marginBottom=8dp
        ImageView iconView = new ImageView(getContext());
        iconView.setId(ID_ICON);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setImageDrawable(resolveIcon(item));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.bottomMargin = dp(8);
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(iconView, iconParams);

        // 2. TextView: width=50dp, textSize=10dp, #FFF0F0F0, shadow 50% black, lines=2
        TextView labelView = new TextView(getContext());
        labelView.setId(ID_TEXT);
        labelView.setText(item.label);
        labelView.setTextColor(Color.parseColor("#FFF0F0F0"));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        labelView.setShadowLayer(dp(2), 0, dp(1), Color.parseColor("#80000000"));
        labelView.setLines(2);
        labelView.setMaxLines(2);
        labelView.setGravity(Gravity.CENTER_HORIZONTAL);
        labelView.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                dp(50), ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.bottomMargin = dp(2);
        textParams.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(labelView, textParams);

        return itemLayout;
    }

    private Drawable resolveIcon(DockItem item) {
        Drawable iconDrawable = item.icon;
        if (iconDrawable == null) {
            if (item.id != null) {
                iconDrawable = BuiltInIconDrawable.create(item.id);
            }
            if (iconDrawable == null) {
                if (item.label != null && item.label.contains("中转站")) {
                    iconDrawable = BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_TRANSFER_DOCK);
                } else if (item.label != null && (item.label.contains("存") || item.label.contains("保"))) {
                    iconDrawable = BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SAVE);
                } else if (item.label != null && item.label.contains("分词")) {
                    iconDrawable = BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SEGMENT);
                } else if (item.label != null && item.label.contains("搜")) {
                    iconDrawable = BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SEARCH);
                } else if (item.label != null && item.label.contains("分享")) {
                    iconDrawable = BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_SHARE);
                } else {
                    iconDrawable = BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_COPY);
                }
            }
        }
        return iconDrawable;
    }

    public void resetScroll() {
        if (mVerticalScrollView != null) {
            mVerticalScrollView.scrollTo(0, 0);
        }
        if (mScrollView != null) {
            mScrollView.scrollTo(0, 0);
        }
        stopAutoScroll();
    }

    private void updateFisheyeDuringScroll() {
        if (mLastRawX < 0 || mLastRawY < 0 || mRow == null || mRow.getChildCount() == 0) {
            return;
        }
        int[] rowScreenLoc = new int[2];
        mRow.getLocationOnScreen(rowScreenLoc);
        float rowTouchX = mLastRawX - rowScreenLoc[0];
        float rowTouchY = mLastRawY - rowScreenLoc[1];
        mLastTouchX = rowTouchX;
        mLastTouchY = rowTouchY;
        applyFisheye(rowTouchX, rowTouchY);
    }

    /**
     * Starts smooth blooming animation from flat icons (1.0) to full fisheye wave.
     * Prevents abrupt scale jumping when the dock bar slides onto screen.
     */
    public void startEnterAnimation() {
        startEnterAnimation(-1f, -1f);
    }

    public void startEnterAnimation(float rawX, float rawY) {
        if (mEnterAnimator != null) {
            mEnterAnimator.cancel();
        }
        mExpansionProgress = 0f;
        resetMagnification(false);
        if (rawX >= 0 && mRow != null) {
            int[] rowScreenLoc = new int[2];
            mRow.getLocationOnScreen(rowScreenLoc);
            mLastTouchX = rawX - rowScreenLoc[0];
            mLastTouchY = rawY - rowScreenLoc[1];
            mLastRawX = rawX;
            mLastRawY = rawY;
        }
        mEnterAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        mEnterAnimator.setDuration(260);
        mEnterAnimator.setInterpolator(sPathInterpolator);
        mEnterAnimator.addUpdateListener(animation -> {
            mExpansionProgress = (Float) animation.getAnimatedValue();
            if (mLastTouchX >= 0) {
                applyFisheye(mLastTouchX, mLastTouchY);
            }
        });
        mEnterAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                mExpansionProgress = 1.0f;
                if (mLastTouchX >= 0) {
                    applyFisheye(mLastTouchX, mLastTouchY);
                }
            }
        });
        mEnterAnimator.start();
    }

    /**
     * Continuous drag location handler called on ACTION_DRAG_LOCATION.
     * Computes ColorOS fisheye magnification and triggers edge auto-scrolling.
     *
     * @param rawX Screen touch X coordinate
     * @param rawY Screen touch Y coordinate
     */
    public void onDragLocation(float rawX, float rawY) {
        if (mRow == null || mRow.getChildCount() == 0) {
            return;
        }

        mLastRawX = rawX;
        mLastRawY = rawY;

        // Measure finger coordinates directly in mRow's coordinate space!
        int[] rowScreenLoc = new int[2];
        mRow.getLocationOnScreen(rowScreenLoc);
        float rowTouchX = rawX - rowScreenLoc[0];
        float rowTouchY = rawY - rowScreenLoc[1];

        int zoomMargin = dp(mZoomMarginDp);
        int quickMargin = (int) (zoomMargin * 0.45f);

        if (mOrientation == ORIENTATION_VERTICAL) {
            int[] dockScreenLoc = new int[2];
            getLocationOnScreen(dockScreenLoc);
            int dockTop = dockScreenLoc[1];
            int dockHeight = getHeight() > 0 ? getHeight() : dp(480);
            boolean canScroll = mVerticalScrollView != null && mRow.getHeight() > mVerticalScrollView.getHeight();

            if (canScroll) {
                if (rawY <= dockTop + zoomMargin) {
                    boolean fast = (rawY <= dockTop + quickMargin);
                    startAutoScroll(-1, fast);
                } else if (rawY >= dockTop + dockHeight - zoomMargin) {
                    boolean fast = (rawY >= dockTop + dockHeight - quickMargin);
                    startAutoScroll(1, fast);
                } else {
                    stopAutoScroll();
                }
            } else {
                stopAutoScroll();
            }

            // Boundary check: if finger moves too far left of dock (into screen interior)
            if (rowTouchX < -dp(80) || rowTouchX > dp(120)) {
                resetMagnification(true);
                return;
            }
        } else {
            // 1. Edge Auto-Scroll Handling (covering screen left and right edges, including lower corners)
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int[] dockScreenLoc = new int[2];
            getLocationOnScreen(dockScreenLoc);
            // Vertical active range: from slightly above the dock all the way to screen bottom
            boolean inLowerZone = (rawY >= dockScreenLoc[1] - dp(60));
            boolean canScroll = mScrollView != null && mRow.getWidth() > mScrollView.getWidth();

            if (canScroll && inLowerZone) {
                if (rawX <= zoomMargin) {
                    boolean fast = (rawX <= quickMargin);
                    startAutoScroll(-1, fast);
                } else if (rawX >= screenWidth - zoomMargin) {
                    boolean fast = (rawX >= screenWidth - quickMargin);
                    startAutoScroll(1, fast);
                } else {
                    stopAutoScroll();
                }
            } else {
                stopAutoScroll();
            }

            // Vertical boundary check: only reset magnification if finger moves significantly ABOVE the dock!
            if (rowTouchY < -dp(50)) {
                resetMagnification(true);
                return;
            }
        }

        // 2. Fisheye Magnification within Dock
        mLastTouchX = rowTouchX;
        mLastTouchY = rowTouchY;
        if (mExpansionProgress < 1.0f && (mEnterAnimator == null || !mEnterAnimator.isRunning())) {
            startEnterAnimation(rawX, rawY);
        } else {
            applyFisheye(rowTouchX, rowTouchY);
        }
    }

    private void applyFisheye(float rowTouchX, float rowTouchY) {
        if (mRow == null || mRow.getChildCount() == 0) return;
        if (mOrientation == ORIENTATION_VERTICAL) {
            applyFisheyeVertical(rowTouchX, rowTouchY);
        } else {
            applyFisheyeHorizontal(rowTouchX, rowTouchY);
        }
    }

    private void applyFisheyeVertical(float rowTouchX, float rowTouchY) {
        View firstChild = mRow.getChildAt(0);
        if (firstChild == null || firstChild.getHeight() == 0) {
            return;
        }
        float itemStep = dp(72);
        int closestIdx = -1;
        float minDist = Float.MAX_VALUE;
        int count = mRow.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = mRow.getChildAt(i);
            float childCenter = child.getTop() + child.getHeight() / 2.0f;
            float dist = Math.abs(rowTouchY - childCenter);
            if (dist < minDist) {
                minDist = dist;
                closestIdx = i;
            }
        }

        if (closestIdx >= 0) {
            View closestChild = mRow.getChildAt(closestIdx);
            float closestCenter = closestChild.getTop() + closestChild.getHeight() / 2.0f;
            float offset = (rowTouchY - closestCenter) / itemStep;
            offset = Math.max(-0.5f, Math.min(0.5f, offset));

            float v2 = offset * 0.13f;
            float centerScale = 1.23f - Math.abs(v2);

            // Proximity factor: smoothly falls off if finger is far from all items (e.g. in top/bottom blank space)
            float proximity = 1.0f;
            if (minDist > itemStep * 0.5f) {
                proximity = Math.max(0.0f, 1.0f - (minDist - itemStep * 0.5f) / (itemStep * 1.0f));
            }

            for (int i = 0; i < count; i++) {
                float targetScale;
                if (i == closestIdx) {
                    targetScale = 1.0f + (centerScale - 1.0f) * proximity;
                } else if (i < closestIdx) {
                    int delta = closestIdx - i;
                    float base = Math.max(1.0f, 1.23f - delta * 0.13f - v2);
                    targetScale = 1.0f + (base - 1.0f) * proximity;
                } else {
                    int delta = i - closestIdx;
                    float base = Math.max(1.0f, 1.23f - delta * 0.13f + v2);
                    targetScale = 1.0f + (base - 1.0f) * proximity;
                }

                float scale = 1.0f + (targetScale - 1.0f) * mExpansionProgress;

                View child = mRow.getChildAt(i);
                ImageView icon = child.findViewById(ID_ICON);
                if (icon != null) {
                    // Anchor right center: expands leftward into screen
                    icon.setPivotX(icon.getWidth());
                    icon.setPivotY(icon.getHeight() / 2.0f);
                    icon.setScaleX(scale);
                    icon.setScaleY(scale);
                }

                // Leftward lift translation (bulges toward screen center)
                float progress = Math.max(0.0f, Math.min(1.0f, (scale - 1.0f) / 0.23f));
                float interpolated = sPathInterpolator.getInterpolation(progress);
                float liftDp = 11.987f * interpolated * mExpansionProgress;
                child.setTranslationX(-liftDp * mDensity);
                child.setTranslationY(0f);
            }
        }
    }

    private void applyFisheyeHorizontal(float rowTouchX, float rowTouchY) {
        View firstChild = mRow.getChildAt(0);
        if (firstChild == null || firstChild.getWidth() == 0) {
            return;
        }
        float itemStep = dp(60); // 50dp + 2 * 5dp

        int closestIdx = -1;
        float minDist = Float.MAX_VALUE;
        int count = mRow.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = mRow.getChildAt(i);
            float childCenter = child.getLeft() + child.getWidth() / 2.0f;
            float dist = Math.abs(rowTouchX - childCenter);
            if (dist < minDist) {
                minDist = dist;
                closestIdx = i;
            }
        }

        if (closestIdx >= 0) {
            View closestChild = mRow.getChildAt(closestIdx);
            float closestCenter = closestChild.getLeft() + closestChild.getWidth() / 2.0f;
            float offset = (rowTouchX - closestCenter) / itemStep;
            offset = Math.max(-0.5f, Math.min(0.5f, offset));

            // ColorOS decompiled math (Lv5/m;->x):
            // v2 = offset * 0.13f
            // centerScale = 1.23f - Math.abs(v2)
            float v2 = offset * 0.13f;
            float centerScale = 1.23f - Math.abs(v2);

            // Proximity factor: smoothly falls off if finger is far from all items
            float proximity = 1.0f;
            if (minDist > itemStep * 0.5f) {
                proximity = Math.max(0.0f, 1.0f - (minDist - itemStep * 0.5f) / (itemStep * 1.0f));
            }

            for (int i = 0; i < count; i++) {
                float targetScale;
                if (i == closestIdx) {
                    targetScale = 1.0f + (centerScale - 1.0f) * proximity;
                } else if (i < closestIdx) {
                    int delta = closestIdx - i;
                    float base = Math.max(1.0f, 1.23f - delta * 0.13f - v2);
                    targetScale = 1.0f + (base - 1.0f) * proximity;
                } else {
                    int delta = i - closestIdx;
                    float base = Math.max(1.0f, 1.23f - delta * 0.13f + v2);
                    targetScale = 1.0f + (base - 1.0f) * proximity;
                }

                // Apply expansion bloom smoothly: (targetScale - 1.0) * mExpansionProgress
                float scale = 1.0f + (targetScale - 1.0f) * mExpansionProgress;

                View child = mRow.getChildAt(i);
                ImageView icon = child.findViewById(ID_ICON);
                if (icon != null) {
                    // Anchor bottom center: pivotY at bottom so icon expands upward
                    icon.setPivotX(icon.getWidth() / 2.0f);
                    icon.setPivotY(icon.getHeight());
                    icon.setScaleX(scale);
                    icon.setScaleY(scale);
                }

                // Upward lift translation (Lv5/k;->d):
                float progress = Math.max(0.0f, Math.min(1.0f, (scale - 1.0f) / 0.23f));
                float interpolated = sPathInterpolator.getInterpolation(progress);
                float liftDp = 11.987f * interpolated * mExpansionProgress;
                child.setTranslationY(-liftDp * mDensity);
                child.setTranslationX(0f);
            }
        }
    }

    private void startAutoScroll(int direction, boolean fast) {
        if (mScrollDirection != direction) {
            mScrollDirection = direction;
            mHoverStartTime = SystemClock.uptimeMillis();
        }
        mScrollFast = fast;
        if (!mAutoScrollRunning) {
            mAutoScrollRunning = true;
            postOnAnimation(mAutoScrollRunnable);
        }
    }

    public void stopAutoScroll() {
        mScrollDirection = 0;
        mAutoScrollRunning = false;
        removeCallbacks(mAutoScrollRunnable);
    }

    public void onDragExited() {
        resetMagnification(true);
    }

    /**
     * Resets magnification smoothly (or immediately) back to 1.0f and 0 translation.
     */
    public void resetMagnification(boolean animate) {
        if (mEnterAnimator != null) {
            mEnterAnimator.cancel();
        }
        mExpansionProgress = 0f;
        mLastTouchX = -1f;
        mLastTouchY = -1f;
        stopAutoScroll();
        if (mRow == null) return;
        for (int i = 0; i < mRow.getChildCount(); i++) {
            View child = mRow.getChildAt(i);
            ImageView icon = child.findViewById(ID_ICON);
            if (animate) {
                if (icon != null) {
                    icon.animate().scaleX(1.0f).scaleY(1.0f)
                            .setDuration(160).setInterpolator(sPathInterpolator).start();
                }
                child.animate().translationX(0f).translationY(0f)
                        .setDuration(160).setInterpolator(sPathInterpolator).start();
            } else {
                if (icon != null) {
                    icon.setScaleX(1.0f);
                    icon.setScaleY(1.0f);
                }
                child.setTranslationX(0f);
                child.setTranslationY(0f);
            }
        }
    }

    /**
     * Finds the dropped target item under (rawX, rawY), taking into account child translation.
     */
    @Nullable
    public DockItem findHitTarget(float rawX, float rawY) {
        if (mRow == null) return null;

        int[] rowScreenLoc = new int[2];
        mRow.getLocationOnScreen(rowScreenLoc);
        float rowTouchX = rawX - rowScreenLoc[0];
        float rowTouchY = rawY - rowScreenLoc[1];

        if (mOrientation == ORIENTATION_VERTICAL) {
            if (rowTouchX < -dp(40) || rowTouchX > mRow.getWidth() + dp(40)
                    || rowTouchY < -dp(20) || rowTouchY > mRow.getHeight() + dp(20)) {
                return null;
            }
            for (int i = 0; i < mRow.getChildCount(); i++) {
                View child = mRow.getChildAt(i);
                float childLeft = child.getLeft() + child.getTranslationX();
                float childRight = child.getRight();
                float childTop = child.getTop();
                float childBottom = child.getBottom();
                if (rowTouchY >= childTop && rowTouchY <= childBottom
                        && rowTouchX >= childLeft - dp(25) && rowTouchX <= childRight + dp(35)) {
                    Object tag = child.getTag();
                    if (tag instanceof DockItem) {
                        return (DockItem) tag;
                    }
                }
            }
            return null;
        }

        if (rowTouchX < 0 || rowTouchX > mRow.getWidth()
                || rowTouchY < -dp(30) || rowTouchY > mRow.getHeight() + dp(30)) {
            return null;
        }

        for (int i = 0; i < mRow.getChildCount(); i++) {
            View child = mRow.getChildAt(i);
            float childLeft = child.getLeft();
            float childRight = child.getRight();
            float childTop = child.getTop() + child.getTranslationY();
            float childBottom = child.getBottom();
            if (rowTouchX >= childLeft && rowTouchX <= childRight
                    && rowTouchY >= childTop && rowTouchY <= childBottom + dp(20)) {
                Object tag = child.getTag();
                if (tag instanceof DockItem) {
                    return (DockItem) tag;
                }
            }
        }
        return null;
    }

    /**
     * Generates a sleek rounded download/save icon for saving images.
     */
    private Drawable createDefaultSaveDrawable() {
        return new Drawable() {
            private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint mFgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                mBgPaint.setColor(Color.parseColor("#38383A"));
                mFgPaint.setColor(Color.WHITE);
                mFgPaint.setStyle(Paint.Style.STROKE);
                mFgPaint.setStrokeWidth(dp(2));
                mFgPaint.setStrokeCap(Paint.Cap.ROUND);
                mFgPaint.setStrokeJoin(Paint.Join.ROUND);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                Rect b = getBounds();
                float w = b.width();
                float h = b.height();
                if (w <= 0 || h <= 0) return;

                // Squircle background
                RectF bgRect = new RectF(0, 0, w, h);
                canvas.drawRoundRect(bgRect, dp(14), dp(14), mBgPaint);

                float cx = w / 2f;
                float cy = h / 2f;

                // Downward arrow: vertical stem
                canvas.drawLine(cx, cy - dp(7), cx, cy + dp(4), mFgPaint);
                // Downward arrowhead
                canvas.drawLine(cx - dp(5), cy - dp(1), cx, cy + dp(4), mFgPaint);
                canvas.drawLine(cx + dp(5), cy - dp(1), cx, cy + dp(4), mFgPaint);

                // Bottom tray bracket: |_|
                float trayY = cy + dp(9);
                float trayW = dp(8);
                float trayH = dp(3);
                canvas.drawLine(cx - trayW, trayY - trayH, cx - trayW, trayY, mFgPaint);
                canvas.drawLine(cx - trayW, trayY, cx + trayW, trayY, mFgPaint);
                canvas.drawLine(cx + trayW, trayY, cx + trayW, trayY - trayH, mFgPaint);
            }

            @Override
            public void setAlpha(int alpha) {
                mBgPaint.setAlpha(alpha);
                mFgPaint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                mBgPaint.setColorFilter(colorFilter);
                mFgPaint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }

    /**
     * Generates a sleek rounded magnifying glass search icon.
     */
    private Drawable createDefaultSearchDrawable() {
        return new Drawable() {
            private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint mFgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                mBgPaint.setColor(Color.parseColor("#38383A"));
                mFgPaint.setColor(Color.WHITE);
                mFgPaint.setStyle(Paint.Style.STROKE);
                mFgPaint.setStrokeWidth(dp(2));
                mFgPaint.setStrokeCap(Paint.Cap.ROUND);
                mFgPaint.setStrokeJoin(Paint.Join.ROUND);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                Rect b = getBounds();
                float w = b.width();
                float h = b.height();
                if (w <= 0 || h <= 0) return;

                // Squircle background
                RectF bgRect = new RectF(0, 0, w, h);
                canvas.drawRoundRect(bgRect, dp(14), dp(14), mBgPaint);

                float cx = w / 2f - dp(2);
                float cy = h / 2f - dp(2);
                float r = dp(6);

                // Magnifying glass circle
                canvas.drawCircle(cx, cy, r, mFgPaint);

                // Handle
                float handleStart = (float) (r / Math.sqrt(2));
                canvas.drawLine(cx + handleStart, cy + handleStart, cx + handleStart + dp(6), cy + handleStart + dp(6), mFgPaint);
            }

            @Override
            public void setAlpha(int alpha) {
                mBgPaint.setAlpha(alpha);
                mFgPaint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                mBgPaint.setColorFilter(colorFilter);
                mFgPaint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }

    /**
     * Generates a sleek, modern rounded clipboard icon for the copy action.
     */
    private Drawable createDefaultCopyDrawable() {
        return new Drawable() {
            private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint mFgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                mBgPaint.setColor(Color.parseColor("#38383A"));
                mFgPaint.setColor(Color.WHITE);
                mFgPaint.setStyle(Paint.Style.STROKE);
                mFgPaint.setStrokeWidth(dp(2));
                mFgPaint.setStrokeCap(Paint.Cap.ROUND);
                mFgPaint.setStrokeJoin(Paint.Join.ROUND);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                Rect b = getBounds();
                float w = b.width();
                float h = b.height();
                if (w <= 0 || h <= 0) return;

                // Squircle background
                RectF bgRect = new RectF(0, 0, w, h);
                canvas.drawRoundRect(bgRect, dp(14), dp(14), mBgPaint);

                // Two overlapping document rectangles for "Copy"
                float pad = dp(13);
                float docW = dp(14);
                float docH = dp(17);
                float rad = dp(3);

                // Back document
                RectF backDoc = new RectF(pad + dp(4), pad, pad + dp(4) + docW, pad + docH);
                canvas.drawRoundRect(backDoc, rad, rad, mFgPaint);

                // Front document
                RectF frontDoc = new RectF(pad, pad + dp(4), pad + docW, pad + dp(4) + docH);
                Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                fillPaint.setColor(Color.parseColor("#38383A"));
                canvas.drawRoundRect(frontDoc, rad, rad, fillPaint);
                canvas.drawRoundRect(frontDoc, rad, rad, mFgPaint);
            }

            @Override
            public void setAlpha(int alpha) {
                mBgPaint.setAlpha(alpha);
                mFgPaint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                mBgPaint.setColorFilter(colorFilter);
                mFgPaint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }

    /**
     * Generates a rounded text-segmentation tile: dark squircle with three white word bars.
     * The SystemUI module cannot read this APK's R resources, so the icon must be drawn.
     */
    private Drawable createDefaultSegmentDrawable() {
        return new Drawable() {
            private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint mFgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                mBgPaint.setColor(Color.parseColor("#38383A"));
                mFgPaint.setColor(Color.WHITE);
                mFgPaint.setStyle(Paint.Style.FILL);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                Rect b = getBounds();
                float w = b.width();
                float h = b.height();
                if (w <= 0 || h <= 0) return;

                canvas.drawRoundRect(new RectF(0, 0, w, h), dp(14), dp(14), mBgPaint);

                float left = w * 0.24f;
                float fullRight = w * 0.76f;
                float barRadius = Math.max(dp(1.5f), h * 0.038f);
                float[] centers = {0.30f, 0.50f, 0.70f};
                float[] widths = {1.0f, 0.68f, 1.0f};
                for (int i = 0; i < centers.length; i++) {
                    float centerY = h * centers[i];
                    float right = left + (fullRight - left) * widths[i];
                    canvas.drawRoundRect(
                            new RectF(left, centerY - barRadius, right, centerY + barRadius),
                            barRadius, barRadius, mFgPaint);
                }
            }

            @Override
            public void setAlpha(int alpha) {
                mBgPaint.setAlpha(alpha);
                mFgPaint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                mBgPaint.setColorFilter(colorFilter);
                mFgPaint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }
}
