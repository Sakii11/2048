# 2048 游戏 UI 界面设计计划

## 概述
为 2048 休闲小游戏设计竖版手机全屏 UI 界面，采用柔和米黄浅奶油渐变背景、低饱和度莫兰迪暖色系、圆角柔和的简约治愈扁平卡通风格。

## 当前状态分析
- **项目**: Android Java 项目，minSdk 24，targetSdk 36
- **主题**: Material3 DayNight NoActionBar
- **布局**: 当前仅有一个 "Hello World" 的 ConstraintLayout
- **依赖**: appcompat、constraintlayout、material、activity-ktx
- **图标资源**: 5 个 PNG 图标位于 `E:\2048\`（back.png, clothing.png, hammer.png, magic.png, setting.png）

## 设计方案

### 颜色体系（莫兰迪暖色系）
| 用途 | 颜色值 | 说明 |
|------|--------|------|
| 背景渐变起 | `#F5E6D3` | 柔和米黄 |
| 背景渐变止 | `#FDF3E7` | 浅奶油色 |
| 预览方块-黄(2) | `#F2D08A` | 低饱和黄 |
| 预览方块-橙(0) | `#F0B27A` | 低饱和橙 |
| 预览方块-深棕(4) | `#A0724E` | 莫兰迪棕 |
| 预览方块-浅橙(8) | `#F5CBA7` | 浅杏色 |
| 棋盘背景 | `#C4B5A5` | 浅灰褐色 |
| 棋盘格子 | `#FDF5E6` | 浅米色 |
| 得分面板 | `#E8DED1` | 灰色圆角面板 |
| 撤回按钮 | `#E07B6C` | 柔红 |
| 锤子按钮 | `#A0724E` | 莫兰迪棕 |
| 魔法按钮 | `#7BA7C9` | 莫兰迪蓝 |
| 文字深色 | `#5D4E37` | 暖棕文字 |
| 文字浅色 | `#FFFFFF` | 白色文字 |

### 圆角规范
- 大按钮/面板: 16dp
- 棋盘格子: 12dp
- 预览方块: 12dp
- 小图标按钮: 12dp

---

## 需要创建/修改的文件

### 1. 复制图标资源
将 `E:\2048\` 下的 PNG 图标复制到 `app/src/main/res/drawable/` 目录：
- `back.png` → `ic_undo.png`
- `clothing.png` → `ic_clothing.png`
- `hammer.png` → `ic_hammer.png`
- `magic.png` → `ic_magic.png`
- `setting.png` → `ic_settings.png` 和 `ic_upload.png`（上传和设置共用同一图标）

### 2. `app/src/main/res/values/colors.xml` — 添加颜色资源
```xml
<color name="cream_gradient_start">#F5E6D3</color>
<color name="cream_gradient_end">#FDF3E7</color>
<color name="tile_yellow_2">#F2D08A</color>
<color name="tile_orange_0">#F0B27A</color>
<color name="tile_brown_4">#A0724E</color>
<color name="tile_light_orange_8">#F5CBA7</color>
<color name="grid_bg">#C4B5A5</color>
<color name="grid_cell">#FDF5E6</color>
<color name="score_panel_bg">#E8DED1</color>
<color name="btn_undo">#E07B6C</color>
<color name="btn_hammer">#A0724E</color>
<color name="btn_magic">#7BA7C9</color>
<color name="text_warm">#5D4E37</color>
<color name="text_white">#FFFFFF</color>
<color name="text_score_label">#8B7D6B</color>
```

### 3. `app/src/main/res/values/strings.xml` — 更新应用名
```xml
<string name="app_name">2048</string>
<string name="score">得分</string>
<string name="best">最佳</string>
```

### 4. `app/src/main/res/drawable/bg_gradient.xml` — 背景渐变
奶油色从上到下渐变，米黄→浅奶油色，圆角 0（全屏背景）。

### 5. `app/src/main/res/drawable/bg_preview_tile_yellow.xml` — 黄色预览方块
黄色 `#F2D08A` 圆角矩形，圆角 12dp，内嵌细微渐变增加立体感。

### 6. `app/src/main/res/drawable/bg_preview_tile_orange.xml` — 橙色预览方块
同结构，颜色 `#F0B27A`。

### 7. `app/src/main/res/drawable/bg_preview_tile_brown.xml` — 深棕预览方块
同结构，颜色 `#A0724E`。

### 8. `app/src/main/res/drawable/bg_preview_tile_light_orange.xml` — 浅橙预览方块
同结构，颜色 `#F5CBA7`。

### 9. `app/src/main/res/drawable/bg_score_panel.xml` — 得分面板背景
灰色 `#E8DED1` 圆角矩形，圆角 16dp，轻微浮雕阴影。

### 10. `app/src/main/res/drawable/bg_grid_cell.xml` — 棋盘格子背景
浅米色 `#FDF5E6` 圆角矩形，圆角 12dp。

### 11. `app/src/main/res/drawable/bg_grid_board.xml` — 棋盘整体背景
灰褐色 `#C4B5A5` 圆角矩形，圆角 16dp。

### 12. `app/src/main/res/drawable/bg_btn_undo.xml` — 撤回按钮背景
柔红 `#E07B6C` 圆角矩形，圆角 12dp，浮雕效果。

### 13. `app/src/main/res/drawable/bg_btn_hammer.xml` — 锤子按钮背景
莫兰迪棕 `#A0724E` 圆角矩形，圆角 12dp，浮雕效果。

### 14. `app/src/main/res/drawable/bg_btn_magic.xml` — 魔法按钮背景
莫兰迪蓝 `#7BA7C9` 圆角矩形，圆角 12dp，浮雕效果。

### 15. `app/src/main/res/layout/activity_main.xml` — 主布局（完整重写）
使用垂直 LinearLayout 作为根布局，从上至下分层：

```
LinearLayout (vertical, bg_gradient)
├── LinearLayout (horizontal, top area)
│   ├── GridLayout (2×2, 预览方块区)
│   │   ├── FrameLayout (黄色 "2")
│   │   ├── FrameLayout (橙色 "0")
│   │   ├── FrameLayout (深棕 "4")
│   │   └── FrameLayout (浅橙 "8")
│   └── LinearLayout (vertical, 得分区)
│       ├── FrameLayout (得分面板: "得分" + "0")
│       └── FrameLayout (最佳面板: "最佳" + "0")
│
├── LinearLayout (horizontal, 功能按钮行)
│   ├── ImageButton (设置齿轮)
│   ├── FrameLayout (下一个方块预览)
│   ├── Space (弹性空间)
│   ├── ImageButton (撤回)
│   ├── ImageButton (锤子)
│   └── ImageButton (魔法)
│
├── FrameLayout (棋盘容器)
│   └── GridLayout (4×4, 棋盘)
│       ├── 16个 FrameLayout (格子)
│       ...
│
└── LinearLayout (horizontal, 底部工具栏)
    ├── ImageButton (上传)
    └── ImageButton (换装)
```

关键尺寸：
- 预览方块：约 60dp × 60dp
- 预览方块间距：6dp
- 得分面板：约 90dp × 50dp
- 功能按钮：约 48dp × 48dp
- 棋盘格子：动态计算，约占屏幕宽度的 85%，均分 4 列
- 棋盘格子间距：8dp
- 底部图标：约 40dp × 40dp

### 16. `app/src/main/java/com/example/myapplication/MainActivity.java` — 微调
- 移除 `EdgeToEdge` 相关代码（使用自定义背景后不需要）
- 设置全屏沉浸式状态栏
- 初始化 UI 引用

---

## 实施步骤

1. 复制 5 个 PNG 图标到 `app/src/main/res/drawable/`
2. 更新 `colors.xml` 添加莫兰迪色系
3. 更新 `strings.xml` 添加文本资源
4. 创建 10 个 drawable 背景资源文件
5. 重写 `activity_main.xml` 布局
6. 调整 `MainActivity.java` 以适配新布局
7. 构建验证

## 验证方式
- 在 Android Studio 中预览布局效果
- `./gradlew assembleDebug` 编译通过
- 检查所有图标资源是否正确引用