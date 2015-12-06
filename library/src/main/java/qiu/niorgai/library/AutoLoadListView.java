package qiu.niorgai.library;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created by qiu on 9/18/15.
 */
public class AutoLoadListView extends ListView implements AbsListView.OnScrollListener, LoadMoreInterface.LoadMoreComplete {

    private Context mContext;

    private float mDownY;
    //是否向下滑动
    private boolean isScrollingDown = false;
    //是否最后一个item可见
    private boolean isLastItemVisible = false;
    //是否正在加载
    private boolean isLoadingMore = false;

    private boolean isHaveMore = false;

    private LoadMoreInterface.BottomLoadingView mLoadingView;

    //TYPE_FOOTER为FooterView
    private static final int TYPE_FOOTER = -1;
    //TYPE_NORMAL为普通item,由adapter控制
    private static final int TYPE_NORMAL = -2;

    private LoadMoreInterface.LoadMoreListener loadMoreListener;

    public AutoLoadListView(Context context) {
        this(context, null);
    }

    public AutoLoadListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnScrollListener(this);
    }


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    //没有正在加载 && 向下滑动 && 最后一个item可见
        if (isHaveMore && !isLoadingMore && isScrollingDown && isLastItemVisible) {
            if (loadMoreListener != null) {
                loadMoreListener.loadMore();
                isLoadingMore = true;
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        //判断是否最后一个item可见).
        if (firstVisibleItem + visibleItemCount >= totalItemCount) {
            isLastItemVisible = true;
        } else {
            isLastItemVisible = false;
        }
    }

    /**
     * 检测用户是否往下滑动
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                isScrollingDown = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (ev.getY() < mDownY) {
                    //有滑动动作且是往下滚动
                    isScrollingDown = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (ev.getY() < mDownY) {
                    //有滑动动作且是往下滚动
                    isScrollingDown = true;
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public void onComplete(boolean hasMore) {
        isLoadingMore = false;
        isHaveMore = hasMore;
    }

    @Override
    public void onError() {
        isLoadingMore = false;
        if (mLoadingView != null) {
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

    @Override
    public void setAdapter(ListAdapter adapter) {
        final WrapAdapter mAdapter = new WrapAdapter(adapter);
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetInvalidated();
                    }
                });
            }
        });
        super.setAdapter(mAdapter);
    }

    //封装Adapter加上LoadingView
    private class WrapAdapter extends BaseAdapter {

        private BaseAdapter mAdapter;

        private WrapAdapter(ListAdapter adapter) {
            mAdapter = (BaseAdapter) adapter;
        }

        @Override
        public int getItemViewType(int position) {
            if(isHaveMore && position == getCount() - 1){
                return TYPE_FOOTER;
            }
            if (mAdapter != null) {
                return mAdapter.getItemViewType(position);
            }
            return TYPE_NORMAL;
        }

        @Override
        public int getCount() {
            if (isHaveMore) {
                return mAdapter.getCount() + 1;
            } else {
                return mAdapter.getCount();
            }
        }

        @Override
        public Object getItem(int position) {
            if (isHaveMore && position == getCount() - 1) {
                return null;
            } else {
                return mAdapter.getItem(position);
            }
        }

        @Override
        public long getItemId(int position) {
            if (isHaveMore && position == getCount() - 1) {
                return 0;
            } else {
                return mAdapter.getItemId(position);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getItemViewType(position) == TYPE_FOOTER) {
                if (convertView == null) {
                    //TODO::empty
//                    convertView = new LoadingView(mContext);
                    convertView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
                mLoadingView = (LoadMoreInterface.BottomLoadingView) convertView;
                int childViewCount = getChildCount();
                int allCount = getCount();
                if (childViewCount == allCount - 1 && isHaveMore) {
                    //未填满屏幕并且有更多
                    mLoadingView.changeToClickStatus(loadMoreListener);
                } else {
                    mLoadingView.changeToLoadingStatus();
                }
                return convertView;
            } else {
                return mAdapter.getView(position, convertView, parent);
            }
        }
    }
}
