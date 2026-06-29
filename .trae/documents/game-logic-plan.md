# 2048 游戏逻辑实现计划

## 概述
实现 2048 经典游戏核心逻辑，包括：4×4 棋盘状态管理、滑动合并、随机生成方块、得分统计、胜负判定，以及滑动手势识别和 UI 渲染。

## 当前状态分析

| 文件 | 状态 |
|------|------|
| `MainActivity.java` | 仅有基础 UI 初始化，无游戏逻辑 |
| `activity_main.xml` | 16 个格子无 ID，无法从 Java 引用；棋盘区域无触摸监听 |
| `colors.xml` | 只有 4 种预览方块颜色，缺少 16/32/64/128/256/512/1024/2048 等方块色 |

## 修改方案

### 文件 1：新建 `Game2048.java` — 纯游戏逻辑（无 Android 依赖）

**路径**：`app/src/main/java/com/example/myapplication/Game2048.java`

**职责**：
- 维护 4×4 `int[][] board` 棋盘状态
- `newGame()` — 清空棋盘，生成 2 个随机方块
- `spawnTile()` — 在空白位置随机生成 2（90%）或 4（10%）
- `move(Direction)` — 向指定方向滑动合并，返回 `MoveResult`（是否移动、得分、合并位置）
- `canMove()` — 判断是否还有可走步数
- `isWin()` — 检查是否达成 2048
- `isGameOver()` — 检查是否无法继续
- `undo()` — 撤销上一步（保存历史状态）
- 提供 `getCell(row, col)` 获取当前格子值

**数据结构**：
```java
enum Direction { UP, DOWN, LEFT, RIGHT }

class MoveResult {
    boolean moved;       // 是否发生了移动
    int score;           // 本次移动得分
    int[][] mergedAt;    // 合并位置标记（用于动画）
}

class GameState {
    int[][] board;
    int score;
}
```

### 文件 2：修改 `activity_main.xml` — 给格子添加 ID + 触摸区域

**改动 1**：给 16 个格子 `FrameLayout` 添加 ID（`cell_00` ~ `cell_33`）

**改动 2**：在棋盘外层 `FrameLayout` 上添加 `android:id="@+id/board_container"`，用于触摸手势识别

**改动 3**：给每个格子添加 `android:clickable="false"`（避免格子拦截触摸事件）

### 文件 3：修改 `MainActivity.java` — 游戏主控逻辑

**改动 1**：添加成员变量
- `Game2048 game`
- `FrameLayout[][] cells` — 16 个格子的引用
- `TextView[][] tileTexts` — 每个格子的数字 TextView（动态创建）
- `GestureDetector` 或 `OnTouchListener` — 滑动手势检测

**改动 2**：`onCreate()` 中初始化
- 创建 `Game2048` 实例
- 绑定 16 个格子到 `cells` 数组
- 为每个格子动态添加 `TextView` 用于显示数字
- 给棋盘容器设置 `OnTouchListener`，检测上下左右滑动
- 调用 `renderBoard()` 初始渲染

**改动 3**：`renderBoard()` 方法
- 遍历 4×4，根据 `board[row][col]` 值设置每个格子的背景色和文字
- 0 显示空背景（`bg_grid_cell`），非 0 显示对应颜色的方块
- 动态设置方块背景色（根据数字值）

**改动 4**：`onSwipe(Direction)` 方法
- 调用 `game.move(direction)`
- 如果 `moved`，调用 `renderBoard()` 更新 UI
- 更新得分显示
- 检查 `isGameOver()` / `isWin()`

**改动 5**：按钮事件绑定
- 撤回按钮：`game.undo()` + `renderBoard()`
- 锤子道具：移除一个随机方块（预留）
- 魔法道具：将当前最大方块翻倍一次（预留）

### 文件 4：修改 `colors.xml` — 添加方块颜色

新增 2048 各数值对应的方块颜色（莫兰迪暖色系）：

| 数值 | 色值 | 说明 |
|------|------|------|
| 2 | `#F2D08A` | 浅黄（已有 tile_yellow_2） |
| 4 | `#F0B27A` | 橙色（已有 tile_orange_0） |
| 8 | `#F5CBA7` | 浅橙（已有 tile_light_orange_8） |
| 16 | `#E8A87C` | 中橙 |
| 32 | `#E59866` | 深橙 |
| 64 | `#D35400` | 砖红 |
| 128 | `#F7DC6F` | 金黄 |
| 256 | `#F4D03F` | 亮黄 |
| 512 | `#F1C40F` | 深黄 |
| 1024 | `#82E0AA` | 浅绿 |
| 2048 | `#2ECC71` | 翠绿 |

### 文件 5：新建 `drawable/bg_tile_*.xml` — 各数字方块圆角背景

为 16/32/64/128/256/512/1024/2048 创建对应的圆角矩形 drawable，与已有的 `bg_preview_tile_*.xml` 风格一致（圆角 12dp，纯色填充）。

## 实施步骤

1. 新建 `Game2048.java`（核心逻辑）
2. 修改 `colors.xml`（新增方块颜色）
3. 新建 8 个 `bg_tile_*.xml`（16~2048 方块背景）
4. 修改 `activity_main.xml`（格子 ID + 触摸容器 ID）
5. 修改 `MainActivity.java`（手势 + 渲染 + 按钮绑定）

## 验证方式
- 启动后棋盘显示 2 个随机方块（2 或 4）
- 上下左右滑动方块正常移动和合并
- 得分正确累加
- 达成 2048 显示胜利提示
- 无法移动时显示游戏结束提示
- 撤回按钮恢复上一步状态