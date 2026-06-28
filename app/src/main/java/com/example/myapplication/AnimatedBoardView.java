package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * 自动演示 2048 棋局的视图，用于登录界面。
 * 内部使用真实的 Game2048 引擎，定期随机移动并重绘棋盘。
 */
public class AnimatedBoardView extends View {

    private static final int GRID_SIZE = 4;
    private static final int TICK_INTERVAL = 2000; // 每 2 秒一步

    // 经典奶油主题配色
    private static final int GRID_BG = 0xFFC4B5A5;
    private static final int CELL_EMPTY = 0xFFFDF5E6;

    private final Game2048 game;
    private final Handler handler;
    private final Random random;

    private final Paint cellPaint;
    private final Paint tilePaint;
    private final Paint textPaint;

    private float cellSize;
    private float gap;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            tick();
            handler.postDelayed(this, TICK_INTERVAL);
        }
    };

    public AnimatedBoardView(Context context) {
        this(context, null);
    }

    public AnimatedBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        game = new Game2048();
        handler = new Handler(Looper.getMainLooper());
        random = new Random();

        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    /**
     * 开始自动演示。在 onAttachedToWindow 时启动，onDetachedFromWindow 时停止。
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.postDelayed(tickRunnable, 800);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(tickRunnable);
    }

    /**
     * 每一 tick：若棋局结束则重置，否则尝试随机方向移动。
     */
    private void tick() {
        if (game.isGameOver()) {
            game.newGame();
        }
        Game2048.Direction[] dirs = Game2048.Direction.values();
        // 尝试随机方向，最多试 4 次以确保尽量有移动
        for (int attempt = 0; attempt < 4; attempt++) {
            Game2048.Direction dir = dirs[random.nextInt(dirs.length)];
            Game2048.MoveResult result = game.move(dir);
            if (result.moved) break;
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float boardSize = Math.min(w, h);

        gap = boardSize * 0.04f;
        cellSize = (boardSize - gap * (GRID_SIZE + 1)) / GRID_SIZE;

        // 棋盘背景
        cellPaint.setColor(GRID_BG);
        cellPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(0, 0, boardSize, boardSize, gap * 3, gap * 3, cellPaint);

        // 绘制每个格子
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                float x = gap + c * (cellSize + gap);
                float y = gap + r * (cellSize + gap);
                RectF rect = new RectF(x, y, x + cellSize, y + cellSize);
                float corner = cellSize * 0.08f;

                int value = game.getCell(r, c);
                tilePaint.setColor(value == 0 ? CELL_EMPTY : getTileColor(value));
                canvas.drawRoundRect(rect, corner, corner, tilePaint);

                if (value > 0) {
                    textPaint.setColor(value <= 4 ? 0xFF776E65 : 0xFFFFFFFF);
                    float textSize = value < 100 ? cellSize * 0.45f :
                                     value < 1000 ? cellSize * 0.35f : cellSize * 0.28f;
                    textPaint.setTextSize(textSize);
                    Paint.FontMetrics fm = textPaint.getFontMetrics();
                    float textY = rect.centerY() - (fm.descent + fm.ascent) / 2f;
                    canvas.drawText(String.valueOf(value), rect.centerX(), textY, textPaint);
                }
            }
        }
    }

    private int getTileColor(int value) {
        switch (value) {
            case 2:    return 0xFFF2D08A;
            case 4:    return 0xFFF0B27A;
            case 8:    return 0xFFA0724E;
            case 16:   return 0xFFF5CBA7;
            case 32:   return 0xFFE8A87C;
            case 64:   return 0xFFE59866;
            case 128:  return 0xFFD35400;
            case 256:  return 0xFFF7DC6F;
            case 512:  return 0xFFF4D03F;
            case 1024: return 0xFFF1C40F;
            case 2048: return 0xFF2ECC71;
            default:   return 0xFF3C3A32;
        }
    }
}
