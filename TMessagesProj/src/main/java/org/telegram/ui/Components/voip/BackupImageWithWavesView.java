package org.telegram.ui.Components.voip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.ui.Cells.GroupCallUserCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class BackupImageWithWavesView extends FrameLayout {
    private final GroupCallUserCell.AvatarWavesDrawable avatarWavesDrawable;
    private final BackupImageView backupImageView;

    public BackupImageWithWavesView(Context context) {
        super(context);
        avatarWavesDrawable = new GroupCallUserCell.AvatarWavesDrawable(AndroidUtilities.dp(90), AndroidUtilities.dp(105));
        avatarWavesDrawable.setAmplitude(3f);
        avatarWavesDrawable.setShowWaves(true, this);
        backupImageView = new BackupImageView(context);
        addView(backupImageView, LayoutHelper.createFrame(135, 135, Gravity.CENTER));
        setWillNotDraw(false);
        //avatarWavesDrawable.setShowWaves(isSpeaking && progressToAvatarPreview == 0, this);
        //callingUserPhotoView.setScaleX(avatarWavesDrawable.getAvatarScale());
        //callingUserPhotoView.setScaleY(avatarWavesDrawable.getAvatarScale());
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

    @Override
    protected void onDraw(Canvas canvas) {
        avatarWavesDrawable.update();
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        avatarWavesDrawable.draw(canvas, cx, cy, this);
        super.onDraw(canvas);
    }
}
