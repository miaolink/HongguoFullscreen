package com.allinpol.hgfloat;

/**
 * 「上滑继续看短剧 / 下一集」：input swipe + DPAD_DOWN。
 */
public final class DownKeyTrigger {
    private DownKeyTrigger() {
    }

    public static String trigger() {
        long t0 = System.currentTimeMillis();
        DebugLog.i("===== down/swipe begin =====");
        try {
            String cmd = ""
                    + "W=$(wm size | tr -cd '0-9x' | cut -d x -f1);"
                    + "H=$(wm size | tr -cd '0-9x' | cut -d x -f2);"
                    + "if [ -z \"$W\" ] || [ -z \"$H\" ]; then W=3840; H=2160; fi;"
                    + "X=$((W/2));"
                    + "Y1=$((H*3/4));"
                    + "Y2=$((H/5));"
                    + "input keyevent 20;"
                    + "input swipe $X $Y1 $X $Y2 100;"
                    + "echo OK:$W" + "x$H";
            String out = SuExec.run(cmd, 5000);
            long ms = System.currentTimeMillis() - t0;
            DebugLog.i("down/swipe done " + ms + "ms out=" + out);
            if (out != null && out.contains("OK")) {
                return "OK 上滑跳过 " + ms + "ms";
            }
            if (out != null && out.contains("Permission denied")) {
                return "su 失败";
            }
            return "已发送上滑 " + ms + "ms";
        } catch (Exception e) {
            DebugLog.e("down/swipe fail", e);
            return "失败: " + e.getMessage();
        } finally {
            DebugLog.i("===== down/swipe end =====");
        }
    }
}
