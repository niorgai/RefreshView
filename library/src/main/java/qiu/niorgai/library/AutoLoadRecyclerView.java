package qiu.niorgai.library;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by qiu on 9/18/15.
 */
public class AutoLoadRecyclerView extends RecyclerView implements LoadMoreInterface.LoadMoreComplete{

    //是否向下滑动
    private boolean isScrollingDown = false;
    //是否正在加载
    private boolean isLoadingMore = false;

    //TYPE_FOOTER为FooterView
    private static final int TYPE_FOOTER = -1;
    //TYPE_NORMAL为普通item,由adapter控制
    private static final int TYPE_NORMAL = -2;

    //Wrapper模式,为adapter再封装一层,同时加入FooterView
    private WrapAdapter mWrapAdapter;

    private LoadMoreInterface.BottomLoadingView mLoadingView;

    private boolean isHasMore = false;

    private LoadMoreInterface.LoadMoreListener loadMoreListener;

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

    private void init(Context context) {
        //TODO:: empty
//        mLoadingView = new LoadingView(context);
        this.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //往下滑动 && 没有正在加载 && 注册了listener
                if (isScrollingDown && !isLoadingMore && loadMoreListener != null) {
                    LayoutManager layoutManager = getLayoutManager();
                    if (layoutManager == null) {
                        return;
                    }
                    int lastVisibleItemPosition = getLastPosition();
                    if (layoutManager.getChildCount() > 0
                            && layoutManager.getItemCount() > layoutManager.getChildCount()
                            && lastVisibleItemPosition >= layoutManager.getItemCount() - 1) {
                        isLoadingMore = true;
                        loadMoreListener.loadMore();
                        mLoadingView.changeToLoadingStatus();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //记录是否正在向下滑动
                isScrollingDown = dy > 0;
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
        return lastVisibleItemPosition;
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

    @Override
    public void setAdapter(Adapter adapter) {
        mWrapAdapter = new WrapAdapter(adapter);
        super.setAdapter(mWrapAdapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                mWrapAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mWrapAdapter.notifyItemRangeInserted(positionStart, itemCount);
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
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                mWrapAdapter.notifyItemMoved(fromPosition, toPosition);
            }
        });
    }

    @Override
    public void onComplete(boolean hasMore) {
        isLoadingMore = false;
        isHasMore = hasMore;
    }

    @Override
    public void onError() {
         isLoadingMore = false;
         mLoadingView.changeToClickStatus(loadMoreListener);
     }

    @Override
    public void setIsHaveMore(boolean isHasMore) {
        this.isHasMore = isHasMore;
    }

    @Override
    public void setLoadMoreListener(LoadMoreInterface.LoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }

    //Wrapper模式封装Adapter
    private class WrapAdapter extends RecyclerView.Adapter<ViewHolder> {

        private Adapter mAdapter;

        public WrapAdapter(RecyclerView.Adapter adapter) {
            this.mAdapter = adapter;
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            //为Footer单独设置SpanSize
            RecyclerView.LayoutManager manager = getLayoutManager();
            if(manager instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) manager);
                final GridLayoutManager.SpanSizeLookup originSizeLookup = gridManager.getSpanSizeLookup();
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return (getItemViewType(position) == TYPE_FOOTER) ? gridManager.getSpanCount() : originSizeLookup.getSpanSize(position);
                    }
                });
            }
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
                return new FooterViewHolder((View) mLoadingView);
            }
            return mAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof FooterViewHolder) {
                int lastPosition = getLastPosition();
                int childCount = getLayoutManager().getChildCount();
                if (lastPosition < childCount) {
                    //不充满的情况下
                    if (isHasMore) {
                        mLoadingView.changeToClickStatus(loadMoreListener);
                    }
                } else {
                    if (lastPosition > childCount) {
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
