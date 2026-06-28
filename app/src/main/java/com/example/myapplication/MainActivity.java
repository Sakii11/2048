package com.example.myapplication;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvScore;
    private TextView tvBest;

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
    }
}