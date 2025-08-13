package com.project.lumina.client.phoenix;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
import java.util.Objects;

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

        String id; // 新增：任务的唯一标识符
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

        TaskItem(String id, Type type, String text) {
            this.id = id;
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
    private float iconContainerCornerRadius;

    // 当前尺寸（用于动画）
    private float currentWidth;
    private float currentHeight;
    private float currentRadius;

    // 画笔
    private Paint backgroundPaint, frostPaint, glowPaint, progressBackgroundPaint, progressPaint, iconContainerPaint;
    private TextPaint textPaint, separatorPaint, timePaint;

    // 数据
    private String persistentText = "Phoen1x";
    private final List<TaskItem> tasks = new ArrayList<>();

    // 动画
    private ValueAnimator sizeAnimator, glowAnimator;
    private float glowAlpha = 0f, glowRotation = 0f;
    private final Matrix glowMatrix = new Matrix();
    private Shader glowShader;

    // FPS 计算
    private long lastFrameTimeNanos = 0;
    private float currentFps = 0f;

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
        iconContainerCornerRadius = 8 * density;
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

        // 新增：图标发光容器画笔
        iconContainerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int primaryColor = MaterialColors.getColor(this, R.attr.colorPrimary);
        iconContainerPaint.setColor(ColorUtils.setAlphaComponent(primaryColor, 60));
        iconContainerPaint.setMaskFilter(new BlurMaskFilter(4 * density, BlurMaskFilter.Blur.NORMAL));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension((int) currentWidth, (int) currentHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // 调用 super.onLayout 以便子视图（如Switch）能正确布局
        super.onLayout(changed, left, top, right, bottom);
        
        if (changed) {
            updateGlowShader();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 在添加到窗口后设置居中位置
        post(() -> {
            ViewGroup.LayoutParams params = getLayoutParams();
            if (params instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) params).gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                setLayoutParams(params);
            }
        });
    }

    private void updateGlowShader() {
        glowShader = new SweepGradient(getWidth() / 2f, getHeight() / 2f, new int[]{Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.CYAN}, null);
        glowPaint.setShader(glowShader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // FPS 计算
        long now = System.nanoTime();
        if (lastFrameTimeNanos != 0) {
            long frameTime = now - lastFrameTimeNanos;
            if (frameTime > 0) {
                currentFps = 1_000_000_000.0f / frameTime;
            }
        }
        lastFrameTimeNanos = now;

        float contentLeft = getPaddingLeft();
        float contentTop = getPaddingTop();
        float contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float contentHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (glowAlpha > 0) {
            glowPaint.setAlpha((int) (glowAlpha * 255));
            glowMatrix.setRotate(glowRotation, getWidth() / 2f, getHeight() / 2f);
            glowShader.setLocalMatrix(glowMatrix);
            RectF glowRect = new RectF(contentLeft - glowPaint.getStrokeWidth() / 2, contentTop - glowPaint.getStrokeWidth() / 2, contentLeft + contentWidth + glowPaint.getStrokeWidth() / 2, contentTop + contentHeight + glowPaint.getStrokeWidth() / 2);
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

        // 强制重绘以更新FPS
        invalidate();
    }

    private void drawCollapsedText(Canvas canvas, float contentWidth, float contentHeight) {
        String staticText = "LuminaCN B22";
        String separator = " • ";
        // 将时间改为显示FPS
        String fpsText = String.format(Locale.US, "%.1f FPS", currentFps);

        float staticWidth = textPaint.measureText(staticText);
        float separatorWidth = separatorPaint.measureText(separator);
        float fpsWidth = timePaint.measureText(fpsText);
        float persistentWidth = textPaint.measureText(persistentText);

        float totalWidth = staticWidth + separatorWidth + fpsWidth + separatorWidth + persistentWidth;
        float startX = (contentWidth - totalWidth) / 2;
        float y = contentHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2;

        canvas.drawText(staticText, startX, y, textPaint);
        startX += staticWidth;
        canvas.drawText(separator, startX, y, separatorPaint);
        startX += separatorWidth;
        canvas.drawText(fpsText, startX, y, timePaint);
        startX += fpsWidth;
        canvas.drawText(separator, startX, y, separatorPaint);
        startX += separatorWidth;
        canvas.drawText(persistentText, startX, y, textPaint);
    }

    private void drawTasks(Canvas canvas, float contentWidth) {
        float yOffset = this.padding;
        for (TaskItem task : tasks) {
            if (task.alpha <= 0) continue;
            int alpha = (int) (task.alpha * 255);
            textPaint.setAlpha(alpha);
            progressBackgroundPaint.setAlpha((int) (alpha * 0.2f));
            progressPaint.setAlpha(alpha);
            iconContainerPaint.setAlpha((int) (alpha * 0.25f)); // 60/255 ≈ 0.25
            if (task.switchView != null) task.switchView.setAlpha(task.alpha);
            drawTask(canvas, task, yOffset, contentWidth);
            yOffset += itemHeight;
        }
        textPaint.setAlpha(255);
        progressBackgroundPaint.setAlpha(50);
        progressPaint.setAlpha(255);
        iconContainerPaint.setAlpha(60);
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
            float iconContainerPadding = 4 * density;
            float iconContainerSize = iconSize + iconContainerPadding * 2;
            float iconContainerTop = (itemHeight - iconContainerSize) / 2;
            RectF containerRect = new RectF(contentLeft, iconContainerTop, contentLeft + iconContainerSize, iconContainerTop + iconContainerSize);
            // 1. 绘制发光容器
            canvas.drawRoundRect(containerRect, iconContainerCornerRadius, iconContainerCornerRadius, iconContainerPaint);

            // 2. 修复图标颜色并绘制
            float iconTop_relative = (itemHeight - iconSize) / 2;
            task.icon.setBounds((int) (contentLeft + iconContainerPadding), (int) iconTop_relative, (int) (contentLeft + iconContainerPadding + iconSize), (int) (iconTop_relative + iconSize));
            task.icon.setColorFilter(new PorterDuffColorFilter(textPaint.getColor(), PorterDuff.Mode.SRC_IN));
            task.icon.draw(canvas);
            task.icon.clearColorFilter(); // 清除，防止影响其他地方

            contentLeft += iconContainerSize + this.padding / 2;
            availableWidth -= (iconContainerSize + this.padding / 2);
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

    /**
     * 为 TargetHUD 定制：添加或更新一个进度条任务。
     * 如果具有相同 id 的任务已存在，则更新其内容并重置进度条；否则，添加新任务。
     *
     * @param id       任务的唯一标识符 (例如，目标名称)
     * @param text     要显示的文本
     * @param icon     要显示的图标
     * @param duration 进度条持续时间 (毫秒)
     */
    public void addOrUpdateProgress(@NonNull String id, String text, @Nullable Drawable icon, long duration) {
        // 查找现有任务
        for (TaskItem existingTask : tasks) {
            if (Objects.equals(existingTask.id, id) && existingTask.type == TaskItem.Type.PROGRESS) {
                // 找到了，更新它
                existingTask.text = text;
                existingTask.icon = icon;
                existingTask.duration = duration;
                existingTask.startTime = System.currentTimeMillis();

                // 重置动画
                if (existingTask.progressAnimator != null) {
                    existingTask.progressAnimator.cancel();
                }
                existingTask.progress = 1.0f;
                existingTask.displayProgress = 1.0f; // 立即重置显示进度
                existingTask.removing = false;
                existingTask.alpha = 1.0f;
                startProgressAnimation(existingTask);

                // 如果视图已折叠，则重新展开
                if (currentState == State.COLLAPSED) {
                    currentState = State.EXPANDED;
                    updateHandler.post(updateRunnable);
                    animateGlow(true);
                    updateExpandedSize();
                }

                invalidate();
                return; // 更新完成，退出
            }
        }

        // 没找到，添加新任务
        TaskItem task = new TaskItem(id, TaskItem.Type.PROGRESS, text);
        task.icon = icon;
        task.duration = duration;
        addTask(task);
    }

    // 旧的 addProgress 方法，用于总是添加新任务的场景
    public void addProgress(String text, @Nullable Drawable icon, long duration) {
        // 使用时间戳作为唯一ID，确保每次都添加
        String uniqueId = "progress_" + System.currentTimeMillis();
        TaskItem task = new TaskItem(uniqueId, TaskItem.Type.PROGRESS, text);
        task.icon = icon;
        task.duration = duration;
        addTask(task);
    }

    /**
     * 添加一个开关通知
     * @param moduleName 模块名称
     * @param moduleState 模块状态（开/关）
     */
    public void addSwitch(String moduleName, boolean moduleState) {
        // 创建一个唯一ID
        String uniqueId = "switch_" + moduleName + "_" + System.currentTimeMillis();
        TaskItem task = new TaskItem(uniqueId, TaskItem.Type.SWITCH, moduleName);
        task.switchState = moduleState;
        
        // 创建 MaterialSwitch
        MaterialSwitch switchView = new MaterialSwitch(getContext());
        switchView.setChecked(moduleState);
        switchView.setEnabled(false); // 设置为不可交互，只用于显示
        
        // 设置布局参数
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            (int) switchWidth,
            (int) switchHeight
        );
        params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        params.leftMargin = (int) padding;
        switchView.setLayoutParams(params);
        
        // 添加 switch 到视图
        addView(switchView);
        task.switchView = switchView;
        
        // 添加任务
        tasks.add(0, task);
        
        // 设置3秒后自动移除
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            task.removing = true;
        }, 3000);
        
        if (currentState == State.COLLAPSED) {
            currentState = State.EXPANDED;
            updateHandler.post(updateRunnable);
            animateGlow(true);
        }
        
        updateExpandedSize();
    }

    private void addTask(TaskItem task) {
        tasks.add(0, task);
        startProgressAnimation(task);

        if (currentState == State.COLLAPSED) {
            currentState = State.EXPANDED;
            updateHandler.post(updateRunnable);
            animateGlow(true);
        }

        updateExpandedSize();
    }

    private void startProgressAnimation(TaskItem task) {
        task.progressAnimator = ValueAnimator.ofFloat(1f, 0f);
        task.progressAnimator.setDuration(task.duration);
        task.progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        task.progressAnimator.addUpdateListener(animation -> {
            task.progress = (float) animation.getAnimatedValue();
            task.displayProgress += (task.progress - task.displayProgress) * 0.1f;
            invalidate();
        });
        task.progressAnimator.start();
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
                if (task.progress <= 0.01f && !task.removing && task.type == TaskItem.Type.PROGRESS) {
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
        String fpsText = "00.0 FPS"; // 用于估算宽度
        float staticWidth = textPaint.measureText(staticText);
        float separatorWidth = separatorPaint.measureText(separator);
        float fpsWidth = timePaint.measureText(fpsText);
        float persistentWidth = textPaint.measureText(persistentText);
        float totalWidth = staticWidth + separatorWidth * 2 + fpsWidth + persistentWidth;
        collapsedWidth = totalWidth + this.padding * 2.5f;
    }

    private void updateExpandedSize() {
        float targetHeight = tasks.size() * itemHeight + this.padding * 2;
        animateToSize(expandedWidth, targetHeight, expandedCornerRadius);
    }

    private void animateToSize(float targetContentWidth, float targetContentHeight, float targetRadius) {
        if (sizeAnimator != null && sizeAnimator.isRunning()) sizeAnimator.cancel();
        float startWidth = currentWidth, startHeight = currentHeight, startRadius = currentRadius;
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
        });
        sizeAnimator.start();
    }

    private void animateGlow(boolean fadeIn) {
        if (glowAnimator != null && glowAnimator.isRunning()) glowAnimator.cancel();
        float start = glowAlpha, end = fadeIn ? 1.0f : 0.0f;
        glowAnimator = ValueAnimator.ofFloat(start, end);
        glowAnimator.setDuration(500);
        glowAnimator.setInterpolator(new DecelerateInterpolator());
        glowAnimator.addUpdateListener(animation -> {
            glowAlpha = (float) animation.getAnimatedValue();
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