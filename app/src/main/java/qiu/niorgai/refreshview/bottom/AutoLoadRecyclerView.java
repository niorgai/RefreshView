package qiu.niorgai.refreshview.bottom;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * 自动加载更多,可以设置EmptyView的RecyclerView
 * Created by qiu on 9/18/15.
 */
public class AutoLoadRecyclerView extends RecyclerView implements LoadMoreInterface.AutoLoadView {

    private View mEmptyView;

    //是否向下滑动
    private boolean isScrollingDown = false;
    //是否反向布局
    private boolean isReverseLayout = false;
    //是否正在加载
    private boolean isLoadingMore = false;

    //TYPE_FOOTER为FooterView
    public static final int TYPE_FOOTER = -1;
    //TYPE_NORMAL为普通item,由adapter控制
    private static final int TYPE_NORMAL = -2;

    //Wrapper模式,为adapter再封装一层,同时加入FooterView
    private WrapAdapter mWrapAdapter;

    private Adapter mOriginAdapter;

    private BottomLoadingView mLoadingView;

    private boolean isHasMore = false;

    private LoadMoreInterface.onLoadMoreListener onLoadMoreListener;

    public AutoLoadRecyclerView(Context context) {
        this(context, null);
    }

    public AutoLoadRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoLoadRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    //original scroll listener
    public interface OnScrolledListener {
        void scrollStateChanged(RecyclerView recyclerView, int newState);
        void scrolled(RecyclerView recyclerView, int dx, int dy);
    }

    private OnScrolledListener scrolledListener;

    public OnScrolledListener getScrolledListener() {
        return scrolledListener;
    }

    public void setScrolledListener(OnScrolledListener scrolledListener) {
        this.scrolledListener = scrolledListener;
    }

    private void init(Context context) {
        mLoadingView = new BottomLoadingView(context);
        this.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (scrolledListener != null) {
                    scrolledListener.scrollStateChanged(recyclerView, newState);
                }
                //往下滑动 && 没有正在加载 && 注册了listener && 滑动结束
                if (isHasMore && isScrollingDown && !isLoadingMore && onLoadMoreListener != null && newState == SCROLL_STATE_IDLE) {
                    LayoutManager layoutManager = getLayoutManager();
                    if (layoutManager == null) {
                        return;
                    }
                    int lastVisibleItemPosition = getLastPosition();
                    if (layoutManager.getChildCount() > 0
                            && layoutManager.getItemCount() >= layoutManager.getChildCount()
                            && lastVisibleItemPosition >= layoutManager.getItemCount()) {
                        isLoadingMore = true;
                        onLoadMoreListener.onLoadMore();
                        mLoadingView.changeToLoadingStatus();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (scrolledListener != null) {
                    scrolledListener.scrolled(recyclerView, dx, dy);
                }
                //记录是否正在向下滑动
                isScrollingDown = isReverseLayout ? dy < 0 : dy > 0;
            }
        });
    }

    //找到最后一个item的position
    private int getLastPosition() {
        int lastVisibleItemPosition;
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
            ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(into);
            lastVisibleItemPosition = findMax(into);
        } else {
            lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        }
        return lastVisibleItemPosition + 1;
    }

    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            mWrapAdapter.notifyDataSetChanged();
            checkEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeInserted(positionStart, itemCount);
            checkEmpty();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            checkEmpty();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mWrapAdapter.notifyItemMoved(fromPosition, toPosition);
        }
    };

    @Override
    public void setAdapter(Adapter adapter) {
        if (mOriginAdapter != null) {
            mOriginAdapter.unregisterAdapterDataObserver(mObserver);
        }
        mWrapAdapter = new WrapAdapter(adapter);
        super.setAdapter(mWrapAdapter);
        adapter.registerAdapterDataObserver(mObserver);
        mOriginAdapter = adapter;
        checkEmpty();
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        if (layout instanceof LinearLayoutManager) {
            isReverseLayout = ((LinearLayoutManager) layout).getReverseLayout();
        } else if (layout instanceof StaggeredGridLayoutManager) {
            isReverseLayout = ((StaggeredGridLayoutManager) layout).getReverseLayout();
        }
    }

    @Override
    public void onSuccess(boolean hasMore) {
        isLoadingMore = false;
        isHasMore = hasMore;
        if (mWrapAdapter != null) {
            mWrapAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFailure() {
        isLoadingMore = false;
        if (mWrapAdapter != null) {
            mWrapAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setIsHaveMore(boolean isHasMore) {
        this.isHasMore = isHasMore;
        if (mWrapAdapter != null) {
            mWrapAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setOnLoadMoreListener(LoadMoreInterface.onLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public Adapter getAutoLoadRecyclerViewAdapter(){
        if (mWrapAdapter != null){
            return mWrapAdapter.getAdapter();
        }else {
            return null;
        }
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    public void setEmptyView(View mEmptyView) {
        this.mEmptyView = mEmptyView;
        checkEmpty();
    }

    private void checkEmpty() {
        if (mEmptyView != null && getAdapter() != null) {
            final boolean emptyFlag = getAdapter().getItemCount() == 0;
            setVisibility(emptyFlag ? GONE : VISIBLE);
            mEmptyView.setVisibility(emptyFlag ? VISIBLE : GONE);
        }
    }

    //Wrapper模式封装Adapter
    private class WrapAdapter extends RecyclerView.Adapter<ViewHolder> {

        private Adapter mAdapter;

        public Adapter getAdapter(){
            return mAdapter;
        }

        public WrapAdapter(RecyclerView.Adapter adapter) {
            this.mAdapter = adapter;
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            //为Footer单独设置SpanSize
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) manager);
                final GridLayoutManager.SpanSizeLookup originSizeLookup = gridManager.getSpanSizeLookup();
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return (getItemViewType(position) == TYPE_FOOTER) ? gridManager.getSpanCount() : originSizeLookup.getSpanSize(position);
                    }
                });
            }
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            //为Footer单独设置SpanSize
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if(lp != null
                    && lp instanceof StaggeredGridLayoutManager.LayoutParams
                    &&  (isFooter( holder.getLayoutPosition())) ) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
        }

        public boolean isFooter(int position) {
            return position < getItemCount() && position >= getItemCount() - getFootersCount();
        }

        public int getFootersCount() {
            return mLoadingView != null && isHasMore ? 1 : 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_FOOTER) {
                return new FooterViewHolder(mLoadingView);
            }
            return mAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof FooterViewHolder) {
                int childCount = getLayoutManager().getChildCount();
                if (position <= childCount) {
                    //不充满的情况下
                    if (isHasMore) {
                        mLoadingView.changeToClickStatus(onLoadMoreListener);
                    }
                } else {
                    if (isHasMore) {
                        mLoadingView.changeToLoadingStatus();
                    }
                }
            } else {
                mAdapter.onBindViewHolder(holder, position);
            }
        }

        @Override
        public int getItemCount() {
            if (mAdapter != null) {
                return getFootersCount() + mAdapter.getItemCount();
            } else {
                return getFootersCount();
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(isFooter(position)){
                return TYPE_FOOTER;
            }
            if (mAdapter != null) {
                return mAdapter.getItemViewType(position);
            }
            return TYPE_NORMAL;
        }

        @Override
        public long getItemId(int position) {
            if (mAdapter != null) {
                return mAdapter.getItemId(position);
            }
            return -1;
        }

        private class FooterViewHolder extends RecyclerView.ViewHolder {
            public FooterViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
