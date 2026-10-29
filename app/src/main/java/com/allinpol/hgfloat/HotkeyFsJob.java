package com.allinpol.hgfloat;

import android.content.Context;

public class HotkeyFsJob implements Runnable {
    private final Context app;

    public HotkeyFsJob(Context ctx) {
        this.app = ctx.getApplicationContext();
    }

    @Override
    public void run() {
        try {
            FullscreenTrigger.init(app);
            String msg = FullscreenTrigger.trigger();
            DebugLog.i("F9 result: " + msg);
        } catch (Exception e) {
            DebugLog.e("F9 fail", e);
        }
    }
}
