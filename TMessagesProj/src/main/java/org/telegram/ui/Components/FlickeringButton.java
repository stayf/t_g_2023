package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.voip.CellFlickerDrawable;

public class FlickeringButton extends TextView {
    private static final float PROGRESS_START_PERCENT = 0f;
    private static final float PROGRESS_END_PERCENT = 1f;
    private static final long ANIMATION_DURATION_MS = 1200;
    private static final long DELAY_DURATION_MS = 1200;

    private final CellFlickerDrawable cellFlickerDrawable;
    private final ValueAnimator animator;
    private final Runnable repeatRunnable = new Runnable() {
        @Override
        public void run() {
            animator.start();
        }
    };
    private boolean needFlickering;

    public FlickeringButton(Context context) {
        super(context);
        cellFlickerDrawable = new CellFlickerDrawable();
        animator = ValueAnimator.ofFloat(PROGRESS_START_PERCENT, PROGRESS_END_PERCENT);
        animator.addUpdateListener(animation -> {
            cellFlickerDrawable.setProgress((float) animation.getAnimatedValue());
            invalidate();
        });
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
    }

    public void setNeedFlickering(boolean needFlickering) {
        this.needFlickering = needFlickering;
    }

    public void pause() {
        animator.removeAllListeners();
        animator.cancel();
        removeCallbacks(repeatRunnable);
    }

    public void resume() {
        if (needFlickering) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    postDelayed(repeatRunnable, DELAY_DURATION_MS);
                }
            });
            animator.start();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (needFlickering) {
            cellFlickerDrawable.setParentWidth(getMeasuredWidth());
            AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            cellFlickerDrawable.simpleDraw(canvas, AndroidUtilities.rectTmp, AndroidUtilities.dp(4));
        }
    }
}
