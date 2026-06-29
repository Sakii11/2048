# 方块动画优化：出现动画 + 合成动画

## 概述
1. 新方块出现时：缩放弹出动画（scale 0→1，150ms）
2. 方块合成时：弹性放大回弹动画（scale 1→1.3→1，200ms）

## 当前状态分析

| 文件 | 当前状态 |
|------|----------|
| `Game2048.java` | `MoveResult` 已有 `mergedAt` 标记合并位置，但 `spawnTile()` 未记录新方块位置 |
| `MainActivity.java` | `renderBoard()` 直接更新 UI，无动画；`onSwipe()` 后仅调用 `renderBoard()` |
| `res/anim/` | 不存在，无需创建（使用 ViewPropertyAnimator 代码方式） |

## 修改方案

### 文件 1：`Game2048.java`

**改动 1**：`MoveResult` 新增字段
```java
public static class MoveResult {
    public boolean moved;
    public int score;
    public int[][] mergedAt;
    public int spawnedRow;    // 新方块所在行
    public int spawnedCol;    // 新方块所在列
    public int newTileValue;  // 新方块的值
}
```

**改动 2**：`spawnTile()` 改为返回值，记录新方块位置
```java
// 改为 private int[] spawnTile()，返回 {row, col, value}
// 或者直接在 move() 中调用 spawnTile() 前后记录位置变化
```

**更简单的实现**：在 `move()` 中，`spawnTile()` 执行前后比较 board 差异，找到新增方块位置：
```java
// 在 move() 的 if (result.moved) 块中：
int[][] beforeSpawn = copyBoard();
spawnTile();
// 比较 beforeSpawn 和 board，找到新增的非零位置
for (int r = 0; r < SIZE; r++) {
    for (int c = 0; c < SIZE; c++) {
        if (beforeSpawn[r][c] == 0 && board[r][c] != 0) {
            result.spawnedRow = r;
            result.spawnedCol = c;
            result.newTileValue = board[r][c];
            break;
        }
    }
}
```

### 文件 2：`MainActivity.java`

**改动 1**：`onSwipe()` 中，`renderBoard()` 后执行动画
```java
private void onSwipe(Direction direction) {
    MoveResult result = game.move(direction);
    if (result.moved) {
        renderBoard();
        // 执行合并动画
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (result.mergedAt[r][c] != 0) {
                    animateMerge(cells[r][c]);
                }
            }
        }
        // 执行新方块出现动画
        FrameLayout spawnedCell = cells[result.spawnedRow][result.spawnedCol];
        animateSpawn(spawnedCell);
        updateScore();
        // ... win/lose 检查 ...
    }
}
```

**改动 2**：新增 `animateSpawn(FrameLayout cell)` 方法
```java
private void animateSpawn(View cell) {
    cell.setScaleX(0f);
    cell.setScaleY(0f);
    cell.animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(150)
        .setInterpolator(new OvershootInterpolator(3f))
        .start();
}
```

**改动 3**：新增 `animateMerge(FrameLayout cell)` 方法
```java
private void animateMerge(View cell) {
    cell.animate()
        .scaleX(1.3f)
        .scaleY(1.3f)
        .setDuration(100)
        .withEndAction(() -> {
            cell.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(100)
                .start();
        })
        .start();
}
```

**改动 4**：`renderBoard()` 中，新方块先隐藏再渲染（避免闪现）
- 不需要改 `renderBoard()`，因为动画在 `renderBoard()` 之后执行，设置 `scaleX/Y=0` 即可

## 实施步骤

1. 修改 `Game2048.java`：`MoveResult` 新增字段 + `move()` 中记录新方块位置
2. 修改 `MainActivity.java`：`onSwipe()` 添加动画调用 + 新增 `animateSpawn()` 和 `animateMerge()` 方法

## 验证方式
- 新方块出现时有弹性缩放弹出效果
- 合并方块时有放大再回弹效果
- 动画流畅，不影响游戏逻辑
- 撤销时不需要动画（直接渲染）