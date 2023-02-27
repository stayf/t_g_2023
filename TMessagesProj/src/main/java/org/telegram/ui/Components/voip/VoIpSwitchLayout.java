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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

public class VoIpSwitchLayout extends FrameLayout {
    public enum Type {
        MICRO,
        CAMERA,
        VIDEO,
        BLUETOOTH,
        SPEAKER,
    }

    private final VoIpButtonView voIpButtonView;
    private Type type;
    private final TextView currentTextView;
    private final TextView newTextView;

    public VoIpSwitchLayout(@NonNull Context context) {
        super(context);
        setWillNotDraw(true);
        voIpButtonView = new VoIpButtonView(context);
        voIpButtonView.setBeforeStateChange(isSelected -> setText(type, isSelected));
        addView(voIpButtonView, LayoutHelper.createFrame(VoIpButtonView.ITEM_SIZE, VoIpButtonView.ITEM_SIZE, Gravity.CENTER_HORIZONTAL));

        currentTextView = new TextView(context);
        currentTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        currentTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        currentTextView.setTextColor(Color.WHITE);
        currentTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(currentTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, VoIpButtonView.ITEM_SIZE + 4, 0, 0));

        newTextView = new TextView(context);
        newTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        newTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        newTextView.setTextColor(Color.WHITE);
        newTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(newTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, VoIpButtonView.ITEM_SIZE + 4, 0, 0));
        currentTextView.setVisibility(GONE);
        newTextView.setVisibility(GONE);
    }

    private void setText(Type type, boolean isSelectedState) {
        final String newText;
        switch (type) {
            case MICRO:
                if (isSelectedState) {
                    newText = "Mute";
                } else {
                    newText = "Unmute";
                }
                break;
            case CAMERA:
                newText = "Flip";
                break;
            case VIDEO:
                if (isSelectedState) {
                    newText = "Start Video";
                } else {
                    newText = "Stop Video";
                }
                break;
            case BLUETOOTH:
                newText = "Bluetooth";
                break;
            case SPEAKER:
                newText = "Speaker";
                break;
            default:
                newText = "";
        }

        if (currentTextView.getVisibility() == GONE && newTextView.getVisibility() == GONE) {
            currentTextView.setVisibility(VISIBLE);
            currentTextView.setText(newText);
            return;
        }

        if (currentTextView.getText().equals(newText)) {
            return;
        }

        currentTextView.animate().alpha(0f).translationY(-AndroidUtilities.dp(4)).setDuration(150).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentTextView.setText(newText);
                currentTextView.setTranslationY(0);
                currentTextView.setAlpha(1.0f);
            }
        }).start();
        newTextView.setText(newText);
        newTextView.setVisibility(VISIBLE);
        newTextView.setAlpha(0);
        newTextView.setTranslationY(AndroidUtilities.dp(8));
        newTextView.animate().alpha(1.0f).translationY(0).setDuration(150).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                newTextView.setVisibility(GONE);
            }
        }).start();
    }

    public void setType(Type type) {
        int size = AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE);
        switch (type) {
            case MICRO:
                voIpButtonView.unSelectedIcon = new RLottieDrawable(R.raw.call_mute, "" + R.raw.call_mute, size, size, true, null);
                voIpButtonView.selectedIcon = new RLottieDrawable(R.raw.call_mute, "" + R.raw.call_mute, size, size, true, null);
                voIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
                voIpButtonView.selectedIcon.setMasterParent(voIpButtonView);
                break;
            case VIDEO:
                voIpButtonView.unSelectedIcon = new RLottieDrawable(R.raw.video_stop, "" + R.raw.video_stop, size, size, true, null);
                voIpButtonView.selectedIcon = new RLottieDrawable(R.raw.video_stop, "" + R.raw.video_stop, size, size, true, null);
                voIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
                voIpButtonView.selectedIcon.setMasterParent(voIpButtonView);
                break;
            case CAMERA:
                if (voIpButtonView.singleIcon != null) {
                    voIpButtonView.singleIcon.recycle(false);
                }
                voIpButtonView.singleIcon = new RLottieDrawable(R.raw.camera_flip2, "" + R.raw.camera_flip2, size, size, true, null);
                voIpButtonView.singleIcon.setMasterParent(voIpButtonView);
                break;
            case SPEAKER:
                if (this.type == Type.BLUETOOTH) {
                    voIpButtonView.singleIcon.setOnAnimationEndListener(() -> {
                        AndroidUtilities.runOnUIThread(() -> {
                            voIpButtonView.singleIcon = new RLottieDrawable(R.raw.speaker_to_bt, "" + R.raw.speaker_to_bt, size, size, true, null);
                            voIpButtonView.singleIcon.setMasterParent(voIpButtonView);
                        });
                    });
                    voIpButtonView.singleIcon.start();
                } else {
                    voIpButtonView.singleIcon = new RLottieDrawable(R.raw.speaker_to_bt, "" + R.raw.speaker_to_bt, size, size, true, null);
                    voIpButtonView.singleIcon.setMasterParent(voIpButtonView);
                }
                break;
            case BLUETOOTH:
                if (this.type == Type.SPEAKER) {
                    voIpButtonView.singleIcon.setOnAnimationEndListener(() -> {
                        AndroidUtilities.runOnUIThread(() -> {
                            voIpButtonView.singleIcon = new RLottieDrawable(R.raw.bt_to_speaker, "" + R.raw.bt_to_speaker, size, size, true, null);
                            voIpButtonView.singleIcon.setMasterParent(voIpButtonView);
                        });
                    });
                    voIpButtonView.singleIcon.start();
                } else {
                    voIpButtonView.singleIcon = new RLottieDrawable(R.raw.bt_to_speaker, "" + R.raw.bt_to_speaker, size, size, true, null);
                    voIpButtonView.singleIcon.setMasterParent(voIpButtonView);
                }
                break;
        }
        setText(type, voIpButtonView.isSelectedState);
        this.type = type;
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
        private BeforeStateChange beforeStateChange;

        interface BeforeStateChange {
            void beforeStateChange(boolean isSelected);
        }

        public void setBeforeStateChange(BeforeStateChange beforeStateChange) {
            this.beforeStateChange = beforeStateChange;
        }

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
                    if (beforeStateChange != null) beforeStateChange.beforeStateChange(true);
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
                    if (beforeStateChange != null) beforeStateChange.beforeStateChange(false);
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
                if (beforeStateChange != null) beforeStateChange.beforeStateChange(true);
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
                if (beforeStateChange != null) beforeStateChange.beforeStateChange(false);
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
