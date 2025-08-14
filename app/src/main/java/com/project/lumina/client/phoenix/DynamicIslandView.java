package com.project.lumina.client.phoenix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DynamicIslandView extends FrameLayout {

    // region State and TaskItem
    private enum State {
        COLLAPSED,
        EXPANDED
    }

    private State currentState = State.COLLAPSED;

    private static class TaskItem {
        enum Type {SWITCH, PROGRESS}

        Type type;
        String identifier;
        String text;
        String subtitle;
        boolean switchState;
        MaterialSwitch switchView;
        Drawable icon;

        boolean isTimeBased;
        float progress = 1.0f;
        float displayProgress = 1.0f;
        long lastUpdateTime;
        boolean isAwaitingData = false;

        float alpha = 1.0f;
        boolean removing = false;
        long duration;
        ValueAnimator progressAnimator;

        TaskItem(Type type, String identifier, String text, String subtitle) {
            this.type = type;
            this.identifier = identifier;
            this.text = text;
            this.subtitle = subtitle;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }
    // endregion

    // region Dimensions
    private float density;
    private float collapsedWidth;
    private float collapsedHeight;
    private float expandedWidth;
    private float itemHeight;
    private float padding;
    private float collapsedCornerRadius;
    private float expandedCornerRadius;
    private float iconSize;
    private float iconContainerSize;
    private float iconContainerCornerRadius;
    private float progressHeight;
    private float switchWidth;
    private float switchHeight;
    private float glowMargin;

    private float currentWidth;
    private float currentHeight;
    private float currentRadius;
    // endregion

    // region Paints
    private Paint backgroundPaint;
    private Paint frostPaint;
    private Paint glowPaint;
    private TextPaint textPaint;
    private TextPaint subtitlePaint;
    private TextPaint separatorPaint;
    private TextPaint timePaint;
    private Paint progressBackgroundPaint;
    private Paint progressPaint;
    private Paint iconContainerPaint;
    // endregion

    // region Data and Animators
    private String persistentText = "Phoen1x";
    private final List<TaskItem> tasks = new ArrayList<>();

    private ValueAnimator sizeAnimator;
    private ValueAnimator glowAnimator;
    private float glowAlpha = 0f;
    private float glowRotation = 0f;
    private final Matrix glowMatrix = new Matrix();
    private Shader glowShader;
    // endregion

    // region Clock Animation
    private final Handler clockUpdateHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private String previousTimeText = "";
    private final List<DigitAnimator> timeDigitAnimators = new ArrayList<>();
    private final int baseTimeAlpha;

    private class DigitAnimator {
        char currentChar = ' ';
        char previousChar = ' ';
        float animationProgress = 0f; // 0: start, 1: end
        boolean isAnimating = false;
        ValueAnimator animator;

        void start(char from, char to) {
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            this.previousChar = from;
            this.currentChar = to;
            this.isAnimating = true;

            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(700);
            animator.setInterpolator(new OvershootInterpolator(1.8f));

            animator.addUpdateListener(animation -> {
                animationProgress = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    isAnimating = false;
                    animationProgress = 0f;
                    invalidate();
                }
            });
            animator.start();
        }
    }

    private final Runnable clockUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentState == State.COLLAPSED) {
                String newTimeText = timeFormat.format(new Date());
                if (!previousTimeText.isEmpty() && newTimeText.length() == previousTimeText.length()) {
                    for (int i = 0; i < newTimeText.length(); i++) {
                        if (newTimeText.charAt(i) != previousTimeText.charAt(i)) {
                            timeDigitAnimators.get(i).start(previousTimeText.charAt(i), newTimeText.charAt(i));
                        }
                    }
                } else {
                    for (int i = 0; i < newTimeText.length(); i++) {
                        timeDigitAnimators.get(i).currentChar = newTimeText.charAt(i);
                    }
                }
                previousTimeText = newTimeText;
                invalidate();
                clockUpdateHandler.postDelayed(this, 1000);
            }
        }
    };
    // endregion

    // region Update Loop
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateTasks();
            if (!tasks.isEmpty() || glowAlpha > 0) {
                updateHandler.postDelayed(this, 16);
            }
        }
    };
    // endregion

    // region Constants
    private static final long VALUE_PROGRESS_TIMEOUT_MS = 3000;
    private static final long TIME_PROGRESS_GRACE_PERIOD_MS = 1000;
    // endregion

    public DynamicIslandView(@NonNull Context context) {
        super(context);
        baseTimeAlpha = ColorUtils.setAlphaComponent(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant), 200);
        init();
    }

    public DynamicIslandView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        baseTimeAlpha = ColorUtils.setAlphaComponent(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant), 200);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        density = getResources().getDisplayMetrics().density;

        glowMargin = 10 * density;
        setPadding((int) glowMargin, (int) glowMargin, (int) glowMargin, (int) glowMargin);
        setClipToPadding(false);
        setClipChildren(false);

        collapsedHeight = 38 * density;
        itemHeight = 68 * density;
        expandedWidth = 320 * density;
        this.padding = 16 * density;
        collapsedCornerRadius = collapsedHeight / 2;
        expandedCornerRadius = 28 * density;
        iconContainerSize = 40 * density;
        iconSize = 24 * density;
        iconContainerCornerRadius = 12 * density;
        progressHeight = 4 * density;
        switchWidth = 52 * density;
        switchHeight = 32 * density;

        for (int i = 0; i < timeFormat.toPattern().length(); i++) {
            timeDigitAnimators.add(new DigitAnimator());
        }

        initPaints();

        updateCollapsedWidth();
        currentWidth = collapsedWidth + getPaddingLeft() + getPaddingRight();
        currentHeight = collapsedHeight + getPaddingTop() + getPaddingBottom();
        currentRadius = collapsedCornerRadius;

        setLayerType(LAYER_TYPE_SOFTWARE, null);
        startClockUpdate();
    }

    private void startClockUpdate() {
        clockUpdateHandler.removeCallbacks(clockUpdateRunnable);
        clockUpdateHandler.post(clockUpdateRunnable);
    }

    private void stopClockUpdate() {
        clockUpdateHandler.removeCallbacks(clockUpdateRunnable);
    }

    private void initPaints() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int bgColor = MaterialColors.getColor(this, R.attr.colorSurfaceVariant);
        backgroundPaint.setColor(ColorUtils.setAlphaComponent(bgColor, 220));

        frostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        frostPaint.setColor(ColorUtils.setAlphaComponent(Color.WHITE, 30));

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(4 * density);
        glowPaint.setMaskFilter(new BlurMaskFilter(6 * density, BlurMaskFilter.Blur.NORMAL));

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant));
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15, getResources().getDisplayMetrics()));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        subtitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        subtitlePaint.setColor(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant));
        subtitlePaint.setAlpha(200);
        subtitlePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, getResources().getDisplayMetrics()));

        separatorPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        separatorPaint.setColor(ColorUtils.setAlphaComponent(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant), 120));
        separatorPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));

        timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(baseTimeAlpha);
        timePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        timePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));


        progressBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressBackgroundPaint.setColor(ColorUtils.setAlphaComponent(MaterialColors.getColor(this, R.attr.colorPrimary), 60));
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(MaterialColors.getColor(this, R.attr.colorPrimary));

        iconContainerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconContainerPaint.setColor(MaterialColors.getColor(this, R.attr.colorPrimaryContainer));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension((int) currentWidth, (int) currentHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updateGlowShader();
        }
    }

    private void updateGlowShader() {
        glowShader = new SweepGradient(
                getWidth() / 2f, getHeight() / 2f,
                new int[]{Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.CYAN}, null
        );
        glowPaint.setShader(glowShader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float contentLeft = getPaddingLeft();
        float contentTop = getPaddingTop();
        float contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float contentHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (glowAlpha > 0) {
            glowPaint.setAlpha((int) (glowAlpha * 255));
            glowMatrix.setRotate(glowRotation, getWidth() / 2f, getHeight() / 2f);
            glowShader.setLocalMatrix(glowMatrix);
            RectF glowRect = new RectF(
                    contentLeft - glowPaint.getStrokeWidth() / 2, contentTop - glowPaint.getStrokeWidth() / 2,
                    contentLeft + contentWidth + glowPaint.getStrokeWidth() / 2, contentTop + contentHeight + glowPaint.getStrokeWidth() / 2
            );
            canvas.drawRoundRect(glowRect, currentRadius, currentRadius, glowPaint);
        }

        RectF bgRect = new RectF(contentLeft, contentTop, contentLeft + contentWidth, contentTop + contentHeight);
        canvas.drawRoundRect(bgRect, currentRadius, currentRadius, backgroundPaint);
        canvas.drawRoundRect(bgRect, currentRadius, currentRadius, frostPaint);

        canvas.save();
        canvas.translate(contentLeft, contentTop);

        if (currentState == State.COLLAPSED) {
            drawCollapsedText(canvas, contentWidth, contentHeight);
        } else {
            drawTasks(canvas, contentWidth);
        }

        canvas.restore();
    }

    private void drawCollapsedText(Canvas canvas, float contentWidth, float contentHeight) {
        String staticText = "LuminaCN B22";
        String separator = " • ";
        Paint.FontMetrics fm = timePaint.getFontMetrics();
        float charHeight = fm.descent - fm.ascent;
        float baseY = contentHeight / 2 - (fm.descent + fm.ascent) / 2;

        float staticWidth = textPaint.measureText(staticText);
        float separatorWidth = separatorPaint.measureText(separator);
        float timeWidth = timePaint.measureText(previousTimeText);
        float persistentWidth = textPaint.measureText(persistentText);

        float totalWidth = staticWidth + separatorWidth + timeWidth + separatorWidth + persistentWidth;
        float currentX = (contentWidth - totalWidth) / 2;

        canvas.drawText(staticText, currentX, baseY, textPaint);
        currentX += staticWidth;
        canvas.drawText(separator, currentX, baseY, separatorPaint);
        currentX += separatorWidth;

        int originalAlpha = timePaint.getAlpha();

        for (int i = 0; i < timeDigitAnimators.size(); i++) {
            DigitAnimator da = timeDigitAnimators.get(i);
            String charStr = String.valueOf(da.currentChar);
            float charWidth = timePaint.measureText(charStr);

            // Create a clipping "window" for each digit, one character high.
            canvas.save();
            canvas.clipRect(currentX, baseY + fm.ascent, currentX + charWidth, baseY + fm.descent);

            if (da.isAnimating) {
                // *** THE ROOT CAUSE FIX ***
                // Instead of calculating coordinates, we translate the entire canvas.
                // This "film strip" model prevents any visual interference.
                float progress = da.animationProgress;
                float verticalShift = progress * charHeight;

                // Create another save point for the translation
                canvas.save();
                // Translate the canvas DOWNWARDS.
                canvas.translate(0, verticalShift);

                // 1. Draw the OLD character at the starting position (baseY).
                // As the canvas moves down, this character slides down and out of the clip window.
                timePaint.setAlpha((int) ((1 - progress) * originalAlpha));
                canvas.drawText(String.valueOf(da.previousChar), currentX, baseY, timePaint);

                // 2. Draw the NEW character one slot ABOVE the old one.
                // As the canvas moves down, this character slides into the clip window from above.
                timePaint.setAlpha((int) (progress * originalAlpha));
                canvas.drawText(charStr, currentX, baseY - charHeight, timePaint);

                // Restore from the translation
                canvas.restore();

            } else {
                // Draw static character.
                timePaint.setAlpha(originalAlpha);
                canvas.drawText(charStr, currentX, baseY, timePaint);
            }

            // Restore from the clipping
            canvas.restore();
            currentX += charWidth;
        }

        timePaint.setAlpha(originalAlpha);

        canvas.drawText(separator, currentX, baseY, separatorPaint);
        currentX += separatorWidth;
        canvas.drawText(persistentText, currentX, baseY, textPaint);
    }


    private void drawTasks(Canvas canvas, float contentWidth) {
        float yOffset = this.padding / 2;
        for (TaskItem task : tasks) {
            if (task.alpha <= 0) continue;
            int alpha = (int) (task.alpha * 255);
            textPaint.setAlpha(alpha);
            subtitlePaint.setAlpha((int) (alpha * 0.8f));
            iconContainerPaint.setAlpha(alpha);
            progressBackgroundPaint.setAlpha((int) (alpha * 0.24f));
            progressPaint.setAlpha(alpha);
            if (task.switchView != null) {
                task.switchView.setAlpha(task.alpha);
            }
            drawTask(canvas, task, yOffset, contentWidth);
            yOffset += itemHeight;
        }
        textPaint.setAlpha(255);
        subtitlePaint.setAlpha(200);
        iconContainerPaint.setAlpha(255);
        progressBackgroundPaint.setAlpha(60);
        progressPaint.setAlpha(255);
    }

    private void drawTask(Canvas canvas, TaskItem task, float yOffset, float contentWidth) {
        int save = canvas.save();
        canvas.translate(0, yOffset);
        float contentLeft = this.padding;
        if (task.type == TaskItem.Type.SWITCH && task.switchView != null) {
            float switchY = (itemHeight - task.switchView.getLayoutParams().height) / 2;
            task.switchView.setTranslationY(yOffset + switchY);
            contentLeft += switchWidth + this.padding / 2;
        } else if (task.icon != null) {
            float containerTop = (itemHeight - iconContainerSize) / 2;
            RectF containerRect = new RectF(contentLeft, containerTop, contentLeft + iconContainerSize, containerTop + iconContainerSize);
            canvas.drawRoundRect(containerRect, iconContainerCornerRadius, iconContainerCornerRadius, iconContainerPaint);
            float iconLeft = contentLeft + (iconContainerSize - iconSize) / 2;
            float iconTop = containerTop + (iconContainerSize - iconSize) / 2;
            task.icon.setBounds((int) iconLeft, (int) iconTop, (int) (iconLeft + iconSize), (int) (iconTop + iconSize));
            DrawableCompat.setTint(task.icon, MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimaryContainer));
            task.icon.draw(canvas);
            contentLeft += iconContainerSize + this.padding / 2;
        }
        float textBlockWidth = contentWidth - contentLeft - this.padding;
        Paint.FontMetrics titleFm = textPaint.getFontMetrics();
        Paint.FontMetrics subtitleFm = subtitlePaint.getFontMetrics();
        float titleHeight = titleFm.descent - titleFm.ascent;
        boolean hasSubtitle = task.subtitle != null && !task.subtitle.isEmpty();
        float subtitleHeight = hasSubtitle ? (subtitleFm.descent - subtitleFm.ascent) : 0;
        float textSpacing = 2 * density;
        float progressSpacing = 6 * density;
        boolean hasProgress = (task.type == TaskItem.Type.PROGRESS || task.type == TaskItem.Type.SWITCH);
        float totalBlockHeight = titleHeight + (hasSubtitle ? textSpacing + subtitleHeight : 0) + (hasProgress ? progressSpacing + progressHeight : 0);
        float blockYStart = (itemHeight - totalBlockHeight) / 2;
        float titleY = blockYStart - titleFm.ascent;
        canvas.drawText(task.text, contentLeft, titleY, textPaint);
        if (hasSubtitle) {
            float subtitleY = titleY + titleFm.descent + textSpacing - subtitleFm.ascent;
            canvas.drawText(task.subtitle, contentLeft, subtitleY, subtitlePaint);
        }
        if (hasProgress) {
            float progressY = blockYStart + titleHeight + (hasSubtitle ? subtitleHeight + textSpacing : 0) + progressSpacing;
            RectF bgRect = new RectF(contentLeft, progressY, contentLeft + textBlockWidth, progressY + progressHeight);
            canvas.drawRoundRect(bgRect, progressHeight, progressHeight, progressBackgroundPaint);
            RectF progressRect = new RectF(contentLeft, progressY, contentLeft + textBlockWidth * task.displayProgress, progressY + progressHeight);
            canvas.drawRoundRect(progressRect, progressHeight, progressHeight, progressPaint);
        }
        canvas.restoreToCount(save);
    }

    // region Public API
    public void setPersistentText(String text) {
        this.persistentText = text;
        if (currentState == State.COLLAPSED) {
            updateCollapsedWidth();
            animateToSize(collapsedWidth, collapsedHeight, collapsedCornerRadius);
        }
    }

    public void addSwitch(String moduleName, boolean state) {
        String subtitle = state ? "已开启" : "已关闭";
        Optional<TaskItem> existingTask = tasks.stream().filter(t -> t.identifier != null && t.identifier.equals(moduleName)).findFirst();
        long duration = 2000L;
        if (existingTask.isPresent()) {
            TaskItem task = existingTask.get();
            task.text = moduleName;
            task.subtitle = subtitle;
            task.switchState = state;
            if (task.switchView != null) task.switchView.setChecked(state);
            task.lastUpdateTime = System.currentTimeMillis();
            task.duration = duration;

            if (task.removing) {
                task.removing = false;
                task.alpha = 1.0f;
            }
            startTimeBasedAnimation(task);
        } else {
            TaskItem task = new TaskItem(TaskItem.Type.SWITCH, moduleName, moduleName, subtitle);
            task.switchState = state;
            task.duration = duration;
            task.isTimeBased = true;
            MaterialSwitch switchView = new MaterialSwitch(getContext());
            switchView.setChecked(state);
            switchView.setClickable(false);
            switchView.setFocusable(false);
            LayoutParams params = new LayoutParams((int) switchWidth, (int) switchHeight, Gravity.TOP | Gravity.START);
            params.leftMargin = (int) this.padding;
            switchView.setLayoutParams(params);
            addView(switchView);
            task.switchView = switchView;
            startTimeBasedAnimation(task);
            addTask(task);
        }
        invalidate();
    }

    public void addOrUpdateProgress(String identifier, String text, @Nullable String subtitle, @Nullable Drawable icon, @Nullable Float progress, @Nullable Long duration) {
        Optional<TaskItem> existingTask = tasks.stream().filter(t -> t.identifier != null && t.identifier.equals(identifier)).findFirst();
        if (existingTask.isPresent()) {
            updateProgressInternal(existingTask.get(), text, subtitle, icon, progress, duration);
        } else {
            addProgressInternal(identifier, text, subtitle, icon, progress, duration);
        }
    }

    public void hide() {
        for (TaskItem task : tasks) {
            if (task.switchView != null) removeView(task.switchView);
            if (task.progressAnimator != null) task.progressAnimator.cancel();
        }
        tasks.clear();
        currentState = State.COLLAPSED;
        updateHandler.removeCallbacks(updateRunnable);
        stopClockUpdate();
        updateCollapsedWidth();
        animateToSize(collapsedWidth, collapsedHeight, collapsedCornerRadius);
        animateGlow(false);
        startClockUpdate();
    }
    // endregion

    // region Internal Logic
    private void addProgressInternal(String identifier, String text, @Nullable String subtitle, @Nullable Drawable icon, @Nullable Float progressValue, @Nullable Long duration) {
        TaskItem task = new TaskItem(TaskItem.Type.PROGRESS, identifier, text, subtitle);
        task.icon = (icon != null) ? icon.mutate() : null;
        if (progressValue != null) {
            task.isTimeBased = false;
            float clampedValue = Math.max(0f, Math.min(1f, progressValue));
            task.progress = clampedValue;
            task.displayProgress = 0f;
            animateProgressTo(task, clampedValue);
        } else {
            task.isTimeBased = true;
            task.duration = (duration != null) ? duration : 5000L;
            startTimeBasedAnimation(task);
        }
        addTask(task);
    }

    private void updateProgressInternal(TaskItem task, String text, @Nullable String subtitle, @Nullable Drawable icon, @Nullable Float progressValue, @Nullable Long duration) {
        task.text = text;
        task.subtitle = subtitle;
        if (icon != null) task.icon = icon.mutate();
        task.lastUpdateTime = System.currentTimeMillis();
        if (task.isAwaitingData || task.removing) {
            task.isAwaitingData = false;
            task.removing = false;
            task.alpha = 1.0f;
        }
        if (progressValue != null) {
            task.isTimeBased = false;
            animateProgressTo(task, progressValue);
        } else {
            task.isTimeBased = true;
            task.duration = (duration != null) ? duration : 5000L;
            startTimeBasedAnimation(task);
        }
    }

    private void animateProgressTo(TaskItem task, float newProgressValue) {
        float targetProgress = Math.max(0f, Math.min(1f, newProgressValue));
        if (task.progressAnimator != null) {
            task.progressAnimator.cancel();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(task.displayProgress, targetProgress);
        animator.setDuration(500);
        animator.setInterpolator(new OvershootInterpolator(0.8f));
        animator.addUpdateListener(animation -> {
            task.displayProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        task.progressAnimator = animator;
        animator.start();
        task.progress = targetProgress;
    }

    private void startTimeBasedAnimation(TaskItem task) {
        if (task.progressAnimator != null) {
            task.progressAnimator.cancel();
        }

        task.progress = 1.0f;

        if (task.displayProgress >= 1.0f) {
            runCountdownAnimation(task);
            return;
        }

        ValueAnimator rewindAnimator = ValueAnimator.ofFloat(task.displayProgress, 1.0f);
        rewindAnimator.setDuration(300);
        rewindAnimator.setInterpolator(new DecelerateInterpolator());
        rewindAnimator.addUpdateListener(animation -> {
            task.displayProgress = (float) animation.getAnimatedValue();
            invalidate();
        });

        rewindAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                runCountdownAnimation(task);
            }
        });

        task.progressAnimator = rewindAnimator;
        rewindAnimator.start();
    }

    private void runCountdownAnimation(TaskItem task) {
        task.displayProgress = 1.0f;
        ValueAnimator countdownAnimator = ValueAnimator.ofFloat(1.0f, 0f);
        countdownAnimator.setDuration(task.duration);
        countdownAnimator.setInterpolator(new LinearInterpolator());
        countdownAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            task.progress = value;
            task.displayProgress = value;
            invalidate();
        });
        countdownAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                task.progress = 0f;
                task.displayProgress = 0f;
                invalidate();
            }
        });
        task.progressAnimator = countdownAnimator;
        countdownAnimator.start();
    }

    private void addTask(TaskItem task) {
        tasks.add(0, task);
        if (currentState == State.COLLAPSED) {
            currentState = State.EXPANDED;
            updateHandler.post(updateRunnable);
            animateGlow(true);
            stopClockUpdate();
        }
        updateExpandedSize();
    }

    private void updateTasks() {
        boolean needsRedraw = false;
        if (currentState == State.EXPANDED) {
            glowRotation = (glowRotation + 1.5f) % 360;
            needsRedraw = true;
        }
        long currentTime = System.currentTimeMillis();
        Iterator<TaskItem> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            TaskItem task = iterator.next();
            if (task.removing) {
                task.alpha -= 0.05f;
                if (task.alpha <= 0) {
                    if (task.switchView != null) removeView(task.switchView);
                    if (task.progressAnimator != null) task.progressAnimator.cancel();
                    iterator.remove();
                    updateExpandedSize();
                }
                needsRedraw = true;
            } else {
                boolean shouldBeRemoved = false;
                if (task.isTimeBased) {
                    if (task.progress <= 0.01f) {
                        if (task.type == TaskItem.Type.PROGRESS && !task.isAwaitingData) {
                            task.isAwaitingData = true;
                            task.lastUpdateTime = currentTime;
                        } else if (task.type == TaskItem.Type.SWITCH) {
                            shouldBeRemoved = true;
                        }
                    }
                    if (task.isAwaitingData && (currentTime - task.lastUpdateTime > TIME_PROGRESS_GRACE_PERIOD_MS)) {
                        shouldBeRemoved = true;
                    }
                } else if (task.type == TaskItem.Type.PROGRESS) {
                    if (currentTime - task.lastUpdateTime > VALUE_PROGRESS_TIMEOUT_MS) {
                        shouldBeRemoved = true;
                    }
                }
                if (shouldBeRemoved) {
                    task.removing = true;
                    needsRedraw = true;
                }
            }
        }
        if (tasks.isEmpty() && currentState == State.EXPANDED) {
            currentState = State.COLLAPSED;
            updateCollapsedWidth();
            animateToSize(collapsedWidth, collapsedHeight, collapsedCornerRadius);
            animateGlow(false);
            startClockUpdate();
        }
        if (needsRedraw) invalidate();
    }

    private void updateCollapsedWidth() {
        String staticText = "LuminaCN B22";
        String separator = " • ";
        String timeText = "00:00:00";
        float staticWidth = textPaint.measureText(staticText);
        float separatorWidth = separatorPaint.measureText(separator);
        float timeWidth = timePaint.measureText(timeText);
        float persistentWidth = textPaint.measureText(persistentText);
        float totalWidth = staticWidth + separatorWidth * 2 + timeWidth + persistentWidth;
        collapsedWidth = totalWidth + this.padding * 2.5f;
    }

    private void updateExpandedSize() {
        float targetHeight = tasks.size() * itemHeight + this.padding;
        animateToSize(expandedWidth, targetHeight, expandedCornerRadius);
    }

    private void animateToSize(float targetContentWidth, float targetContentHeight, float targetRadius) {
        if (sizeAnimator != null && sizeAnimator.isRunning()) {
            sizeAnimator.cancel();
        }
        float startWidth = currentWidth;
        float startHeight = currentHeight;
        float startRadius = currentRadius;
        float finalTargetWidth = targetContentWidth + getPaddingLeft() + getPaddingRight();
        float finalTargetHeight = targetContentHeight + getPaddingTop() + getPaddingBottom();
        sizeAnimator = ValueAnimator.ofFloat(0f, 1f);
        sizeAnimator.setDuration(400);
        sizeAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        sizeAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            currentWidth = startWidth + (finalTargetWidth - startWidth) * fraction;
            currentHeight = startHeight + (finalTargetHeight - startHeight) * fraction;
            currentRadius = startRadius + (targetRadius - startRadius) * fraction;
            requestLayout();
            invalidate();
        });
        sizeAnimator.start();
    }

    private void animateGlow(boolean fadeIn) {
        if (glowAnimator != null && glowAnimator.isRunning()) glowAnimator.cancel();
        float start = glowAlpha;
        float end = fadeIn ? 1.0f : 0.0f;
        glowAnimator = ValueAnimator.ofFloat(start, end);
        glowAnimator.setDuration(500);
        glowAnimator.setInterpolator(new DecelerateInterpolator());
        glowAnimator.addUpdateListener(animation -> {
            glowAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        glowAnimator.start();
    }
    // endregion

    // region Lifecycle
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateHandler.removeCallbacks(updateRunnable);
        stopClockUpdate();
        if (sizeAnimator != null) sizeAnimator.cancel();
        if (glowAnimator != null) glowAnimator.cancel();
        for (TaskItem task : tasks) {
            if (task.progressAnimator != null) task.progressAnimator.cancel();
        }
        for (DigitAnimator da : timeDigitAnimators) {
            if (da.animator != null) da.animator.cancel();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (currentState == State.COLLAPSED) {
            startClockUpdate();
        }
    }
    // endregion
}