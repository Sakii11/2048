package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_MODE = "game_mode";
    public static final String MODE_LEVEL = "level";
    public static final String MODE_FREE = "free";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 沉浸式状态栏
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.cream_gradient_start));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.cream_gradient_end));

        Button btnLevel = findViewById(R.id.btn_level_mode);
        Button btnFree = findViewById(R.id.btn_free_mode);

        btnLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, LevelSelectActivity.class);
                startActivity(intent);
            }
        });

        btnFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra(EXTRA_GAME_MODE, MODE_FREE);
                startActivity(intent);
            }
        });
    }
}
