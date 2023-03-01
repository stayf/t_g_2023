package org.telegram.ui.Components.voip;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VoIPNotificationsLayout extends LinearLayout {

    HashMap<String, NotificationView> viewsByTag = new HashMap<>();
    ArrayList<NotificationView> viewToAdd = new ArrayList<>();
    ArrayList<NotificationView> viewToRemove = new ArrayList<>();
    TransitionSet transitionSet;
    boolean lockAnimation;
    boolean wasChanged;
    Runnable onViewsUpdated;
    private boolean isDarkBg;

    public VoIPNotificationsLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
        isDarkBg = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            transitionSet = new TransitionSet();
            transitionSet.addTransition(new Fade(Fade.OUT).setDuration(150))
                    .addTransition(new ChangeBounds().setDuration(200))
                    .addTransition(new Visibility() {
                        @Override
                        public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
                            AnimatorSet set = new AnimatorSet();
                            view.setAlpha(0);
                            view.setScaleY(0.6f);
                            view.setScaleX(0.6f);
                            set.playTogether(
                                    ObjectAnimator.ofFloat(view, View.ALPHA, 0, 1f),
                                    ObjectAnimator.ofFloat(view, View.SCALE_X, 0.6f, 1f),
                                    ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.6f, 1f)
                            );
                            set.setInterpolator(CubicBezierInterpolator.EASE_OUT_BACK);
                            return set;
                        }

                        @Override
                        public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
                            AnimatorSet set = new AnimatorSet();
                            set.playTogether(
                                    ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f),
                                    ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.6f),
                                    ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.6f)
                            );
                            set.setInterpolator(CubicBezierInterpolator.DEFAULT);
                            return set;
                        }
                    }.setDuration(200));
            transitionSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
        }
    }

    public void updateBgDrawable(boolean isDark) {
        if (isDarkBg != isDark) {
            isDarkBg = isDark;
            for (Map.Entry<String, VoIPNotificationsLayout.NotificationView> entry : viewsByTag.entrySet()) {
                VoIPNotificationsLayout.NotificationView view = entry.getValue();
                view.updateBgDrawable(isDarkBg);
            }
        }
    }

    public void addNotification(int iconRes, String text, String tag, boolean animated) {
        if (viewsByTag.get(tag) != null) {
            return;
        }

        NotificationView view = new NotificationView(getContext());
        view.tag = tag;
        view.iconView.setImageResource(iconRes);
        view.textView.setText(text);
        view.updateBgDrawable(isDarkBg);
        viewsByTag.put(tag, view);

        /*if (animated) {
            view.startAnimation();
        }*/
        if (lockAnimation) {
            viewToAdd.add(view);
        } else {
            wasChanged = true;
            addView(view, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 4, 0, 0, 4));
        }
    }

    public void removeNotification(String tag) {
        NotificationView view = viewsByTag.remove(tag);
        if (view != null) {
            if (lockAnimation) {
                if (viewToAdd.remove(view)) {
                    return;
                }
                viewToRemove.add(view);
            } else {
                wasChanged = true;
                removeView(view);
            }
        }
    }

    private void lock() {
        lockAnimation = true;
        AndroidUtilities.runOnUIThread(() -> {
            lockAnimation = false;
            runDelayed();
        }, 700);
    }

    private void runDelayed() {
        if (viewToAdd.isEmpty() && viewToRemove.isEmpty()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewParent parent = getParent();
            if (parent != null) {
                TransitionManager.beginDelayedTransition(this, transitionSet);
            }
        }

        for (int i = 0; i < viewToAdd.size(); i++) {
            NotificationView view = viewToAdd.get(i);
            for (int j = 0; j < viewToRemove.size(); j++) {
                if (view.tag.equals(viewToRemove.get(j).tag)) {
                    viewToAdd.remove(i);
                    viewToRemove.remove(j);
                    i--;
                    break;
                }
            }
        }

        for (int i = 0; i < viewToAdd.size(); i++) {
            addView(viewToAdd.get(i), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 4, 0, 0, 4));
        }
        for (int i = 0; i < viewToRemove.size(); i++) {
            removeView(viewToRemove.get(i));
        }
        viewsByTag.clear();
        for (int i = 0; i < getChildCount(); i++) {
            NotificationView v = (NotificationView) getChildAt(i);
            viewsByTag.put(v.tag, v);
        }
        viewToAdd.clear();
        viewToRemove.clear();
        lock();
        if (onViewsUpdated != null) {
            onViewsUpdated.run();
        }
    }

    public void beforeLayoutChanges() {
        wasChanged = false;
        if (!lockAnimation) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ViewParent parent = getParent();
                if (parent != null) {
                    TransitionManager.beginDelayedTransition(this, transitionSet);
                }
            }
        }
    }

    public void animateLayoutChanges() {
        if (wasChanged) {
            lock();
        }
        wasChanged = false;
    }

    public int getChildsHight() {
        int n = getChildCount();
        return (n > 0 ? AndroidUtilities.dp(16) : 0) + n * AndroidUtilities.dp(32);
    }

    private static class NotificationView extends FrameLayout {

        public String tag;
        ImageView iconView;
        TextView textView;
        private Drawable lightBgDrawable;
        private Drawable darkBgDrawable;
        private boolean isDarkBg;

        public NotificationView(@NonNull Context context) {
            super(context);
            setFocusable(true);
            setFocusableInTouchMode(true);

            lightBgDrawable = Theme.createRoundRectDrawable(AndroidUtilities.dp(16), ColorUtils.setAlphaComponent(Color.BLACK, (int) (255 * 0.12f)));
            darkBgDrawable = Theme.createRoundRectDrawable(AndroidUtilities.dp(16), ColorUtils.setAlphaComponent(Color.BLACK, (int) (255 * 0.4f)));
            isDarkBg = false;

            iconView = new ImageView(context);
            setBackground(lightBgDrawable);
            addView(iconView, LayoutHelper.createFrame(24, 24, 0, 10, 4, 10, 4));

            textView = new TextView(context);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 44, 4, 16, 4));
        }

        public void updateBgDrawable(boolean isDark) {
            if (isDarkBg != isDark) {
                isDarkBg = isDark;
                if (isDarkBg) {
                    setBackground(darkBgDrawable);
                } else {
                    setBackground(lightBgDrawable);
                }
            }
        }

        public void startAnimation() {
            textView.setVisibility(View.INVISIBLE);
            postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionSet transitionSet = new TransitionSet();
                    transitionSet.
                            addTransition(new Fade(Fade.IN).setDuration(150))
                            .addTransition(new ChangeBounds().setDuration(200));
                    transitionSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
                    ViewParent parent = getParent();
                    if (parent != null) {
                        TransitionManager.beginDelayedTransition((ViewGroup) parent, transitionSet);
                    }
                }

                textView.setVisibility(View.VISIBLE);
            }, 400);
        }
    }

    public void setOnViewsUpdated(Runnable onViewsUpdated) {
        this.onViewsUpdated = onViewsUpdated;
    }
}
