package com.allinpol.hgfloat;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 写 logcat + 应用私有目录。 */
public final class DebugLog {
    public static final String TAG = "HgFloat";
    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static File logFile;

    private DebugLog() {
    }

    public static void init(Context ctx) {
        if (logFile == null && ctx != null) {
            logFile = new File(ctx.getApplicationContext().getFilesDir(), "hgfloat_debug.log");
        }
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
        append("I", msg);
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
        append("E", msg);
    }

    public static void e(String msg, Throwable t) {
        Log.e(TAG, msg, t);
        append("E", msg + " | " + t);
    }

    public static synchronized String readTail(int maxChars) {
        if (logFile == null || !logFile.exists()) {
            return "";
        }
        try {
            long len = logFile.length();
            long start = Math.max(0, len - maxChars);
            FileInputStream in = new FileInputStream(logFile);
            if (start > 0) {
                in.skip(start);
            }
            byte[] buf = new byte[(int) Math.min(maxChars, len)];
            int n = in.read(buf);
            in.close();
            if (n <= 0) {
                return "";
            }
            return new String(buf, 0, n, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(read log fail: " + e.getMessage() + ")";
        }
    }

    private static synchronized void append(String level, String msg) {
        if (logFile == null) {
            return;
        }
        String line = FMT.format(new Date()) + " " + level + " " + msg.replace('\n', ' ') + "\n";
        try {
            FileOutputStream out = new FileOutputStream(logFile, true);
            out.write(line.getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (Exception ignored) {
        }
    }
}
