package com.company;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

public class SelectionThread extends ShutDownThread {

    /**
     * Used for selecting channel sockets for reading an writing
     */
    private Selector selector;
    /**
     * Used for dispatching each socket handle to new thread
     * while keeping the active threads to a certain number
     */
    private ExecutorService handlers;
    /**
     * Each socket that needs to be registered on the
     */
    private ConcurrentLinkedQueue<SocketChannel> registerQueue;

    private Function<SelectionKey, Runnable> readable;
    private Function<SelectionKey, Runnable> writable;


    public SelectionThread(Selector selector, ExecutorService handlers) {
        this.setName("Selector thread");
        this.selector = selector;
        this.handlers = handlers;
        this.registerQueue = new ConcurrentLinkedQueue<>();
    }


    public void run() {

        try {
            while (isRunning()) {
                registerSockets();
                doSelection();
            }
            selector.close();
        } catch (IOException e) {
            //if the selector is broken
            this.log(e);
            e.printStackTrace();
            //TODO put a logging method here
        }
    }

    /**
     * @param socket
     */
    public void registerSocket(SocketChannel socket) {
        registerQueue.add(socket);
        selector.wakeup();
    }

    private void registerSockets() {
        if (!registerQueue.isEmpty()) {
            for (SocketChannel chan : registerQueue) {
                try {
                    chan.configureBlocking(false);
                    chan.register(selector, SelectionKey.OP_READ);
                } catch (IOException e) {
                    //ignore the failure of not registering a socket
                    //it shouldn't affect the rest of the program
                    this.log(e);
                    e.printStackTrace();
                }
            }
        }
    }

    private void doSelection() throws IOException {

        int selected = selector.select();

        //else the selector what probably woken up for registration
        if (selected > 0) {
            for (SelectionKey key : selector.selectedKeys()) {
                if (key.isReadable()) {

                    //TODO call an executable handler for reading from the key
                }
                if (key.isWritable()) {
                    //TODO call an executable handler for writing to the key
                }
            }
        }
    }

    public void onReading(Function<SelectionKey, Runnable> function) {
        this.readable = function;
    }

    public void onWriting(Function<SelectionKey, Runnable> function) {
        this.writable = function;
    }
}
