package io.electrosoft.helloworld;

import com.avira.mavapi.MavapiScanner;

/**
 * Custom Thread class used for storing the scanner instance
 * Also, it can be configured for later features
 */
public class MyThread extends Thread {
    MavapiScanner scanner;

    public MyThread(Runnable r, MavapiScanner scanner) {
        super(r);
        this.scanner = scanner;
    }

    public MavapiScanner getScanner() {
        return scanner;
    }
}

