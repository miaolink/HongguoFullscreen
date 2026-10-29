package com.allinpol.hgfloat;

import android.content.Context;

/** F9 / 无障碍 / getevent 共用防抖，避免连触发。 */
public final class FsHotkeyGate {
    private static final long COOLDOWN_MS = 1200L;
    private static volatile long lastAt;

    private FsHotkeyGate() {
    }

    public static boolean tryFire(Context ctx, String via) {
        long now = System.currentTimeMillis();
        if (now - lastAt < COOLDOWN_MS) {
            DebugLog.i("F9 skip debounce via=" + via);
            return false;
        }
        lastAt = now;
        DebugLog.i("F9 fire via=" + via);
        FullscreenTrigger.init(ctx);
        new Thread(new HotkeyFsJob(ctx), "hg-f9").start();
        return true;
    }
}
