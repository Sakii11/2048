package com.example.myapplication;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
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

    private static final int REQUEST_WRITE_STORAGE = 100;

    private AppDatabaseHelper dbHelper;
    private Game2048 game;
    private BoardView boardView;
    private BoardScrollView scrollView;
    private StarsView starsView;
    private TextView tvScore;
    private TextView tvBest;
    private TextView tvScoreLabel;
    private TextView tvBestLabel;
    private FrameLayout scorePanelCurrent;
    private FrameLayout scorePanelBest;
    private int bestScore;
    private View nextTileView;
    private TextView nextTileText;
    private Bitmap capturedBitmap;

    private MediaPlayer mediaPlayer;
    private MediaPlayer mergePlayer;
    private AudioManager audioManager;
    private boolean isMusicPlaying = false;

    // 道具选择模式
    private enum SelectMode { NONE, HAMMER, MAGIC }
    private SelectMode currentSelectMode = SelectMode.NONE;
    private int magicTargetValue = 0;
    private TextView tvSelectHint;

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
        final int colorStart;
        final int colorEnd;
        final int gridBg;
        final int gridCell;
        final int scorePanelBg;
        final int textWarm;
        final int textLabel;

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
    private String gameMode = "free";
    private int levelTarget = 0;
    private int levelNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 沉浸式状态栏
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.cream_gradient_start));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.cream_gradient_end));

        // UI 引用
        tvScore = findViewById(R.id.tv_score);
        tvBest = findViewById(R.id.tv_best);
        tvScoreLabel = findViewById(R.id.tv_score_label);
        tvBestLabel = findViewById(R.id.tv_best_label);
        scorePanelCurrent = findViewById(R.id.score_panel_current);
        scorePanelBest = findViewById(R.id.score_panel_best);
        nextTileView = findViewById(R.id.next_tile_view);
        nextTileText = findViewById(R.id.next_tile_text);
        tvSelectHint = findViewById(R.id.tv_select_hint);
        boardView = findViewById(R.id.board_view);

        dbHelper = new AppDatabaseHelper(this);

        bestScore = dbHelper.getBestScore();
        tvBest.setText(String.valueOf(bestScore));
        String savedTheme = dbHelper.getAppTheme();
        try {
            currentTheme = Theme.valueOf(savedTheme);
        } catch (IllegalArgumentException e) {
            currentTheme = Theme.CLASSIC;
        }

        // AudioManager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // 背景音乐
        mediaPlayer = MediaPlayer.create(this, R.raw.game_music);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0.5f, 0.5f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                mediaPlayer.setAudioAttributes(attrs);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
        }
        isMusicPlaying = true;

        // 合成音效
        mergePlayer = MediaPlayer.create(this, R.raw.merge_sound);
        if (mergePlayer != null) {
            mergePlayer.setVolume(0.4f, 0.4f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes sfxAttrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                mergePlayer.setAudioAttributes(sfxAttrs);
            } else {
                mergePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
        }

        // 游戏模式
        if (getIntent().hasExtra(LoginActivity.EXTRA_GAME_MODE)) {
            gameMode = getIntent().getStringExtra(LoginActivity.EXTRA_GAME_MODE);
        }
        levelTarget = getIntent().getIntExtra("level_target", 0);
        levelNumber = getIntent().getIntExtra("level_number", 0);

        // 初始化游戏
        game = new Game2048();
        scrollView = (BoardScrollView) findViewById(R.id.scroll_view);
        starsView = findViewById(R.id.stars_view);
        scrollView.setBoardContainer(findViewById(R.id.board_container));

        // 设置 BoardView
        boardView.setListener(new BoardView.BoardListener() {
            @Override
            public void onSwipe(Direction direction) {
                handleSwipe(direction);
            }

            @Override
            public void onCellSelected(int row, int col) {
                onCellSelectedByBoard(row, col);
            }

            @Override
            public void onAnimationComplete() {
                // 动画完成后显示新生成的方块
                boardView.setBoardState(game.getBoard());
                updateNextTilePreview();
                checkGameState();
            }
        });
        boardView.setBoardState(game.getBoard());
        applyTheme(currentTheme);

        // 按钮事件
        findViewById(R.id.btn_undo).setOnClickListener(v -> {
            if (game.canUndo()) {
                showCustomDialog("撤回", "确定要撤回上一步吗？",
                        "确定", "取消",
                        () -> {
                            game.undo();
                            boardView.setBoardState(game.getBoard());
                            updateScore();
                            updateNextTilePreview();
                        }, null);
            }
        });

        findViewById(R.id.btn_settings).setOnClickListener(v -> showSettingsDialog());

        findViewById(R.id.btn_hammer).setOnClickListener(v -> {
            if (levelTarget > 0) {
                Toast.makeText(MainActivity.this, "关卡模式不允许使用道具", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentSelectMode != SelectMode.NONE) return;
            showCustomDialog("锤子道具", "是否要使用道具消除一个方块？",
                    "确定", "取消",
                    () -> enterSelectMode(SelectMode.HAMMER, 0),
                    null);
        });

        findViewById(R.id.btn_magic).setOnClickListener(v -> {
            if (levelTarget > 0) {
                Toast.makeText(MainActivity.this, "关卡模式不允许使用道具", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentSelectMode != SelectMode.NONE) return;
            showCustomDialog("魔法道具", "是否要使用道具变幻方块？",
                    "确定", "取消",
                    () -> showMagicValueDialog(),
                    null);
        });

        findViewById(R.id.btn_upload).setOnClickListener(v -> {
            Bitmap screenshot = captureScreenshot();
            if (screenshot != null) {
                capturedBitmap = screenshot;
                showSaveScreenshotDialog();
            } else {
                Toast.makeText(MainActivity.this, "截图失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_clothing).setOnClickListener(v -> showThemeDialog());
    }

    // ===== 滑动处理 =====

    private void handleSwipe(Direction direction) {
        int[][] oldBoard = game.getBoard();
        MoveResult result = game.move(direction);
        if (!result.moved) return;

        // 构建不含新生成方块的棋盘，让动画在移动/合成完成后的干净棋盘上播放
        int[][] displayBoard = game.getBoard();
        if (result.spawnedRow >= 0 && result.spawnedCol >= 0) {
            displayBoard[result.spawnedRow][result.spawnedCol] = 0;
        }
        boardView.setBoardState(displayBoard);
        updateScore();
        updateNextTilePreview();

        // 检查是否有合成（用于音效）
        boolean hasMerged = false;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (result.mergedAt[r][c] != 0) {
                    hasMerged = true;
                    break;
                }
            }
        }
        if (hasMerged) playMergeSound();

        boardView.animateMove(oldBoard, direction, result.mergedAt);
    }

    // ===== 游戏状态检查 =====

    private void checkGameState() {
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
                        () -> {},
                        () -> {
                            exitSelectMode();
                            game.newGame();
                            boardView.setBoardState(game.getBoard());
                            updateScore();
                            updateNextTilePreview();
                        });
                return;
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
                            boardView.setBoardState(game.getBoard());
                            updateScore();
                            updateNextTilePreview();
                        },
                        () -> finish());
            } else {
                showCustomDialog("游戏结束", "没有可移动的格子了！\n得分：" + game.getScore(),
                        "重新开始", null,
                        () -> {
                            exitSelectMode();
                            game.newGame();
                            boardView.setBoardState(game.getBoard());
                            updateScore();
                            updateNextTilePreview();
                        }, null);
            }
        }
    }

    // ===== 分数 =====

    private void updateScore() {
        int score = game.getScore();
        tvScore.setText(String.valueOf(score));
        if (score > bestScore) {
            bestScore = score;
            tvBest.setText(String.valueOf(bestScore));
            dbHelper.setBestScore(bestScore);
        }
    }

    private void updateNextTilePreview() {
        int next = game.getNextTile();
        nextTileView.setBackground(getTileDrawable(next));
        nextTileText.setText(String.valueOf(next));
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
        boardView.enterSelectMode(
                mode == SelectMode.HAMMER ? 1 : 2,
                targetValue);
    }

    private void onCellSelectedByBoard(int row, int col) {
        int value = game.getCell(row, col);
        if (value == 0) {
            Toast.makeText(this, "请选择一个有效的方块", Toast.LENGTH_SHORT).show();
            exitSelectMode();
            return;
        }

        boolean success = false;
        if (currentSelectMode == SelectMode.HAMMER) {
            success = game.removeTile(row, col);
        } else if (currentSelectMode == SelectMode.MAGIC) {
            success = game.transformTile(row, col, magicTargetValue);
        }

        SelectMode completedMode = currentSelectMode;
        exitSelectMode();

        if (success) {
            boardView.setBoardState(game.getBoard());
            updateScore();
            updateNextTilePreview();
            Toast.makeText(this,
                    completedMode == SelectMode.HAMMER ? "方块已消除" : "方块已变幻",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "操作失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void exitSelectMode() {
        tvSelectHint.setVisibility(View.GONE);
        currentSelectMode = SelectMode.NONE;
        boardView.exitSelectMode();
    }

    // ===== 主题 =====

    private void applyTheme(Theme theme) {
        currentTheme = theme;
        if (scrollView == null) return;

        // 更新星空颜色和渐变背景（由 StarsView 统一绘制）
        if (starsView != null) {
            starsView.setTheme(theme.ordinal());
        }

        // 分数面板
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

        // 状态栏
        Window window = getWindow();
        window.setStatusBarColor(darkenColor(theme.colorStart, 0.85f));
        window.setNavigationBarColor(darkenColor(theme.colorEnd, 0.85f));

        // BoardView 主题
        boardView.setThemeColors(theme.gridBg, theme.gridCell,
                theme.getTileStartColors(), theme.getTileEndColors(), theme.textWarm);
        boardView.setBoardState(game.getBoard());
        updateNextTilePreview();
    }

    private void showThemeDialog() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_theme_select, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // ScrollView 内层 LinearLayout 背景随主题
        ScrollView scrollView = (ScrollView) view;
        android.view.ViewGroup innerLayout = (android.view.ViewGroup) scrollView.getChildAt(0);
        GradientDrawable itemBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{currentTheme.scorePanelBg, currentTheme.gridCell});
        itemBg.setCornerRadius(dpToPx(16));
        innerLayout.setBackground(itemBg);

        // 标题颜色随主题
        TextView themeTitle = (TextView) innerLayout.getChildAt(0);
        themeTitle.setTextColor(currentTheme.textWarm);

        dialog.show();
        int widthPx = Math.round(320 * getResources().getDisplayMetrics().density);
        dialog.getWindow().setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT);

        int[][] themeRows = {
                {R.id.theme_classic, R.id.theme_check_classic},
                {R.id.theme_dark, R.id.theme_check_dark},
                {R.id.theme_ocean, R.id.theme_check_ocean},
                {R.id.theme_sakura, R.id.theme_check_sakura},
                {R.id.theme_forest, R.id.theme_check_forest},
                {R.id.theme_sunset, R.id.theme_check_sunset},
        };
        Theme[] themes = Theme.values();

        for (int i = 0; i < themes.length; i++) {
            View check = view.findViewById(themeRows[i][1]);
            check.setVisibility(themes[i] == currentTheme ? View.VISIBLE : View.GONE);
        }

        for (int i = 0; i < themes.length; i++) {
            final Theme theme = themes[i];
            // 主题项背景随主题
            View itemView = view.findViewById(themeRows[i][0]);
            GradientDrawable themeItemBg = new GradientDrawable();
            themeItemBg.setColor(currentTheme.gridCell);
            themeItemBg.setCornerRadius(dpToPx(12));
            themeItemBg.setStroke(dpToPx(1), currentTheme.textWarm);
            itemView.setBackground(themeItemBg);

            // 主题名文字颜色
            android.view.ViewGroup rowLayout = (android.view.ViewGroup) itemView;
            TextView nameText = (TextView) rowLayout.getChildAt(1);
            nameText.setTextColor(currentTheme.textWarm);

            itemView.setOnClickListener(v -> {
                applyTheme(theme);
                dbHelper.setAppTheme(theme.name());
                dialog.dismiss();
            });
        }
    }

    private int darkenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a, r, g, b);
    }

    // ===== 方块绘制（仅用于预览） =====

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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ===== 音效 =====

    private void playMergeSound() {
        if (mergePlayer != null) {
            mergePlayer.seekTo(0);
            mergePlayer.start();
        }
    }

    // ===== 对话框 =====

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 弹窗背景随主题
        GradientDrawable dialogBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{currentTheme.scorePanelBg, currentTheme.gridCell});
        dialogBg.setCornerRadius(dpToPx(20));
        view.setBackground(dialogBg);

        // 标题颜色随主题
        TextView tvTitle = view.findViewById(R.id.dialog_title);
        tvTitle.setTextColor(currentTheme.textWarm);

        ImageButton btnVoice = view.findViewById(R.id.dialog_btn_voice);
        ImageButton btnRestart = view.findViewById(R.id.dialog_btn_restart);

        // 图标按钮背景随主题
        GradientDrawable iconBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{currentTheme.gridCell, currentTheme.gridBg});
        iconBg.setCornerRadius(dpToPx(12));
        btnVoice.setBackground(iconBg);
        btnRestart.setBackground(iconBg);

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
                        boardView.setBoardState(game.getBoard());
                        updateScore();
                        updateNextTilePreview();
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

        // 弹窗背景随主题
        GradientDrawable dialogBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{currentTheme.scorePanelBg, currentTheme.gridCell});
        dialogBg.setCornerRadius(dpToPx(20));
        view.setBackground(dialogBg);

        TextView tvTitle = view.findViewById(R.id.dialog_title);
        TextView tvMessage = view.findViewById(R.id.dialog_message);
        Button btnPositive = view.findViewById(R.id.dialog_btn_positive);
        Button btnNegative = view.findViewById(R.id.dialog_btn_negative);

        tvTitle.setText(title);
        tvTitle.setTextColor(currentTheme.textWarm);
        tvMessage.setText(message);
        tvMessage.setTextColor(currentTheme.textLabel);
        btnPositive.setText(positiveText);

        // 确认按钮：主题渐变色
        GradientDrawable posBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{currentTheme.colorStart, currentTheme.colorEnd});
        posBg.setCornerRadius(dpToPx(26));
        btnPositive.setBackground(posBg);
        btnPositive.setTextColor(Color.WHITE);

        // 取消按钮：主题边框色
        GradientDrawable negBg = new GradientDrawable();
        negBg.setColor(currentTheme.gridCell);
        negBg.setCornerRadius(dpToPx(26));
        negBg.setStroke(dpToPx(2), currentTheme.textWarm);
        btnNegative.setBackground(negBg);
        btnNegative.setTextColor(currentTheme.textWarm);

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

    private void showMagicValueDialog() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_magic_select, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 弹窗背景随主题
        GradientDrawable dialogBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{currentTheme.scorePanelBg, currentTheme.gridCell});
        dialogBg.setCornerRadius(dpToPx(20));
        view.setBackground(dialogBg);

        // 标题颜色随主题
        TextView title = view.findViewById(R.id.magic_title);
        title.setTextColor(currentTheme.textWarm);

        int[] values = {2, 4, 8, 16, 32};
        int[] btnIds = {R.id.magic_btn_2, R.id.magic_btn_4, R.id.magic_btn_8,
                R.id.magic_btn_16, R.id.magic_btn_32};

        for (int i = 0; i < values.length; i++) {
            final int val = values[i];
            TextView btn = view.findViewById(btnIds[i]);
            btn.setOnClickListener(v -> {
                dialog.dismiss();
                enterSelectMode(SelectMode.MAGIC, val);
            });
            btn.setBackground(getTileDrawable(val));
            btn.setTextColor(Color.WHITE);
        }

        // 取消按钮随主题
        Button btnCancel = view.findViewById(R.id.magic_btn_cancel);
        GradientDrawable cancelBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{currentTheme.gridCell, currentTheme.gridBg});
        cancelBg.setCornerRadius(dpToPx(26));
        cancelBg.setStroke(dpToPx(2), currentTheme.textWarm);
        btnCancel.setBackground(cancelBg);
        btnCancel.setTextColor(currentTheme.textWarm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ===== 截图 =====

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

    private void saveScreenshotToGallery(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                android.media.MediaScannerConnection.scanFile(this,
                        new String[]{file.getAbsolutePath()}, null, null);
                Toast.makeText(this, "截图已保存到相册", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

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
                if (capturedBitmap != null) {
                    saveScreenshotToGallery(capturedBitmap);
                }
            } else {
                Toast.makeText(this, "需要存储权限才能保存截图", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===== 生命周期 =====

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
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .build();
                audioManager.requestAudioFocus(focusRequest);
            } else {
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        }
        if (isMusicPlaying && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(
                        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build());
            } else {
                audioManager.abandonAudioFocus(null);
            }
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
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(
                        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build());
            } else {
                audioManager.abandonAudioFocus(null);
            }
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mergePlayer != null) {
            mergePlayer.release();
            mergePlayer = null;
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
