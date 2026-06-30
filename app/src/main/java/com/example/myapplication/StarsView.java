package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StarsView extends View {

    private static final int PARTICLE_COUNT = 30;
    private static final float MIN_SIZE_DP = 6f;
    private static final float MAX_SIZE_DP = 18f;
    private static final float MIN_SPEED_DP = 0.3f;
    private static final float MAX_SPEED_DP = 1.2f;

    private enum Shape { CIRCLE, ROUNDED_RECT, DIAMOND }

    private List<Particle> particles = new ArrayList<>();
    private Paint paint;
    private Paint bgPaint;
    private Path diamondPath;
    private Random random;
    private ValueAnimator animator;
    private float time = 0f;
    private int themeIndex = 0;
    private boolean particlesInitialized = false;
    private float density = 1f;

    private int[][] themeConfigs = {
            {0xFFF5E6D3, 0xFFE8D5B7, 0xFFF2D08A, 0xFFE8A87C, 0xFFF5CBA7},
            {0xFF2D2D3F, 0xFF1A1A2E, 0xFFFFD700, 0xFFB8C5D6, 0xFFC9B1FF},
            {0xFFB8D4E3, 0xFF7BA7BC, 0xFF87CEEB, 0xFF98D8C8, 0xFFA8D5E5},
            {0xFFFFD1DC, 0xFFFFB7C5, 0xFFFFB6C1, 0xFFF48FB1, 0xFFFFCCBC},
            {0xFFC8E6C9, 0xFF81C784, 0xFFA5D6A7, 0xFF81C784, 0xFFA7E9C8},
            {0xFFFFCCBC, 0xFFFF8A65, 0xFFFFAB91, 0xFFFF8A65, 0xFFFFCCBC},
    };

    private int[] currentParticleColors;

    public StarsView(Context context) {
        this(context, null);
    }

    public StarsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diamondPath = new Path();
        random = new Random();
        currentParticleColors = new int[]{
                themeConfigs[themeIndex][2],
                themeConfigs[themeIndex][3],
                themeConfigs[themeIndex][4]
        };
    }

    private void initParticles() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float minSize = MIN_SIZE_DP * density;
        float maxSize = MAX_SIZE_DP * density;
        float minSpeed = MIN_SPEED_DP * density;
        float maxSpeed = MAX_SPEED_DP * density;

        particles.clear();
        Shape[] shapes = Shape.values();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.x = random.nextFloat() * w;
            p.y = random.nextFloat() * h;
            p.size = minSize + random.nextFloat() * (maxSize - minSize);
            p.shape = shapes[i % shapes.length];
            p.color = currentParticleColors[random.nextInt(currentParticleColors.length)];
            p.alpha = 30 + random.nextInt(50);
            p.vy = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);
            p.rotation = random.nextFloat() * 360f;
            p.rotationSpeed = (random.nextFloat() - 0.5f) * 3f;
            if (p.rotationSpeed < 0) p.rotationSpeed -= 0.3f;
            else p.rotationSpeed += 0.3f;
            p.swayPhase = random.nextFloat() * (float) Math.PI * 2;
            p.swayAmplitude = (5f + random.nextFloat() * 10f) * density;
            p.swaySpeed = 0.5f + random.nextFloat() * 1f;
            particles.add(p);
        }
        particlesInitialized = true;
    }

    private void updateGradient() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        int startColor = themeConfigs[themeIndex][0];
        int endColor = themeConfigs[themeIndex][1];
        bgPaint.setShader(new LinearGradient(
                0, 0, 0, h,
                startColor, endColor,
                Shader.TileMode.CLAMP));
    }

    public void setTheme(int themeIndex) {
        this.themeIndex = themeIndex;
        currentParticleColors = new int[]{
                themeConfigs[themeIndex][2],
                themeConfigs[themeIndex][3],
                themeConfigs[themeIndex][4]
        };

        for (Particle p : particles) {
            p.color = currentParticleColors[random.nextInt(currentParticleColors.length)];
        }

        updateGradient();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient();
        if (!particlesInitialized) {
            initParticles();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    private void startAnimation() {
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0, 1000);
            animator.setDuration(60000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                time += 0.016f;
                updateParticles();
                invalidate();
            });
        }
        if (!animator.isRunning()) {
            animator.start();
        }
    }

    private void stopAnimation() {
        if (animator != null) {
            animator.cancel();
        }
    }

    private void updateParticles() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        int count = particles.size();
        for (int i = 0; i < count; i++) {
            Particle p = particles.get(i);
            float swayX = (float) Math.sin(time * p.swaySpeed + p.swayPhase) * p.swayAmplitude * 0.02f;
            p.x += swayX;
            p.y -= p.vy;
            p.rotation += p.rotationSpeed;

            if (p.y + p.size < 0) {
                p.y = h + p.size;
                p.x = random.nextFloat() * w;
            }
            if (p.x < -p.size) p.x = w + p.size;
            if (p.x > w + p.size) p.x = -p.size;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bgPaint.getShader() != null) {
            canvas.drawPaint(bgPaint);
        }

        int count = particles.size();
        for (int i = 0; i < count; i++) {
            Particle p = particles.get(i);
            paint.setColor(p.color);
            paint.setAlpha(p.alpha);

            canvas.save();
            canvas.translate(p.x, p.y);
            canvas.rotate(p.rotation);

            switch (p.shape) {
                case CIRCLE:
                    canvas.drawCircle(0, 0, p.size / 2f, paint);
                    break;
                case ROUNDED_RECT:
                    float half = p.size / 2f;
                    float radius = p.size * 0.2f;
                    canvas.drawRoundRect(-half, -half, half, half, radius, radius, paint);
                    break;
                case DIAMOND:
                    float halfD = p.size / 2f;
                    diamondPath.reset();
                    diamondPath.moveTo(0, -halfD);
                    diamondPath.lineTo(halfD, 0);
                    diamondPath.lineTo(0, halfD);
                    diamondPath.lineTo(-halfD, 0);
                    diamondPath.close();
                    canvas.drawPath(diamondPath, paint);
                    break;
            }

            canvas.restore();
        }
    }

    private static class Particle {
        float x, y;
        float size;
        Shape shape;
        int color;
        int alpha;
        float vy;
        float rotation;
        float rotationSpeed;
        float swayPhase;
        float swayAmplitude;
        float swaySpeed;
    }
}
