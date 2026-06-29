# 棋盘放大 + 整体下移

## 概述
将棋盘等比例放大，并通过调整间距使按钮和棋盘整体下移，让布局更协调。

## 当前状态

| 元素 | 当前值 |
|------|--------|
| 棋盘容器 | 336dp × 336dp |
| 每行高度 | 80dp |
| 棋盘 padding | 8dp |
| 棋盘居中方式 | FrameLayout gravity="center"（在剩余空间中居中，导致底部工具栏远离棋盘） |
| 顶部 padding | 16dp |
| 按钮行 padding | 16dp |
| 底部工具栏 padding | 16dp |

## 问题分析
1. 棋盘 336dp 偏小，可以放大
2. 棋盘用 FrameLayout gravity="center" 居中，导致棋盘和底部工具栏之间有大量空白
3. 整体内容偏上，需要下移

## 修改方案

### 修改文件：`app/src/main/res/layout/activity_main.xml`

#### 1. 棋盘放大：336dp → 360dp
- 外层容器：`layout_height` 从 336dp 改为 **360dp**
- 内层棋盘：`336dp × 336dp` 改为 **360dp × 360dp**
- 每行高度：80dp 改为 **86dp**（(360 - 8×2) / 4 = 86dp）
- padding 保持 8dp

#### 2. 整体下移
- 在顶部区域（预览方块+得分面板）上方添加一个 `Space` 占位，将整个内容往下推
- 顶部 Space 高度设为 **24dp**

#### 3. 移除棋盘居中，改为自然排列
- 移除棋盘外层 FrameLayout 的 `gravity="center"`
- 让棋盘自然垂直排列，不再在剩余空间中居中
- 这样棋盘会紧贴按钮行下方，底部工具栏也会自然跟随

#### 4. 底部工具栏保持适当间距
- 底部工具栏 padding 保持 **16dp**（不紧贴棋盘）

### 具体改动

**改动 1**：在顶部区域之前添加 Space（L14 之前）
```xml
<!-- 顶部留白，整体下移 -->
<View
    android:layout_width="0dp"
    android:layout_height="24dp" />
```

**改动 2**：棋盘容器（L244-252）
```xml
<!-- 棋盘容器（固定 360dp × 360dp） -->
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="360dp">

    <FrameLayout
        android:layout_width="360dp"
        android:layout_height="360dp"
        android:layout_gravity="center"
        android:background="@drawable/bg_grid_board"
        android:padding="8dp">
```

**改动 3**：每行高度 80dp → 86dp（共 4 处，L267, L298, L329, L360）

## 验证方式
- 棋盘显示为 4×4 = 16 格，比之前更大
- 整体布局视觉重心下移
- 底部工具栏与棋盘保持适当间距，不紧贴
- 布局协调自然