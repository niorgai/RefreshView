package qiu.niorgai.refreshview;

/**
 * Created by qiu on 9/18/15.
 */
public class LoadMoreInterface {

    public interface LoadMoreListener {
        void loadMore();
    }

    public interface LoadMoreComplete {
        void onComplete(boolean hasMore);

        void onError();

        void setIsHaveMore(boolean isHasMore);

        void setLoadMoreListener(LoadMoreListener listener);
    }

    public interface BottomLoadingViewInterface {
        void changeToClickStatus(LoadMoreInterface.LoadMoreListener loadMoreListener);

        void changeToLoadingStatus();

        void changeToHideStatus();
    }

}
