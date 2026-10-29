package com.allinpol.hgfloat;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

/** root 写入 secure settings，打开 F9 无障碍热键服务。 */
public final class HotkeyEnabler {
    private HotkeyEnabler() {
    }

    public static String componentId() {
        return new ComponentName("com.allinpol.hgfloat",
                "com.allinpol.hgfloat.HotkeyService").flattenToString();
    }

    public static boolean isEnabled(Context ctx) {
        try {
            String enabled = Settings.Secure.getString(
                    ctx.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled == null) {
                return false;
            }
            String mine = componentId();
            TextUtils.SimpleStringSplitter splitter =
                    new TextUtils.SimpleStringSplitter(':');
            splitter.setString(enabled);
            while (splitter.hasNext()) {
                if (mine.equalsIgnoreCase(splitter.next())) {
                    return true;
                }
            }
            // 兼容短名
            return enabled.contains("com.allinpol.hgfloat/.HotkeyService")
                    || enabled.contains("HotkeyService");
        } catch (Exception e) {
            return false;
        }
    }

    /** 尽量用 su 打开；返回说明文字。 */
    public static String ensureEnabled(Context ctx) {
        if (isEnabled(ctx)) {
            return "快捷键 F9 可用";
        }
        String mine = componentId();
        String cur = null;
        try {
            cur = Settings.Secure.getString(
                    ctx.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        } catch (Exception ignored) {
        }
        String next;
        if (cur == null || cur.trim().isEmpty()) {
            next = mine;
        } else if (cur.contains("HotkeyService")) {
            next = cur;
        } else {
            next = cur + ":" + mine;
        }
        // settings 值里的 / 与 : 需原样写入
        String cmd = "settings put secure enabled_accessibility_services '" + next + "';"
                + "settings put secure accessibility_enabled 1;"
                + "echo A11Y_OK";
        String out = SuExec.run(cmd, 4000);
        DebugLog.i("HotkeyEnabler out=" + out);
        if (isEnabled(ctx) || (out != null && out.contains("A11Y_OK"))) {
            return "已开启快捷键 F9";
        }
        return "快捷键未就绪，请在系统设置中开启「红果全屏」";
    }
}
