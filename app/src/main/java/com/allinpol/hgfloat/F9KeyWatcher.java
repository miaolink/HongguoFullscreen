package com.allinpol.hgfloat;

import android.content.Context;

/**
 * root + getevent 监听 F9（雷电宿主键盘）。
 * 同时匹配 KEY_F9 文本 与 原始码 0001 0043（EV_KEY KEY_F9）。
 */
public final class F9KeyWatcher {
    private final Context app;
    private volatile boolean running;
    private Thread thread;
    private Process process;

    public F9KeyWatcher(Context ctx) {
        this.app = ctx.getApplicationContext();
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(new F9WatchLoop(this), "hg-f9-getevent");
        thread.start();
        DebugLog.i("F9KeyWatcher start");
    }

    public synchronized void stop() {
        running = false;
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception ignored) {
            }
            process = null;
        }
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        SuExec.run("pkill -f 'hg_f9_getevent' 2>/dev/null; echo OK", 2000);
        DebugLog.i("F9KeyWatcher stop");
    }

    boolean isRunning() {
        return running;
    }

    void loop() {
        while (running) {
            try {
                DebugLog.i("F9 getevent attach");
                // 用脚本打标，方便 stop 时 pkill；同时输出 -lq 与原始两路风险大，只用 -q 带标签
                String cmd = "sh -c 'echo hg_f9_getevent; exec getevent -lq'";
                ProcessBuilder pb = new ProcessBuilder("su", "-c", cmd);
                pb.redirectErrorStream(true);
                process = pb.start();
                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String line;
                while (running && (line = br.readLine()) != null) {
                    if (isF9Down(line)) {
                        FsHotkeyGate.tryFire(app, "getevent");
                    }
                }
            } catch (Exception e) {
                if (running) {
                    DebugLog.e("F9 getevent loop", e);
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            } finally {
                if (process != null) {
                    try {
                        process.destroy();
                    } catch (Exception ignored) {
                    }
                    process = null;
                }
            }
        }
    }

    private static boolean isF9Down(String line) {
        if (line == null) {
            return false;
        }
        // /dev/input/eventX: EV_KEY       KEY_F9               DOWN
        if (line.contains("KEY_F9") && line.contains("DOWN")) {
            return true;
        }
        // 原始：0001 0043 00000001 （type=EV_KEY code=KEY_F9 value=DOWN）
        String t = line.toLowerCase();
        return t.contains("0001") && t.contains("0043") && t.contains("00000001");
    }
}
