package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Game2048.Direction;
import com.example.myapplication.Game2048.MoveResult;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "game2048_prefs";
    private static final String KEY_BEST = "best_score";

    private Game2048 game;
    private FrameLayout[][] cells;
    private TextView[][] tileTexts;
    private TextView tvScore;
    private TextView tvBest;
    private int bestScore;
    private View nextTileView;
    private TextView nextTileText;

    private float startX, startY;
    private static final int SWIPE_THRESHOLD = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置沉浸式状态栏
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.cream_gradient_start));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.cream_gradient_end));

        // 初始化 UI 引用
        tvScore = findViewById(R.id.tv_score);
        tvBest = findViewById(R.id.tv_best);

        // 加载最佳分数
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bestScore = prefs.getInt(KEY_BEST, 0);
        tvBest.setText(String.valueOf(bestScore));

        // 初始化游戏
        game = new Game2048();
        nextTileView = findViewById(R.id.next_tile_view);
        nextTileText = findViewById(R.id.next_tile_text);
        initCells();
        renderBoard();

        // 设置棋盘触摸监听
        FrameLayout boardContainer = findViewById(R.id.board_container);
        boardContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float dx = event.getX() - startX;
                        float dy = event.getY() - startY;
                        if (Math.abs(dx) > Math.abs(dy)) {
                            if (Math.abs(dx) > SWIPE_THRESHOLD) {
                                onSwipe(dx > 0 ? Direction.RIGHT : Direction.LEFT);
                            }
                        } else {
                            if (Math.abs(dy) > SWIPE_THRESHOLD) {
                                onSwipe(dy > 0 ? Direction.DOWN : Direction.UP);
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        // 按钮事件
        findViewById(R.id.btn_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (game.canUndo()) {
                    game.undo();
                    renderBoard();
                    updateScore();
                }
            }
        });

        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("2048")
                        .setMessage("重新开始游戏？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            game.newGame();
                            renderBoard();
                            updateScore();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        findViewById(R.id.btn_hammer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "锤子道具：移除一个方块", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_magic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "魔法道具：翻倍最大方块", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initCells() {
        cells = new FrameLayout[4][4];
        tileTexts = new TextView[4][4];

        int[][] ids = {
                {R.id.cell_00, R.id.cell_01, R.id.cell_02, R.id.cell_03},
                {R.id.cell_10, R.id.cell_11, R.id.cell_12, R.id.cell_13},
                {R.id.cell_20, R.id.cell_21, R.id.cell_22, R.id.cell_23},
                {R.id.cell_30, R.id.cell_31, R.id.cell_32, R.id.cell_33}
        };

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                cells[r][c] = findViewById(ids[r][c]);
                // 为每个格子创建 TextView
                TextView tv = new TextView(this);
                tv.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setTextSize(22);
                tv.setTextColor(ContextCompat.getColor(this, R.color.text_white));
                tv.setShadowLayer(2, 0, 1, 0x40000000);
                cells[r][c].addView(tv);
                tileTexts[r][c] = tv;
            }
        }
    }

    private void onSwipe(Direction direction) {
        MoveResult result = game.move(direction);
        if (result.moved) {
            renderBoard();
            updateScore();

            // 合并方块动画
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (result.mergedAt[r][c] != 0) {
                        animateMerge(cells[r][c]);
                    }
                }
            }

            // 新方块出现动画
            if (result.spawnedRow >= 0) {
                animateSpawn(cells[result.spawnedRow][result.spawnedCol]);
            }
            if (game.isWin()) {
                new AlertDialog.Builder(this)
                        .setTitle("恭喜！")
                        .setMessage("你达成了 2048！\n是否继续游戏？")
                        .setPositiveButton("继续", (dialog, which) -> {
                            game.move(direction); // 无效操作，仅为清除 win 状态
                        })
                        .setNegativeButton("重新开始", (dialog, which) -> {
                            game.newGame();
                            renderBoard();
                            updateScore();
                        })
                        .setCancelable(false)
                        .show();
            } else if (game.isGameOver()) {
                new AlertDialog.Builder(this)
                        .setTitle("游戏结束")
                        .setMessage("没有可移动的格子了！\n得分：" + game.getScore())
                        .setPositiveButton("重新开始", (dialog, which) -> {
                            game.newGame();
                            renderBoard();
                            updateScore();
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void renderBoard() {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int value = game.getCell(r, c);
                TextView tv = tileTexts[r][c];
                FrameLayout cell = cells[r][c];

                if (value == 0) {
                    tv.setText("");
                    tv.setVisibility(View.INVISIBLE);
                    cell.setBackgroundResource(R.drawable.bg_grid_cell);
                } else {
                    tv.setText(String.valueOf(value));
                    tv.setVisibility(View.VISIBLE);
                    cell.setBackgroundResource(getTileBackground(value));

                    // 调整字体大小
                    if (value >= 1024) {
                        tv.setTextSize(16);
                    } else if (value >= 128) {
                        tv.setTextSize(18);
                    } else {
                        tv.setTextSize(22);
                    }

                    // 大数字用深色文字
                    if (value >= 128) {
                        tv.setTextColor(ContextCompat.getColor(this, R.color.text_warm));
                    } else {
                        tv.setTextColor(ContextCompat.getColor(this, R.color.text_white));
                    }
                }
            }
        }
        updateNextTilePreview();
    }

    private int getTileBackground(int value) {
        switch (value) {
            case 2: return R.drawable.bg_preview_tile_yellow;
            case 4: return R.drawable.bg_preview_tile_orange;
            case 8: return R.drawable.bg_preview_tile_light_orange;
            case 16: return R.drawable.bg_tile_16;
            case 32: return R.drawable.bg_tile_32;
            case 64: return R.drawable.bg_tile_64;
            case 128: return R.drawable.bg_tile_128;
            case 256: return R.drawable.bg_tile_256;
            case 512: return R.drawable.bg_tile_512;
            case 1024: return R.drawable.bg_tile_1024;
            case 2048: return R.drawable.bg_tile_2048;
            default: return R.drawable.bg_tile_2048; // 超大值
        }
    }

    private void updateScore() {
        int score = game.getScore();
        tvScore.setText(String.valueOf(score));
        if (score > bestScore) {
            bestScore = score;
            tvBest.setText(String.valueOf(bestScore));
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putInt(KEY_BEST, bestScore).apply();
        }
    }

    private void updateNextTilePreview() {
        int next = game.getNextTile();
        nextTileView.setBackgroundResource(getTileBackground(next));
        nextTileText.setText(String.valueOf(next));
    }

    private void animateSpawn(View cell) {
        cell.setScaleX(0f);
        cell.setScaleY(0f);
        cell.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .setInterpolator(new OvershootInterpolator(3f))
                .start();
    }

    private void animateMerge(View cell) {
        cell.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(100)
                .withEndAction(() -> {
                    cell.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }
}