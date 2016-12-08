package qiu.niorgai.refreshview.bottom;

/**
 * Created by qiu on 9/18/15.
 */
public class LoadMoreInterface {

    public interface onLoadMoreListener {
        void onLoadMore();
    }

    public interface AutoLoadView {

        void onStart();

        void onSuccess(boolean hasMore);

        void onFailure();

        void setIsHaveMore(boolean isHasMore);

        void setOnLoadMoreListener(onLoadMoreListener listener);
    }

}
