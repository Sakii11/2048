# 弹窗风格统一（莫兰迪暖色系）

## 概述
将所有 AlertDialog 替换为自定义弹窗，风格与整体 UI 一致：奶油渐变背景、圆角、暖色文字、莫兰迪色系按钮。

## 当前弹窗

| 位置 | 用途 | 按钮 |
|------|------|------|
| 撤回按钮 | 确认撤回 | 确定 / 取消 |
| 设置按钮 | 确认重新开始 | 确定 / 取消 |
| 胜利 | 达成 2048 | 继续 / 重新开始 |
| 失败 | 游戏结束 | 重新开始 |

## 修改方案

### 文件 1：新建 `res/drawable/bg_dialog.xml`

奶油渐变 + 圆角 16dp + 浅阴影，与 `bg_score_panel.xml` 风格一致：

```xml
<layer-list>
    <item android:bottom="2dp" android:right="2dp">
        <shape>
            <solid color="#20000000" />
            <corners android:radius="20dp" />
        </shape>
    </item>
    <item android:bottom="1dp" android:right="1dp">
        <shape>
            <gradient
                android:startColor="@color/cream_gradient_start"
                android:endColor="#FDF3E7"
                android:angle="270" />
            <corners android:radius="20dp" />
        </shape>
    </item>
</layer-list>
```

### 文件 2：新建 `res/layout/dialog_confirm.xml`

自定义弹窗布局，包含标题、消息、两个按钮：

```xml
<LinearLayout
    android:background="@drawable/bg_dialog"
    android:padding="24dp"
    android:orientation="vertical"
    android:minWidth="260dp">
    
    <TextView (标题) — textColor="@color/text_warm", textSize="18sp", bold
    <TextView (消息) — textColor="@color/text_score_label", textSize="14sp", padding="12dp"
    <LinearLayout (水平) — 两个按钮
        <Button (取消) — bg_score_panel 风格, text_color=text_warm
        <Button (确定) — bg_btn_undo 红色风格, text_color=text_white
    </LinearLayout>
</LinearLayout>
```

### 文件 3：修改 `MainActivity.java`

**改动 1**：新增 `showCustomDialog()` 通用方法

```java
private void showCustomDialog(String title, String message, 
        String positiveText, String negativeText,
        Runnable onPositive, Runnable onNegative) {
    Dialog dialog = new Dialog(this);
    View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
    dialog.setContentView(view);
    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    
    TextView tvTitle = view.findViewById(R.id.dialog_title);
    TextView tvMessage = view.findViewById(R.id.dialog_message);
    Button btnPositive = view.findViewById(R.id.dialog_btn_positive);
    Button btnNegative = view.findViewById(R.id.dialog_btn_negative);
    
    tvTitle.setText(title);
    tvMessage.setText(message);
    btnPositive.setText(positiveText);
    btnNegative.setText(negativeText);
    
    btnPositive.setOnClickListener(v -> { dialog.dismiss(); onPositive.run(); });
    btnNegative.setOnClickListener(v -> { dialog.dismiss(); if (onNegative != null) onNegative.run(); });
    
    dialog.show();
}
```

**改动 2**：替换 4 处 AlertDialog 为 `showCustomDialog()`

- 撤回确认：`showCustomDialog("撤回", "确定要撤回上一步吗？", "确定", "取消", () -> {...}, null)`
- 重新开始：`showCustomDialog("2048", "重新开始游戏？", "确定", "取消", () -> {...}, null)`
- 胜利弹窗：`showCustomDialog("恭喜！", "你达成了 2048！\n是否继续游戏？", "继续", "重新开始", () -> {...}, () -> {...})`
- 失败弹窗：`showCustomDialog("游戏结束", "没有可移动的格子了！\n得分：" + score, "重新开始", null, () -> {...}, null)` — 单按钮时隐藏取消按钮

## 实施步骤

1. 新建 `bg_dialog.xml`
2. 新建 `dialog_confirm.xml`
3. 修改 `MainActivity.java`（添加 showCustomDialog + 替换 4 处弹窗）

## 验证方式
- 所有弹窗背景为奶油渐变、圆角
- 文字和按钮颜色与整体风格一致
- 弹窗外区域半透明遮罩
- 点击确定/取消正常执行对应逻辑