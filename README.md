# RefreshView

包含下拉刷新和自动加载的View.

###下拉刷新
改自 Google 的 `SwipeRefreshLayout` , 作了如下改动:

1. 修改其下拉样式, 抽取出来作为TopLoadingView, 可根据自身需求自定义View.
2. 修改 `canChildScrollUp()` 方法支持 `AbsListView.setEmptyView()` . 但emptyView的 `onTouchEvent` 方法需要返回true.
3. LoadingView 可以根据拉动的百分比(拉伸\透明)变化.

```Java
	refreshLayout.setLoadingViewScale(boolean scale);
	refreshLayout.setLoadingViewAlpha(boolean alpha);

```
	
4. `onTouchEvent` 在滑动过程中联动子View, 详情可以查看[滑动冲突解决-联动子View](http://niorgai.github.io/2015/10/12/%E6%BB%91%E5%8A%A8%E5%86%B2%E7%AA%81%E8%A7%A3%E5%86%B3-%E8%81%94%E5%8A%A8%E5%AD%90View/).
5. 兼容 `ViewPager` , 解决方案可以查看[滑动冲突解决-更合理的拦截](http://niorgai.github.io/2015/10/15/%E6%BB%91%E5%8A%A8%E5%86%B2%E7%AA%81%E8%A7%A3%E5%86%B3-%E6%9B%B4%E5%90%88%E7%90%86%E7%9A%84%E6%8B%A6%E6%88%AA/).

用法如 `SwipeRefreshLayout` .

###自动加载
目前提供 `AutoLoadListView` , `AutoLoadRecyclerView` 和 `AutoLoadScrollView` 三个控件, 因为涉及加载失败和有没有更多内容的情况, 用法比较复杂.

AutoLoadView的加载会有两种不同的形式:

1. 点击加载: 当View没有填满屏幕时, 此时View无法滚动, 所以需要点击加载.
2. 自动加载: 当View填满屏幕且有更多内容 (`isHaveMore` 为 `true` ) 时, 滚动到底部会自动加载.

查看 `AutoLoadView` 的接口.

 ```Java
	public interface AutoLoadView {

            void onStart();

            void onSuccess(boolean hasMore);

            void onFailure();

            void setIsHaveMore(boolean isHasMore);

            void setOnLoadMoreListener(onLoadMoreListener listener);
        }
 ```
    
以此来说明 `AutoLoadView` 的用法:
    
1. 默认 `AutoLoadView` 没有更多内容,所以第一次需要设置 `setIsHaveMore(true)` . 同时设置 `setLoadMoreListener` .
2. 每次加载完需要设置有没有更多. `onSuccess(boolean hasMore)` .
3. 加载失败设置 `onFailure()` .