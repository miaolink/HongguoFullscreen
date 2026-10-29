package com.allinpol.hgfloat;

public class SkipTickWork implements Runnable {
    private final SkipPromptWatcher watcher;

    public SkipTickWork(SkipPromptWatcher watcher) {
        this.watcher = watcher;
    }

    @Override
    public void run() {
        watcher.onTick();
    }
}
