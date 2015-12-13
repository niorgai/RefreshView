package qiu.niorgai.refreshview.bottom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import qiu.niorgai.refreshview.R;

/**
 * Created by qiu on 9/20/15.
 */
public class BottomLoadingView extends FrameLayout {

    private ImageView progressImage;

    private TextView textView;

    private RotateAnimation rotateAnimation;


    public BottomLoadingView(Context context) {
        this(context, null);
    }

    public BottomLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.bottom_loading_view, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int padding = (int) (context.getResources().getDisplayMetrics().density * 5f);
        setPadding(padding, padding, padding, padding);
        textView = (TextView) findViewById(R.id.bottom_text);
        progressImage = (ImageView) findViewById(R.id.bottom_progress_bar);

        rotateAnimation = new RotateAnimation(0, 1080, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setRepeatCount(-1);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(2000);
        rotateAnimation.setFillAfter(true);

        progressImage.setAnimation(rotateAnimation);
    }

    public void changeToClickStatus(final LoadMoreInterface.onLoadMoreListener onLoadMoreListener) {
        setVisibility(VISIBLE);
        progressImage.setVisibility(GONE);
        rotateAnimation.cancel();
        textView.setText(R.string.click_to_load);
        if (onLoadMoreListener != null) {
            BottomLoadingView.this.setClickable(true);
            BottomLoadingView.this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    textView.setText(R.string.load_view_loading);
                    onLoadMoreListener.onLoadMore();
                    BottomLoadingView.this.setClickable(false);
                }
            });
        }
    }

    public void changeToLoadingStatus() {
        setVisibility(VISIBLE);
        progressImage.setVisibility(VISIBLE);
        rotateAnimation.start();
        textView.setText(R.string.load_view_loading);
    }

    public void changeToHideStatus() {
        setVisibility(GONE);
        rotateAnimation.cancel();
    }
}
