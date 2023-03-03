package org.telegram.ui.Components.voip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.DataSetObserver;
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
import android.os.Parcelable;
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
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

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
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.LaunchActivity;
import org.webrtc.RendererCommon;

import java.io.File;
import java.io.FileOutputStream;

@TargetApi(21)
public abstract class PrivateVideoPreviewDialogNew extends FrameLayout implements VoIPService.StateListener {

    private boolean isDismissed;
    private float outProgress;

    private ViewPager viewPager;
    private TextView positiveButton;
    private LinearLayout titlesLayout;
    private RLottieImageView micIconView;
    private TextView[] titles;
    private VoIPTextureView textureView;
    private int currentTexturePage = 1;
    private int visibleCameraPage = 1;
    private boolean cameraReady;
    private ActionBar actionBar;

    public boolean micEnabled;

    private float pageOffset;
    private int currentPage;

    private boolean needScreencast;
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
        needScreencast = screencast;
        titles = new TextView[needScreencast ? 3 : 2];

        viewPager = new ViewPager(context);
        AndroidUtilities.setViewPagerEdgeEffectColor(viewPager, 0x7f000000);
        viewPager.setAdapter(new Adapter());
        viewPager.setPageMargin(0);
        viewPager.setOffscreenPageLimit(1);
        addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            private int scrollState = ViewPager.SCROLL_STATE_IDLE;
            private int willSetPage;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                currentPage = position;
                pageOffset = positionOffset;
                updateTitlesLayout();
            }

            @Override
            public void onPageSelected(int i) {
                if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
                    if (i <= (needScreencast ? 1 : 0)) {
                        currentTexturePage = 1;
                    } else {
                        currentTexturePage = 2;
                    }
                    onFinishMoveCameraPage();
                } else {
                    if (i <= (needScreencast ? 1 : 0)) {
                        willSetPage = 1;
                    } else {
                        willSetPage = 2;
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                scrollState = state;
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    currentTexturePage = willSetPage;
                    onFinishMoveCameraPage();
                }
            }
        });

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
            private Paint whitePaint = new Paint();

            private Paint[] gradientPaint = new Paint[titles.length];

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
                    if (a == 0 && needScreencast) {
                        color1 = 0xff77E55C;
                        color2 = 0xff56C7FE;
                        color3 = 0;
                    } else if (a == 0 || a == 1 && needScreencast) {
                        color1 = 0xff57A4FE;
                        color2 = 0xff766EE9;
                        color3 = 0;
                    } else {
                        color1 = 0xff766EE9;
                        color2 = 0xffF05459;
                        color3 = 0xffE4A756;
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
                gradientPaint[currentPage].setAlpha(255);
                int round = AndroidUtilities.dp(6) + (int) ((AndroidUtilities.dp(26) - AndroidUtilities.dp(6)) * (1f - openProgress1));
                canvas.drawRoundRect(AndroidUtilities.rectTmp, round, round, gradientPaint[currentPage]);
                if (pageOffset > 0 && currentPage + 1 < gradientPaint.length) {
                    gradientPaint[currentPage + 1].setAlpha((int) (255 * pageOffset));
                    canvas.drawRoundRect(AndroidUtilities.rectTmp, round, round, gradientPaint[currentPage + 1]);
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
            positiveButton.setForeground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Color.TRANSPARENT, ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_voipgroup_nameText), (int) (255 * 0.3f))));
        }
        positiveButton.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        positiveButton.setOnClickListener(view -> {
            if (isDismissed) {
                return;
            }
            if (currentPage == 0 && needScreencast) {
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                ((Activity) getContext()).startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), LaunchActivity.SCREEN_CAPTURE_REQUEST_CODE);
            } else {
                dismiss(false, true);
            }
        });

        addView(positiveButton, LayoutHelper.createFrame(52, 48, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, 64));

        titlesLayout = new LinearLayout(context);
        addView(titlesLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 64, Gravity.BOTTOM));

        for (int a = 0; a < titles.length; a++) {
            titles[a] = new TextView(context);
            titles[a].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            titles[a].setTextColor(0xffffffff);
            titles[a].setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titles[a].setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
            titles[a].setGravity(Gravity.CENTER_VERTICAL);
            titles[a].setSingleLine(true);
            titlesLayout.addView(titles[a], LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
            if (a == 0 && needScreencast) {
                titles[a].setText(LocaleController.getString("VoipPhoneScreen", R.string.VoipPhoneScreen));
            } else if (a == 0 || a == 1 && needScreencast) {
                titles[a].setText(LocaleController.getString("VoipFrontCamera", R.string.VoipFrontCamera));
            } else {
                titles[a].setText(LocaleController.getString("VoipBackCamera", R.string.VoipBackCamera));
            }
            int num = a;
            titles[a].setOnClickListener(view -> viewPager.setCurrentItem(num, true));
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
        viewPager.setCurrentItem(needScreencast ? 1 : 0);

        if (mic) {
            micIconView = new RLottieImageView(context);
            micIconView.setPadding(AndroidUtilities.dp(9), AndroidUtilities.dp(9), AndroidUtilities.dp(9), AndroidUtilities.dp(9));
            micIconView.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(48), ColorUtils.setAlphaComponent(Color.BLACK, (int) (255 * 0.3f))));
            RLottieDrawable micIcon = new RLottieDrawable(R.raw.voice_mini, "" + R.raw.voice_mini, AndroidUtilities.dp(24), AndroidUtilities.dp(24), true, null);
            micIconView.setAnimation(micIcon);
            micIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            micEnabled = true;
            micIcon.setCurrentFrame(micEnabled ? 69 : 36);
            micIconView.setOnClickListener(v -> {
                micEnabled = !micEnabled;
                if (micEnabled) {
                    micIcon.setCurrentFrame(36);
                    micIcon.setCustomEndFrame(69);
                } else {
                    micIcon.setCurrentFrame(69);
                    micIcon.setCustomEndFrame(99);
                }
                micIcon.start();
            });
            addView(micIconView, LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.BOTTOM, 24, 0, 0, 136));
        }

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
        positiveButton.setTranslationY(AndroidUtilities.dp(34));
        positiveButton.setTranslationX(startLocationX - (AndroidUtilities.displaySize.x / 2f) + AndroidUtilities.dp(8) + AndroidUtilities.dp(26));
        positiveButton.animate().translationY(0).translationX(0).setDuration(openAnimationTime).start();
        AndroidUtilities.runOnUIThread(() -> positiveButton.setText(LocaleController.getString("VoipShareVideo", R.string.VoipShareVideo)), (long) (openAnimationTime / 4.5f));
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
        layoutParams.bottomMargin = AndroidUtilities.dp(64) + padding;

        layoutParams = (LayoutParams) titlesLayout.getLayoutParams();
        layoutParams.bottomMargin = padding;
    }

    private void updateTitlesLayout() {
        View current = titles[currentPage];
        View next = currentPage < titles.length - 1 ? titles[currentPage + 1] : null;
        float cx = getMeasuredWidth() / 2;
        float currentCx = current.getLeft() + current.getMeasuredWidth() / 2;
        float tx = getMeasuredWidth() / 2 - currentCx;
        if (next != null) {
            float nextCx = next.getLeft() + next.getMeasuredWidth() / 2;
            tx -= (nextCx - currentCx) * pageOffset;
        }
        for (int a = 0; a < titles.length; a++) {
            float alpha;
            float scale;
            if (a < currentPage || a > currentPage + 1) {
                alpha = 0.7f;
                scale = 0.9f;
            } else if (a == currentPage) {
                alpha = 1.0f - 0.3f * pageOffset;
                scale = 1.0f - 0.1f * pageOffset;
            } else {
                alpha = 0.7f + 0.3f * pageOffset;
                scale = 0.9f + 0.1f * pageOffset;
            }
            titles[a].setAlpha(alpha);
            titles[a].setScaleX(scale);
            titles[a].setScaleY(scale);
        }
        titlesLayout.setTranslationX(tx);
        positiveButton.invalidate();
        if (needScreencast && currentPage == 0 && pageOffset <= 0) {
            textureView.setVisibility(INVISIBLE);
        } else {
            textureView.setVisibility(VISIBLE);
            if (currentPage + (needScreencast ? 0 : 1) == currentTexturePage) {
                textureView.setTranslationX(-pageOffset * getMeasuredWidth());
            } else {
                textureView.setTranslationX((1.0f - pageOffset) * getMeasuredWidth());
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        VoIPService service = VoIPService.getSharedInstance();
        if (service != null) {
            service.registerStateListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        VoIPService service = VoIPService.getSharedInstance();
        if (service != null) {
            service.unregisterStateListener(this);
        }
    }

    private void onFinishMoveCameraPage() {
        VoIPService service = VoIPService.getSharedInstance();
        if (currentTexturePage == visibleCameraPage || service == null) {
            return;
        }
        boolean currentFrontface = service.isFrontFaceCamera();
        if (currentTexturePage == 1 && !currentFrontface || currentTexturePage == 2 && currentFrontface) {
            saveLastCameraBitmap();
            cameraReady = false;
            VoIPService.getSharedInstance().switchCamera();
            textureView.setAlpha(0.0f);
        }
        visibleCameraPage = currentTexturePage;
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
                    View view = viewPager.findViewWithTag(visibleCameraPage - (needScreencast ? 0 : 1));
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
            textureView.animate().alpha(1f).setDuration(250);
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
        if (micIconView != null) {
            marginLayoutParams = (MarginLayoutParams) micIconView.getLayoutParams();
            if (isLandscape) {
                marginLayoutParams.rightMargin = marginLayoutParams.leftMargin = AndroidUtilities.dp(88);
            } else {
                marginLayoutParams.rightMargin = marginLayoutParams.leftMargin = AndroidUtilities.dp(24);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildWithMargins(titlesLayout, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY), 0);
    }

    public int getBackgroundColor() {
        int color = Theme.getColor(Theme.key_voipgroup_actionBar);
        color = ColorUtils.setAlphaComponent(color, (int) (255 * (getAlpha() * (1f - outProgress))));
        return color;
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

    private class Adapter extends PagerAdapter {
        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view;
            if (needScreencast && position == 0) {
                FrameLayout frameLayout = new FrameLayout(getContext());
                frameLayout.setBackground(new MotionBackgroundDrawable(0xff212E3A, 0xff2B5B4D, 0xff245863, 0xff274558, true));
                view = frameLayout;

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
            } else {
                ImageView imageView = new ImageView(getContext());
                imageView.setTag(position);

                Bitmap bitmap = null;
                try {
                    File file = new File(ApplicationLoader.getFilesDirFixed(), "cthumb" + (position == 0 || position == 1 && needScreencast ? 1 : 2) + ".jpg");
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                } catch (Throwable ignore) {

                }
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(R.drawable.icplaceholder);
                }

                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                view = imageView;
            }
            if (view.getParent() != null) {
                ViewGroup parent = (ViewGroup) view.getParent();
                parent.removeView(view);
            }
            container.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }
}
