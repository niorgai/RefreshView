package qiu.niorgai.refreshview.top;

/**
 * Created by qiu on 12/3/15.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ScrollView;

/**
 * 下拉刷新的Layout
 * Created by qiu on 9/8/15.
 */
public class SuperRefreshLayout extends ViewGroup implements NestedScrollingParent,
        NestedScrollingChild {

    private static final String TAG = SuperRefreshLayout.class.getSimpleName();

    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

    private static final int CIRCLE_DIAMETER = 30;
    private static final int CIRCLE_DIAMETER_LARGE = 56;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    // Max amount of circle that can be filled by progress during swipe gesture,
    // where 1.0 is a full circle
    private static final float MAX_PROGRESS_ANGLE = .8f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 64;

    private View mTarget; // the target of the gesture
    private OnRefreshListener mListener;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private float mTotalDragDistance = -1;
    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];

    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;
    // Whether or not the starting offset has been determined.
    private boolean mOriginalOffsetCalculated = false;

    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    // Whether this item is scaled up rather than clipped
    private boolean mScale = true;
    // Whether this item is use alpha
    private boolean mAlpha = true;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.enabled
    };

    //是否是第一个需要拦截的Touch事件
    private boolean isFirstIntercept = false;

    //是否是同一点击事件序列中第一个拦截后应该处理的move事件
    private boolean isFirstMoveAfterIntercept = false;

    //dispatch方法记录第一次按下的x
    private float mInitialDisPatchDownX;

    //dispatch方法记录第一次按下的y
    private float mInitialDisPatchDownY;

    //dispatch方法记录的手指
    private int mActiveDispatchPointerId = INVALID_POINTER;

    //是否请求拦截
    private boolean hasRequestDisallowIntercept = false;

    private TopLoadingView mLoadingView;
    private int mCircleViewIndex = -1;

    protected int mFrom;

    private float mStartingScale;

    protected int mOriginalOffsetTop;

    private Animation mScaleAnimation;

    private Animation mScaleDownAnimation;

    private Animation mAlphaStartAnimation;

    private Animation mAlphaMaxAnimation;

    private Animation mScaleDownToStartAnimation;

    private float mSpinnerFinalOffset;

    private boolean mNotify;

    private int mCircleWidth;

    private int mCircleHeight;

    // Whether the client has set a custom starting position;
    private boolean mUsingCustomStart;

    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                mLoadingView.setViewAlpha(1);
                mLoadingView.startAnimation();
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
            } else {
                mLoadingView.stopAnimation();
                mLoadingView.setVisibility(View.GONE);
                setColorViewAlpha();
                // Return the circle to its start position
                if (mScale) {
                    setAnimationProgress(0 /* animation complete and view is hidden */);
                } else {
                    setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop,
                            true /* requires update */);
                }
            }
            mCurrentTargetOffsetTop = mLoadingView.getTop();
        }
    };

    private void setColorViewAlpha() {
        mLoadingView.setViewAlpha(1);
    }

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *            where the progress spinner is set to appear.
     * @param start The offset in pixels from the top of this view at which the
     *            progress spinner should appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mScale = scale;
        mLoadingView.setVisibility(View.GONE);
        mOriginalOffsetTop = mCurrentTargetOffsetTop = start;
        mSpinnerFinalOffset = end;
        mUsingCustomStart = true;
        mLoadingView.invalidate();
    }

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *            where the progress spinner is set to appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerFinalOffset = end;
        mScale = scale;
        mLoadingView.invalidate();
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public SuperRefreshLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public SuperRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        mCircleHeight = (int) (CIRCLE_DIAMETER * metrics.density);

        createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density;
        mTotalDragDistance = mSpinnerFinalOffset;
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (mCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mCircleViewIndex;
        } else if (i >= mCircleViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    private void createProgressView() {
        mLoadingView = new TopLoadingView(getContext());
        mLoadingView.setVisibility(View.GONE);
        addView(mLoadingView);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            // scale and show
            mRefreshing = refreshing;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset + mOriginalOffsetTop);
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop,
                    true /* requires update */);
            mNotify = false;
            startScaleUpAnimation(mRefreshListener);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

    private void startScaleUpAnimation(Animation.AnimationListener listener) {
        mLoadingView.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
        }
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mLoadingView.setAnimationListener(listener);
        }
        mLoadingView.clearAnimation();
        mLoadingView.startAnimation(mScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     * @param pro
     */
    private void setAnimationProgress(float pro) {
        //y = - [(x-1)*(x-1)] + 1
        float progress = (float) (-(Math.pow(pro - 1f, 2d)) + 1);
        mLoadingView.setProgress(progress);
        if (mScale) {
            ViewCompat.setScaleX(mLoadingView, progress);
            ViewCompat.setScaleY(mLoadingView, progress);
        }
        if (mAlpha) {
            mLoadingView.setViewAlpha(progress);
        }

    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
            } else {
                startScaleDownAnimation(mRefreshListener);
            }
        }
    }

    private void startScaleDownAnimation(Animation.AnimationListener listener) {
        animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
//        mScaleDownAnimation = new Animation() {
//            @Override
//            public void applyTransformation(float interpolatedTime, Transformation t) {
//                setAnimationProgress(1 - interpolatedTime);
//            }
//        };
//        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
//        mLoadingView.setAnimationListener(listener);
//        mLoadingView.clearAnimation();
//        mLoadingView.startAnimation(mScaleDownAnimation);
    }

    private void startProgressAlphaStartAnimation() {
        mAlphaStartAnimation = startAlphaAnimation((int) (mLoadingView.getAlpha() * MAX_ALPHA), STARTING_PROGRESS_ALPHA);
    }

    private void startProgressAlphaMaxAnimation() {
        mAlphaMaxAnimation = startAlphaAnimation((int) (mLoadingView.getAlpha() * MAX_ALPHA), MAX_ALPHA);
    }

    private Animation startAlphaAnimation(final int startingAlpha, final int endingAlpha) {
        // Pre API 11, alpha is used in place of scale. Don't also use it to
        // show the trigger point.
        if (mScale && isAlphaUsedForScale()) {
            return null;
        }
        Animation alpha = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                mLoadingView
                        .setAlpha((int) (startingAlpha+ ((endingAlpha - startingAlpha)
                                * interpolatedTime)));
            }
        };
        alpha.setDuration(ALPHA_ANIMATION_DURATION);
        // Clear out the previous animation listeners.
        mLoadingView.setAnimationListener(null);
        mLoadingView.clearAnimation();
        mLoadingView.startAnimation(alpha);
        return alpha;
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     *         progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i=getChildCount()-1; i>=0; i--) {
                View child = getChildAt(i);
                if (!child.equals(mLoadingView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        int circleWidth = mLoadingView.getMeasuredWidth();
        int circleHeight = mLoadingView.getMeasuredHeight();
        mLoadingView.layout((width / 2 - circleWidth / 2), mCurrentTargetOffsetTop,
                (width / 2 + circleWidth / 2), mCurrentTargetOffsetTop + circleHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
//        mLoadingView.measure(MeasureSpec.makeMeasureSpec(mCircleWidth, MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(mCircleHeight, MeasureSpec.EXACTLY));
        measureChild(mLoadingView, widthMeasureSpec, heightMeasureSpec);
        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffsetTop = mOriginalOffsetTop = -mLoadingView.getMeasuredHeight();
        }
        mCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mLoadingView) {
                mCircleViewIndex = index;
                break;
            }
        }
    }

    /**
     * Get the diameter of the progress circle that is displayed as part of the
     * swipe to refresh layout. This is not valid until a measure pass has
     * completed.
     *
     * @return Diameter in pixels of the progress circle view.
     */
    public int getProgressCircleDiameter() {
        return mLoadingView != null ? mLoadingView.getMeasuredHeight() : 0;
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (!isScrollableChildView(mTarget) && mTarget instanceof ViewGroup) {
            ViewGroup group = ((ViewGroup) mTarget);
            for (int i=0; i<group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (isScrollableChildView(child)) {
                    return ViewCompat.canScrollVertically(child, -1);
                }
            }
        }
        return ViewCompat.canScrollVertically(mTarget, -1);
    }

    public boolean isScrollableChildView(View view) {
        return view instanceof AbsListView || view instanceof ScrollView || view instanceof RecyclerView;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isFirstMoveAfterIntercept = true;
                isFirstIntercept = true;
                mActiveDispatchPointerId = MotionEventCompat.getPointerId(ev, 0);
                final float initialDownX = getMotionEventX(ev, mActiveDispatchPointerId);
                if (initialDownX != INVALID_POINTER) {
                    mInitialDisPatchDownX = initialDownX;
                }
                final float initialDownY = getMotionEventY(ev, mActiveDispatchPointerId);
                if (mInitialDisPatchDownY != INVALID_POINTER) {
                    mInitialDisPatchDownY = initialDownY;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (hasRequestDisallowIntercept) {
                    //解决viewPager滑动冲突问题
                    final float x = getMotionEventX(ev, mActiveDispatchPointerId);
                    final float y = getMotionEventY(ev, mActiveDispatchPointerId);
                    if (mInitialDisPatchDownX != INVALID_POINTER && x != INVALID_POINTER &&
                            mInitialDisPatchDownY != INVALID_POINTER && y != INVALID_POINTER) {
                        final float xDiff = Math.abs(x - mInitialDisPatchDownX);
                        final float yDiff = Math.abs(y - mInitialDisPatchDownY);
                        if (xDiff > mTouchSlop && xDiff * 0.7f > yDiff) {
                            //横向滚动不需要拦截
                            super.requestDisallowInterceptTouchEvent(true);
                        } else {
                            super.requestDisallowInterceptTouchEvent(false);
                        }
                    } else {
                        super.requestDisallowInterceptTouchEvent(false);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                    isFirstMoveAfterIntercept = false;
                    hasRequestDisallowIntercept = false;
                }
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp() || mRefreshing) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mLoadingView.getTop(), true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                mInitialDownY = initialDownY;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }
                //重新计算initDown来检测是否需要拦截Touch事件
                if (isFirstIntercept && !canChildScrollUp()) {
                    final float moveInitialDownY = getMotionEventY(ev, mActivePointerId);
                    if (moveInitialDownY == -1) {
                        return false;
                    }
                    mInitialDownY = moveInitialDownY;
                    isFirstIntercept = false;
                }
                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialDownY;
                Log.d(TAG, "onInterceptTouchEvent: ydiff: " + yDiff + " touch: " + mTouchSlop);
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    private float getMotionEventX(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getX(ev, index);
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
//        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
//                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
//            // Nope.
//        } else {
//            super.requestDisallowInterceptTouchEvent(b);
//        }
        hasRequestDisallowIntercept = true;
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        if (isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0) {
            // Dispatch up to the nested parent
            startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
            return true;
        }
        return false;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        mTotalUnconsumed = 0;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;

            }
            moveSpinner(mTotalUnconsumed);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed) {
        if (dyUnconsumed < 0) {
            dyUnconsumed = Math.abs(dyUnconsumed);
            mTotalUnconsumed += dyUnconsumed;
            moveSpinner(mTotalUnconsumed);
        }
        // Dispatch up to the nested parent
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dxConsumed, null);
    }

    // NestedScrollingChild

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    private void moveSpinner(float overscrollTop) {
        float originalDragPercent = overscrollTop / mTotalDragDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
        float slingshotDist = mUsingCustomStart ? mSpinnerFinalOffset - mOriginalOffsetTop
                : mSpinnerFinalOffset;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
        // where 1.0f is a full circle
        if (mLoadingView.getVisibility() != View.VISIBLE) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
        if (!mScale) {
            ViewCompat.setScaleX(mLoadingView, 1f);
            ViewCompat.setScaleY(mLoadingView, 1f);
        }
        if (overscrollTop < mTotalDragDistance) {
            if (mScale) {
                setAnimationProgress(overscrollTop / mTotalDragDistance);
            }
        }
        setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop, true /* requires update */);
    }

    private void finishSpinner(float overscrollTop) {
        if (overscrollTop > mTotalDragDistance) {
            setRefreshing(true, true /* notify */);
        } else {
            // cancel refresh
            mRefreshing = false;
            Animation.AnimationListener listener = null;
            if (!mScale) {
                listener = new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (!mScale) {
                            startScaleDownAnimation(null);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                };
            }
            animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp()) {
            if (action != MotionEvent.ACTION_DOWN && !isFirstMoveAfterIntercept) {
                mTarget.dispatchTouchEvent(ev);
            }
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overScrollTop = (y - mInitialMotionY) * DRAG_RATE;
                if (mIsBeingDragged) {
                    if (overScrollTop > 0) {
                        moveSpinner(overScrollTop);
                    } else {
                        if (isFirstMoveAfterIntercept) {
                            // 如果是被拦截过(mIsBeingDragged) 并且是第一个move事件(isFirstMoveAfterIntercept)
                            // 先为子View模拟一个Action_Down事件
                            MotionEvent event = MotionEvent.obtain(ev);
                            event.setAction(MotionEvent.ACTION_DOWN);
                            mTarget.dispatchTouchEvent(event);
                            isFirstMoveAfterIntercept = false;
                            return false;
                        }
                        //将Move事件传递到子View
                        mTarget.dispatchTouchEvent(ev);
                        return false;
                    }
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    if (action == MotionEvent.ACTION_UP) {
                        Log.e(TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    }
                    return false;
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                finishSpinner(overscrollTop);
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    private void animateOffsetToCorrectPosition(int from, Animation.AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mLoadingView.setAnimationListener(listener);
        }
        mLoadingView.clearAnimation();
        mLoadingView.startAnimation(mAnimateToCorrectPosition);
    }

    private void peek(int from, Animation.AnimationListener listener) {
        mFrom = from;
        mPeek.reset();
        mPeek.setDuration(500);
        mPeek.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mLoadingView.setAnimationListener(listener);
        }
        mLoadingView.clearAnimation();
        mLoadingView.startAnimation(mPeek);
    }

    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mLoadingView.setAnimationListener(listener);
            }
            mLoadingView.clearAnimation();
            mLoadingView.startAnimation(mAnimateToStartPosition);
        }
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop));
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mLoadingView.getTop();
            setTargetOffsetTopAndBottom(offset, false /* requires update */);
        }
    };

    private final Animation mPeek = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop));
            } else {
                endTarget = (int) mSpinnerFinalOffset; //mSpinnerFinalOffset;
            }
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mLoadingView.getTop();
            setTargetOffsetTopAndBottom(offset, false /* requires update */);
        }
    };

    private void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - mLoadingView.getTop();
        setTargetOffsetTopAndBottom(offset, false /* requires update */);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(int from,
                                                      Animation.AnimationListener listener) {
        mFrom = from;
        mStartingScale = ViewCompat.getScaleX(mLoadingView);
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale  * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mLoadingView.setAnimationListener(listener);
        }
        mLoadingView.clearAnimation();
        mLoadingView.startAnimation(mScaleDownToStartAnimation);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mLoadingView.bringToFront();
        mLoadingView.offsetTopAndBottom(offset);
        mCurrentTargetOffsetTop = mLoadingView.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        public void onRefresh();
    }

    public void setScale(boolean mScale) {
        this.mScale = mScale;
    }

    public void setAlpha(boolean mAlpha) {
        this.mAlpha = mAlpha;
    }
}