package qiu.niorgai.refreshview.bottom;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * 自动加载更多的ListView
 * Created by qiu on 9/18/15.
 */
public class AutoLoadListView extends ListView implements AbsListView.OnScrollListener, LoadMoreInterface.AutoLoadView {

    public static final String TAG = "AutoLoadListView";

    private Context mContext;

    private float mDownY;
    //是否向下滑动
    private boolean isScrollingDown = false;
    //是否从下面布局
    private boolean isStackFromBottom = false;
    //是否最后一个item可见
    private boolean isLastItemVisible = false;
    //是否加载失败,此时应该变成点击加载更多
    private boolean isLoadingFail = false;
    //是否正在加载
    private boolean isLoadingMore = false;

    private boolean isHaveMore = false;

    private BottomLoadingView mLoadingView;

    private WrapAdapter mAdapter;

    //TYPE_FOOTER为FooterView
    private static final int TYPE_FOOTER = -1;
    //TYPE_NORMAL为普通item,由adapter控制
    private static final int TYPE_NORMAL = -2;

    private LoadMoreInterface.onLoadMoreListener onLoadMoreListener;

    public AutoLoadListView(Context context) {
        this(context, null);
    }

    public AutoLoadListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnScrollListener(this);
    }

    public interface onScrolledListener {
        void scrollStateChanged(AbsListView view, int scrollState);
        void scroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount);
    }

    private onScrolledListener listener;

    public void setListener(onScrolledListener listener) {
        this.listener = listener;
    }

    @Override
    public void setStackFromBottom(boolean stackFromBottom) {
        isStackFromBottom = stackFromBottom;
        super.setStackFromBottom(stackFromBottom);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (listener != null) {
            listener.scrollStateChanged(view, scrollState);
        }
        //没有正在加载 && 向下滑动 && 最后一个item可见 && 滑动结束
        if (isHaveMore && !isLoadingMore && isScrollingDown && isLastItemVisible && scrollState == SCROLL_STATE_IDLE) {
            if (onLoadMoreListener != null) {
                onLoadMoreListener.onLoadMore();
                isLoadingMore = true;
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (listener != null) {
            listener.scroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
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
                if (!isStackFromBottom) {
                    if (ev.getY() < mDownY) {
                        //有滑动动作且是往下滚动
                        isScrollingDown = true;
                    }
                } else {
                    if (ev.getY() > mDownY) {
                        isScrollingDown = true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isStackFromBottom) {
                    if (ev.getY() < mDownY) {
                        //有滑动动作且是往下滚动
                        isScrollingDown = true;
                    }
                } else {
                    if (ev.getY() > mDownY) {
                        isScrollingDown = true;
                    }
                }
                break;
            default:
                break;
        }
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    @Override
    public void onStart() {
        if (onLoadMoreListener != null) {
            isLoadingMore = true;
            isLoadingFail = false;
            onLoadMoreListener.onLoadMore();
        }
    }

    @Override
    public void onSuccess(boolean hasMore) {
        isLoadingMore = false;
        isHaveMore = hasMore;
        isLoadingFail = false;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFailure() {
        isLoadingMore = false;
        isLoadingFail = true;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setIsHaveMore(boolean isHaveMore) {
        this.isHaveMore = isHaveMore;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setOnLoadMoreListener(LoadMoreInterface.onLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        mAdapter = new WrapAdapter(adapter);
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                requestLayout();
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                requestLayout();
                mAdapter.notifyDataSetInvalidated();
            }
        });
        super.setAdapter(mAdapter);
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        final OnItemClickListener listenerToSet = listener;
        super.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isHaveMore && position == mAdapter.getCount()) {
                    //防止点击了Footer
                } else {
                    if (listenerToSet != null) {
                        listenerToSet.onItemClick(parent, view, position, id);
                    }
                }
            }
        });
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
        public int getViewTypeCount() {
            if (mAdapter == null) {
                return 2;
            } else {
                return mAdapter.getViewTypeCount() + 2;
            }
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
                    convertView = new BottomLoadingView(mContext);
                    convertView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!isLoadingFail) {
                                return;
                            }
                            mLoadingView.changeToLoadingStatus();
                            onStart();
                        }
                    });
                    convertView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
                mLoadingView = (BottomLoadingView) convertView;
                int childViewCount = getChildCount();
                int allCount = getCount();
                if (childViewCount == allCount - 1 && isHaveMore) {
                    //未填满屏幕并且有更多
                    isLoadingFail = true;
                    mLoadingView.changeToClickStatus();
                } else {
                    mLoadingView.changeToLoadingStatus();
                }
                return convertView;
            } else {
                if (convertView instanceof BottomLoadingView) {
                    //防止错位
                    return mAdapter.getView(position, null, parent);
                }
                return mAdapter.getView(position, convertView, parent);
            }
        }
    }
}

