package com.allinpol.hgfloat;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String NAME = "hgfloat";
    private static final String KEY_BOOT = "auto_start_boot";
    private static final String KEY_SKIP = "auto_skip_prompt";

    private Prefs() {
    }

    private static SharedPreferences sp(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isAutoStartBoot(Context ctx) {
        return sp(ctx).getBoolean(KEY_BOOT, true);
    }

    public static void setAutoStartBoot(Context ctx, boolean on) {
        sp(ctx).edit().putBoolean(KEY_BOOT, on).apply();
    }

    public static boolean isAutoSkipPrompt(Context ctx) {
        return sp(ctx).getBoolean(KEY_SKIP, true);
    }

    public static void setAutoSkipPrompt(Context ctx, boolean on) {
        sp(ctx).edit().putBoolean(KEY_SKIP, on).apply();
    }
}
