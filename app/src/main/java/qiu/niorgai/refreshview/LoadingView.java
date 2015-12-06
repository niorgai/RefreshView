package qiu.niorgai.refreshview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import qiu.niorgai.library.LoadMoreInterface;

/**
 * Created by qiu on 9/20/15.
 */
public class LoadingView extends LinearLayout{

    private TextView textView;

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        inflate(context, R.layout.loading_view, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        textView = (TextView) findViewById(R.id.text);
    }

    public void changeToClickStatus(final LoadMoreInterface.LoadMoreListener loadMoreListener) {
        setVisibility(VISIBLE);
        textView.setText("点击加载");
        if (loadMoreListener != null) {
            textView.setClickable(true);
            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    textView.setText("加载中");
                    loadMoreListener.loadMore();
                    textView.setClickable(false);
                }
            });
        }
    }

    public void changeToLoadingStatus() {
        textView.setText("加载中");
    }

    public void changeToHideStatus() {
        setVisibility(GONE);
    }
}
