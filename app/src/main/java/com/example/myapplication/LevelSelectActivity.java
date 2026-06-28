package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class LevelSelectActivity extends AppCompatActivity {

    // 六关的目标：合成出对应数字的方块
    private static final int[] LEVEL_TARGETS = {32, 128, 256, 512, 1024, 2048};

    // 方块预览色（索引对应 getTileColorIndex）
    private static final int[] TILE_COLORS = {
            0xFFF2D08A, // 2
            0xFFF0B27A, // 4
            0xFFA0724E, // 8
            0xFFF5CBA7, // 16
            0xFFE8A87C, // 32
            0xFFE59866, // 64
            0xFFD35400, // 128
            0xFFF7DC6F, // 256
            0xFFF4D03F, // 512
            0xFFF1C40F, // 1024
            0xFF82E0AA, // 2048
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        // 沉浸式状态栏
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.cream_gradient_start));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.cream_gradient_end));

        LinearLayout container = findViewById(R.id.level_cards_container);
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < LEVEL_TARGETS.length; i++) {
            final int target = LEVEL_TARGETS[i];
            View card = inflater.inflate(R.layout.item_level_card, container, false);

            // 设置关卡名称
            TextView tvName = card.findViewById(R.id.level_name);
            tvName.setText("第 " + (i + 1) + " 关");

            // 设置目标描述
            TextView tvGoal = card.findViewById(R.id.level_goal);
            tvGoal.setText("合成出 " + target);

            // 设置方块预览色和数字
            View preview = card.findViewById(R.id.level_tile_preview);
            View tileBg = card.findViewById(R.id.level_tile_bg);
            TextView tileText = card.findViewById(R.id.level_tile_text);
            int colorIdx = getTileColorIndex(target);
            int tileColor = TILE_COLORS[Math.min(colorIdx, TILE_COLORS.length - 1)];
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setColor(tileColor);
            gd.setCornerRadius(dpToPx(10));
            tileBg.setBackground(gd);
            tileText.setText(String.valueOf(target));
            // 浅色方块用深色文字
            if (target <= 4 || target == 8 || target == 16 || target == 32) {
                tileText.setTextColor(0xFF776E65);
            }

            // 点击启动对应关卡
            final int levelNum = i + 1;
            card.setOnClickListener(v -> {
                Intent intent = new Intent(LevelSelectActivity.this, MainActivity.class);
                intent.putExtra(LoginActivity.EXTRA_GAME_MODE, LoginActivity.MODE_LEVEL);
                intent.putExtra("level_target", target);
                intent.putExtra("level_number", levelNum);
                startActivity(intent);
            });

            container.addView(card);
        }
    }

    private int getTileColorIndex(int value) {
        int exp = 0;
        int v = value;
        while (v > 1) { v /= 2; exp++; }
        return Math.min(exp - 1, 10);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
