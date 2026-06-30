package com.example.myapplication;

import android.util.Log;
import android.view.Choreographer;

public class FPSMonitor implements Choreographer.FrameCallback {

    private static final String TAG = "FPSMonitor";
    private static final long MONITOR_INTERVAL_MS = 1000;

    private Choreographer choreographer;
    private long lastFrameTimeNanos = 0;
    private int frameCount = 0;
    private long startTimeMs = 0;
    private boolean isRunning = false;
    private FPSListener listener;

    public interface FPSListener {
        void onFPSUpdate(float fps);
    }

    public FPSMonitor() {
        choreographer = Choreographer.getInstance();
    }

    public void setListener(FPSListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        frameCount = 0;
        startTimeMs = System.currentTimeMillis();
        lastFrameTimeNanos = 0;
        choreographer.postFrameCallback(this);
        Log.d(TAG, "FPS monitor started");
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;
        choreographer.removeFrameCallback(this);
        Log.d(TAG, "FPS monitor stopped");
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isRunning) return;

        frameCount++;

        long currentTimeMs = System.currentTimeMillis();
        long elapsedMs = currentTimeMs - startTimeMs;

        if (elapsedMs >= MONITOR_INTERVAL_MS) {
            float fps = (frameCount * 1000f) / elapsedMs;
            if (listener != null) {
                listener.onFPSUpdate(fps);
            }
            Log.d(TAG, String.format("FPS: %.1f", fps));
            frameCount = 0;
            startTimeMs = currentTimeMs;
        }

        lastFrameTimeNanos = frameTimeNanos;
        choreographer.postFrameCallback(this);
    }

    public static float getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return used / (1024f * 1024f);
    }

    public static void logMemoryUsage(String label) {
        float mb = getMemoryUsageMB();
        Log.d(TAG, String.format("[%s] Memory: %.1f MB", label, mb));
    }
}
