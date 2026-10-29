package com.allinpol.hgfloat;

/** 保留空壳避免旧引用；实际用 SkipResultJob。 */
public class SkipNotifyJob implements Runnable {
    private final String msg;

    public SkipNotifyJob(String msg) {
        this.msg = msg;
    }

    @Override
    public void run() {
        DebugLog.i("SkipNotify " + msg);
    }
}
