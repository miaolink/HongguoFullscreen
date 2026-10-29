package com.allinpol.hgfloat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloatService extends Service implements FloatClickCallback {
    private static final String CH = "hg_float";
    private WindowManager wm;
    private ImageView floatView;
    private WindowManager.LayoutParams lp;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private SkipPromptWatcher skipWatcher;
    private F9KeyWatcher f9Watcher;
    private boolean busy;
    private String lastMsg = "待命";

    @Override
    public void onCreate() {
        super.onCreate();
        DebugLog.i("FloatService.onCreate");
        FullscreenTrigger.init(this);
        FullscreenTrigger.prepareAsync(this);
        skipWatcher = new SkipPromptWatcher(this);
        skipWatcher.bindService(this);
        f9Watcher = new F9KeyWatcher(this);
        startAsForeground();
        showFloat();
        skipWatcher.start();
        String a11y = HotkeyEnabler.ensureEnabled(this);
        DebugLog.i("F9 enable: " + a11y);
        f9Watcher.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DebugLog.i("FloatService.onStartCommand");
        if (skipWatcher == null) {
            return START_STICKY;
        }
        if (intent != null && "reload_prefs".equals(intent.getAction())) {
            skipWatcher.restartIfEnabled();
        } else if (!skipWatcher.isRunning() && Prefs.isAutoSkipPrompt(this)) {
            skipWatcher.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        DebugLog.i("FloatService.onDestroy");
        if (skipWatcher != null) {
            skipWatcher.stop();
        }
        if (f9Watcher != null) {
            f9Watcher.stop();
        }
        removeFloat();
        io.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAsForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(
                    CH, "红果全屏", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(c);
        }
        Intent open = new Intent(this, MainActivity.class);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, piFlags);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CH);
        } else {
            b = new Notification.Builder(this);
        }
        Notification n = b.setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setSmallIcon(R.drawable.ic_float_fs)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(1, n);
    }

    private void showFloat() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int size = dp(24);

        ImageView btn = new ImageView(this);
        btn.setImageResource(R.drawable.ic_float_fs);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        btn.setPadding(dp(2), dp(2), dp(2), dp(2));
        btn.setAlpha(0.95f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btn.setElevation(0);
        }

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        lp = new WindowManager.LayoutParams(
                size, size, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = dp(10);
        lp.y = dp(160);

        btn.setOnTouchListener(new FloatDragListener(wm, lp, dp(6), this));
        floatView = btn;
        wm.addView(floatView, lp);
        DebugLog.i("float icon added size=" + size);
    }

    private void removeFloat() {
        if (wm != null && floatView != null) {
            try {
                wm.removeView(floatView);
            } catch (Exception ignored) {
            }
        }
        floatView = null;
    }

    @Override
    public void onFloatClick() {
        DebugLog.i("float CLICK busy=" + busy);
        if (busy) {
            return;
        }
        busy = true;
        if (floatView != null) {
            floatView.setColorFilter(0xFFFFC107);
        }
        io.execute(new TriggerJob(this));
    }

    void onTriggerDone(String msg) {
        busy = false;
        lastMsg = msg;
        DebugLog.i("UI result: " + msg);
        if (floatView != null) {
            boolean ok = msg != null && (msg.equals("OK") || msg.startsWith("OK "));
            if (ok) {
                floatView.clearColorFilter();
            } else {
                floatView.setColorFilter(Color.RED);
                main.postDelayed(new ResetBtnColor(this), 2000);
            }
        }
        updateNotification(msg);
    }

    /** 自动检测到「上滑继续」并跳过 */
    void onAutoSkipDone(String msg) {
        lastMsg = msg;
        DebugLog.i("auto-skip UI: " + msg);
        if (floatView != null) {
            floatView.setColorFilter(0xFF4CAF50);
            main.postDelayed(new ResetBtnColor(this), 800);
        }
        updateNotification("自动跳过: " + msg);
    }

    private void updateNotification(String msg) {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            Intent open = new Intent(this, MainActivity.class);
            int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                piFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getActivity(this, 0, open, piFlags);
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                b = new Notification.Builder(this, CH);
            } else {
                b = new Notification.Builder(this);
            }
            Notification n = b.setContentTitle(getString(R.string.notif_title))
                    .setContentText(msg)
                    .setSmallIcon(R.drawable.ic_float_fs)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .setStyle(new Notification.BigTextStyle().bigText(msg))
                    .build();
            nm.notify(1, n);
        } catch (Exception e) {
            DebugLog.e("notif update fail", e);
        }
    }

    Handler getMainHandler() {
        return main;
    }

    String getLastMsg() {
        return lastMsg;
    }

    void resetBtnColor() {
        if (floatView != null) {
            floatView.clearColorFilter();
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
