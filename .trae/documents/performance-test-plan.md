# 游戏性能测试与优化计划

## 一、性能问题分析

通过代码审查，发现以下性能瓶颈：

### 1. Shader 在 onDraw 中动态创建（严重）

**位置**：`BoardView.java` 第 416-420 行

```java
Shader gradient = new LinearGradient(
        left, top, right, bottom,
        tileStartColors[idx], tileEndColors[idx],
        Shader.TileMode.CLAMP);
tilePaint.setShader(gradient);
```

**问题**：每次 `onDraw()` 调用都会为每个非空方块创建新的 `LinearGradient` 对象。动画期间每帧可能创建 16 个 Shader，导致大量垃圾对象和频繁 GC。

### 2. isSliding 数组在 onDraw 中重复创建（中等）

**位置**：`BoardView.java` 第 361 行

```java
boolean[][] isSliding = new boolean[4][4];
```

**问题**：每次 `onDraw()` 都创建新的二维数组。

### 3. Paint alpha 频繁修改（轻微）

**位置**：`BoardView.java` 第 421、427 行

```java
tilePaint.setAlpha((int) (255 * alpha));
textPaint.setAlpha((int) (255 * alpha));
```

**问题**：每次绘制都设置 alpha，虽然开销不大但可以优化。

### 4. cellCenter 频繁调用（轻微）

**位置**：`BoardView.java` 第 375-381 行

```java
float[] from = cellCenter(slideSourceRow(r, c), slideSourceCol(r, c));
float[] to = cellCenter(r, c);
```

**问题**：多次调用计算坐标，可以缓存。

## 二、测试方案

### 测试指标

| 指标 | 工具 | 目标值 |
|---|---|---|
| FPS（帧率） | Android Studio Profiler | ≥ 58fps |
| CPU 占用 | Android Studio Profiler | ≤ 20%（空闲），≤ 40%（动画） |
| 内存占用 | Android Studio Profiler | ≤ 50MB |
| GC 频率 | Android Studio Profiler | 动画期间无频繁 GC |

### 测试场景

1. **空闲状态**：棋盘静止，无操作
2. **快速滑动**：连续快速滑动 10 次
3. **道具模式**：锤子/魔法选择模式（闪烁动画）
4. **游戏结束**：填满棋盘直到无法移动

## 三、优化方案

### 优化 1：Shader 缓存

将 Shader 创建移到 `init()` 和 `setThemeColors()` 中，只在主题变化时重新创建。由于方块的渐变方向是固定的（左上到右下），且同一值的方块共享同一个 Paint，可以预创建所有 Shader。

### 优化 2：复用 isSliding 数组

将 `isSliding` 改为成员变量，在动画开始时初始化，避免每次创建。

### 优化 3：减少 alpha 设置次数

只有当 alpha 变化超过阈值时才设置，或者使用 Canvas 的 `saveAlpha()`/`restore()`。

### 优化 4：缓存 cellCenter 计算结果

预计算所有格子的中心坐标。

## 四、具体改动

### 4.1 修改 BoardView.java

#### 改动 1：预缓存 Shader

在 `init()` 方法中添加：

```java
private Shader[] tileShaders = new Shader[12];
```

在 `setThemeColors()` 中预创建 Shader：

```java
for (int i = 0; i < 12; i++) {
    tileShaders[i] = new LinearGradient(
            0, 0, cellSize, cellSize,
            tileStartColors[i], tileEndColors[i],
            Shader.TileMode.CLAMP);
}
```

在 `drawTileAt()` 中直接使用缓存的 Shader：

```java
tilePaint.setShader(tileShaders[idx]);
```

#### 改动 2：复用 isSliding 数组

```java
private boolean[][] isSliding = new boolean[4][4];
```

在动画开始时清空：

```java
for (int r = 0; r < 4; r++) {
    for (int c = 0; c < 4; c++) {
        isSliding[r][c] = false;
    }
}
```

#### 改动 3：预缓存格子中心坐标

```java
private float[][] cellCenters = new float[4][4];
```

在 `onSizeChanged()` 中计算：

```java
for (int r = 0; r < 4; r++) {
    for (int c = 0; c < 4; c++) {
        float left = boardLeft + c * (cellSize + gapPx);
        float top = boardTop + r * (cellSize + gapPx);
        cellCenters[r][c][0] = left + cellSize / 2f;
        cellCenters[r][c][1] = top + cellSize / 2f;
    }
}
```

## 五、验证步骤

1. 编译项目
2. 使用 Android Studio Profiler 监控：
   - 录制 30 秒快速滑动操作
   - 检查 FPS 是否稳定在 60fps
   - 检查 GC 次数是否减少
   - 检查 CPU 占用是否下降
3. 在不同设备上测试（模拟器 + 真机）
