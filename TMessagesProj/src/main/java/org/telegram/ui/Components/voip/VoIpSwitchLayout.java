package org.telegram.ui.Components.voip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

public class VoIpSwitchLayout extends FrameLayout {
    public enum Type {
        MICRO,
        CAMERA,
        VIDEO
    }

    private final VoIpButtonView voIpButtonView;

    public VoIpSwitchLayout(@NonNull Context context) {
        super(context);
        setWillNotDraw(true);
        voIpButtonView = new VoIpButtonView(context);
        addView(voIpButtonView, LayoutHelper.createFrame(VoIpButtonView.ITEM_SIZE, VoIpButtonView.ITEM_SIZE));
    }

    public void setType(Type type) {
        switch (type) {
            case MICRO:
                voIpButtonView.unSelectedIcon = new RLottieDrawable(R.raw.call_mute, "" + R.raw.call_mute, AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), true, null);
                voIpButtonView.selectedIcon = new RLottieDrawable(R.raw.call_mute, "" + R.raw.call_mute, AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), true, null);
                voIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
                voIpButtonView.selectedIcon.setMasterParent(voIpButtonView);
                break;
            case VIDEO:
                voIpButtonView.unSelectedIcon = new RLottieDrawable(R.raw.video_stop, "" + R.raw.video_stop, AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), true, null);
                voIpButtonView.selectedIcon = new RLottieDrawable(R.raw.video_stop, "" + R.raw.video_stop, AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), true, null);
                voIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
                voIpButtonView.selectedIcon.setMasterParent(voIpButtonView);
                break;
            case CAMERA:
                voIpButtonView.singleIcon = new RLottieDrawable(R.raw.camera_flip2, "" + R.raw.camera_flip2, AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), true, null);
                voIpButtonView.singleIcon = new RLottieDrawable(R.raw.camera_flip2, "" + R.raw.camera_flip2, AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE), true, null);
                voIpButtonView.singleIcon.setMasterParent(voIpButtonView);
                break;
        }
    }

    public static class VoIpButtonView extends View {
        private static final float DARK_LIGHT_PERCENT = 0.15f;
        private static final int DARK_LIGHT_DEFAULT_ALPHA = (int) (255 * DARK_LIGHT_PERCENT);
        private static final int ITEM_SIZE = 52;

        private RLottieDrawable unSelectedIcon;
        private RLottieDrawable selectedIcon;
        private RLottieDrawable singleIcon;
        private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint whiteCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint whiteCircleAlphaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clipPath = new Path();
        private final int maxRadius = AndroidUtilities.dp(ITEM_SIZE / 2f);
        private int unselectedRadius = maxRadius;
        private int selectedRadius = 0;
        private boolean isSelectedState = false;
        private int singleIconBackgroundAlphaPercent = 0;

        public VoIpButtonView(@NonNull Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);

            whiteCirclePaint.setColor(Color.WHITE);
            whiteCircleAlphaPaint.setColor(Color.WHITE);
            whiteCircleAlphaPaint.setAlpha(DARK_LIGHT_DEFAULT_ALPHA);

            maskPaint.setColor(Color.BLACK);
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

            darkPaint.setColor(Color.BLACK);
            darkPaint.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP));
            darkPaint.setAlpha(DARK_LIGHT_DEFAULT_ALPHA);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            if (singleIcon != null) {
                if (isSelectedState) {
                    darkPaint.setAlpha((int) (DARK_LIGHT_DEFAULT_ALPHA * singleIconBackgroundAlphaPercent / 100f));
                    whiteCirclePaint.setAlpha((int) (255 * singleIconBackgroundAlphaPercent / 100f));
                    canvas.drawCircle(cx, cy, maxRadius, whiteCirclePaint);
                    singleIcon.draw(canvas, maskPaint);
                    singleIcon.draw(canvas, darkPaint); //затемнение иконки
                } else {
                    canvas.drawCircle(cx, cy, unselectedRadius, whiteCircleAlphaPaint); //добавляем светлый фон
                    singleIcon.draw(canvas);
                }
                return;
            }

            if (isSelectedState && unselectedRadius > 0 && unselectedRadius != maxRadius) {
                //в процессе смены с выбранно на НЕ выбранно.
                canvas.drawCircle(cx, cy, selectedRadius, whiteCirclePaint);
                canvas.drawCircle(cx, cy, unselectedRadius, maskPaint);

                selectedIcon.setAlpha(255);
                selectedIcon.draw(canvas, maskPaint);
                selectedIcon.setAlpha((int) (255 * DARK_LIGHT_PERCENT));
                selectedIcon.draw(canvas); //затемнение иконки

                clipPath.reset();
                clipPath.addCircle(cx, cy, unselectedRadius, Path.Direction.CW);
                canvas.clipPath(clipPath);
                canvas.drawCircle(cx, cy, unselectedRadius, maskPaint); //убираем весь задний фон
            }

            if (!isSelectedState || unselectedRadius > 0) {
                //не выбранно или в процессе смены с выбранно на НЕ выбранно
                canvas.drawCircle(cx, cy, unselectedRadius, whiteCircleAlphaPaint); //добавляем светлый фон
                unSelectedIcon.draw(canvas);
            }

            if ((isSelectedState && unselectedRadius == 0) || (!isSelectedState && selectedRadius > 0 && unselectedRadius == maxRadius)) {
                //выбранно и не в процессе смены или в процессе смены с НЕ выбранно на выбранно.
                clipPath.reset();
                clipPath.addCircle(cx, cy, selectedRadius, Path.Direction.CW);
                canvas.clipPath(clipPath);
                canvas.drawColor(Color.WHITE); //круговой фон
                selectedIcon.setAlpha(255);
                selectedIcon.draw(canvas, maskPaint);
                selectedIcon.setAlpha((int) (255 * DARK_LIGHT_PERCENT));
                selectedIcon.draw(canvas); //затемнение иконки
            }
        }

        private void switchSelectedState() {
            if (singleIcon != null) {
                if (singleIconBackgroundAlphaPercent != 100 && singleIconBackgroundAlphaPercent != 0)
                    return;
                if (isSelectedState) {
                    ValueAnimator animator = ValueAnimator.ofInt(100, 20);
                    animator.addUpdateListener(animation -> {
                        singleIconBackgroundAlphaPercent = (int) animation.getAnimatedValue();
                        invalidate();
                    });
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            isSelectedState = false;
                            singleIconBackgroundAlphaPercent = 0;
                            invalidate();
                        }
                    });
                    animator.setDuration(200);
                    animator.start();
                } else {
                    isSelectedState = true;
                    ValueAnimator animator = ValueAnimator.ofInt(20, 100);
                    animator.addUpdateListener(animation -> {
                        singleIconBackgroundAlphaPercent = (int) animation.getAnimatedValue();
                        invalidate();
                    });
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            singleIconBackgroundAlphaPercent = 100;
                            invalidate();
                        }
                    });
                    animator.setDuration(200);
                    animator.start();
                }
                singleIcon.setCurrentFrame(0, false);
                singleIcon.start();
                return;
            }
            boolean isUnSelected = unselectedRadius == maxRadius && selectedRadius == 0;
            boolean isSelected = selectedRadius == maxRadius && unselectedRadius == 0;
            if (isUnSelected) {
                ValueAnimator animator = ValueAnimator.ofInt(0, maxRadius);
                animator.addUpdateListener(animation -> {
                    selectedRadius = (int) animation.getAnimatedValue();
                    invalidate();
                });
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isSelectedState = true;
                        unselectedRadius = 0;
                        invalidate();
                    }
                });
                animator.setDuration(200);
                animator.start();
                selectedIcon.setCurrentFrame(0, false);
                selectedIcon.start();
            }
            if (isSelected) {
                ValueAnimator animator = ValueAnimator.ofInt(0, maxRadius);
                animator.addUpdateListener(animation -> {
                    unselectedRadius = (int) animation.getAnimatedValue();
                    invalidate();
                });
                animator.setDuration(200);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isSelectedState = false;
                        selectedRadius = 0;
                        invalidate();
                    }
                });
                animator.start();
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    animate().scaleX(0.8f).scaleY(0.8f).setDuration(150).start();
                    animate().scaleX(0.8f).scaleY(0.8f).setDuration(150).start();
                    break;
                case MotionEvent.ACTION_UP:
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    if (event.getX() < getWidth() && event.getY() < getHeight()) {
                        switchSelectedState();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    break;
            }
            return true;
        }
    }
}
