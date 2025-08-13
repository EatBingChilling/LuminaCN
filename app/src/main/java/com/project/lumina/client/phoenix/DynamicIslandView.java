package com.project.lumina.client.phoenix;

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
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class DynamicIslandView extends FrameLayout {

    // 状态
    private enum State {
        COLLAPSED,
        EXPANDED
    }

    private State currentState = State.COLLAPSED;

    // 任务项
    private static class TaskItem {
        enum Type {SWITCH, PROGRESS}

        Type type;
        String text;
        boolean switchState;
        MaterialSwitch switchView;
        Drawable icon;
        float progress = 1.0f;
        float displayProgress = 1.0f;
        float alpha = 1.0f;
        boolean removing = false;
        long startTime;
        long duration;
        ValueAnimator progressAnimator;

        TaskItem(Type type, String text) {
            this.type = type;
            this.text = text;
            this.startTime = System.currentTimeMillis();
        }
    }

    // 尺寸
    private float density;
    private float collapsedWidth;
    private float collapsedHeight;
    private float expandedWidth;
    private float itemHeight;
    private float padding;
    private float collapsedCornerRadius;
    private float expandedCornerRadius;
    private float iconSize;
    private float progressHeight;
    private float switchWidth;
    private float switchHeight;
    private float glowMargin;

    // 当前尺寸（用于动画）
    private float currentWidth;
    private float currentHeight;
    private float currentRadius;

    // 画笔
    private Paint backgroundPaint;
    private Paint frostPaint;
    private Paint glowPaint;
    private TextPaint textPaint;
    private TextPaint separatorPaint;
    private TextPaint timePaint;
    private Paint progressBackgroundPaint;
    private Paint progressPaint;

    // 数据
    private String persistentText = "Phoen1x";
    private final List<TaskItem> tasks = new ArrayList<>();

    // 动画
    private ValueAnimator sizeAnimator;
    private ValueAnimator glowAnimator;
    private float glowAlpha = 0f;
    private float glowRotation = 0f;
    private final Matrix glowMatrix = new Matrix();
    private Shader glowShader;

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

    public DynamicIslandView(@NonNull Context context) {
        super(context);
        init();
    }

    public DynamicIslandView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
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
        itemHeight = 56 * density;
        expandedWidth = 290 * density;
        this.padding = 16 * density;
        collapsedCornerRadius = collapsedHeight / 2;
        expandedCornerRadius = 28 * density;
        iconSize = 24 * density;
        progressHeight = 4 * density;
        switchWidth = 52 * density;
        switchHeight = 32 * density;

        initPaints();

        updateCollapsedWidth();
        currentWidth = collapsedWidth + getPaddingLeft() + getPaddingRight();
        currentHeight = collapsedHeight + getPaddingTop() + getPaddingBottom();
        currentRadius = collapsedCornerRadius;

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    private void initPaints() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int bgColor = MaterialColors.getColor(this, R.attr.colorSurfaceVariant);
        backgroundPaint.setColor(ColorUtils.setAlphaComponent(bgColor, 180));
        frostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        frostPaint.setColor(ColorUtils.setAlphaComponent(Color.WHITE, 40));
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(4 * density);
        glowPaint.setMaskFilter(new BlurMaskFilter(6 * density, BlurMaskFilter.Blur.NORMAL));
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant));
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        separatorPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        separatorPaint.setColor(ColorUtils.setAlphaComponent(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant), 120));
        separatorPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant));
        timePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        progressBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressBackgroundPaint.setColor(ColorUtils.setAlphaComponent(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant), 50));
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant));
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
                getWidth() / 2f,
                getHeight() / 2f,
                new int[]{Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.CYAN},
                null
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
                    contentLeft - glowPaint.getStrokeWidth() / 2,
                    contentTop - glowPaint.getStrokeWidth() / 2,
                    contentLeft + contentWidth + glowPaint.getStrokeWidth() / 2,
                    contentTop + contentHeight + glowPaint.getStrokeWidth() / 2
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
        String timeText = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        float staticWidth = textPaint.measureText(staticText);
        float separatorWidth = separatorPaint.measureText(separator);
        float timeWidth = timePaint.measureText(timeText);
        float persistentWidth = textPaint.measureText(persistentText);

        float totalWidth = staticWidth + separatorWidth + timeWidth + separatorWidth + persistentWidth;
        float startX = (contentWidth - totalWidth) / 2;
        float y = contentHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2;

        canvas.drawText(staticText, startX, y, textPaint);
        startX += staticWidth;
        canvas.drawText(separator, startX, y, separatorPaint);
        startX += separatorWidth;
        canvas.drawText(timeText, startX, y, timePaint);
        startX += timeWidth;
        canvas.drawText(separator, startX, y, separatorPaint);
        startX += separatorWidth;
        canvas.drawText(persistentText, startX, y, textPaint);

        postInvalidateDelayed(1000);
    }

    private void drawTasks(Canvas canvas, float contentWidth) {
        float yOffset = this.padding;

        for (TaskItem task : tasks) {
            if (task.alpha <= 0) continue;

            int alpha = (int) (task.alpha * 255);
            textPaint.setAlpha(alpha);
            progressBackgroundPaint.setAlpha((int) (alpha * 0.2f));
            progressPaint.setAlpha(alpha);

            if (task.switchView != null) {
                task.switchView.setAlpha(task.alpha);
            }

            drawTask(canvas, task, yOffset, contentWidth);

            yOffset += itemHeight;
        }

        textPaint.setAlpha(255);
        progressBackgroundPaint.setAlpha(50);
        progressPaint.setAlpha(255);
    }

    private void drawTask(Canvas canvas, TaskItem task, float yOffset, float contentWidth) {
        int save = canvas.save();
        canvas.translate(0, yOffset);

        float contentLeft = this.padding;
        float availableWidth = contentWidth - this.padding * 2;

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float contentBlockHeight = textHeight + (4 * density) + progressHeight;
        float contentTopY_relative = (itemHeight - contentBlockHeight) / 2;

        float textY = contentTopY_relative - fm.ascent;
        float progressY = contentTopY_relative + textHeight + (4 * density);

        if (task.type == TaskItem.Type.SWITCH) {
            if (task.switchView != null) {
                float switchY_relative = (itemHeight - task.switchView.getLayoutParams().height) / 2;
                task.switchView.setTranslationY(yOffset + switchY_relative);
            }
            contentLeft += switchWidth + this.padding / 2;
            availableWidth -= (switchWidth + this.padding / 2);
        } else if (task.icon != null) {
            float iconTop_relative = (itemHeight - iconSize) / 2;
            task.icon.setBounds(
                    (int) contentLeft, (int) iconTop_relative,
                    (int) (contentLeft + iconSize), (int) (iconTop_relative + iconSize)
            );
            task.icon.draw(canvas);
            contentLeft += iconSize + this.padding / 2;
            availableWidth -= (iconSize + this.padding / 2);
        }

        canvas.drawText(task.text, contentLeft, textY, textPaint);

        float progressWidth = availableWidth - (contentLeft - this.padding);
        RectF bgRect = new RectF(contentLeft, progressY, contentLeft + progressWidth, progressY + progressHeight);
        canvas.drawRoundRect(bgRect, progressHeight, progressHeight, progressBackgroundPaint);
        RectF progressRect = new RectF(contentLeft, progressY, contentLeft + progressWidth * task.displayProgress, progressY + progressHeight);
        canvas.drawRoundRect(progressRect, progressHeight, progressHeight, progressPaint);

        canvas.restoreToCount(save);
    }

    public void setPersistentText(String text) {
        this.persistentText = text;
        if (currentState == State.COLLAPSED) {
            updateCollapsedWidth();
            animateToSize(collapsedWidth, collapsedHeight, collapsedCornerRadius);
        }
    }

    public void addSwitch(String text, boolean state) {
        TaskItem task = new TaskItem(TaskItem.Type.SWITCH, text + (state ? " 已开启" : " 已关闭"));
        task.switchState = state;
        task.duration = 4000;

        MaterialSwitch switchView = new MaterialSwitch(getContext());
        switchView.setChecked(!state);
        switchView.setClickable(false);
        switchView.setFocusable(false);

        LayoutParams params = new LayoutParams((int) switchWidth, (int) switchHeight);
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = (int) this.padding;
        switchView.setLayoutParams(params);

        addView(switchView);
        task.switchView = switchView;

        postDelayed(() -> {
            if (switchView.getParent() != null) switchView.setChecked(state);
        }, 300);

        addTask(task);
    }

    public void addProgress(String text, @Nullable Drawable icon, long duration) {
        TaskItem task = new TaskItem(TaskItem.Type.PROGRESS, text);
        task.icon = icon;
        task.duration = duration;
        addTask(task);
    }

    private void addTask(TaskItem task) {
        tasks.add(0, task);

        task.progressAnimator = ValueAnimator.ofFloat(1f, 0f);
        task.progressAnimator.setDuration(task.duration);
        task.progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        task.progressAnimator.addUpdateListener(animation -> {
            task.progress = (float) animation.getAnimatedValue();
            task.displayProgress += (task.progress - task.displayProgress) * 0.1f;
            invalidate();
        });
        task.progressAnimator.start();

        if (currentState == State.COLLAPSED) {
            currentState = State.EXPANDED;
            updateHandler.post(updateRunnable);
            animateGlow(true);
        }

        updateExpandedSize();
    }

    private void updateTasks() {
        boolean needsRedraw = false;

        if (currentState == State.EXPANDED) {
            glowRotation = (glowRotation + 1.5f) % 360;
            needsRedraw = true;
        }

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
                if (task.progress <= 0.01f && !task.removing) {
                    task.removing = true;
                    needsRedraw = true;
                }
                if (Math.abs(task.displayProgress - task.progress) > 0.001f) {
                    task.displayProgress += (task.progress - task.displayProgress) * 0.15f;
                    needsRedraw = true;
                }
            }
        }

        if (tasks.isEmpty() && currentState == State.EXPANDED) {
            currentState = State.COLLAPSED;
            updateCollapsedWidth();
            animateToSize(collapsedWidth, collapsedHeight, collapsedCornerRadius);
            animateGlow(false);
        }

        if (needsRedraw) invalidate();
    }

    private void updateCollapsedWidth() {
        String staticText = "LuminaCN B22";
        String separator = " • ";
        String timeText = "00:00";
        float staticWidth = textPaint.measureText(staticText);
        float separatorWidth = separatorPaint.measureText(separator);
        float timeWidth = timePaint.measureText(timeText);
        float persistentWidth = textPaint.measureText(persistentText);
        float totalWidth = staticWidth + separatorWidth * 2 + timeWidth + persistentWidth;
        collapsedWidth = totalWidth + this.padding * 2.5f;
    }

    private void updateExpandedSize() {
        float targetHeight = tasks.size() * itemHeight + this.padding * 2;
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

    public void hide() {
        for (TaskItem task : tasks) {
            if (task.switchView != null) removeView(task.switchView);
            if (task.progressAnimator != null) task.progressAnimator.cancel();
        }
        tasks.clear();
        currentState = State.COLLAPSED;
        updateHandler.removeCallbacks(updateRunnable);
        updateCollapsedWidth();
        animateToSize(collapsedWidth, collapsedHeight, collapsedCornerRadius);
        animateGlow(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateHandler.removeCallbacks(updateRunnable);
        if (sizeAnimator != null) sizeAnimator.cancel();
        if (glowAnimator != null) glowAnimator.cancel();
        for (TaskItem task : tasks) {
            if (task.progressAnimator != null) task.progressAnimator.cancel();
        }
    }
}