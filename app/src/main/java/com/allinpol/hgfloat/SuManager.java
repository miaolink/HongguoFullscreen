package com.allinpol.hgfloat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** 打开系统 / 常见 Root 授权管理界面。 */
public final class SuManager {
    private static final String[][] CANDIDATES = {
            {"com.android.settings", "com.android.settings.Settings$SuperuserActivity"},
            {"com.android.settings", "com.android.settings.cyanogenmod.superuser.PolicyActivity"},
            {"eu.chainfire.supersu", "eu.chainfire.supersu.MainActivity"},
            {"com.noshufou.android.su", "com.noshufou.android.su.Su"},
            {"com.koushikdutta.superuser", "com.koushikdutta.superuser.MainActivity"},
            {"com.topjohnwu.magisk", "com.topjohnwu.magisk.ui.MainActivity"},
            {"io.github.vvb2060.magisk", "com.topjohnwu.magisk.ui.MainActivity"},
    };

    private SuManager() {
    }

    public static boolean open(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        for (int i = 0; i < CANDIDATES.length; i++) {
            String pkg = CANDIDATES[i][0];
            String cls = CANDIDATES[i][1];
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(pkg, cls));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (intent.resolveActivity(pm) != null) {
                    ctx.startActivity(intent);
                    DebugLog.i("open SU manager " + pkg + "/" + cls);
                    return true;
                }
            } catch (Exception e) {
                DebugLog.e("try SU " + pkg + " fail: " + e.getMessage());
            }
        }
        // 再试：按包名启动 Magisk / SuperSU
        String[] pkgs = {
                "com.android.settings",
                "eu.chainfire.supersu",
                "com.topjohnwu.magisk",
                "io.github.vvb2060.magisk",
        };
        for (int i = 0; i < pkgs.length; i++) {
            try {
                Intent launch = pm.getLaunchIntentForPackage(pkgs[i]);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(launch);
                    DebugLog.i("open package launcher " + pkgs[i]);
                    return true;
                }
            } catch (Exception e) {
                DebugLog.e("launch pkg " + pkgs[i] + " " + e.getMessage());
            }
        }
        return false;
    }
}
