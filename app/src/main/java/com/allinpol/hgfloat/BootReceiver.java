package com.allinpol.hgfloat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/** 开机后自动拉起悬浮球（可在主界面关闭）。 */
public class BootReceiver extends BroadcastReceiver {
    private static final long DELAY_MS = 5000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        DebugLog.init(context);
        DebugLog.i("BootReceiver action=" + action);
        if (action == null) {
            return;
        }
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)
                && !"com.htc.intent.action.QUICKBOOT_POWERON".equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            return;
        }
        if (!Prefs.isAutoStartBoot(context)) {
            DebugLog.i("boot auto-start disabled");
            return;
        }
        if (!Settings.canDrawOverlays(context)) {
            DebugLog.i("boot skip: no overlay permission");
            return;
        }
        PendingResult pending = goAsync();
        Context app = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(
                new BootStartJob(app, pending), DELAY_MS);
    }

    static void startFloat(Context ctx) {
        try {
            Intent i = new Intent(ctx, FloatService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i);
            } else {
                ctx.startService(i);
            }
            DebugLog.i("boot started FloatService");
        } catch (Exception e) {
            DebugLog.e("boot start FloatService fail", e);
        }
    }
}

class BootStartJob implements Runnable {
    private final Context app;
    private final BroadcastReceiver.PendingResult pending;

    BootStartJob(Context app, BroadcastReceiver.PendingResult pending) {
        this.app = app;
        this.pending = pending;
    }

    @Override
    public void run() {
        try {
            BootReceiver.startFloat(app);
        } finally {
            pending.finish();
        }
    }
}
