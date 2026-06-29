# 修改 1：初始只有一个方块 + 显示下一个方块预览

## 概述
1. 初始时棋盘上只有一个方块 2（而非两个）
2. 设置按钮旁边的预览区域实时显示下一个要刷新的方块

## 当前状态分析

| 文件 | 当前状态 |
|------|----------|
| `Game2048.java` L33-34 | `newGame()` 调用两次 `spawnTile()` |
| `Game2048.java` L37-50 | `spawnTile()` 随机生成方块，但无预生成机制 |
| `activity_main.xml` L196-207 | 预览区域是静态 View，无 ID，无法动态更新 |
| `MainActivity.java` | 没有预览方块更新逻辑 |

## 修改方案

### 文件 1：`Game2048.java`

**改动 1**：`newGame()` 中只调用一次 `spawnTile()`（L33-34）
```java
spawnTile();  // 只生成一个
// 删除第二个 spawnTile()
```

**改动 2**：新增 `nextTile` 字段和预生成机制
- 添加 `private int nextTile;` 成员变量
- `newGame()` 中初始化 `nextTile = generateNextTileValue();`
- `spawnTile()` 改为使用预先确定的 `nextTile` 值，生成后再预生成下一个
- 新增 `public int getNextTile()` 方法，暴露给 MainActivity

**改动 3**：`spawnTile()` 修改
```java
private void spawnTile() {
    // ... 找到空位 ...
    board[pos[0]][pos[1]] = nextTile;
    nextTile = generateNextTileValue();
}
```

**改动 4**：新增 `generateNextTileValue()` 方法
```java
private int generateNextTileValue() {
    return random.nextInt(10) < 9 ? 2 : 4;
}
```

### 文件 2：`activity_main.xml`

**改动 1**：给预览方块区域添加 ID（L197）
```xml
<FrameLayout
    android:id="@+id/next_tile_preview"
    ...>
```

**改动 2**：把静态 View 改为动态 TextView + 背景
保持结构，但给 View 添加 ID 以便 Java 引用：
```xml
<View
    android:id="@+id/next_tile_view"
    .../>
```

### 文件 3：`MainActivity.java`

**改动 1**：添加成员变量
```java
private FrameLayout nextTilePreview;
private View nextTileView;
```

**改动 2**：`onCreate()` 中绑定预览 UI
```java
nextTilePreview = findViewById(R.id.next_tile_preview);
nextTileView = findViewById(R.id.next_tile_view);
```

**改动 3**：新增 `updateNextTilePreview()` 方法
```java
private void updateNextTilePreview() {
    int next = game.getNextTile();
    nextTileView.setBackgroundResource(getTileBackground(next));
}
```

**改动 4**：在 `renderBoard()` 末尾调用 `updateNextTilePreview()`

**改动 5**：在 `onSwipe()` 中移动后也要更新预览

**改动 6**：在 `newGame` 相关调用后更新预览

## 实施步骤

1. 修改 `Game2048.java`（newGame 只 spawn 一次 + nextTile 机制）
2. 修改 `activity_main.xml`（预览区域加 ID）
3. 修改 `MainActivity.java`（更新预览）

## 验证方式
- 启动后棋盘上只有一个方块 2
- 预览区域显示下一个方块的颜色（2=黄色，4=橙色）
- 每次滑动后预览区域更新为下一个方块的颜色