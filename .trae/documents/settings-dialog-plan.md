# 设置弹窗优化计划

## 需求概述

点击设置图标 → 弹出设置弹窗，包含两个纯图标按钮：
- 声音按钮（voice.png）：切换背景音乐播放/停止，停止时图标切换为 voiceclose.png
- 重新开始按钮（upload.png）：触发二次确认弹窗

## 当前状态

- `dialog_confirm.xml`：现有文字按钮弹窗，不适合图标按钮布局
- `MainActivity.java` 第112-123行：设置按钮目前直接弹出"重新开始游戏？"确认弹窗
- `res/raw/` 目录不存在，需要创建

## 具体改动

### 1. 复制资源文件

| 源文件 | 目标位置 | 用途 |
|---|---|---|
| `E:\2048\voice.png` | `res/drawable/ic_voice.png` | 声音按钮图标 |
| `C:\Users\MOHEI\Downloads\voiceclose.png` | `res/drawable/ic_voice_close.png` | 静音按钮图标 |
| `E:\2048\upload.png` | `res/drawable/ic_restart.png` | 重新开始按钮图标 |
| `D:\Qt\c\MusicPlayer\music\HOYO-MiX - 悠适的光景 Idle Days of Ease.ogg` | `res/raw/game_music.ogg` | 背景音乐 |

### 2. 新建 `res/layout/dialog_settings.xml`

风格与 `dialog_confirm.xml` 一致，沿用 `bg_dialog` 奶油渐变背景、24dp padding、minWidth 260dp。

```
LinearLayout (vertical)
├── TextView  标题 "设置"（textColor=text_warm, 18sp bold）
├── LinearLayout (horizontal, gravity=center)
│   ├── ImageButton  dialog_btn_voice（64dp×64dp, bg=bg_btn_hammer, src=ic_voice）
│   │   └── 间距 24dp
│   └── ImageButton  dialog_btn_restart（64dp×64dp, bg=bg_btn_hammer, src=ic_restart）
```

ImageButton 需要设置 `android:backgroundTint="@null"` 避免 Material 主题覆盖颜色。

### 3. 修改 `MainActivity.java`

**新增成员变量：**
- `MediaPlayer mediaPlayer` — 背景音乐播放器
- `boolean isMusicPlaying = false` — 音乐播放状态

**onCreate 中新增（在 initCells() 之前）：**
- 创建 `MediaPlayer.create(this, R.raw.game_music)`，设置 `setLooping(true)`

**修改设置按钮点击逻辑（第112-123行）：**
- 使用 `Dialog` + `dialog_settings.xml` 弹出设置弹窗
- 声音按钮 `dialog_btn_voice` 点击：
  - 若 `isMusicPlaying`：停止音乐 → `mediaPlayer.pause()`，切换图标为 `ic_voice_close`，Toast "音乐已关闭"
  - 否则：播放音乐 → `mediaPlayer.start()`，切换图标为 `ic_voice`，Toast "音乐已开启"
  - 切换 `isMusicPlaying` 状态
- 重新开始按钮 `dialog_btn_restart` 点击：
  - 关闭设置弹窗 → 调用 `showCustomDialog()` 弹出二次确认弹窗

**onDestroy 中新增：**
- 释放 MediaPlayer：`if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }`

## 决策
- 初始状态无音乐播放，显示 voice.png
- 重新开始按钮直接复用 `ic_upload.png`（`E:\2048\upload.png` 与底部工具栏图标为同一文件）
- 弹窗完全沿用 `bg_dialog` 背景 + 棕色按钮风格，与整体 UI 一致

## 验证

1. 点击设置图标 → 弹出设置弹窗，两个棕色图标按钮并排显示
2. 点击声音按钮 → 音乐开始播放，Toast 提示，图标切换为 voice_close.png；再次点击 → 音乐停止，图标切回 voice.png
3. 点击重新开始按钮 → 关闭设置弹窗，弹出"重新开始游戏？"确认弹窗，确认后游戏重新开始