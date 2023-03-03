package org.telegram.ui.Components.voip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.voip.VideoCapturerDevice;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MotionBackgroundDrawable;
import org.telegram.ui.LaunchActivity;
import org.webrtc.RendererCommon;

import java.io.File;
import java.io.FileOutputStream;

@TargetApi(21)
public abstract class PrivateVideoPreviewDialogNew extends FrameLayout implements VoIPService.StateListener {

    private boolean isDismissed;

    private FrameLayout viewPager;
    private TextView positiveButton;
    private LinearLayout titlesLayout;
    private TextView[] titles;
    private VoIPTextureView textureView;
    private int visibleCameraPage = 1;
    private boolean cameraReady;
    private ActionBar actionBar;

    public boolean micEnabled;

    private float pageOffset;
    private int strangeCurrentPage;
    private int realCurrentPage;

    private float openProgress1 = 0f;
    private float openProgress2 = 0f;
    private float closeProgress = 0f;
    private float openTranslationX;
    private float openTranslationY;
    private final float startLocationX;
    private final float startLocationY;
    private final Path clipPath = new Path();

    public PrivateVideoPreviewDialogNew(Context context, boolean mic, boolean screencast, float startLocationX, float startLocationY) {
        super(context);

        this.startLocationX = startLocationX;
        this.startLocationY = startLocationY;
        titles = new TextView[3];

        viewPager = new FrameLayout(context);
        addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        textureView = new VoIPTextureView(context, false, false);
        textureView.renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        textureView.scaleType = VoIPTextureView.SCALE_TYPE_FIT;
        textureView.clipToTexture = true;
        textureView.renderer.setAlpha(0);
        textureView.renderer.setRotateTextureWithScreen(true);
        textureView.renderer.setUseCameraRotation(true);
        addView(textureView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        actionBar = new ActionBar(context);
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setBackgroundColor(Color.TRANSPARENT);
        actionBar.setItemsColor(Theme.getColor(Theme.key_voipgroup_actionBarItems), false);
        actionBar.setOccupyStatusBar(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    dismiss(false, false);
                }
            }
        });
        addView(actionBar);

        positiveButton = new TextView(getContext()) {
            private final Paint whitePaint = new Paint();

            private final Paint[] gradientPaint = new Paint[titles.length];

            {
                whitePaint.setColor(Color.WHITE);
                for (int a = 0; a < gradientPaint.length; a++) {
                    gradientPaint[a] = new Paint(Paint.ANTI_ALIAS_FLAG);
                }
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                for (int a = 0; a < gradientPaint.length; a++) {
                    int color1;
                    int color2;
                    int color3;
                    if (a == 0) {
                        color1 = 0xff77E55C;
                        color2 = 0xff56C7FE;
                        color3 = 0;
                    } else if (a == 1) {
                        color1 = 0xff227df4;
                        color2 = 0xff1792ed;
                        color3 = 0xff0a9ee8;
                    } else {
                        color1 = 0xff1ab87f;
                        color2 = 0xff3dc069;
                        color3 = 0xff55c958;
                    }
                    Shader gradient;
                    if (color3 != 0) {
                        gradient = new LinearGradient(0, 0, getMeasuredWidth(), 0, new int[]{color1, color2, color3}, null, Shader.TileMode.CLAMP);
                    } else {
                        gradient = new LinearGradient(0, 0, getMeasuredWidth(), 0, new int[]{color1, color2}, null, Shader.TileMode.CLAMP);
                    }
                    gradientPaint[a].setShader(gradient);
                }
            }

            @Override
            protected void onDraw(Canvas canvas) {
                AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                gradientPaint[strangeCurrentPage].setAlpha(255);
                int round = AndroidUtilities.dp(8) + (int) ((AndroidUtilities.dp(26) - AndroidUtilities.dp(8)) * (1f - openProgress1));
                canvas.drawRoundRect(AndroidUtilities.rectTmp, round, round, gradientPaint[strangeCurrentPage]);
                if (pageOffset > 0 && strangeCurrentPage + 1 < gradientPaint.length) {
                    gradientPaint[strangeCurrentPage + 1].setAlpha((int) (255 * pageOffset));
                    canvas.drawRoundRect(AndroidUtilities.rectTmp, round, round, gradientPaint[strangeCurrentPage + 1]);
                }
                if (openProgress1 < 1f) {
                    whitePaint.setAlpha((int) (255 * (1f - openProgress1)));
                    canvas.drawRoundRect(AndroidUtilities.rectTmp, round, round, whitePaint);
                }
                super.onDraw(canvas);
            }
        };
        positiveButton.setMinWidth(AndroidUtilities.dp(64));
        positiveButton.setTag(Dialog.BUTTON_POSITIVE);
        positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        positiveButton.setTextColor(Theme.getColor(Theme.key_voipgroup_nameText));
        positiveButton.setGravity(Gravity.CENTER);
        positiveButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            positiveButton.setForeground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Color.TRANSPARENT, ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_voipgroup_nameText), (int) (255 * 0.3f))));
        }
        positiveButton.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        positiveButton.setOnClickListener(view -> {
            if (isDismissed) {
                return;
            }
            if (realCurrentPage == 0) {
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                ((Activity) getContext()).startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), LaunchActivity.SCREEN_CAPTURE_REQUEST_CODE);
            } else {
                dismiss(false, true);
            }
        });

        addView(positiveButton, LayoutHelper.createFrame(52, 52, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, 80));

        titlesLayout = new LinearLayout(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
            }
        };
        addView(titlesLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 64, Gravity.BOTTOM));

        for (int i = 0; i < titles.length; i++) {
            titles[i] = new TextView(context);
            titles[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            titles[i].setTextColor(0xffffffff);
            titles[i].setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titles[i].setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(10), 0);
            titles[i].setGravity(Gravity.CENTER_VERTICAL);
            titles[i].setSingleLine(true);
            titlesLayout.addView(titles[i], LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
            if (i == 0) {
                titles[i].setText(LocaleController.getString("VoipPhoneScreen", R.string.VoipPhoneScreen));
            } else if (i == 1) {
                titles[i].setText(LocaleController.getString("VoipFrontCamera", R.string.VoipFrontCamera));
            } else {
                titles[i].setText(LocaleController.getString("VoipBackCamera", R.string.VoipBackCamera));
            }
            final int num = i;
            titles[i].setOnClickListener(view -> {
                if (view.getAlpha() == 0f) return;
                setCurrentPage(num, true);
            });
        }

        setWillNotDraw(false);

        VoIPService service = VoIPService.getSharedInstance();
        if (service != null) {
            textureView.renderer.setMirror(service.isFrontFaceCamera());
            textureView.renderer.init(VideoCapturerDevice.getEglBase().getEglBaseContext(), new RendererCommon.RendererEvents() {
                @Override
                public void onFirstFrameRendered() {

                }

                @Override
                public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {

                }
            });
            service.setLocalSink(textureView.renderer, false);
        }
        createPages(viewPager);
        setCurrentPage(1, false);

        ValueAnimator openAnimator1 = ValueAnimator.ofFloat(0.1f, 1f);
        openAnimator1.addUpdateListener(animation -> {
            openProgress1 = (float) animation.getAnimatedValue();
            float startLocationXWithOffset = startLocationX + AndroidUtilities.dp(28);
            float startLocationYWithOffset = startLocationY + AndroidUtilities.dp(52);
            openTranslationX = startLocationXWithOffset - (startLocationXWithOffset * openProgress1);
            openTranslationY = startLocationYWithOffset - (startLocationYWithOffset * openProgress1);
            invalidate();
        });
        ValueAnimator openAnimator2 = ValueAnimator.ofFloat(0f, 1f);
        openAnimator2.addUpdateListener(animation -> {
            openProgress2 = (float) animation.getAnimatedValue();
            int w = AndroidUtilities.displaySize.x - AndroidUtilities.dp(36) - AndroidUtilities.dp(52);
            positiveButton.getLayoutParams().width = AndroidUtilities.dp(52) + (int) (w * openProgress2);
            positiveButton.requestLayout();
        });
        int openAnimationTime = 400;
        openAnimator1.setInterpolator(CubicBezierInterpolator.DEFAULT);
        openAnimator1.setDuration(openAnimationTime);
        openAnimator1.start();
        openAnimator2.setInterpolator(CubicBezierInterpolator.DEFAULT);
        openAnimator2.setDuration(openAnimationTime);
        openAnimator2.setStartDelay(openAnimationTime / 10);
        openAnimator2.start();
        titlesLayout.setAlpha(0f);
        titlesLayout.setScaleY(0.8f);
        titlesLayout.setScaleX(0.8f);
        titlesLayout.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(150).setDuration(250).start();
        positiveButton.setTranslationY(AndroidUtilities.dp(53));
        positiveButton.setTranslationX(startLocationX - (AndroidUtilities.displaySize.x / 2f) + AndroidUtilities.dp(8) + AndroidUtilities.dp(26));
        positiveButton.animate().translationY(0).translationX(0).setDuration(openAnimationTime).start();
        AndroidUtilities.runOnUIThread(() -> positiveButton.setText(LocaleController.getString("VoipShareVideo", R.string.VoipShareVideo)), (long) (openAnimationTime / 4.5f));
    }

    private void showStub(boolean show, boolean animate) {
        ImageView imageView = (ImageView) viewPager.findViewWithTag("image_stab");
        if (!show) {
            imageView.setVisibility(GONE);
            return;
        }
        Bitmap bitmap = null;
        try {
            File file = new File(ApplicationLoader.getFilesDirFixed(), "cthumb" + visibleCameraPage + ".jpg");
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Throwable ignore) {

        }

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.icplaceholder);
        }
        if (animate) {
            imageView.setVisibility(VISIBLE);
            imageView.setAlpha(0f);
            imageView.animate().alpha(1f).setDuration(250).start();
        } else {
            imageView.setVisibility(VISIBLE);
        }
    }

    private void setCurrentPage(int position, boolean animate) {
        if (strangeCurrentPage == position || realCurrentPage == position) return;

        if (animate) {
            if (realCurrentPage == 0) {
                //переключаемся с скринкаста на любую камеру
                if (visibleCameraPage != position) {
                    visibleCameraPage = position;
                    cameraReady = false;
                    showStub(true, true);
                    VoIPService.getSharedInstance().switchCamera();
                } else {
                    showStub(false, false);
                    textureView.animate().alpha(1f).setDuration(250).start();
                }
            } else {
                if (position == 0) {
                    //переключаемся на скринкаст c любой камеры
                    viewPager.findViewWithTag("screencast_stub").setVisibility(VISIBLE);
                    saveLastCameraBitmap();
                    showStub(false, false);
                    textureView.animate().alpha(0f).setDuration(250).start();
                } else {
                    //переключение между камерами
                    saveLastCameraBitmap();
                    visibleCameraPage = position;
                    cameraReady = false;
                    showStub(true, false);
                    textureView.animate().alpha(0f).setDuration(250).start();
                    VoIPService.getSharedInstance().switchCamera();
                }
            }

            ValueAnimator animator;
            if (position > strangeCurrentPage) {
                //на право
                realCurrentPage = strangeCurrentPage + 1;
                animator = ValueAnimator.ofFloat(0.1f, 1f);
            } else {
                //на лево
                realCurrentPage = strangeCurrentPage - 1;
                strangeCurrentPage = position;
                animator = ValueAnimator.ofFloat(1f, 0f);
            }

            animator.addUpdateListener(animation -> {
                pageOffset = (float) animation.getAnimatedValue();
                updateTitlesLayout();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    strangeCurrentPage = position;
                    pageOffset = 0;
                    updateTitlesLayout();
                }
            });
            animator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            animator.setDuration(400);
            animator.start();
        } else {
            //всегда будет 1
            realCurrentPage = position;
            strangeCurrentPage = position;
            pageOffset = 0;
            updateTitlesLayout();
            textureView.setVisibility(VISIBLE);
            cameraReady = false;
            visibleCameraPage = 1;
            showStub(true, false);
        }
    }

    private void createPages(FrameLayout container) {
        {
            FrameLayout frameLayout = new FrameLayout(getContext());
            frameLayout.setBackground(new MotionBackgroundDrawable(0xff212E3A, 0xff2B5B4D, 0xff245863, 0xff274558, true));

            ImageView imageView = new ImageView(getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageResource(R.drawable.screencast_big);
            frameLayout.addView(imageView, LayoutHelper.createFrame(82, 82, Gravity.CENTER, 0, 0, 0, 60));

            TextView textView = new TextView(getContext());
            textView.setText(LocaleController.getString("VoipVideoPrivateScreenSharing", R.string.VoipVideoPrivateScreenSharing));
            textView.setGravity(Gravity.CENTER);
            textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            textView.setTextColor(0xffffffff);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            frameLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 21, 28, 21, 0));
            frameLayout.setTag("screencast_stub");
            frameLayout.setVisibility(GONE);
            container.addView(frameLayout);
        }
        {
            ImageView imageView = new ImageView(getContext());
            imageView.setTag("image_stab");
            imageView.setImageResource(R.drawable.icplaceholder);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            container.addView(imageView);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (openProgress1 < 1f) {
            int w = AndroidUtilities.displaySize.x;
            int h = AndroidUtilities.displaySize.y + AndroidUtilities.statusBarHeight + AndroidUtilities.navigationBarHeight;
            float rounded = AndroidUtilities.dp(28) - (AndroidUtilities.dp(28) * openProgress1);
            clipPath.reset();
            clipPath.addCircle(startLocationX + AndroidUtilities.dp(34), startLocationY + AndroidUtilities.dp(26), AndroidUtilities.dp(26), Path.Direction.CW);
            clipPath.addRoundRect(openTranslationX, openTranslationY, openTranslationX + (w * openProgress1), openTranslationY + (h * openProgress1), rounded, rounded, Path.Direction.CW);
            canvas.clipPath(clipPath);
        }

        if (closeProgress > 0f) {
            int[] loc = getFloatingViewLocation();
            int x = (int) (closeProgress * loc[0]);
            int y = (int) (closeProgress * loc[1]);
            int destWidth = loc[2];
            int w = AndroidUtilities.displaySize.x;
            float currentWidth = destWidth + ((w - destWidth) * (1f - closeProgress));
            float scale = currentWidth / w;
            clipPath.reset();
            clipPath.addRoundRect(0f, 0f, getWidth() * scale, getHeight() * scale, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Path.Direction.CW);
            canvas.translate(x, y);
            canvas.clipPath(clipPath);
            canvas.scale(scale, scale);
        }

        super.dispatchDraw(canvas);
    }

    public void dismiss(boolean screencast, boolean apply) {
        if (isDismissed) {
            return;
        }
        isDismissed = true;
        saveLastCameraBitmap();
        onDismiss(screencast, apply);
        if (isHasVideoOnMainScreen() && apply) {
            ValueAnimator closeAnimator = ValueAnimator.ofFloat(0f, 1f);
            closeAnimator.addUpdateListener(animation -> {
                closeProgress = (float) animation.getAnimatedValue();
                invalidate();
            });
            closeAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            closeAnimator.setStartDelay(100);
            closeAnimator.setDuration(400);
            closeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (getParent() != null) {
                        ((ViewGroup) getParent()).removeView(PrivateVideoPreviewDialogNew.this);
                    }
                }
            });
            closeAnimator.start();
            positiveButton.animate().setStartDelay(100).alpha(0f).setDuration(100).start();
            actionBar.animate().setStartDelay(100).alpha(0f).setDuration(100).start();
            titlesLayout.animate().setStartDelay(100).alpha(0f).setDuration(100).start();
        } else {
            animate().setStartDelay(100).alpha(0f).setDuration(250).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (getParent() != null) {
                        ((ViewGroup) getParent()).removeView(PrivateVideoPreviewDialogNew.this);
                    }
                }
            });
        }

        invalidate();
    }

    public void setBottomPadding(int padding) {
        LayoutParams layoutParams = (LayoutParams) positiveButton.getLayoutParams();
        layoutParams.bottomMargin = AndroidUtilities.dp(80) + padding;

        layoutParams = (LayoutParams) titlesLayout.getLayoutParams();
        layoutParams.bottomMargin = padding;
    }

    private void updateTitlesLayout() {
        View current = titles[strangeCurrentPage];
        View next = strangeCurrentPage < titles.length - 1 ? titles[strangeCurrentPage + 1] : null;
        float cx = getMeasuredWidth() / 2;
        float currentCx = current.getLeft() + current.getMeasuredWidth() / 2;
        float tx = getMeasuredWidth() / 2 - currentCx;
        if (next != null) {
            float nextCx = next.getLeft() + next.getMeasuredWidth() / 2;
            tx -= (nextCx - currentCx) * pageOffset;
        }
        for (int i = 0; i < titles.length; i++) {
            float alpha;
            float scale;
            if (i < strangeCurrentPage || i > strangeCurrentPage + 1) {
                alpha = 0.7f;
                scale = 0.9f;
            } else if (i == strangeCurrentPage) {
                //движение на право или выбранно
                alpha = 1.0f - 0.3f * pageOffset;
                scale = 1.0f - 0.1f * pageOffset;
            } else {
                alpha = 0.7f + 0.3f * pageOffset;
                scale = 0.9f + 0.1f * pageOffset;
            }
            titles[i].setAlpha(alpha);
            titles[i].setScaleX(scale);
            titles[i].setScaleY(scale);
        }
        titlesLayout.setTranslationX(tx);
        positiveButton.invalidate();
        if (realCurrentPage == 0) {
            titles[2].setAlpha(0.7f * pageOffset);
        }
        if (realCurrentPage == 2) {
            if (pageOffset > 0f) {
                titles[0].setAlpha(0.7f * (1f - pageOffset));
            } else {
                titles[0].setAlpha(0f);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        VoIPService service = VoIPService.getSharedInstance();
        if (service != null) service.registerStateListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        VoIPService service = VoIPService.getSharedInstance();
        if (service != null) service.unregisterStateListener(this);
    }

    private void saveLastCameraBitmap() {
        if (!cameraReady) {
            return;
        }
        try {
            Bitmap bitmap = textureView.renderer.getBitmap();
            if (bitmap != null) {
                Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), textureView.renderer.getMatrix(), true);
                bitmap.recycle();
                bitmap = newBitmap;
                Bitmap lastBitmap = Bitmap.createScaledBitmap(bitmap, 80, (int) (bitmap.getHeight() / (bitmap.getWidth() / 80.0f)), true);
                if (lastBitmap != null) {
                    if (lastBitmap != bitmap) {
                        bitmap.recycle();
                    }
                    Utilities.blurBitmap(lastBitmap, 7, 1, lastBitmap.getWidth(), lastBitmap.getHeight(), lastBitmap.getRowBytes());
                    File file = new File(ApplicationLoader.getFilesDirFixed(), "cthumb" + visibleCameraPage + ".jpg");
                    FileOutputStream stream = new FileOutputStream(file);
                    lastBitmap.compress(Bitmap.CompressFormat.JPEG, 87, stream);
                    stream.close();
                    View view = viewPager.findViewWithTag("image_stab");
                    if (view instanceof ImageView) {
                        ((ImageView) view).setImageBitmap(lastBitmap);
                    }
                }
            }
        } catch (Throwable ignore) {

        }
    }

    @Override
    public void onCameraFirstFrameAvailable() {
        if (!cameraReady) {
            cameraReady = true;
            textureView.animate().alpha(1f).setDuration(250).start();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateTitlesLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    protected void onDismiss(boolean screencast, boolean apply) {

    }

    protected int[] getFloatingViewLocation() {
        return null;
    }

    protected boolean isHasVideoOnMainScreen() {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean isLandscape = MeasureSpec.getSize(widthMeasureSpec) > MeasureSpec.getSize(heightMeasureSpec);
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) positiveButton.getLayoutParams();
        if (isLandscape) {
            marginLayoutParams.rightMargin = marginLayoutParams.leftMargin = AndroidUtilities.dp(80);
        } else {
            marginLayoutParams.rightMargin = marginLayoutParams.leftMargin = AndroidUtilities.dp(16);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildWithMargins(titlesLayout, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY), 0);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (getParent() != null) {
            ((View) getParent()).invalidate();
        }
    }

    @Override
    public void onCameraSwitch(boolean isFrontFace) {
        update();
    }

    public void update() {
        if (VoIPService.getSharedInstance() != null) {
            textureView.renderer.setMirror(VoIPService.getSharedInstance().isFrontFaceCamera());
        }
    }
}
