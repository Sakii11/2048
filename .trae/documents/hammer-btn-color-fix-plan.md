# 弹窗按钮颜色不生效原因分析与修复计划

## 问题分析

`dialog_confirm.xml` 中两个按钮已正确引用 `@drawable/bg_btn_hammer`，但运行时颜色没有变化。

**根本原因**：项目使用 `Theme.Material3.DayNight.NoActionBar` 作为父主题。Material 主题中，`Button` 控件有默认的 `backgroundTint` 属性，它会覆盖 `android:background` 设置的 drawable 颜色，导致自定义的背景色不生效。

## 修复方案

在 `dialog_confirm.xml` 中两个按钮上添加 `android:backgroundTint="@null"`，显式清除 Material 主题的默认背景色调覆盖。

## 具体改动

### 文件：`app/src/main/res/layout/dialog_confirm.xml`

1. **取消按钮**（第34-43行）：添加 `android:backgroundTint="@null"`
2. **确定按钮**（第45-55行）：添加 `android:backgroundTint="@null"`

## 验证方式

1. Clean Project → Rebuild Project（清除缓存后编译）
2. 卸载手机上已安装的旧 APK → 重新运行安装
3. 检查弹窗按钮是否显示为棕色渐变圆角样式