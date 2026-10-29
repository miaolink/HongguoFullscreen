package com.allinpol.hgfloat;

import android.app.Application;

public class App extends Application {
    private static App INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        DebugLog.init(this);
        FullscreenTrigger.init(this);
        DebugLog.i("App.onCreate on-device inject build");
    }

    public static App get() {
        return INSTANCE;
    }
}
