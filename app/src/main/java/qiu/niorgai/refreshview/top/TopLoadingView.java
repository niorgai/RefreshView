package qiu.niorgai.refreshview.top;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import qiu.niorgai.refreshview.R;

/**
 * Created by qiu on 12/5/15.
 */
public class TopLoadingView extends FrameLayout {

    protected Animation.AnimationListener mListener;

    private RotateAnimation rotateAnimation;

    private ImageView loadingImageView;

    private boolean isAnimating = false;

    public void setAnimationListener(Animation.AnimationListener listener) {
        mListener = listener;
    }

    public TopLoadingView(Context context) {
        this(context, null);
    }

    public TopLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadingImageView = new ImageView(context);
        loadingImageView.setImageResource(R.drawable.loading_rotate);
        addView(loadingImageView);

        rotateAnimation = new RotateAnimation(0, 1080, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setRepeatCount(-1);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(2000);
        rotateAnimation.setFillAfter(true);
    }

    public void setProgress(float pre) {
        loadingImageView.setRotation(pre * 1080);
        Log.d("run", "setProgress() called with: " + "pre = [" + pre + "]" + " is: " + isAnimating);
        if (pre > 0.99) {
            if (!isAnimating) {
                startAnimation();
            }
        } else {
            stopAnimation();
        }
    }

    public void setViewAlpha(float alpha) {
        loadingImageView.setAlpha(alpha);
    }

    public void startAnimation(){
        loadingImageView.startAnimation(rotateAnimation);
        isAnimating = true;
    }

    public void stopAnimation(){
        rotateAnimation.cancel();
        isAnimating = false;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (mListener != null) {
            mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (mListener != null) {
            mListener.onAnimationEnd(getAnimation());
        }
    }
}