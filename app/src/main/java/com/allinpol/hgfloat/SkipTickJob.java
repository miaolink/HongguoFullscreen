package com.allinpol.hgfloat;

public class SkipTickJob implements Runnable {
    private final SkipPromptWatcher watcher;

    public SkipTickJob(SkipPromptWatcher watcher) {
        this.watcher = watcher;
    }

    @Override
    public void run() {
        // 切到 IO 线程做 su/uiautomator
        new Thread(new SkipTickWork(watcher), "hg-skip").start();
    }
}
