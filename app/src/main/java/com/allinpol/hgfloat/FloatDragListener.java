package com.allinpol.hgfloat;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/** 区分拖动 / 短按（仅全屏）。 */
public class FloatDragListener implements View.OnTouchListener {
    private final WindowManager wm;
    private final WindowManager.LayoutParams lp;
    private final FloatClickCallback clickCb;
    private final int touchSlop;

    private float downX, downY;
    private int startX, startY;
    private boolean moved;

    public FloatDragListener(WindowManager wm, WindowManager.LayoutParams lp, int touchSlop,
                             FloatClickCallback clickCb) {
        this.wm = wm;
        this.lp = lp;
        this.touchSlop = touchSlop;
        this.clickCb = clickCb;
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        int action = e.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = e.getRawX();
            downY = e.getRawY();
            startX = lp.x;
            startY = lp.y;
            moved = false;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dx = e.getRawX() - downX;
            float dy = e.getRawY() - downY;
            if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                moved = true;
                lp.x = Math.max(0, startX - Math.round(dx));
                lp.y = Math.max(0, startY + Math.round(dy));
                wm.updateViewLayout(v, lp);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (!moved && action == MotionEvent.ACTION_UP && clickCb != null) {
                clickCb.onFloatClick();
            }
            return true;
        }
        return false;
    }
}
