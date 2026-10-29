package com.allinpol.hgfloat;

public class F9WatchLoop implements Runnable {
    private final F9KeyWatcher watcher;

    public F9WatchLoop(F9KeyWatcher watcher) {
        this.watcher = watcher;
    }

    @Override
    public void run() {
        watcher.loop();
    }
}
