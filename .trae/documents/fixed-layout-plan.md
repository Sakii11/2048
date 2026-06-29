# 固定布局 — 防止应用拉伸适配不同屏幕

## 概述
当前布局使用 `match_parent`、`layout_weight` 等弹性属性，导致在不同屏幕尺寸上自动拉伸。需要改为固定宽度居中布局，使界面在所有屏幕上保持相同尺寸，不拉伸变形。

## 当前问题分析

| 文件 | 问题 |
|------|------|
| `activity_main.xml` L3 | `ScrollView` 使用 `match_parent` 全宽 |
| L10 | 内层 `LinearLayout` 使用 `match_parent` 全宽 |
| L105-108 | 弹性空间 `View` 使用 `layout_weight` 拉伸 |
| L209-213 | 功能按钮行弹性空间 `layout_weight` |
| L250-254 | 棋盘容器使用 `layout_weight="1"` 自动撑满剩余高度 |
| L267-268 | `GridLayout` 使用 `match_parent` + `layout_columnWeight` 自动均分 |

## 修改方案

### 核心思路：固定宽度容器 + 居中显示

将整个布局内容包裹在一个固定宽度（360dp，标准手机设计宽度）的容器中，居中显示。背景渐变仍然铺满全屏，但内容区域保持固定尺寸。

### 需要修改的文件：仅 1 个

#### `app/src/main/res/layout/activity_main.xml`

**改动点：**

1. **ScrollView 保持不变** — 作为全屏背景容器，`match_parent` 宽高，`fillViewport="true"`

2. **新增固定宽度容器** — 在 ScrollView 内部增加一层 `FrameLayout`，宽度固定 `360dp`，高度 `match_parent`，通过 `layout_gravity="center"` 居中

3. **移除弹性拉伸**：
   - 删除顶部区域中的弹性 `View`（L105-108）
   - 删除功能按钮行中的弹性 `View`（L209-213）
   - 改为：固定宽度内的左右分布，左边方块固定，右边按钮固定，使用 `layout_gravity` 或 `gravity` 控制对齐

4. **棋盘固定尺寸**：
   - 棋盘容器移除 `layout_weight="1"`
   - 改为固定 `360dp` 宽 × `360dp` 高（正方形）
   - 棋盘格子使用 `layout_columnWeight` 仍然可以（在固定宽度内均分是合理的），但改为显式 `90dp` 更精确

5. **顶部区域布局调整**：
   - 2×2 预览方块靠左（`layout_gravity="start"`），得分面板靠右（`layout_gravity="end"`）
   - 使用 `Space` 或 `layout_weight` 在固定宽度内做弹性分隔（此时弹性在 360dp 内，不会有跨屏幕拉伸问题）

6. **功能按钮行布局调整**：
   - 左侧：设置 + 预览方块靠左
   - 右侧：撤回 + 锤子 + 魔法靠右
   - 同样使用弹性空间在 360dp 内分隔

### 具体布局结构（修改后）

```xml
<ScrollView (match_parent, fillViewport, bg_gradient)>
  <FrameLayout (layout_width="360dp", layout_height="match_parent", layout_gravity="center")>
    <LinearLayout (orientation="vertical", width="360dp", height="wrap_content")>
      
      <!-- 顶部区域 -->
      <LinearLayout (horizontal, width="360dp")>
        <GridLayout (2×2, 预览方块, 靠左) />
        <Space (弹性) />
        <LinearLayout (得分面板, 靠右) />
      </LinearLayout>
      
      <!-- 功能按钮行 -->
      <LinearLayout (horizontal, width="360dp")>
        <ImageButton (设置) />
        <FrameLayout (预览) />
        <Space (弹性) />
        <ImageButton (撤回) />
        <ImageButton (锤子) />
        <ImageButton (魔法) />
      </LinearLayout>
      
      <!-- 4×4 棋盘 -->
      <FrameLayout (360dp × 360dp, 固定尺寸)>
        <GridLayout (4×4, 格子各90dp) />
      </FrameLayout>
      
      <!-- 底部工具栏 -->
      <LinearLayout (水平, 靠左)>
        <ImageButton (上传) />
        <ImageButton (换装) />
      </LinearLayout>
      
    </LinearLayout>
  </FrameLayout>
</ScrollView>
```

### 关键尺寸
- 固定容器宽度：**360dp**（标准手机设计宽度）
- 棋盘：**360dp × 360dp**（正方形）
- 每个格子：**90dp × 90dp**（含 4dp 外边距）
- 预览方块：保持 **60dp × 60dp**
- 功能按钮：保持 **48dp × 48dp**
- 得分面板：保持 **90dp × 50dp**

## 实施步骤
1. 修改 `activity_main.xml`，添加固定宽度居中容器，移除弹性拉伸，棋盘改用固定尺寸
2. 构建验证

## 验证方式
- 在不同分辨率模拟器上运行，界面大小保持一致
- 在宽屏设备上，背景铺满，内容居中不拉伸