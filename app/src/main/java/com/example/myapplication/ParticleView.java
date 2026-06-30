package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleView extends View {

    private static final int PARTICLE_COUNT = 25;
    private static final int MIN_RADIUS = 4;
    private static final int MAX_RADIUS = 12;
    private static final float MIN_SPEED = 0.5f;
    private static final float MAX_SPEED = 2.0f;

    private List<Particle> particles = new ArrayList<>();
    private Paint paint;
    private Random random;
    private ValueAnimator animator;

    private int[] colors = {
            Color.parseColor("#F2D08A"),
            Color.parseColor("#F0B27A"),
            Color.parseColor("#E8A87C"),
            Color.parseColor("#F5CBA7"),
            Color.parseColor("#D8956E"),
    };

    public ParticleView(Context context) {
        this(context, null);
    }

    public ParticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        random = new Random();
        post(this::initParticles);
    }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.x = random.nextFloat() * getWidth();
            p.y = random.nextFloat() * getHeight();
            p.radius = MIN_RADIUS + random.nextFloat() * (MAX_RADIUS - MIN_RADIUS);
            p.color = colors[random.nextInt(colors.length)];
            p.alpha = 40 + random.nextInt(60);
            p.vx = (random.nextFloat() - 0.5f) * MAX_SPEED * 2;
            p.vy = random.nextFloat() * MAX_SPEED + MIN_SPEED;
            if (p.vy < 0) p.vy = -p.vy;
            particles.add(p);
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
            animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(16);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> updateParticles());
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

        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;

            if (p.x < 0 || p.x > w) {
                p.vx = -p.vx;
                p.x = Math.max(0, Math.min(w, p.x));
            }

            if (p.y > h + p.radius) {
                p.y = -p.radius;
                p.x = random.nextFloat() * w;
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Particle p : particles) {
            paint.setColor(p.color);
            paint.setAlpha(p.alpha);
            canvas.drawCircle(p.x, p.y, p.radius, paint);
        }
    }

    private static class Particle {
        float x, y;
        float vx, vy;
        float radius;
        int color;
        int alpha;
    }
}
