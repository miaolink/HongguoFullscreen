package com.allinpol.hgfloat;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** 全局 F9 → 全屏（与悬浮球点击同一路径）。 */
public class HotkeyService extends AccessibilityService {
    private static final int KEY_F9 = KeyEvent.KEYCODE_F9; // 139

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        DebugLog.init(this);
        FullscreenTrigger.init(this);
        try {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info != null) {
                info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
                setServiceInfo(info);
            }
        } catch (Exception e) {
            DebugLog.e("HotkeyService set flags", e);
        }
        DebugLog.i("HotkeyService connected (F9)");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event == null) {
            return false;
        }
        if (event.getKeyCode() != KEY_F9) {
            return false;
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return true;
        }
        if (event.getRepeatCount() > 0) {
            return true;
        }
        FsHotkeyGate.tryFire(this, "a11y");
        return true;
    }
}
