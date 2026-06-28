package com.example.myapplication;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
    private FrameLayout boardContainer;
    private TextView tvScore;
    private TextView tvBest;
    private int bestScore;
    private View nextTileView;
    private TextView nextTileText;

    private float startX, startY;
    private float selectStartX, selectStartY;
    private static final int SWIPE_THRESHOLD = 50;

    private MediaPlayer mediaPlayer;
    private MediaPlayer mergePlayer;
    private boolean isMusicPlaying = false;

    // 道具选择模式
    private enum SelectMode { NONE, HAMMER, MAGIC }
    private SelectMode currentSelectMode = SelectMode.NONE;
    private int magicTargetValue = 0;
    private TextView tvSelectHint;

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

        // 初始化背景音乐播放器，不在此处启动（等 onResume 时 AudioManager 就绪再播）
        mediaPlayer = MediaPlayer.create(this, R.raw.game_music);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
        }
        isMusicPlaying = true; // 默认开启，onResume 时根据此标志恢复播放

        // 初始化合成音效播放器（独立实例，与背景音乐互不干扰）
        mergePlayer = MediaPlayer.create(this, R.raw.merge_sound);
        if (mergePlayer != null) {
            mergePlayer.setVolume(0.6f, 0.6f);
        }

        // 初始化游戏
        game = new Game2048();
        nextTileView = findViewById(R.id.next_tile_view);
        nextTileText = findViewById(R.id.next_tile_text);
        tvSelectHint = findViewById(R.id.tv_select_hint);
        initCells();
        renderBoard();

        // 设置棋盘触摸监听
        boardContainer = findViewById(R.id.board_container);

        // 将棋盘容器告知自定义 BoardScrollView，使其在棋盘区域内永不拦截触摸事件
        BoardScrollView scrollView = (BoardScrollView) findViewById(R.id.scroll_view);
        scrollView.setBoardContainer(boardContainer);

        boardContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // === 道具选择模式：由触摸监听器统一处理方块点击，不再修改格子 clickable 状态 ===
                if (currentSelectMode != SelectMode.NONE) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            selectStartX = event.getX();
                            selectStartY = event.getY();
                            return true;
                        case MotionEvent.ACTION_UP: {
                            float dx2 = Math.abs(event.getX() - selectStartX);
                            float dy2 = Math.abs(event.getY() - selectStartY);
                            // 只有轻触（位移极小）才视为点击选格，滑动则忽略
                            if (dx2 < 30 && dy2 < 30) {
                                int[] cell = findCellFromTouch(event);
                                if (cell != null) {
                                    onCellSelected(cell[0], cell[1]);
                                }
                            }
                            return true;
                        }
                        default:
                            return true;
                    }
                }

                // === 正常模式：处理滑动手势 ===
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
                    default:
                        // 消费 ACTION_MOVE 等事件，防止冒泡回 ScrollView 触发滚动
                        return true;
                }
            }
        });

        // 按钮事件
        findViewById(R.id.btn_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (game.canUndo()) {
                    showCustomDialog("撤回", "确定要撤回上一步吗？",
                            "确定", "取消",
                            () -> {
                                game.undo();
                                renderBoard();
                                updateScore();
                            }, null);
                }
            }
        });

        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        findViewById(R.id.btn_hammer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSelectMode != SelectMode.NONE) return;
                showCustomDialog("锤子道具", "是否要使用道具消除一个方块？",
                        "确定", "取消",
                        () -> enterSelectMode(SelectMode.HAMMER, 0),
                        null);
            }
        });

        findViewById(R.id.btn_magic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSelectMode != SelectMode.NONE) return;
                showCustomDialog("魔法道具", "是否要使用道具变幻方块？",
                        "确定", "取消",
                        () -> showMagicValueDialog(),
                        null);
            }
        });
    }

    /**
     * 根据触摸事件的屏幕绝对坐标，找到被点击的格子。
     * 使用 getLocationOnScreen 避免坐标系转换问题。
     */
    private int[] findCellFromTouch(MotionEvent event) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int[] loc = new int[2];
                cells[r][c].getLocationOnScreen(loc);
                int left = loc[0];
                int top = loc[1];
                int right = left + cells[r][c].getWidth();
                int bottom = top + cells[r][c].getHeight();
                if (rawX >= left && rawX <= right && rawY >= top && rawY <= bottom) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
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
            boolean hasMerged = false;
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (result.mergedAt[r][c] != 0) {
                        animateMerge(cells[r][c]);
                        hasMerged = true;
                    }
                }
            }
            if (hasMerged) {
                playMergeSound();
            }

            // 新方块出现动画
            if (result.spawnedRow >= 0) {
                animateSpawn(cells[result.spawnedRow][result.spawnedCol]);
            }
            if (game.isWin()) {
                showCustomDialog("恭喜！", "你达成了 2048！\n是否继续游戏？",
                        "继续", "重新开始",
                        () -> { }, // 继续游戏
                        () -> {
                            exitSelectMode();
                            game.newGame();
                            renderBoard();
                            updateScore();
                        });
            } else if (game.isGameOver()) {
                showCustomDialog("游戏结束", "没有可移动的格子了！\n得分：" + game.getScore(),
                        "重新开始", null,
                        () -> {
                            exitSelectMode();
                            game.newGame();
                            renderBoard();
                            updateScore();
                        }, null);
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

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageButton btnVoice = view.findViewById(R.id.dialog_btn_voice);
        ImageButton btnRestart = view.findViewById(R.id.dialog_btn_restart);

        btnVoice.setOnClickListener(v -> {
            if (isMusicPlaying) {
                mediaPlayer.pause();
                btnVoice.setImageResource(R.drawable.ic_voice_close);
                Toast.makeText(MainActivity.this, "音乐已关闭", Toast.LENGTH_SHORT).show();
            } else {
                mediaPlayer.start();
                btnVoice.setImageResource(R.drawable.ic_voice);
                Toast.makeText(MainActivity.this, "音乐已开启", Toast.LENGTH_SHORT).show();
            }
            isMusicPlaying = !isMusicPlaying;
        });

        btnRestart.setOnClickListener(v -> {
            dialog.dismiss();
            showCustomDialog("2048", "重新开始游戏？",
                    "确定", "取消",
                    () -> {
                        exitSelectMode();
                        game.newGame();
                        renderBoard();
                        updateScore();
                    }, null);
        });

        dialog.show();
    }

    private void showCustomDialog(String title, String message,
            String positiveText, String negativeText,
            Runnable onPositive, Runnable onNegative) {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = view.findViewById(R.id.dialog_title);
        TextView tvMessage = view.findViewById(R.id.dialog_message);
        Button btnPositive = view.findViewById(R.id.dialog_btn_positive);
        Button btnNegative = view.findViewById(R.id.dialog_btn_negative);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPositive.setText(positiveText);

        if (negativeText != null) {
            btnNegative.setText(negativeText);
            btnNegative.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNegative != null) onNegative.run();
            });
        } else {
            btnNegative.setVisibility(View.GONE);
        }

        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            onPositive.run();
        });

        dialog.show();
    }

    // ===== 道具选择模式 =====

    private void enterSelectMode(SelectMode mode, int targetValue) {
        currentSelectMode = mode;
        magicTargetValue = targetValue;

        if (mode == SelectMode.HAMMER) {
            tvSelectHint.setText("🔨 请点击要消除的方块");
        } else {
            tvSelectHint.setText("✨ 请点击要变幻为 " + targetValue + " 的方块");
        }
        tvSelectHint.setVisibility(View.VISIBLE);
        // 格子始终由 boardContainer 的触摸监听器统一处理点击，不修改 clickable 状态
    }

    private void onCellSelected(int row, int col) {
        int value = game.getCell(row, col);
        if (value == 0) {
            Toast.makeText(this, "请选择一个有效的方块", Toast.LENGTH_SHORT).show();
            // 选中了无效方块也退出选择模式，防止状态卡住
            exitSelectMode();
            return;
        }

        boolean success = false;
        if (currentSelectMode == SelectMode.HAMMER) {
            success = game.removeTile(row, col);
        } else if (currentSelectMode == SelectMode.MAGIC) {
            success = game.transformTile(row, col, magicTargetValue);
        }

        if (success) {
            SelectMode completedMode = currentSelectMode;
            exitSelectMode();
            renderBoard();
            updateScore();
            Toast.makeText(this,
                    completedMode == SelectMode.HAMMER ? "方块已消除" : "方块已变幻",
                    Toast.LENGTH_SHORT).show();
        } else {
            // 操作失败也退出选择模式，防止状态卡住
            exitSelectMode();
            Toast.makeText(this, "操作失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void exitSelectMode() {
        tvSelectHint.setVisibility(View.GONE);
        currentSelectMode = SelectMode.NONE;
        // 格子从未改变 clickable 状态，无需清理
    }

    private void showMagicValueDialog() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_magic_select, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        int[] values = {2, 4, 8, 16, 32};
        int[] btnIds = {R.id.magic_btn_2, R.id.magic_btn_4, R.id.magic_btn_8,
                R.id.magic_btn_16, R.id.magic_btn_32};

        for (int i = 0; i < values.length; i++) {
            final int val = values[i];
            view.findViewById(btnIds[i]).setOnClickListener(v -> {
                dialog.dismiss();
                enterSelectMode(SelectMode.MAGIC, val);
            });
        }

        view.findViewById(R.id.magic_btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void playMergeSound() {
        if (mergePlayer != null) {
            mergePlayer.seekTo(0);
            mergePlayer.start();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentSelectMode != SelectMode.NONE) {
            exitSelectMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到前台时恢复背景音乐（仅当用户未手动关闭时）
        if (isMusicPlaying && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 切到后台时暂停音乐，回来时 onResume 自动恢复
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (currentSelectMode != SelectMode.NONE) {
            exitSelectMode();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mergePlayer != null) {
            mergePlayer.release();
            mergePlayer = null;
        }
    }
}