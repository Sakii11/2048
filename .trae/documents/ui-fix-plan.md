# UI 修复计划 — 铺满宽度 + 棋盘居中 + 修复 9 宫格 bug

## 概述
根据用户反馈和截图，当前 UI 存在以下问题：
1. 内容区域太窄（固定 320dp），两侧大量留白
2. 底部图标太小看不清
3. 棋盘下方大片空白，布局不饱满
4. 用户提到出现 9 宫格错误（应为 4×4 = 16 格）

## 用户选择
- **宽度适配**：铺满屏幕宽度（match_parent）
- **空白处理**：棋盘居中，上下留白，底部工具栏紧贴棋盘下方

## 当前问题分析

| 问题 | 原因 |
|------|------|
| 内容太窄 | 固定 320dp 宽度容器 + layout_gravity="center" |
| 底部图标小 | 36dp 图标在宽屏上显得很小 |
| 棋盘下空白 | 棋盘固定 296dp 高度，没有居中 |
| 9 宫格 bug | GridLayout 的 rowCount/columnCount 可能在某些设备上被错误解析为 3×3 |

## 修改方案

### 修改文件：仅 `app/src/main/res/layout/activity_main.xml`

#### 1. 移除固定宽度容器，改为铺满屏幕
- 删除 `FrameLayout` 固定 320dp 居中容器
- 根 `LinearLayout` 改为 `match_parent` 宽度 + `gravity="center_horizontal"`
- 所有子元素宽度从固定值改为 `match_parent` 或 `wrap_content`

#### 2. 棋盘居中 + 上下留白
- 使用 `ScrollView` + `fillViewport="true"`
- 内部 `LinearLayout` 高度设为 `match_parent`
- 棋盘区域使用 `layout_weight="1"` + `gravity="center"` 居中
- 顶部区域（预览+得分+按钮行）在上方
- 棋盘在中间居中
- 底部工具栏紧贴棋盘下方

#### 3. 修复 9 宫格 bug
- 将 `GridLayout` 改为 `androidx.gridlayout.widget.GridLayout`（支持库版本更稳定）
- 或者改用 `LinearLayout` 嵌套实现 4×4 网格，避免 GridLayout 兼容性问题
- **决定**：改用 4 个垂直 `LinearLayout`（每行一个），每行内 4 个 `FrameLayout`，完全避免 GridLayout 的兼容性问题

#### 4. 增大底部图标
- 底部图标从 36dp 增大到 48dp

#### 5. 各区域宽度调整
- 预览方块：60dp × 60dp（恢复原大小）
- 得分面板：90dp × 50dp（恢复原大小）
- 功能按钮：48dp × 48dp（恢复原大小）
- 棋盘：`match_parent` 宽度，最大 400dp，正方形
- 底部图标：48dp × 48dp

### 具体布局结构

```xml
<ScrollView (match_parent, fillViewport, bg_gradient)>
  <LinearLayout (match_parent, match_parent, vertical, gravity="center_horizontal")>
    
    <!-- 顶部区域 -->
    <LinearLayout (match_parent, wrap_content, horizontal, padding="16dp")>
      <GridLayout (2×2, 预览方块, wrap_content) />
      <Space (弹性) />
      <LinearLayout (得分面板, wrap_content) />
    </LinearLayout>
    
    <!-- 功能按钮行 -->
    <LinearLayout (match_parent, wrap_content, horizontal, padding="0 16dp")>
      <ImageButton (设置) />
      <FrameLayout (预览) />
      <Space (弹性) />
      <ImageButton (撤回) />
      <ImageButton (锤子) />
      <ImageButton (魔法) />
    </LinearLayout>
    
    <!-- 棋盘居中容器 -->
    <FrameLayout (match_parent, 0dp, layout_weight="1", gravity="center")>
      <FrameLayout (max 400dp × 400dp, bg_grid_board, padding)>
        <!-- 4 行 LinearLayout 替代 GridLayout -->
        <LinearLayout (vertical)>
          <LinearLayout (horizontal, 4 个格子)>
          <LinearLayout (horizontal, 4 个格子)>
          <LinearLayout (horizontal, 4 个格子)>
          <LinearLayout (horizontal, 4 个格子)>
        </LinearLayout>
      </FrameLayout>
    </FrameLayout>
    
    <!-- 底部工具栏 -->
    <LinearLayout (match_parent, wrap_content, horizontal, padding="16dp")>
      <ImageButton (上传, 48dp) />
      <ImageButton (换装, 48dp) />
    </LinearLayout>
    
  </LinearLayout>
</ScrollView>
```

## 实施步骤
1. 重写 `activity_main.xml`：铺满宽度 + 棋盘居中 + LinearLayout 替代 GridLayout

## 验证方式
- 在模拟器上确认 UI 铺满屏幕宽度
- 确认棋盘显示 4×4 = 16 格
- 确认棋盘在屏幕中间，上下留白
- 确认底部工具栏紧贴棋盘下方