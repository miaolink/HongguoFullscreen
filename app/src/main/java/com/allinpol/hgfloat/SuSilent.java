package com.allinpol.hgfloat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 关闭雷电 CM Superuser 对应用的每次 su Toast/通知。
 * 策略库: /data/user_de/0/com.android.settings/databases/su.sqlite
 */
public final class SuSilent {
    private static final String PKG = "com.allinpol.hgfloat";
    private static final String DB =
            "/data/user_de/0/com.android.settings/databases/su.sqlite";
    private static final String SDB =
            "/data/user_de/0/com.android.settings/databases/superuser.sqlite";

    private SuSilent() {
    }

    public static String disablePrompt() {
        String cmd = ""
                + "DB='" + DB + "';"
                + "SDB='" + SDB + "';"
                + "sqlite3 \"$DB\" \"UPDATE uid_policy SET notification=0,policy='allow' "
                + "WHERE package_name='" + PKG + "';\";"
                // 若还没有策略行，插入一条永久允许且不通知
                + "CNT=$(sqlite3 \"$DB\" \"SELECT count(*) FROM uid_policy WHERE package_name='"
                + PKG + "';\");"
                + "if [ \"$CNT\" = \"0\" ]; then "
                + "UID=$(dumpsys package " + PKG + " | grep userId= | head -1 | sed 's/.*userId=\\([0-9]*\\).*/\\1/'); "
                + "sqlite3 \"$DB\" \"INSERT OR REPLACE INTO uid_policy"
                + "(logging,desired_name,username,policy,until,command,uid,desired_uid,package_name,name,notification) "
                + "VALUES(0,'','','allow',0,'',${UID:-10075},0,'" + PKG + "','红果全屏',0);\"; "
                + "fi;"
                + "sqlite3 \"$SDB\" \"INSERT OR REPLACE INTO settings(key,value) VALUES('notifications','0');\";"
                + "echo OK;"
                + "sqlite3 -header -column \"$DB\" "
                + "\"SELECT package_name,policy,notification FROM uid_policy WHERE package_name='"
                + PKG + "';\"";
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (sb.length() < 2000) {
                    sb.append(line).append('\n');
                }
            }
            p.waitFor();
            String out = sb.toString().trim();
            DebugLog.i("SuSilent: " + out.replace('\n', ' '));
            if (out.contains("OK") || out.contains("notification")) {
                return "已关闭授权弹窗";
            }
            return "设置未完成，请稍后重试";
        } catch (Exception e) {
            DebugLog.e("SuSilent fail", e);
            return "设置失败，请确认已授予 root 权限";
        }
    }
}
