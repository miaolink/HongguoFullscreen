package com.allinpol.hgfloat;

public class ResetBtnColor implements Runnable {
    private final FloatService service;

    public ResetBtnColor(FloatService service) {
        this.service = service;
    }

    @Override
    public void run() {
        service.resetBtnColor();
    }
}
