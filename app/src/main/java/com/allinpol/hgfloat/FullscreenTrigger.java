package com.allinpol.hgfloat;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 设备端直接注入：su + frida-inject + 打包了 frida-java-bridge 的脚本。
 * 首次预热脚本到 /data/local/tmp，之后点击只跑注入。
 */
public final class FullscreenTrigger {
    private static final String ASSET_BUNDLE = "hg_fs_bundle.js";
    private static final String REMOTE_INJECT = "/data/local/tmp/frida-inject";
    private static final String REMOTE_BUNDLE = "/data/local/tmp/hg_fs_bundle.js";
    private static final String REMOTE_OUT = "/data/local/tmp/hg_fs_out.txt";
    private static final String PKG = "com.phoenix.read";

    private static Context appCtx;
    private static volatile boolean prepared;

    private FullscreenTrigger() {
    }

    public static void init(Context ctx) {
        if (ctx != null) {
            appCtx = ctx.getApplicationContext();
        }
    }

    /** 服务启动时预热，避免第一次点击才拷贝大脚本。 */
    public static void prepareAsync(final Context ctx) {
        if (ctx == null) {
            return;
        }
        appCtx = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensurePrepared(true);
                } catch (Exception e) {
                    DebugLog.e("prepare fail", e);
                }
            }
        }, "hg-prepare").start();
    }

    public static String trigger() {
        long t0 = System.currentTimeMillis();
        DebugLog.i("===== trigger begin =====");
        try {
            if (appCtx == null) {
                return "未初始化 Context";
            }
            ensurePrepared(false);
            // 单次 su：关掉系统动画（减轻横竖屏切换「转一下」）+ 注入 + 回读
            String cmd = ""
                    + "settings put global window_animation_scale 0;"
                    + "settings put global transition_animation_scale 0;"
                    + "settings put global animator_duration_scale 0;"
                    + "OUT=" + REMOTE_OUT + ";"
                    + "rm -f \"$OUT\";"
                    + "if [ ! -x " + REMOTE_INJECT + " ]; then echo NO_INJECT>\"$OUT\"; exit 2; fi;"
                    + "P=$(pidof " + PKG + ");"
                    + "if [ -z \"$P\" ]; then echo NO_PID>\"$OUT\"; exit 3; fi;"
                    + "echo pid=$P>\"$OUT\";"
                    // -e: 注入后退出，避免后台 wait 拖慢
                    + REMOTE_INJECT + " -p \"$P\" -s " + REMOTE_BUNDLE + " -e >>\"$OUT\" 2>&1;"
                    + "cat \"$OUT\"";

            DebugLog.i("su inject");
            String log = su(cmd, 20000);
            if (log == null) {
                log = "";
            }
            long ms = System.currentTimeMillis() - t0;
            DebugLog.i("done " + ms + "ms log=" + trim(log, 800));

            if (log.contains("NO_INJECT")) {
                return "缺少 " + REMOTE_INJECT;
            }
            if (log.contains("NO_PID")) {
                return "未找到红果进程，请先打开播放页";
            }
            if (log.contains("onClick OK") || log.contains("ok=true")) {
                return "OK " + ms + "ms";
            }
            if (log.contains("NO_HOLDER") || log.contains("NO_K3")) {
                return "未找到播放控件，请进入短剧播放页后再点";
            }
            if (log.contains("ReferenceError") || log.contains("\"type\":\"error\"")) {
                return "注入脚本失败: " + firstError(log);
            }
            if (log.contains("Permission denied") || log.contains("not found")) {
                return "su 失败: " + trim(log, 120);
            }
            return "未确认全屏: " + trim(log.replace('\n', ' '), 180);
        } catch (Exception e) {
            DebugLog.e("trigger fail", e);
            return "失败: " + e.getMessage();
        } finally {
            DebugLog.i("===== trigger end =====");
        }
    }

    private static synchronized void ensurePrepared(boolean force) throws Exception {
        if (prepared && !force) {
            // 远端脚本还在就跳过
            String check = su("[ -f " + REMOTE_BUNDLE + " ] && [ -s " + REMOTE_BUNDLE + " ] && echo OK", 3000);
            if (check != null && check.contains("OK")) {
                return;
            }
        }
        File local = extractAsset(appCtx, ASSET_BUNDLE, ASSET_BUNDLE);
        if (local == null) {
            throw new IllegalStateException("缺少 hg_fs_bundle.js");
        }
        // 只在尺寸变化时覆盖远端，减少无谓拷贝
        String cmd = ""
                + "SRC='" + local.getAbsolutePath() + "';"
                + "DST=" + REMOTE_BUNDLE + ";"
                + "SZ=$(wc -c < \"$SRC\" 2>/dev/null | tr -d ' ');"
                + "DSZ=$(wc -c < \"$DST\" 2>/dev/null | tr -d ' ');"
                + "if [ \"$SZ\" != \"$DSZ\" ]; then cp \"$SRC\" \"$DST\"; fi;"
                + "chmod 644 \"$DST\";"
                + "chmod 755 " + REMOTE_INJECT + " 2>/dev/null;"
                + "echo PREP_OK:$SZ";
        String out = su(cmd, 10000);
        DebugLog.i("prepare: " + trim(out, 200));
        if (out == null || !out.contains("PREP_OK")) {
            // 兜底强拷贝
            su("cp '" + local.getAbsolutePath() + "' " + REMOTE_BUNDLE
                    + " && chmod 644 " + REMOTE_BUNDLE + " && echo PREP_FALLBACK", 10000);
        }
        prepared = true;
    }

    private static File extractAsset(Context ctx, String assetName, String fileName) {
        try {
            File out = new File(ctx.getFilesDir(), fileName);
            InputStream in = ctx.getAssets().open(assetName);
            FileOutputStream fos = new FileOutputStream(out, false);
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
            fos.flush();
            fos.close();
            in.close();
            out.setReadable(true, false);
            DebugLog.i("asset " + out.length() + " bytes");
            return out;
        } catch (Exception e) {
            DebugLog.e("extract asset fail " + assetName, e);
            return null;
        }
    }

    private static String su(String cmd, int timeoutMs) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("su", "-c", cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        final StringBuilder sb = new StringBuilder();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (sb.length() < 12000) {
                            sb.append(line).append('\n');
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        });
        t.start();
        boolean finished = waitFor(p, timeoutMs);
        if (!finished) {
            p.destroy();
            DebugLog.e("su timeout " + timeoutMs);
        }
        t.join(500);
        return sb.toString();
    }

    private static boolean waitFor(Process p, int timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                p.exitValue();
                return true;
            } catch (IllegalThreadStateException e) {
                Thread.sleep(20);
            }
        }
        return false;
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        s = s.trim();
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }

    private static String firstError(String log) {
        int i = log.indexOf("description");
        if (i >= 0) {
            return trim(log.substring(i), 120);
        }
        return trim(log, 120);
    }
}
