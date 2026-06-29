# 修复 16 宫格消失问题

## 问题分析

从截图看，棋盘变成了一个扁长的灰色条，16 个格子完全不可见。

**根本原因**：棋盘容器使用了 `layout_height="wrap_content"` + 内部行使用 `layout_height="0dp"` + `layout_weight="1"`。当父容器是 `wrap_content` 时，`layout_weight` 不会生效（因为父容器没有确定的高度来分配权重），导致每行高度为 0，整个棋盘塌陷。

## 修改方案

### 修改文件：`app/src/main/res/layout/activity_main.xml`

将棋盘区域改为固定尺寸（320dp × 320dp），内部每行固定高度（80dp），彻底避免 weight 在 wrap_content 下不生效的问题。

**具体改动**：

1. 棋盘外层 `FrameLayout`（L245-249）：
   - `layout_height` 从 `0dp` + `layout_weight="1"` 改为 `320dp`（固定正方形）
   - 移除 `layout_weight`

2. 棋盘内层 `FrameLayout`（L251-257）：
   - `layout_height` 从 `wrap_content` 改为 `match_parent`

3. 内部 `LinearLayout`（L260-264）：
   - `layout_height` 从 `wrap_content` 改为 `match_parent`

4. 每行 `LinearLayout`（L267, L299, L331, L363）：
   - `layout_height` 从 `0dp` + `layout_weight="1"` 改为 `80dp`（固定高度）
   - 移除 `layout_weight`

5. 每格 `FrameLayout`：
   - 保持 `layout_width="0dp"` + `layout_weight="1"`（在确定的父宽度下 weight 会正常生效）
   - `layout_height` 从 `match_parent` 改为 `match_parent`（不变）

## 验证方式
- 棋盘显示为正方形，包含 4×4 = 16 个格子
- 格子之间有均匀间距
- 棋盘在屏幕中间居中显示