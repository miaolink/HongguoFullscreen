package com.allinpol.hgfloat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * 轮询界面：出现「上滑继续看短剧」时自动上滑跳过。
 */
public final class SkipPromptWatcher {
    private static final long INTERVAL_MS = 1200L;
    private static final long COOLDOWN_MS = 2800L;
    private static final String UI_XML = "/data/local/tmp/hg_skip_ui.xml";

    private final Context app;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SkipTickJob tickJob = new SkipTickJob(this);
    private volatile boolean running;
    private long lastSkipAt;
    private FloatService service;

    public SkipPromptWatcher(Context ctx) {
        this.app = ctx.getApplicationContext();
    }

    public void bindService(FloatService svc) {
        this.service = svc;
    }

    public void start() {
        if (running) {
            return;
        }
        if (!Prefs.isAutoSkipPrompt(app)) {
            DebugLog.i("SkipWatch disabled");
            return;
        }
        running = true;
        DebugLog.i("SkipWatch start");
        main.post(tickJob);
    }

    public void stop() {
        running = false;
        main.removeCallbacks(tickJob);
        DebugLog.i("SkipWatch stop");
    }

    public void restartIfEnabled() {
        stop();
        start();
    }

    boolean isRunning() {
        return running;
    }

    /** 由后台线程调用 */
    void onTick() {
        if (!running) {
            return;
        }
        try {
            if (!Prefs.isAutoSkipPrompt(app)) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastSkipAt < COOLDOWN_MS) {
                return;
            }
            String cmd = ""
                    + "uiautomator dump " + UI_XML + " >/dev/null 2>&1;"
                    + "if grep -qE '上滑继续|继续看短剧' " + UI_XML + " 2>/dev/null; then echo HIT;"
                    + "else echo MISS; fi";
            String out = SuExec.run(cmd, 9000);
            if (out != null && out.contains("HIT")) {
                DebugLog.i("SkipWatch HIT");
                String r = DownKeyTrigger.trigger();
                lastSkipAt = System.currentTimeMillis();
                DebugLog.i("SkipWatch result " + r);
                final FloatService svc = service;
                final String msg = r;
                if (svc != null) {
                    main.post(new SkipResultJob(svc, msg));
                }
            }
        } catch (Exception e) {
            DebugLog.e("SkipWatch tick fail", e);
        } finally {
            if (running) {
                main.postDelayed(tickJob, INTERVAL_MS);
            }
        }
    }
}
