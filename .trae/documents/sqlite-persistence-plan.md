# SQLite 持久化改造计划

## 一、需求概述

将当前使用 `SharedPreferences` 存储的最佳分数和主题设置，改为使用 **SQLite 数据库** 存储。

## 二、当前状态分析

### 当前使用 SharedPreferences 的位置（MainActivity.java）

| 位置 | 用途 | 代码 |
|---|---|---|
| 第 45-47 行 | 常量定义 | `PREFS_NAME`, `KEY_BEST`, `KEY_THEME` |
| 第 173-181 行 | 启动加载 | 读取最佳分数 + 读取主题 |
| 第 409-410 行 | 保存最佳分数 | `putInt(KEY_BEST, bestScore).apply()` |
| 第 572-573 行 | 保存主题 | `putString(KEY_THEME, theme.name()).apply()` |

### 当前存储的数据

| 键名 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `best_score` | int | 0 | 游戏最佳分数 |
| `app_theme` | String | "CLASSIC" | 当前主题名称 |

## 三、设计方案

### 3.1 数据库表设计

**表名**：`game_settings`（游戏设置表，单记录模式）

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER PRIMARY KEY | 主键，固定为 1（始终只有一条记录） |
| `best_score` | INTEGER NOT NULL DEFAULT 0 | 最佳分数 |
| `app_theme` | TEXT NOT NULL DEFAULT 'CLASSIC' | 主题名称 |

> 为什么用单记录模式？因为这些设置是全局唯一的，不需要多行数据，固定 id=1 方便查询和更新。

### 3.2 数据库帮助类

**新建文件**：`AppDatabaseHelper.java`（继承 `SQLiteOpenHelper`）

职责：
- `onCreate()`：创建 `game_settings` 表，并插入一条初始记录（id=1, best_score=0, app_theme='CLASSIC'）
- `onUpgrade()`：数据库版本升级时的处理
- 提供 `getBestScore()` / `setBestScore()` 方法
- 提供 `getAppTheme()` / `setAppTheme()` 方法

### 3.3 修改 MainActivity

- 删除 `SharedPreferences` 相关 import 和常量
- 创建 `AppDatabaseHelper` 实例
- 启动时从数据库读取最佳分数和主题
- 分数更新时写入数据库
- 主题切换时写入数据库

## 四、具体改动文件

### 4.1 新建文件

#### `app/src/main/java/com/example/myapplication/AppDatabaseHelper.java`

```java
package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "game2048.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_SETTINGS = "game_settings";
    private static final String COL_ID = "id";
    private static final String COL_BEST_SCORE = "best_score";
    private static final String COL_APP_THEME = "app_theme";

    private static final int SETTINGS_ROW_ID = 1;

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_BEST_SCORE + " INTEGER NOT NULL DEFAULT 0, " +
                COL_APP_THEME + " TEXT NOT NULL DEFAULT 'CLASSIC')";
        db.execSQL(createTable);

        // 插入初始记录（id=1，始终只有这一条）
        ContentValues values = new ContentValues();
        values.put(COL_ID, SETTINGS_ROW_ID);
        values.put(COL_BEST_SCORE, 0);
        values.put(COL_APP_THEME, "CLASSIC");
        db.insert(TABLE_SETTINGS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    // ===== 最佳分数 =====

    public int getBestScore() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COL_BEST_SCORE},
                COL_ID + " = ?", new String[]{String.valueOf(SETTINGS_ROW_ID)},
                null, null, null);
        int score = 0;
        if (cursor.moveToFirst()) {
            score = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BEST_SCORE));
        }
        cursor.close();
        return score;
    }

    public void setBestScore(int score) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BEST_SCORE, score);
        db.update(TABLE_SETTINGS, values, COL_ID + " = ?",
                new String[]{String.valueOf(SETTINGS_ROW_ID)});
    }

    // ===== 主题设置 =====

    public String getAppTheme() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COL_APP_THEME},
                COL_ID + " = ?", new String[]{String.valueOf(SETTINGS_ROW_ID)},
                null, null, null);
        String theme = "CLASSIC";
        if (cursor.moveToFirst()) {
            theme = cursor.getString(cursor.getColumnIndexOrThrow(COL_APP_THEME));
        }
        cursor.close();
        return theme;
    }

    public void setAppTheme(String theme) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_APP_THEME, theme);
        db.update(TABLE_SETTINGS, values, COL_ID + " = ?",
                new String[]{String.valueOf(SETTINGS_ROW_ID)});
    }
}
```

### 4.2 修改文件

#### `app/src/main/java/com/example/myapplication/MainActivity.java`

**改动 1**：删除 SharedPreferences 相关 import（第 6 行）
- 删除：`import android.content.SharedPreferences;`

**改动 2**：添加数据库帮助类 import
- 新增：`import com.example.myapplication.AppDatabaseHelper;`

**改动 3**：删除常量（第 45-48 行）
- 删除：`PREFS_NAME`, `KEY_BEST`, `KEY_THEME`

**改动 4**：添加成员变量
- 新增：`private AppDatabaseHelper dbHelper;`

**改动 5**：onCreate 中初始化数据库（替换第 172-181 行）
```java
// 初始化数据库
dbHelper = new AppDatabaseHelper(this);

// 加载存档
bestScore = dbHelper.getBestScore();
tvBest.setText(String.valueOf(bestScore));
String savedTheme = dbHelper.getAppTheme();
try {
    currentTheme = Theme.valueOf(savedTheme);
} catch (IllegalArgumentException e) {
    currentTheme = Theme.CLASSIC;
}
```

**改动 6**：保存最佳分数（替换第 409-410 行）
```java
dbHelper.setBestScore(bestScore);
```

**改动 7**：保存主题（替换第 572-573 行）
```java
dbHelper.setAppTheme(theme.name());
```

**改动 8**：onDestroy 中关闭数据库（在合适位置添加）
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (dbHelper != null) {
        dbHelper.close();
    }
    // ... 其他原有 onDestroy 逻辑（如 mediaPlayer 释放）
}
```

## 五、数据迁移考虑

从 SharedPreferences 迁移到 SQLite 时，**旧数据会丢失**（最佳分数和主题设置会重置为默认值）。

可选的迁移方案（如果需要保留旧数据）：
- 在 `AppDatabaseHelper` 初始化时，先读取 SharedPreferences 中的旧数据，如果有值就写入数据库，然后清空 SharedPreferences

考虑到这是单机小游戏，数据量很小，**首次安装重置为默认值是可接受的**。如需要迁移可后续补充。

## 六、验证步骤

1. 编译项目，确保无报错
2. 运行应用，检查：
   - 初始最佳分数为 0
   - 初始主题为经典奶油
3. 玩游戏获得分数，退出应用再重新打开，检查：
   - 最佳分数是否保留
4. 切换主题，退出应用再重新打开，检查：
   - 主题是否保留
5. 使用 Android Studio 的 Device File Explorer 查看数据库文件是否创建：
   - `/data/data/com.example.myapplication/databases/game2048.db`
