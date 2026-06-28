package com.example.myapplication;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.example.myapplication.Game2048.Direction;
import com.example.myapplication.Game2048.MoveResult;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "game2048_prefs";
    private static final String KEY_BEST = "best_score";
    private static final String KEY_THEME = "app_theme";
    private static final int REQUEST_WRITE_STORAGE = 100;

    private Game2048 game;
    private FrameLayout[][] cells;
    private TextView[][] tileTexts;
    private FrameLayout boardContainer;
    private FrameLayout boardFrame;
    private FrameLayout scorePanelCurrent;
    private FrameLayout scorePanelBest;
    private TextView tvScore;
    private TextView tvBest;
    private TextView tvScoreLabel;
    private TextView tvBestLabel;
    private int bestScore;
    private View nextTileView;
    private TextView nextTileText;
    private Bitmap capturedBitmap;

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

    // 闪烁效果
    private Handler blinkHandler;
    private boolean blinkPhase = false;

    // 主题风格
    private enum Theme {
        CLASSIC("经典奶油", 0xFFF5E6D3, 0xFFE8D5B7,
                0xFFC4B5A5, 0xFFFDF5E6, 0xFFE8DED1, 0xFF5D4E37, 0xFF8B7D6B),
        DARK("暗夜模式", 0xFF2D2D3F, 0xFF1A1A2E,
                0xFF1A1A2E, 0xFF252540, 0xFF1E1E30, 0xFFCCCCCC, 0xFF888888),
        OCEAN("海洋微风", 0xFFB8D4E3, 0xFF7BA7BC,
                0xFF6C8D9A, 0xFFE8F2F5, 0xFFD0E4ED, 0xFF3D5A6B, 0xFF5C7D8A),
        SAKURA("樱花物语", 0xFFFFD1DC, 0xFFFFB7C5,
                0xFFC4959D, 0xFFFFF0F3, 0xFFFFE4EA, 0xFF7D5A60, 0xFF9B7D82),
        FOREST("森林绿意", 0xFFC8E6C9, 0xFF81C784,
                0xFF8DA892, 0xFFF0F5F1, 0xFFD8E9DA, 0xFF4A6B50, 0xFF6B8A70),
        SUNSET("日落暖橙", 0xFFFFCCBC, 0xFFFF8A65,
                0xFFC49A7D, 0xFFFFF5F0, 0xFFFFEBE0, 0xFF7D5038, 0xFF9B6D55);

        final String label;
        final int colorStart;   // 主背景渐变起始
        final int colorEnd;     // 主背景渐变结束
        final int gridBg;       // 棋盘底色
        final int gridCell;     // 空格子底色
        final int scorePanelBg; // 分数面板底色
        final int textWarm;     // 暖色文字（标签/方块数字）
        final int textLabel;    // 灰色文字（"分数""最佳"标签）

        Theme(String label, int colorStart, int colorEnd,
              int gridBg, int gridCell, int scorePanelBg, int textWarm, int textLabel) {
            this.label = label;
            this.colorStart = colorStart;
            this.colorEnd = colorEnd;
            this.gridBg = gridBg;
            this.gridCell = gridCell;
            this.scorePanelBg = scorePanelBg;
            this.textWarm = textWarm;
            this.textLabel = textLabel;
        }

        /** 经典方块配色（CLASSIC / OCEAN / SAKURA / FOREST / SUNSET 共用） */
        static final int[] CLASSIC_TILE_START = {
                0xFFF2D08A, 0xFFF0B27A, 0xFFA0724E, 0xFFF5CBA7,
                0xFFE8A87C, 0xFFE59866, 0xFFD35400,
                0xFFF7DC6F, 0xFFF4D03F, 0xFFF1C40F,
                0xFF82E0AA, 0xFF2ECC71
        };
        static final int[] CLASSIC_TILE_END = {
                0xFFD9B870, 0xFFD89860, 0xFF805030, 0xFFD8B090,
                0xFFD4956E, 0xFFD08050, 0xFFB84500,
                0xFFE8C860, 0xFFE0B830, 0xFFD4A800,
                0xFF6CC898, 0xFF24A85A
        };

        /** 暗夜方块配色 */
        static final int[] DARK_TILE_START = {
                0xFFC9A85C, 0xFFC88050, 0xFF805030, 0xFFD0A060,
                0xFFC07040, 0xFFA04000, 0xFF882200,
                0xFFD4B850, 0xFFCCB030, 0xFFCCA000,
                0xFF60C080, 0xFF20A040
        };
        static final int[] DARK_TILE_END = {
                0xFFB09040, 0xFFB06838, 0xFF603820, 0xFFB88848,
                0xFFA85830, 0xFF882800, 0xFF6E1800,
                0xFFC0A040, 0xFFB89820, 0xFFB08800,
                0xFF48A868, 0xFF18882E
        };

        public int[] getTileStartColors() {
            return this == DARK ? DARK_TILE_START : CLASSIC_TILE_START;
        }
        public int[] getTileEndColors() {
            return this == DARK ? DARK_TILE_END : CLASSIC_TILE_END;
        }
    }
    private Theme currentTheme = Theme.CLASSIC;
    private BoardScrollView scrollView;
    private String gameMode = "free";  // 游戏模式："level" 关卡模式 / "free" 自由模式
    private int levelTarget = 0;       // 关卡目标值（0 表示自由模式）
    private int levelNumber = 0;       // 关卡编号（1-6）

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
        tvScoreLabel = findViewById(R.id.tv_score_label);
        tvBestLabel = findViewById(R.id.tv_best_label);
        boardFrame = findViewById(R.id.board_frame);
        scorePanelCurrent = findViewById(R.id.score_panel_current);
        scorePanelBest = findViewById(R.id.score_panel_best);

        // 加载最佳分数和主题
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bestScore = prefs.getInt(KEY_BEST, 0);
        tvBest.setText(String.valueOf(bestScore));
        String savedTheme = prefs.getString(KEY_THEME, "CLASSIC");
        try {
            currentTheme = Theme.valueOf(savedTheme);
        } catch (IllegalArgumentException e) {
            currentTheme = Theme.CLASSIC;
        }

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

        // 读取从登录界面传入的游戏模式
        if (getIntent().hasExtra(LoginActivity.EXTRA_GAME_MODE)) {
            gameMode = getIntent().getStringExtra(LoginActivity.EXTRA_GAME_MODE);
        }
        levelTarget = getIntent().getIntExtra("level_target", 0);
        levelNumber = getIntent().getIntExtra("level_number", 0);

        blinkHandler = new Handler(Looper.getMainLooper());

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
        scrollView = (BoardScrollView) findViewById(R.id.scroll_view);
        scrollView.setBoardContainer(boardContainer);
        applyTheme(currentTheme);

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
                if (levelTarget > 0) {
                    Toast.makeText(MainActivity.this, "关卡模式不允许使用道具", Toast.LENGTH_SHORT).show();
                    return;
                }
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
                if (levelTarget > 0) {
                    Toast.makeText(MainActivity.this, "关卡模式不允许使用道具", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentSelectMode != SelectMode.NONE) return;
                showCustomDialog("魔法道具", "是否要使用道具变幻方块？",
                        "确定", "取消",
                        () -> showMagicValueDialog(),
                        null);
            }
        });

        // 截图上传按钮
        findViewById(R.id.btn_upload).setOnClickListener(v -> {
            Bitmap screenshot = captureScreenshot();
            if (screenshot != null) {
                capturedBitmap = screenshot;
                showSaveScreenshotDialog();
            } else {
                Toast.makeText(MainActivity.this, "截图失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });

        // 服装风格切换按钮
        findViewById(R.id.btn_clothing).setOnClickListener(v -> showThemeDialog());
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
            // 关卡模式：检查是否已达成通关目标
            if (levelTarget > 0) {
                boolean reached = false;
                for (int r = 0; r < 4; r++) {
                    for (int c = 0; c < 4; c++) {
                        if (game.getCell(r, c) >= levelTarget) {
                            reached = true;
                            break;
                        }
                    }
                }
                if (reached) {
                    showCustomDialog("恭喜通关！",
                            "你成功合成了 " + levelTarget + "！\n第 " + levelNumber + " 关完成！",
                            "返回选关", null,
                            () -> finish(),
                            null);
                    return;
                }
            } else {
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
                }
            }

            if (game.isGameOver()) {
                if (levelTarget > 0) {
                    showCustomDialog("关卡挑战失败",
                            "没有可移动的格子了！\n目标：" + levelTarget + "  得分：" + game.getScore(),
                            "重试", "返回选关",
                            () -> {
                                exitSelectMode();
                                game.newGame();
                                renderBoard();
                                updateScore();
                            },
                            () -> finish());
                } else {
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
                    cell.setBackground(createGridCellBackground());
                } else {
                    tv.setText(String.valueOf(value));
                    tv.setVisibility(View.VISIBLE);
                    cell.setBackground(getTileDrawable(value));

                    // 调整字体大小
                    if (value >= 1024) {
                        tv.setTextSize(16);
                    } else if (value >= 128) {
                        tv.setTextSize(18);
                    } else {
                        tv.setTextSize(22);
                    }

                    // 方块数字颜色：浅色方块用白色字，大数字用暖色字
                    if (value >= 128) {
                        tv.setTextColor(currentTheme.textWarm);
                    } else {
                        tv.setTextColor(Color.WHITE);
                    }
                }
            }
        }
        updateNextTilePreview();
    }

    private int getTileColorIndex(int value) {
        int exp = 0;
        int v = value;
        while (v > 1) { v /= 2; exp++; }
        return Math.min(exp - 1, 10);
    }

    private GradientDrawable getTileDrawable(int value) {
        int idx = getTileColorIndex(value);
        int[] startColors = currentTheme.getTileStartColors();
        int[] endColors = currentTheme.getTileEndColors();
        int start = startColors[Math.min(idx, startColors.length - 1)];
        int end = endColors[Math.min(idx, endColors.length - 1)];
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        gd.setCornerRadius(dpToPx(12));
        return gd;
    }

    private GradientDrawable createGridCellBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(currentTheme.gridCell);
        gd.setCornerRadius(dpToPx(12));
        return gd;
    }

    private GradientDrawable createBoardBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(currentTheme.gridBg);
        gd.setCornerRadius(dpToPx(16));
        return gd;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
        nextTileView.setBackground(getTileDrawable(next));
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
        startBlinking();
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
        stopBlinking();
    }

    private void startBlinking() {
        blinkPhase = false;
        doBlinkStep();
    }

    private void doBlinkStep() {
        if (currentSelectMode == SelectMode.NONE) {
            // 恢复所有格子透明度
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    cells[r][c].setAlpha(1f);
                }
            }
            return;
        }
        float alpha = blinkPhase ? 1f : 0.3f;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (game.getCell(r, c) > 0) {
                    cells[r][c].setAlpha(alpha);
                }
            }
        }
        blinkPhase = !blinkPhase;
        blinkHandler.postDelayed(this::doBlinkStep, 400);
    }

    private void stopBlinking() {
        blinkHandler.removeCallbacksAndMessages(null);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                cells[r][c].setAlpha(1f);
            }
        }
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

    // ===== 截图功能 =====

    /**
     * 捕获当前界面截图，返回 Bitmap。
     * 通过 DecorView.draw 渲染到 Canvas 上，包含状态栏在内。
     */
    private Bitmap captureScreenshot() {
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(
                rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        rootView.draw(canvas);
        rootView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    /**
     * 将截图保存到系统相册。
     * Android 10+ 使用 MediaStore API，无需存储权限；
     * 旧版本先检查 WRITE_EXTERNAL_STORAGE 权限。
     */
    private void saveScreenshotToGallery(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+：通过 MediaStore 保存
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "2048_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/2048");

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = resolver.openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    Toast.makeText(this, "截图已保存到相册", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // Android 9 及以下：写入外部存储
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE);
                return;
            }
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "2048");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "2048_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                // 通知相册刷新
                android.media.MediaScannerConnection.scanFile(this,
                        new String[]{file.getAbsolutePath()}, null, null);
                Toast.makeText(this, "截图已保存到相册", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 弹出对话框让用户选择是否保存截图。
     */
    private void showSaveScreenshotDialog() {
        showCustomDialog("截图", "截图已生成，是否保存到相册？",
                "保存", "取消",
                () -> {
                    if (capturedBitmap != null) {
                        saveScreenshotToGallery(capturedBitmap);
                    }
                    capturedBitmap = null;
                },
                () -> {
                    capturedBitmap = null;
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，重试保存
                if (capturedBitmap != null) {
                    saveScreenshotToGallery(capturedBitmap);
                }
            } else {
                Toast.makeText(this, "需要存储权限才能保存截图", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===== 主题风格切换 =====

    /**
     * 弹出主题选择对话框，列出所有可用风格。
     */
    private void showThemeDialog() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_theme_select, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        // 在 show() 之后强制设置窗口宽度，避免 Dialog 默认窄宽截断内容
        int widthPx = Math.round(320 * getResources().getDisplayMetrics().density);
        dialog.getWindow().setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT);

        // 各主题行与对应的 View ID 及 check 标记 ID
        int[][] themeRows = {
                {R.id.theme_classic, R.id.theme_check_classic},
                {R.id.theme_dark, R.id.theme_check_dark},
                {R.id.theme_ocean, R.id.theme_check_ocean},
                {R.id.theme_sakura, R.id.theme_check_sakura},
                {R.id.theme_forest, R.id.theme_check_forest},
                {R.id.theme_sunset, R.id.theme_check_sunset},
        };
        Theme[] themes = Theme.values();

        // 初始化：当前主题显示 ✓，其余隐藏
        for (int i = 0; i < themes.length; i++) {
            View check = view.findViewById(themeRows[i][1]);
            check.setVisibility(themes[i] == currentTheme ? View.VISIBLE : View.GONE);
        }

        // 设置各主题行的点击事件
        for (int i = 0; i < themes.length; i++) {
            final Theme theme = themes[i];
            view.findViewById(themeRows[i][0]).setOnClickListener(v -> {
                applyTheme(theme);
                // 保存到 SharedPreferences
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putString(KEY_THEME, theme.name()).apply();
                dialog.dismiss();
            });
        }
    }

    /**
     * 应用主题：更新主背景、棋盘底色、格子底色、分数面板和文字颜色，
     * 然后刷新棋盘渲染。
     */
    private void applyTheme(Theme theme) {
        currentTheme = theme;
        if (scrollView == null) return;

        // 主背景渐变
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{theme.colorStart, theme.colorEnd});
        scrollView.setBackground(gd);

        // 棋盘底色
        if (boardFrame != null) {
            boardFrame.setBackground(createBoardBackground());
        }

        // 分数面板背景
        GradientDrawable panelBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{theme.scorePanelBg, theme.scorePanelBg});
        panelBg.setCornerRadius(dpToPx(16));
        if (scorePanelCurrent != null) scorePanelCurrent.setBackground(panelBg);
        if (scorePanelBest != null) {
            GradientDrawable panelBg2 = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{theme.scorePanelBg, theme.scorePanelBg});
            panelBg2.setCornerRadius(dpToPx(16));
            scorePanelBest.setBackground(panelBg2);
        }

        // 文字颜色
        if (tvScore != null) tvScore.setTextColor(theme.textWarm);
        if (tvBest != null) tvBest.setTextColor(theme.textWarm);
        if (tvScoreLabel != null) tvScoreLabel.setTextColor(theme.textLabel);
        if (tvBestLabel != null) tvBestLabel.setTextColor(theme.textLabel);

        // 状态栏 / 导航栏
        Window window = getWindow();
        window.setStatusBarColor(darkenColor(theme.colorStart, 0.85f));
        window.setNavigationBarColor(darkenColor(theme.colorEnd, 0.85f));

        // 重新渲染棋盘（格子底色和方块色随主题变化）
        renderBoard();
    }

    /**
     * 将颜色按比例加深，用于状态栏/导航栏。
     */
    private int darkenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a, r, g, b);
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
        stopBlinking();
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