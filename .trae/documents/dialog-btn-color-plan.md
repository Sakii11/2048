# 弹窗按钮颜色修改计划

## 修改内容

将弹窗（`dialog_confirm.xml`）中两个按钮的背景色改为锤子同款棕色，字体颜色改为白色。

## 具体改动

### 文件：`app/src/main/res/layout/dialog_confirm.xml`

1. **取消按钮**（`dialog_btn_negative`，第39-43行）：
   - `android:background`：`@drawable/bg_score_panel` → `@drawable/bg_btn_hammer`
   - `android:textColor`：`@color/text_warm` → `@color/text_white`

2. **确定按钮**（`dialog_btn_positive`，第49-53行）：
   - `android:background`：`@drawable/bg_btn_undo` → `@drawable/bg_btn_hammer`
   - `android:textColor`：保持不变（已是 `@color/text_white`）

## 所用资源说明

- `bg_btn_hammer.xml`：深棕色渐变圆角背景（`#B5865E` → `#A0724E`，圆角 12dp）—— 已存在，直接引用
- `@color/text_white`：白色 `#FFFFFF` —— 已存在，直接引用

## 验证方式

编译运行，确认弹窗中两个按钮均显示为棕色圆角样式，文字为白色。