package com.company;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.ExecutorService;

public class SelectionThread extends ShutDownThread {

    private Selector selector;
    private ExecutorService handlers;
    private volatile boolean suspended;


    public SelectionThread(Selector selector, ExecutorService handlers) {
        this.selector = selector;
        this.handlers = handlers;
    }


    public void run() {
        try {
            synchronized (selector) {
                while (isRunning()) {
                    int selected = selector.select();
                    if (selected > 0) {

                    } else if (!isRunning()) {
                        //selector was interrupted
                        break;
                    } else {
                        //TODO create a way to suspend selection so that a new channel could be registered
                        while (suspended)
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                // only know interruptions are for terminating the thread
                            }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public void suspendSelection() {
        this.suspended = true;
    }

    /**
     *
     */
    public void resumeSelection() {
        this.suspended = false;
    }
}
