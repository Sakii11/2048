package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.example.myapplication.Game2048.Direction;

import java.util.ArrayList;
import java.util.List;

public class BoardView extends View {

    // ===== 回调接口 =====
    public interface BoardListener {
        void onSwipe(Direction direction);
        void onCellSelected(int row, int col);
        void onAnimationComplete();
    }

    private BoardListener listener;

    // ===== 布局常量 =====
    private static final float PADDING_DP = 8f;
    private static final float GAP_DP = 8f;
    private static final float CORNER_RADIUS_DP = 12f;
    private static final float BOARD_RADIUS_DP = 16f;

    // ===== 主题色 =====
    private int gridBgColor = 0xFFC4B5A5;
    private int cellBgColor = 0xFFFDF5E6;
    private int textWarmColor = 0xFF5D4E37;
    private int[] tileStartColors = {
            0xFFF2D08A, 0xFFF0B27A, 0xFFA0724E, 0xFFF5CBA7,
            0xFFE8A87C, 0xFFE59866, 0xFFD35400,
            0xFFF7DC6F, 0xFFF4D03F, 0xFFF1C40F,
            0xFF82E0AA, 0xFF2ECC71
    };
    private int[] tileEndColors = {
            0xFFD9B870, 0xFFD89860, 0xFF805030, 0xFFD8B090,
            0xFFD4956E, 0xFFD08050, 0xFFB84500,
            0xFFE8C860, 0xFFE0B830, 0xFFD4A800,
            0xFF6CC898, 0xFF24A85A
    };

    // ===== 布局计算 =====
    private float density;
    private float boardLeft, boardTop, boardRight, boardBottom;
    private float cellSize;
    private float gapPx;
    private float cornerRadius;
    private float boardRadius;

    // ===== 棋盘数据 =====
    private int[][] board = new int[4][4];

    // ===== Paint 缓存 =====
    private Paint boardBgPaint;
    private Paint cellBgPaint;
    private Paint[] tilePaints = new Paint[12];
    private Paint[] tileTextPaints = new Paint[12];
    private Paint mergeTextPaint;
    private RectF tempRect = new RectF();

    // ===== 动画状态 =====
    private static final int ANIM_IDLE = 0;
    private static final int ANIM_SLIDE = 1;
    private static final int ANIM_MERGE = 2;

    private int animPhase = ANIM_IDLE;
    private int animGen = 0; // 代际计数器，防止旧动画回调干扰
    private ValueAnimator slideAnimator;
    private ValueAnimator mergeAnimator;

    // 滑动动画数据
    private List<int[]> slideMovements = new ArrayList<>(); // {toRow, toCol, fromRow, fromCol}
    private float slideProgress = 0f;

    // 合成动画数据
    private List<int[]> mergeCells = new ArrayList<>(); // {row, col}
    private float mergeProgress = 0f;

    // ===== 选择模式 =====
    private int selectMode = 0; // 0=none, 1=hammer, 2=magic
    private int magicTargetValue = 0;
    private ValueAnimator blinkAnimator;
    private float blinkAlpha = 1f;

    // ===== 触摸 =====
    private float touchStartX, touchStartY;
    private static final float SWIPE_THRESHOLD_DP = 50f;

    public BoardView(Context context) {
        super(context);
        init(context);
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        gapPx = GAP_DP * density;
        cornerRadius = CORNER_RADIUS_DP * density;
        boardRadius = BOARD_RADIUS_DP * density;

        // 棋盘背景画笔
        boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 格子背景画笔
        cellBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 预创建 tile Paints（12 种方块色，索引 0-11 对应 2^1 ~ 2^12）
        for (int i = 0; i < 12; i++) {
            tilePaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            tileTextPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            tileTextPaints[i].setTextAlign(Paint.Align.CENTER);
            tileTextPaints[i].setTypeface(Typeface.DEFAULT_BOLD);
            tileTextPaints[i].setShadowLayer(2f * density, 0, 1f * density, 0x40000000);
        }
        mergeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mergeTextPaint.setTextAlign(Paint.Align.CENTER);
        mergeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mergeTextPaint.setShadowLayer(2f * density, 0, 1f * density, 0x40000000);

        // 闪烁动画：柔和呼吸效果
        blinkAnimator = ValueAnimator.ofFloat(1f, 0.55f);
        blinkAnimator.setDuration(900);
        blinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        blinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        blinkAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        blinkAnimator.addUpdateListener(a -> {
            blinkAlpha = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    // ===== 公开 API =====

    public void setListener(BoardListener l) {
        this.listener = l;
    }

    public void setBoardState(int[][] newBoard) {
        for (int r = 0; r < 4; r++) {
            System.arraycopy(newBoard[r], 0, board[r], 0, 4);
        }
        invalidate();
    }

    /**
     * 触发滑动 + 合成动画。
     * oldBoard: 移动前的棋盘快照
     * direction: 滑动方向
     * mergedAt: Game2048.move() 返回的合并位置标记
     */
    public void animateMove(int[][] oldBoard, Direction direction, int[][] mergedAt) {
        // 取消正在运行的动画，同时递增代际使旧回调失效
        cancelAnimations();
        final int gen = ++animGen;

        // 计算滑动映射
        slideMovements = calculateSlideMovements(oldBoard, direction, mergedAt);
        mergeCells.clear();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (mergedAt[r][c] != 0) {
                    mergeCells.add(new int[]{r, c});
                }
            }
        }

        // 阶段 1：滑动动画
        if (!slideMovements.isEmpty()) {
            animPhase = ANIM_SLIDE;
            slideProgress = 0f;
            slideAnimator = ValueAnimator.ofFloat(0f, 1f);
            slideAnimator.setDuration(180);
            slideAnimator.addUpdateListener(a -> {
                if (gen != animGen) return;
                slideProgress = (float) a.getAnimatedValue();
                invalidate();
            });
            slideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator a) {
                    if (gen != animGen) return;
                    slideProgress = 1f;
                    if (!mergeCells.isEmpty()) {
                        startMergeAnimation(gen);
                    } else {
                        finishAnimation(gen);
                    }
                    invalidate();
                }
            });
            slideAnimator.start();
        } else if (!mergeCells.isEmpty()) {
            startMergeAnimation(gen);
        } else {
            finishAnimation(gen);
        }
    }

    private void startMergeAnimation(final int gen) {
        animPhase = ANIM_MERGE;
        mergeProgress = 0f;
        mergeAnimator = ValueAnimator.ofFloat(0f, 1f);
        mergeAnimator.setDuration(200); // 80ms expand + 120ms contract
        mergeAnimator.addUpdateListener(a -> {
            if (gen != animGen) return;
            mergeProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        mergeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (gen != animGen) return;
                mergeProgress = 1f;
                finishAnimation(gen);
                invalidate();
            }
        });
        mergeAnimator.start();
    }

    private void finishAnimation(int gen) {
        if (gen != animGen) return;
        animPhase = ANIM_IDLE;
        slideMovements.clear();
        mergeCells.clear();
        if (listener != null) {
            listener.onAnimationComplete();
        }
    }

    private void cancelAnimations() {
        animGen++; // 先递增代际，使旧回调在 fire 时检查失败
        if (slideAnimator != null) {
            slideAnimator.cancel();
            slideAnimator = null;
        }
        if (mergeAnimator != null) {
            mergeAnimator.cancel();
            mergeAnimator = null;
        }
        animPhase = ANIM_IDLE;
        slideMovements.clear();
        mergeCells.clear();
        slideProgress = 0f;
        mergeProgress = 0f;
    }

    // ===== 主题 =====

    public void setThemeColors(int gridBg, int cellBg, int[] tileStarts, int[] tileEnds, int textWarm) {
        this.gridBgColor = gridBg;
        this.cellBgColor = cellBg;
        System.arraycopy(tileStarts, 0, tileStartColors, 0, Math.min(tileStarts.length, tileStartColors.length));
        System.arraycopy(tileEnds, 0, tileEndColors, 0, Math.min(tileEnds.length, tileEndColors.length));
        this.textWarmColor = textWarm;
        updatePaints();
        invalidate();
    }

    // ===== 选择模式 =====

    public void enterSelectMode(int mode, int targetValue) {
        this.selectMode = mode;
        this.magicTargetValue = targetValue;
        if (blinkAnimator != null && !blinkAnimator.isRunning()) {
            blinkAnimator.start();
        }
        invalidate();
    }

    public void exitSelectMode() {
        this.selectMode = 0;
        if (blinkAnimator != null) {
            blinkAnimator.cancel();
        }
        blinkAlpha = 1f;
        invalidate();
    }

    // ===== 测量与布局 =====

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padPx = PADDING_DP * density;
        float totalSize = Math.min(w, h);
        float available = totalSize - 2 * padPx - 3 * gapPx;
        cellSize = available / 4f;

        // 居中棋盘
        boardLeft = (w - totalSize) / 2f + padPx;
        boardTop = (h - totalSize) / 2f + padPx;
        boardRight = boardLeft + 4 * cellSize + 3 * gapPx;
        boardBottom = boardTop + 4 * cellSize + 3 * gapPx;

        updatePaints();
    }

    private void updatePaints() {
        boardBgPaint.setColor(gridBgColor);
        cellBgPaint.setColor(cellBgColor);

        for (int i = 0; i < 12; i++) {
            // 文本颜色：索引 0-5 (2-64) 白色，6+ (128+) 暖色
            int textColor = (i >= 6) ? textWarmColor : Color.WHITE;
            tileTextPaints[i].setColor(textColor);
        }
        mergeTextPaint.setColor(textWarmColor);
    }

    // ===== 绘制 =====

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. 棋盘背景
        tempRect.set(boardLeft - gapPx, boardTop - gapPx, boardRight + gapPx, boardBottom + gapPx);
        canvas.drawRoundRect(tempRect, boardRadius, boardRadius, boardBgPaint);

        // 2. 所有格子背景（始终绘制）
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                float left = boardLeft + c * (cellSize + gapPx);
                float top = boardTop + r * (cellSize + gapPx);
                tempRect.set(left, top, left + cellSize, top + cellSize);
                canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, cellBgPaint);
            }
        }

        // 3. 方块绘制
        float currentBlink = (selectMode != 0) ? blinkAlpha : 1f;

        // 构建滑动目标集合（动画期间用于跳过正常位置的绘制）
        boolean[][] isSliding = new boolean[4][4];
        if (animPhase == ANIM_SLIDE) {
            for (int[] m : slideMovements) {
                isSliding[m[0]][m[1]] = true;
            }
        }

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int value = board[r][c];
                if (value == 0) continue;

                // 滑动动画中的方块：移到插值位置绘制
                if (animPhase == ANIM_SLIDE && isSliding[r][c]) {
                    float[] from = cellCenter(slideSourceRow(r, c), slideSourceCol(r, c));
                    float[] to = cellCenter(r, c);
                    float cx = from[0] + (to[0] - from[0]) * slideProgress;
                    float cy = from[1] + (to[1] - from[1]) * slideProgress;
                    drawTileAt(canvas, value, cx, cy, 1f, currentBlink);
                } else {
                    float[] center = cellCenter(r, c);
                    float scale = 1f;
                    float alpha = 1f;

                    // 合成动画
                    if (animPhase == ANIM_MERGE && isMergeCell(r, c)) {
                        scale = computeMergeScale(mergeProgress);
                    }

                    // 选择模式闪烁
                    if (selectMode != 0) {
                        alpha = currentBlink;
                    }

                    drawTileAt(canvas, value, center[0], center[1], scale, alpha);
                }
            }
        }
    }

    // 在画布上以指定中心绘制一个方块
    private void drawTileAt(Canvas canvas, int value, float cx, float cy, float scale, float alpha) {
        int idx = tileColorIndex(value);
        if (idx >= 12) idx = 11;

        Paint tilePaint = tilePaints[idx];
        Paint textPaint = tileTextPaints[idx];

        // 动态创建渐变着色器（因方块位置可能随时变化）
        float halfSize = cellSize / 2f * scale;
        float left = cx - halfSize;
        float top = cy - halfSize;
        float right = cx + halfSize;
        float bottom = cy + halfSize;

        Shader gradient = new LinearGradient(
                left, top, right, bottom,
                tileStartColors[idx], tileEndColors[idx],
                Shader.TileMode.CLAMP);
        tilePaint.setShader(gradient);
        tilePaint.setAlpha((int) (255 * alpha));

        tempRect.set(left, top, right, bottom);
        canvas.drawRoundRect(tempRect, cornerRadius * scale, cornerRadius * scale, tilePaint);

        // 文字
        textPaint.setAlpha((int) (255 * alpha));
        // 字体大小随方块值和方块大小调整
        float textSize;
        if (value >= 1024) {
            textSize = 16f * density;
        } else if (value >= 128) {
            textSize = 18f * density;
        } else {
            textSize = 22f * density;
        }
        textPaint.setTextSize(textSize);

        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf(value), cx, textY, textPaint);
    }

    // 计算格子中心坐标
    private float[] cellCenter(int row, int col) {
        float x = boardLeft + col * (cellSize + gapPx) + cellSize / 2f;
        float y = boardTop + row * (cellSize + gapPx) + cellSize / 2f;
        return new float[]{x, y};
    }

    // 查找滑动方块的源位置
    private int slideSourceRow(int tr, int tc) {
        for (int[] m : slideMovements) {
            if (m[0] == tr && m[1] == tc) return m[2];
        }
        return tr;
    }

    private int slideSourceCol(int tr, int tc) {
        for (int[] m : slideMovements) {
            if (m[0] == tr && m[1] == tc) return m[3];
        }
        return tc;
    }

    private boolean isMergeCell(int r, int c) {
        for (int[] mc : mergeCells) {
            if (mc[0] == r && mc[1] == c) return true;
        }
        return false;
    }

    private float computeMergeScale(float progress) {
        // 0-0.4: 放大 1.0→1.15
        // 0.4-1.0: 缩回 1.0，带过冲
        if (progress < 0.4f) {
            return 1f + 0.15f * (progress / 0.4f);
        } else {
            float t = (progress - 0.4f) / 0.6f;
            // OvershootInterpolator(1.5f)
            t -= 1f;
            float overshoot = t * t * (2.5f * t + 1.5f) + 1f;
            return 1f + 0.15f * (1f - overshoot);
        }
    }

    private int tileColorIndex(int value) {
        int exp = 0;
        int v = value;
        while (v > 1) { v /= 2; exp++; }
        return Math.min(exp - 1, 11);
    }

    // ===== 触摸事件 =====

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                float threshold = SWIPE_THRESHOLD_DP * density;

                if (selectMode != 0) {
                    // 选择模式：轻触选格
                    if (Math.abs(dx) < threshold && Math.abs(dy) < threshold) {
                        int[] cell = findCellAt(event.getX(), event.getY());
                        if (cell != null && listener != null) {
                            listener.onCellSelected(cell[0], cell[1]);
                        }
                    }
                } else {
                    // 正常模式：滑动手势
                    if (Math.abs(dx) > Math.abs(dy)) {
                        if (Math.abs(dx) > threshold) {
                            if (listener != null) {
                                listener.onSwipe(dx > 0 ? Direction.RIGHT : Direction.LEFT);
                            }
                        }
                    } else {
                        if (Math.abs(dy) > threshold) {
                            if (listener != null) {
                                listener.onSwipe(dy > 0 ? Direction.DOWN : Direction.UP);
                            }
                        }
                    }
                }
                return true;

            default:
                return true;
        }
    }

    private int[] findCellAt(float x, float y) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                float left = boardLeft + c * (cellSize + gapPx);
                float top = boardTop + r * (cellSize + gapPx);
                if (x >= left && x <= left + cellSize && y >= top && y <= top + cellSize) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    // ===== 方块移动计算 =====

    private List<int[]> calculateSlideMovements(int[][] oldBoard, Direction direction, int[][] mergedAt) {
        List<int[]> movements = new ArrayList<>();

        switch (direction) {
            case LEFT:
                for (int r = 0; r < 4; r++) {
                    List<Integer> oldCols = new ArrayList<>();
                    for (int c = 0; c < 4; c++) {
                        if (oldBoard[r][c] != 0) oldCols.add(c);
                    }
                    int oldIdx = 0;
                    for (int c = 0; c < 4; c++) {
                        if (board[r][c] == 0) continue;
                        if (mergedAt[r][c] != 0) { oldIdx += 2; continue; }
                        if (oldIdx < oldCols.size()) {
                            int src = oldCols.get(oldIdx);
                            if (src != c) movements.add(new int[]{r, c, r, src});
                            oldIdx++;
                        }
                    }
                }
                break;
            case RIGHT:
                for (int r = 0; r < 4; r++) {
                    List<Integer> oldCols = new ArrayList<>();
                    for (int c = 3; c >= 0; c--) {
                        if (oldBoard[r][c] != 0) oldCols.add(c);
                    }
                    int oldIdx = 0;
                    for (int c = 3; c >= 0; c--) {
                        if (board[r][c] == 0) continue;
                        if (mergedAt[r][c] != 0) { oldIdx += 2; continue; }
                        if (oldIdx < oldCols.size()) {
                            int src = oldCols.get(oldIdx);
                            if (src != c) movements.add(new int[]{r, c, r, src});
                            oldIdx++;
                        }
                    }
                }
                break;
            case UP:
                for (int c = 0; c < 4; c++) {
                    List<Integer> oldRows = new ArrayList<>();
                    for (int r = 0; r < 4; r++) {
                        if (oldBoard[r][c] != 0) oldRows.add(r);
                    }
                    int oldIdx = 0;
                    for (int r = 0; r < 4; r++) {
                        if (board[r][c] == 0) continue;
                        if (mergedAt[r][c] != 0) { oldIdx += 2; continue; }
                        if (oldIdx < oldRows.size()) {
                            int src = oldRows.get(oldIdx);
                            if (src != r) movements.add(new int[]{r, c, src, c});
                            oldIdx++;
                        }
                    }
                }
                break;
            case DOWN:
                for (int c = 0; c < 4; c++) {
                    List<Integer> oldRows = new ArrayList<>();
                    for (int r = 3; r >= 0; r--) {
                        if (oldBoard[r][c] != 0) oldRows.add(r);
                    }
                    int oldIdx = 0;
                    for (int r = 3; r >= 0; r--) {
                        if (board[r][c] == 0) continue;
                        if (mergedAt[r][c] != 0) { oldIdx += 2; continue; }
                        if (oldIdx < oldRows.size()) {
                            int src = oldRows.get(oldIdx);
                            if (src != r) movements.add(new int[]{r, c, src, c});
                            oldIdx++;
                        }
                    }
                }
                break;
        }
        return movements;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAnimations();
        if (blinkAnimator != null) {
            blinkAnimator.cancel();
        }
    }
}
