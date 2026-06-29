# 撤回确认弹窗

## 概述
点击撤回按钮后弹出确认弹窗，用户确认后才执行撤回操作。

## 修改方案

### 文件：`MainActivity.java`（L94-103）

将当前直接撤回的逻辑改为先弹窗确认：

```java
findViewById(R.id.btn_undo).setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        if (game.canUndo()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("撤回")
                    .setMessage("确定要撤回上一步吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        game.undo();
                        renderBoard();
                        updateScore();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }
});
```

## 验证方式
- 点击撤回按钮，弹出确认弹窗
- 点击"确定"执行撤回
- 点击"取消"不做任何操作
- 无法撤回时（canUndo=false）点击按钮无反应