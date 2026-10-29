package com.allinpol.hgfloat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/** adb shell am broadcast -a com.allinpol.hgfloat.DEBUG_TRIGGER -n com.allinpol.hgfloat/.DebugReceiver */
public class DebugReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DebugLog.i("DebugReceiver action=" + intent.getAction());
        PendingResult pending = goAsync();
        new Thread(new DebugTriggerJob(context.getApplicationContext(), pending)).start();
    }
}

class DebugTriggerJob implements Runnable {
    private final Context app;
    private final BroadcastReceiver.PendingResult pending;

    DebugTriggerJob(Context app, BroadcastReceiver.PendingResult pending) {
        this.app = app;
        this.pending = pending;
    }

    @Override
    public void run() {
        String msg = FullscreenTrigger.trigger();
        new Handler(Looper.getMainLooper()).post(new DebugToastJob(app, msg, pending));
    }
}

class DebugToastJob implements Runnable {
    private final Context app;
    private final String msg;
    private final BroadcastReceiver.PendingResult pending;

    DebugToastJob(Context app, String msg, BroadcastReceiver.PendingResult pending) {
        this.app = app;
        this.msg = msg;
        this.pending = pending;
    }

    @Override
    public void run() {
        DebugLog.i("DebugReceiver result: " + msg);
        pending.finish();
    }
}
