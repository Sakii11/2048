# 预览方块显示数字

## 概述
预览区域当前只显示方块背景色，需要同时显示数字（2 或 4）。

## 修改方案

### 文件 1：`activity_main.xml`（L196-209）

在预览 `FrameLayout` 内添加一个 `TextView` 显示数字：

```xml
<FrameLayout
    android:id="@+id/next_tile_preview"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:layout_marginEnd="8dp"
    android:background="@drawable/bg_grid_cell">
    <View
        android:id="@+id/next_tile_view"
        android:layout_width="36dp"
        android:layout_height="36dp"
        android:layout_gravity="center"
        android:background="@drawable/bg_preview_tile_yellow" />
    <TextView
        android:id="@+id/next_tile_text"
        android:layout_width="36dp"
        android:layout_height="36dp"
        android:layout_gravity="center"
        android:gravity="center"
        android:textColor="@color/text_white"
        android:textSize="16sp"
        android:textStyle="bold"
        android:text="2"
        android:shadowColor="#40000000"
        android:shadowRadius="2" />
</FrameLayout>
```

### 文件 2：`MainActivity.java`

**改动 1**：添加成员变量
```java
private TextView nextTileText;
```

**改动 2**：`onCreate()` 中绑定
```java
nextTileText = findViewById(R.id.next_tile_text);
```

**改动 3**：`updateNextTilePreview()` 同时更新背景和文字
```java
private void updateNextTilePreview() {
    int next = game.getNextTile();
    nextTileView.setBackgroundResource(getTileBackground(next));
    nextTileText.setText(String.valueOf(next));
}
```

## 验证方式
- 预览区域显示方块颜色和数字
- 每次滑动后数字随方块值更新