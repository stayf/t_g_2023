package org.telegram.ui.Components.voip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.MotionBackgroundDrawable;

public class VoIpGradientLayout extends FrameLayout {
    public enum GradientState {
        CALLING,
        CONNECTED,
        BAD_CONNECTION
    }

    private final MotionBackgroundDrawable bgBlueViolet;
    private final MotionBackgroundDrawable bgBlueGreen;
    private final MotionBackgroundDrawable bgGreen;
    private final MotionBackgroundDrawable bgOrangeRed;
    private int degree = 0;
    private int alphaBlueViolet = 0;
    private int alphaBlueGreen = 0;
    private int alphaGreen = 0;
    private int alphaOrangeRed = 0;
    private float scale = 1.0f;
    private float clipRadius = 0f;
    private boolean showClip = false;
    private final Path clipPath = new Path();
    private int clipCx = 0;
    private int clipCy = 0;
    private ValueAnimator callingAnimator;
    private ValueAnimator badConnectionAnimator;
    private AnimatorSet connectedAnimatorSet;
    private final AnimatorSet defaultAnimatorSet;
    private GradientState state;
    private boolean isPaused = false;
    public volatile boolean blockDrawable = false;

    public VoIpGradientLayout(@NonNull Context context) {
        super(context);
        //низ право, лево. верх лево, право
        bgBlueViolet = new MotionBackgroundDrawable(0xFFB456D8, 0xFF8148EC, 0xFF20A4D7, 0xFF3F8BEA, false);
        bgBlueGreen = new MotionBackgroundDrawable(0xFF4576E9, 0xFF3B7AF1, 0xFF08B0A3, 0xFF17AAE4, false);
        bgGreen = new MotionBackgroundDrawable(0xFF07A9AC, 0xFF07BA63, 0xFFA9CC66, 0xFF5AB147, false);
        bgOrangeRed = new MotionBackgroundDrawable(0xFFE86958, 0xFFE7618F, 0xFFDB904C, 0xFFDE7238, false);
        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        defaultAnimatorSet = new AnimatorSet();
        ValueAnimator rotationAnimator = ValueAnimator.ofInt(0, 360);
        rotationAnimator.addUpdateListener(animation -> {
            degree = (int) animation.getAnimatedValue();
            if (degree == 0 || degree == 1 || degree == 180 || degree == 181) {
                if (isPaused) {
                    defaultAnimatorSet.pause();
                    if (connectedAnimatorSet != null) connectedAnimatorSet.pause();
                }
            }
        });
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setRepeatMode(ValueAnimator.RESTART);
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1.1f, 1.6f, 1.6f, 1.1f, 1.6f, 1.6f, 1.1f);
        scaleAnimator.addUpdateListener(animation -> {
            scale = (float) animation.getAnimatedValue();
        });
        scaleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scaleAnimator.setRepeatMode(ValueAnimator.RESTART);
        defaultAnimatorSet.setInterpolator(new LinearInterpolator());
        defaultAnimatorSet.playTogether(rotationAnimator, scaleAnimator);
        defaultAnimatorSet.setDuration(12000);
        defaultAnimatorSet.start();

        switchToCalling();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (defaultAnimatorSet != null) defaultAnimatorSet.cancel();
        if (connectedAnimatorSet != null) connectedAnimatorSet.cancel();
    }

    public void switchToCalling() {
        if (state == GradientState.CALLING) return;
        state = GradientState.CALLING;
        alphaBlueGreen = 255;
        callingAnimator = ValueAnimator.ofInt(255, 0, 255);
        callingAnimator.addUpdateListener(animation -> {
            alphaBlueViolet = (int) animation.getAnimatedValue();
            invalidate();
        });
        callingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        callingAnimator.setRepeatMode(ValueAnimator.RESTART);
        callingAnimator.setInterpolator(new LinearInterpolator());
        callingAnimator.setDuration(12000);
        callingAnimator.start();
    }

    public boolean isConnectedCalled() {
        return state == GradientState.CONNECTED || state == GradientState.BAD_CONNECTION;
    }

    public void switchToCallConnected(int x, int y) {
        if (state == GradientState.CONNECTED || state == GradientState.BAD_CONNECTION) return;
        state = GradientState.CONNECTED;
        if (callingAnimator != null) {
            callingAnimator.removeAllUpdateListeners();
            callingAnimator.cancel();
        }
        clipCx = x;
        clipCy = y;
        int w = AndroidUtilities.displaySize.x;
        int h = AndroidUtilities.displaySize.y + AndroidUtilities.statusBarHeight + AndroidUtilities.navigationBarHeight;
        double d1 = Math.sqrt((w - x) * (w - x) + (h - y) * (h - y));
        double d2 = Math.sqrt(x * x + (h - y) * (h - y));
        double d3 = Math.sqrt(x * x + y * y);
        double d4 = Math.sqrt((w - x) * (w - x) + y * y);
        double revealMaxRadius = Math.max(Math.max(Math.max(d1, d2), d3), d4);

        showClip = true;
        ValueAnimator revealAnimator = ValueAnimator.ofFloat(0f, (float) revealMaxRadius);
        revealAnimator.addUpdateListener(animation -> {
            clipRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        revealAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showClip = false;
                alphaGreen = 255;
                connectedAnimatorSet = new AnimatorSet();
                ValueAnimator blueGreenAnimator = ValueAnimator.ofInt(0, 255, 255, 255, 0);
                blueGreenAnimator.addUpdateListener(animation2 -> {
                    alphaBlueGreen = (int) animation2.getAnimatedValue();
                    invalidate();
                });
                blueGreenAnimator.setRepeatCount(ValueAnimator.INFINITE);
                blueGreenAnimator.setRepeatMode(ValueAnimator.RESTART);

                ValueAnimator blueVioletAnimator = ValueAnimator.ofInt(0, 0, 255, 0, 0);
                blueVioletAnimator.addUpdateListener(animation2 -> {
                    alphaBlueViolet = (int) animation2.getAnimatedValue();
                    invalidate();
                });
                blueVioletAnimator.setRepeatCount(ValueAnimator.INFINITE);
                blueVioletAnimator.setRepeatMode(ValueAnimator.RESTART);

                connectedAnimatorSet.playTogether(blueVioletAnimator, blueGreenAnimator);
                connectedAnimatorSet.setInterpolator(new LinearInterpolator());
                connectedAnimatorSet.setDuration(24000);
                connectedAnimatorSet.start();
                invalidate();
            }
        });
        revealAnimator.setDuration(400);
        revealAnimator.start();
    }

    public void showToBadConnection() {
        if (state == GradientState.BAD_CONNECTION) return;
        state = GradientState.BAD_CONNECTION;
        badConnectionAnimator = ValueAnimator.ofInt(alphaOrangeRed, 255);
        badConnectionAnimator.addUpdateListener(animation -> {
            alphaOrangeRed = (int) animation.getAnimatedValue();
            invalidate();
        });
        badConnectionAnimator.setDuration(500);
        badConnectionAnimator.start();
    }

    public void hideBadConnection() {
        if (state == GradientState.CONNECTED) return;
        state = GradientState.CONNECTED;
        if (badConnectionAnimator != null) {
            badConnectionAnimator.removeAllUpdateListeners();
            badConnectionAnimator.cancel();
        }
        badConnectionAnimator = ValueAnimator.ofInt(alphaOrangeRed, 0);
        badConnectionAnimator.addUpdateListener(animation -> {
            alphaOrangeRed = (int) animation.getAnimatedValue();
            invalidate();
        });
        badConnectionAnimator.setDuration(500);
        badConnectionAnimator.start();
    }

    public void pause() {
        if (isPaused) return;
        isPaused = true;
    }

    public void resume() {
        if (!isPaused) return;
        isPaused = false;
        if (defaultAnimatorSet.isPaused()) defaultAnimatorSet.resume();
        if (connectedAnimatorSet != null && connectedAnimatorSet.isPaused())
            connectedAnimatorSet.resume();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        bgGreen.setBounds(0, 0, getWidth(), getHeight());
        bgOrangeRed.setBounds(0, 0, getWidth(), getHeight());
        bgBlueGreen.setBounds(0, 0, getWidth(), getHeight());
        bgBlueViolet.setBounds(0, 0, getWidth(), getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (blockDrawable) return;
        float halfWidth = getWidth() / 2f;
        float halfHeight = getHeight() / 2f;
        canvas.save();
        canvas.scale(scale, scale, halfWidth, halfHeight);
        canvas.rotate(degree, halfWidth, halfHeight);

        if (alphaGreen != 0 && alphaOrangeRed != 255) {
            bgGreen.setAlpha(alphaGreen);
            bgGreen.draw(canvas);
        }
        if (alphaBlueGreen != 0 && alphaOrangeRed != 255) {
            bgBlueGreen.setAlpha(alphaBlueGreen);
            bgBlueGreen.draw(canvas);
        }
        if (alphaBlueViolet != 0 && alphaOrangeRed != 255) {
            bgBlueViolet.setAlpha(alphaBlueViolet);
            bgBlueViolet.draw(canvas);
        }

        if (alphaOrangeRed != 0) {
            bgOrangeRed.setAlpha(alphaOrangeRed);
            bgOrangeRed.draw(canvas);
        }
        canvas.restore();

        if (showClip) {
            clipPath.reset();
            clipPath.addCircle(clipCx, clipCy, clipRadius, Path.Direction.CW);
            canvas.clipPath(clipPath);
            canvas.scale(scale, scale, halfWidth, halfHeight);
            canvas.rotate(degree, halfWidth, halfHeight);
            bgGreen.setAlpha(255);
            bgGreen.draw(canvas);
        }
        super.onDraw(canvas);
    }
}
