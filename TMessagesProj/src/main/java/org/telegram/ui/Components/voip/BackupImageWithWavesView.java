package org.telegram.ui.Components.voip;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.ui.Cells.GroupCallUserCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

public class BackupImageWithWavesView extends FrameLayout {
    private final GroupCallUserCell.AvatarWavesDrawable avatarWavesDrawable;
    private final BackupImageView backupImageView;
    private AnimatorSet animatorSet;
    private boolean isConnectedCalled;

    public BackupImageWithWavesView(Context context) {
        super(context);
        avatarWavesDrawable = new GroupCallUserCell.AvatarWavesDrawable(AndroidUtilities.dp(90), AndroidUtilities.dp(105));
        avatarWavesDrawable.setAmplitude(3f);
        avatarWavesDrawable.setShowWaves(true, this);
        backupImageView = new BackupImageView(context);
        addView(backupImageView, LayoutHelper.createFrame(135, 135, Gravity.CENTER));
        setWillNotDraw(false);
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(this, View.SCALE_X, 1.f, 1.05f, 1f, 1.05f, 1f),
                ObjectAnimator.ofFloat(this, View.SCALE_Y, 1.f, 1.05f, 1f, 1.05f, 1f)
        );
        animatorSet.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        animatorSet.setDuration(3000);
        animatorSet.start();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        /*avatarImageView.setScaleX(avatarWavesDrawable.getAvatarScale());
        avatarImageView.setScaleY(avatarWavesDrawable.getAvatarScale());*/
        super.dispatchDraw(canvas);
    }

    public void setImage(ImageLocation imageLocation, String imageFilter, String ext, Drawable thumb, Object parentObject) {
        backupImageView.setImage(imageLocation, imageFilter, ext, thumb, parentObject);
    }

    public void setImage(ImageLocation imageLocation, String imageFilter, Drawable thumb, Object parentObject) {
        backupImageView.setImage(imageLocation, imageFilter, thumb, parentObject);
    }

    public void setRoundRadius(int value) {
        backupImageView.setRoundRadius(value);
    }

    public void setShowWaves(boolean showWaves) {
        avatarWavesDrawable.setShowWaves(showWaves, this);
    }

    public void setAmplitude(double value) {
        if (value > 1.5f) {
            avatarWavesDrawable.setAmplitude(value);
        } else {
            avatarWavesDrawable.setAmplitude(0);
        }
    }

    public void onConnected() {
        if (isConnectedCalled) return;
        if (animatorSet != null) animatorSet.cancel();
        isConnectedCalled = true;
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(this, View.SCALE_X, this.getScaleX(), 1.05f, 1f),
                ObjectAnimator.ofFloat(this, View.SCALE_Y, this.getScaleY(), 1.05f, 1f)
        );
        animatorSet.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        animatorSet.setDuration(400);
        animatorSet.start();
    }

    public void onNeedRating() {
        setShowWaves(false);
        if (animatorSet != null) animatorSet.cancel();
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(this, View.ALPHA, this.getAlpha(), 1f),
                ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, this.getTranslationY(), -(float) AndroidUtilities.dp(24)),
                ObjectAnimator.ofFloat(this, View.SCALE_X, this.getScaleX(), 0.9f, 1f),
                ObjectAnimator.ofFloat(this, View.SCALE_Y, this.getScaleY(), 0.9f, 1f)
        );
        animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
        animatorSet.setDuration(300);
        animatorSet.setStartDelay(250);
        animatorSet.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        avatarWavesDrawable.update();
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        avatarWavesDrawable.draw(canvas, cx, cy, this);
        super.onDraw(canvas);
    }
}
