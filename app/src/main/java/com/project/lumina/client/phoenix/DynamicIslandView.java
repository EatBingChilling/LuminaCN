package com.project.lumina.client.phoenix;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // 【修复】添加缺失的 @Nullable 注解导入
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
        enum Type { SWITCH, PROGRESS }
        
        String key;
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
        
        TaskItem(Type type, String text, String key) {
            this.type = type;
            this.text = text;
            this.key = key;
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
    private float cornerRadius;
    private float iconSize;
    private float progressHeight;
    private float switchWidth;
    private float switchHeight;
    private float iconContainerSize;
    
    // 当前尺寸（用于动画）
    private float currentWidth;
    private float currentHeight;
    private float currentRadius;
    
    // 画笔
    private Paint backgroundPaint;
    private TextPaint textPaint;
    private TextPaint separatorPaint;
    private TextPaint timePaint;
    private Paint progressBackgroundPaint;
    private Paint progressPaint;
    private Paint iconContainerPaint;
    private Paint iconGlowPaint;
    
    private int colorOnPrimary;
    private int colorPrimary;

    // 数据
    private String persistentText = "Phoen1x";
    private final List<TaskItem> tasks = new ArrayList<>();
    
    // 动画
    private ValueAnimator sizeAnimator;
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateTasks();
            if (!tasks.isEmpty()) {
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
        
        collapsedHeight = 36 * density;
        itemHeight = 48 * density;
        expandedWidth = 280 * density;
        padding = 12 * density;
        cornerRadius = collapsedHeight / 2;
        iconSize = 24 * density;
        iconContainerSize = 36 * density;
        progressHeight = 3 * density;
        switchWidth = 52 * density;
        switchHeight = 32 * density;
        
        initPaints();
        
        updateCollapsedWidth();
        currentWidth = collapsedWidth;
        currentHeight = collapsedHeight;
        currentRadius = cornerRadius;
        
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }
    
    private void initPaints() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorPrimary = MaterialColors.getColor(this, R.attr.colorPrimary);
        backgroundPaint.setColor(ColorUtils.setAlphaComponent(colorPrimary, 200));

        colorOnPrimary = MaterialColors.getColor(this, R.attr.colorOnPrimary);
        
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(colorOnPrimary);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, getResources().getDisplayMetrics()));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        
        separatorPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        separatorPaint.setColor(ColorUtils.setAlphaComponent(colorOnPrimary, 100));
        separatorPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, getResources().getDisplayMetrics()));
        
        timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(colorOnPrimary);
        timePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, getResources().getDisplayMetrics()));
        
        progressBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressBackgroundPaint.setColor(ColorUtils.setAlphaComponent(colorOnPrimary, 50));
        
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(colorOnPrimary);

        iconContainerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconContainerPaint.setColor(ColorUtils.setAlphaComponent(colorPrimary, 180));

        iconGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconGlowPaint.setColor(colorPrimary);
        setLayerType(View.LAYER_TYPE_SOFTWARE, iconGlowPaint);
        iconGlowPaint.setShadowLayer(6 * density, 0, 0, ColorUtils.setAlphaComponent(colorPrimary, 100));
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(measuredWidth, (int) currentHeight);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        float left = (getWidth() - currentWidth) / 2;
        
        canvas.save();
        canvas.translate(left, 0);

        RectF bgRect = new RectF(0, 0, currentWidth, currentHeight);
        canvas.drawRoundRect(bgRect, currentRadius, currentRadius, backgroundPaint);
        
        if (currentState == State.COLLAPSED) {
            drawCollapsedText(canvas);
        } else {
            drawTasks(canvas);
        }

        canvas.restore();
    }
    
    private void drawCollapsedText(Canvas canvas) {
        String staticText = "LuminaCN B22";
        String separator = " | ";
        String timeText = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        
        float staticWidth = textPaint.measureText(staticText);
        float separatorWidth = separatorPaint.measureText(separator);
        float timeWidth = timePaint.measureText(timeText);
        float persistentWidth = textPaint.measureText(persistentText);
        
        float totalWidth = staticWidth + separatorWidth + timeWidth + separatorWidth + persistentWidth;
        float startX = (currentWidth - totalWidth) / 2;
        float y = currentHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2;
        
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
    
    private void drawTasks(Canvas canvas) {
        float y = padding;
        
        for (int i = 0; i < tasks.size(); i++) {
            TaskItem task = tasks.get(i);
            if (task.alpha <= 0) continue;
            
            int saveCount = canvas.save();
            canvas.translate(0, y);
            
            int alpha = (int) (task.alpha * 255);
            textPaint.setAlpha(alpha);
            progressBackgroundPaint.setAlpha((int) (alpha * 0.2f));
            progressPaint.setAlpha(alpha);
            iconContainerPaint.setAlpha((int) (alpha * 0.7f));

            if (task.switchView != null) {
                task.switchView.setAlpha(task.alpha);
                float switchY = y + (itemHeight - switchHeight) / 2;
                float leftOffset = (getWidth() - currentWidth) / 2;
                task.switchView.setTranslationX(leftOffset);
                task.switchView.setTranslationY(switchY);
            }
            
            drawTask(canvas, task);
            
            canvas.restoreToCount(saveCount);
            y += itemHeight;
        }
        
        textPaint.setAlpha(255);
        progressBackgroundPaint.setAlpha(50);
        progressPaint.setAlpha(255);
        iconContainerPaint.setAlpha(180);
    }
    
    private void drawTask(Canvas canvas, TaskItem task) {
        float textX = padding;
        float textY = itemHeight / 2 - progressHeight;
        
        if (task.type == TaskItem.Type.SWITCH) {
            textX += switchWidth + padding / 2;
        } else if (task.icon != null) {
            float containerCX = padding + iconContainerSize / 2;
            float containerCY = itemHeight / 2;
            float containerRadius = iconContainerSize / 2;
            
            canvas.drawCircle(containerCX, containerCY, containerRadius - (2 * density), iconGlowPaint);
            canvas.drawCircle(containerCX, containerCY, containerRadius, iconContainerPaint);

            Drawable mutableIcon = DrawableCompat.wrap(task.icon.mutate());
            DrawableCompat.setTint(mutableIcon, colorOnPrimary);
            int iconLeft = (int) (containerCX - iconSize / 2);
            int iconTop = (int) (containerCY - iconSize / 2);
            mutableIcon.setBounds(iconLeft, iconTop, (int) (iconLeft + iconSize), (int) (iconTop + iconSize));
            mutableIcon.setAlpha((int) (task.alpha * 255));
            mutableIcon.draw(canvas);

            textX += iconContainerSize + padding / 2;
        }
        
        canvas.drawText(task.text, textX, textY, textPaint);
        
        float progressY = itemHeight - progressHeight - padding;
        float progressWidth = currentWidth - textX - padding;
        
        RectF bgRect = new RectF(textX, progressY, textX + progressWidth, progressY + progressHeight);
        canvas.drawRoundRect(bgRect, progressHeight / 2, progressHeight / 2, progressBackgroundPaint);
        
        float progressRight = textX + progressWidth * task.displayProgress;
        RectF progressRect = new RectF(textX, progressY, progressRight, progressY + progressHeight);
        canvas.drawRoundRect(progressRect, progressHeight / 2, progressHeight / 2, progressPaint);
    }
    
    public void setPersistentText(String text) {
        this.persistentText = text;
        if (currentState == State.COLLAPSED) {
            updateCollapsedWidth();
            animateToSize(collapsedWidth, collapsedHeight, cornerRadius);
        }
    }
    
    public void addSwitch(String text, boolean state) {
        String key = "switch-" + text;
        
        for (TaskItem existingTask : tasks) {
            if (Objects.equals(existingTask.key, key)) {
                return;
            }
        }

        TaskItem task = new TaskItem(TaskItem.Type.SWITCH, text + (state ? " 已开启" : " 已关闭"), key);
        task.switchState = state;
        task.duration = 4000;
        
        MaterialSwitch switchView = new MaterialSwitch(getContext());
        switchView.setChecked(!state);
        switchView.setClickable(false);
        switchView.setFocusable(false);
        
        LayoutParams params = new LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.START | Gravity.TOP;
        params.leftMargin = (int) padding;
        params.topMargin = 0;
        
        switchView.setLayoutParams(params);
        addView(switchView);
        
        task.switchView = switchView;
        
        postDelayed(() -> {
            if (switchView.getParent() != null) {
                switchView.setChecked(state);
            }
        }, 300);
        
        addTask(task);
    }
    
    public void addProgress(String text, @Nullable Drawable icon, long duration, String key) {
        for (TaskItem existingTask : tasks) {
            if (Objects.equals(existingTask.key, key)) {
                existingTask.text = text;
                existingTask.startTime = System.currentTimeMillis();
                existingTask.duration = duration;
                if (existingTask.progressAnimator != null) {
                    existingTask.progressAnimator.cancel();
                }
                existingTask.progressAnimator = ValueAnimator.ofFloat(1f, 0f);
                existingTask.progressAnimator.setDuration(duration);
                existingTask.progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                existingTask.progressAnimator.addUpdateListener(animation -> {
                    existingTask.progress = (float) animation.getAnimatedValue();
                    invalidate();
                });
                existingTask.progressAnimator.start();
                return;
            }
        }

        TaskItem task = new TaskItem(TaskItem.Type.PROGRESS, text, key);
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
            
            float diff = task.progress - task.displayProgress;
            task.displayProgress += diff * 0.1f;
            
            invalidate();
        });
        task.progressAnimator.start();
        
        if (currentState == State.COLLAPSED) {
            currentState = State.EXPANDED;
            updateHandler.post(updateRunnable);
        }
        
        updateExpandedSize();
    }
    
    private void updateTasks() {
        boolean needsRedraw = false;
        
        Iterator<TaskItem> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            TaskItem task = iterator.next();
            
            if (task.removing) {
                task.alpha -= 0.05f;
                if (task.alpha <= 0) {
                    if (task.switchView != null) {
                        removeView(task.switchView);
                    }
                    if (task.progressAnimator != null) {
                        task.progressAnimator.cancel();
                    }
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
                    float diff = task.progress - task.displayProgress;
                    task.displayProgress += diff * 0.15f;
                    needsRedraw = true;
                }
            }
        }
        
        if (tasks.isEmpty() && currentState == State.EXPANDED) {
            currentState = State.COLLAPSED;
            updateCollapsedWidth();
            animateToSize(collapsedWidth, collapsedHeight, cornerRadius);
        }
        
        if (needsRedraw) {
            invalidate();
        }
    }
    
    private void updateCollapsedWidth() {
        String staticText = "LuminaCN B22";
        String separator = " | ";
        String timeText = "00:00:00";
        
        float staticWidth = textPaint.measureText(staticText);
        float separatorWidth = separatorPaint.measureText(separator);
        float timeWidth = timePaint.measureText(timeText);
        float persistentWidth = textPaint.measureText(persistentText);
        
        float totalWidth = staticWidth + separatorWidth * 2 + timeWidth + persistentWidth;
        collapsedWidth = totalWidth + padding * 2;
    }
    
    private void updateExpandedSize() {
        float targetHeight = tasks.size() * itemHeight + padding * 2;
        animateToSize(expandedWidth, targetHeight, cornerRadius);
    }
    
    private void animateToSize(float targetWidth, float targetHeight, float targetRadius) {
        if (sizeAnimator != null && sizeAnimator.isRunning()) {
            sizeAnimator.cancel();
        }
        
        float startWidth = currentWidth;
        float startHeight = currentHeight;
        float startRadius = currentRadius;
        
        sizeAnimator = ValueAnimator.ofFloat(0, 1);
        sizeAnimator.setDuration(300);
        sizeAnimator.setInterpolator(new DecelerateInterpolator());
        sizeAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            currentWidth = startWidth + (targetWidth - startWidth) * fraction;
            currentHeight = startHeight + (targetHeight - startHeight) * fraction;
            currentRadius = startRadius + (targetRadius - startRadius) * fraction;
            requestLayout();
        });
        sizeAnimator.start();
    }
    
    public void hide() {
        for (TaskItem task : tasks) {
            if (task.switchView != null) {
                removeView(task.switchView);
            }
            if (task.progressAnimator != null) {
                task.progressAnimator.cancel();
            }
        }
        
        tasks.clear();
        currentState = State.COLLAPSED;
        updateHandler.removeCallbacks(updateRunnable);
        updateCollapsedWidth();
        animateToSize(collapsedWidth, collapsedHeight, cornerRadius);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateHandler.removeCallbacks(updateRunnable);
        if (sizeAnimator != null) {
            sizeAnimator.cancel();
        }
        for (TaskItem task : tasks) {
            if (task.progressAnimator != null) {
                task.progressAnimator.cancel();
            }
        }
    }
}