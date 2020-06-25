package io.electrosoft.helloworld;

import io.electrosoft.helloworld.MavapiExecutor;

public class ScanTask implements Runnable {

    final private String file;

    MavapiExecutor scanExecutor;

    public ScanTask(String file) {
        this.file = file;
    }

    public void setScanExecutor(MavapiExecutor executor) {
        scanExecutor = executor;
    }

    @Override
    public void run() {
        try {
            scanExecutor.doScanning(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
