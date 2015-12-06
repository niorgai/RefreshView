package qiu.niorgai.refreshview.bottom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by qiu on 9/20/15.
 */
public class BottomLoadingView extends LinearLayout {

    private TextView textView;

    public BottomLoadingView(Context context) {
        this(context, null);
    }

    public BottomLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        textView = new TextView(context);
        addView(textView);
        LinearLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        textView.setLayoutParams(params);
    }

    public void changeToClickStatus(final Interface.LoadMoreListener loadMoreListener) {
        setVisibility(VISIBLE);
        textView.setText("click to load");
        if (loadMoreListener != null) {
            textView.setClickable(true);
            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    textView.setText("loading");
                    loadMoreListener.loadMore();
                    textView.setClickable(false);
                }
            });
        }
    }

    public void changeToLoadingStatus() {
        textView.setText("loading");
    }

    public void changeToHideStatus() {
        setVisibility(GONE);
    }
}
