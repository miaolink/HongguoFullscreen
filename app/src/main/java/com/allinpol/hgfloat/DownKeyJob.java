package com.allinpol.hgfloat;

public class DownKeyJob implements Runnable {
    private final FloatService service;

    public DownKeyJob(FloatService service) {
        this.service = service;
    }

    @Override
    public void run() {
        final String msg = DownKeyTrigger.trigger();
        service.getMainHandler().post(new TriggerDoneJob(service, msg));
    }
}
