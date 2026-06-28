package com.example.myapplication;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * 自定义 ScrollView：当触摸点在棋盘容器内时，禁止自身拦截事件，
 * 彻底杜绝 ScrollView 和棋盘滑动手势的冲突。
 * 使用屏幕绝对坐标 + 缓存区域，确保判断准确。
 */
public class BoardScrollView extends ScrollView {

    private View boardContainer;
    private final Rect boardRect = new Rect();

    public BoardScrollView(Context context) {
        super(context);
    }

    public BoardScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BoardScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置棋盘容器的引用，用于判断触摸是否落在棋盘区域内。
     */
    public void setBoardContainer(View boardContainer) {
        this.boardContainer = boardContainer;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 如果触摸点落在棋盘区域内，永远不拦截，把事件完整交给棋盘处理
        if (boardContainer != null && isTouchInsideBoard(ev)) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 同样，棋盘区域内的 touch 事件不参与滚动
        if (boardContainer != null && isTouchInsideBoard(ev)) {
            return false;
        }
        return super.onTouchEvent(ev);
    }

    private boolean isTouchInsideBoard(MotionEvent ev) {
        int[] location = new int[2];
        boardContainer.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + boardContainer.getWidth();
        int bottom = top + boardContainer.getHeight();

        float x = ev.getRawX();
        float y = ev.getRawY();

        return x >= left && x <= right && y >= top && y <= bottom;
    }
}
