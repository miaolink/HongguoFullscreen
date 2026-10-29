package com.allinpol.hgfloat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 共用 su 执行。 */
public final class SuExec {
    private SuExec() {
    }

    public static String run(String cmd, int timeoutMs) {
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            ReadJob reader = new ReadJob(br, sb);
            Thread t = new Thread(reader, "su-read");
            t.start();
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    p.exitValue();
                    break;
                } catch (IllegalThreadStateException e) {
                    Thread.sleep(20);
                }
            }
            if (System.currentTimeMillis() >= deadline) {
                p.destroy();
            }
            t.join(400);
            return sb.toString();
        } catch (Exception e) {
            DebugLog.e("su fail", e);
            return "";
        }
    }

    static final class ReadJob implements Runnable {
        private final BufferedReader br;
        private final StringBuilder sb;

        ReadJob(BufferedReader br, StringBuilder sb) {
            this.br = br;
            this.sb = sb;
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    if (sb.length() < 12000) {
                        sb.append(line).append('\n');
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
