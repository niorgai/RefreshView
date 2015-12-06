package qiu.niorgai.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * Created by qiu on 9/20/15.
 */
public class AutoLoadScrollView extends ScrollView implements LoadMoreInterface.LoadMoreComplete{

    //是否正在加载
    private boolean isLoadingMore = false;
//    是否有更多
    private boolean isHaveMore = true;

    private LoadMoreInterface.LoadMoreListener loadMoreListener;

    private LoadMoreInterface.BottomLoadingView mLoadingView;

    public AutoLoadScrollView(Context context) {
        this(context, null);
    }

    public AutoLoadScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //TODO::empty
//        mLoadingView = new LoadingView(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //布局成功后初始化LoadingView
        final ViewGroup group = getChildViewGroup();
        if (group == null) {
            //未设置子View则不监听滑动
            isHaveMore = false;
            return;
        }
        if (isHaveMore) {
            int scrollViewHeight = getMeasuredHeight();
            int childViewHeight = group.getMeasuredHeight();
            if (scrollViewHeight == childViewHeight) {
                //此时未填满屏幕
                group.addView((View) mLoadingView, group.getChildCount());
                mLoadingView.changeToClickStatus(loadMoreListener);
            } else {
                //填满屏幕了
                group.addView((View) mLoadingView, group.getChildCount());
                mLoadingView.changeToLoadingStatus();
            }
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        //需要监听滑动
        if (isHaveMore && !isLoadingMore && loadMoreListener != null) {
            if (t > oldt) {
            //正在向下滑动
            if (t + getHeight() >= computeVerticalScrollRange() - ((View) mLoadingView).getMeasuredHeight() / 2) {
                //已经滑动到底部
                    loadMoreListener.loadMore();
                    isLoadingMore = true;
                    mLoadingView.changeToLoadingStatus();
                }
            }
        }
    }

    @Override
    public void onComplete(boolean hasMore) {
        isLoadingMore = false;
        isHaveMore = hasMore;
        if (!isHaveMore) {
            mLoadingView.changeToHideStatus();
        } else {
            final ViewGroup group = getChildViewGroup();
            if (group == null) {
                //未设置子View则不监听滑动
                isHaveMore = false;
                return;
            }
            int scrollViewHeight = getMeasuredHeight();
            int childViewHeight = group.getMeasuredHeight();
            if (scrollViewHeight == childViewHeight) {
                //此时未填满屏幕
                mLoadingView.changeToClickStatus(loadMoreListener);
            } else {
                //填满屏幕了
                mLoadingView.changeToLoadingStatus();
            }
        }
    }

    @Override
    public void onError() {
        isLoadingMore = false;
        if (isHaveMore) {
            mLoadingView.changeToClickStatus(loadMoreListener);
        }
    }

    @Override
    public void setIsHaveMore(boolean isHaveMore) {
        this.isHaveMore = isHaveMore;
    }

    @Override
    public void setLoadMoreListener(LoadMoreInterface.LoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }

    //获取子ViewGroup
    private ViewGroup getChildViewGroup() {
        return getChildCount() == 0 ? null : (ViewGroup)getChildAt(0);
    }

}
