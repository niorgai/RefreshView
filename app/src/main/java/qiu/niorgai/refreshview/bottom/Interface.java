package qiu.niorgai.refreshview.bottom;

/**
 * Created by qiu on 9/18/15.
 */
public class Interface {

    public interface LoadMoreListener {
        void loadMore();
    }

    public interface AutoLoadView {
        void onComplete(boolean hasMore);

        void onError();

        void setIsHaveMore(boolean isHasMore);

        void setLoadMoreListener(LoadMoreListener listener);
    }

}
