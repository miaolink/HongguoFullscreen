package com.allinpol.hgfloat;

public class SkipResultJob implements Runnable {
    private final FloatService service;
    private final String msg;

    public SkipResultJob(FloatService service, String msg) {
        this.service = service;
        this.msg = msg;
    }

    @Override
    public void run() {
        service.onAutoSkipDone(msg);
    }
}
