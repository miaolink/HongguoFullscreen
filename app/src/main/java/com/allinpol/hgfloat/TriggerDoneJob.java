package com.allinpol.hgfloat;

public class TriggerDoneJob implements Runnable {
    private final FloatService service;
    private final String msg;

    public TriggerDoneJob(FloatService service, String msg) {
        this.service = service;
        this.msg = msg;
    }

    @Override
    public void run() {
        service.onTriggerDone(msg);
    }
}
