package com.allinpol.hgfloat;

public class TriggerJob implements Runnable {
    private final FloatService service;

    public TriggerJob(FloatService service) {
        this.service = service;
    }

    @Override
    public void run() {
        final String msg = FullscreenTrigger.trigger();
        service.getMainHandler().post(new TriggerDoneJob(service, msg));
    }
}
