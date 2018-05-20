package com.company;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    /**
     * provides functionality in the case that the socket is ready to be read from
     */
    private BiFunction<SelectionKey, Integer, Runnable> readable;

    /**
     * provides functionality in the case that the socket is ready to be written to
     */
    private BiFunction<SelectionKey, Integer, Runnable> writable;


    /**
     * create a new Selection thread that selects {@link SocketChannel}s and hands the selected keys off
     * to specified readers and riders
     *
     * @param selector
     * @param handlers
     */
    public SelectionThread(Selector selector, ExecutorService handlers) {
        this.setName("Selector thread");
        this.selector = selector;
        this.handlers = handlers;
        this.registerQueue = new ConcurrentLinkedQueue<>();
    }


    public void run() {
        System.out.println("Selection thread started");
        try {
            for (int i = 0; i < 10; i++) {
                registerSockets();
                doSelection();
            }
//            while (isRunning()) {
//                registerSockets();
//                doSelection();
//            }

            handlers.shutdown();
            selector.close();
        } catch (IOException e) {
            //if the selector is broken
            this.logger().log(e);
            e.printStackTrace();
        }
    }

    /**
     * Add a socket to be registered for reading from.
     *
     * @param socket to be registered
     */
    public void registerSocket(SocketChannel socket) {
        registerQueue.add(socket);
        selector.wakeup();
    }

    /**
     * Goes through the queue of waiting sockets and registers them
     */
    private void registerSockets() {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!there is " + registerQueue.size() + " sockets to register");
        if (!registerQueue.isEmpty()) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!registering new sockets");
            SocketChannel chan;
            while ((chan = registerQueue.poll()) != null) {
                try {
                    chan.configureBlocking(false);
                    chan.register(selector, SelectionKey.OP_READ);
                } catch (IOException e) {
                    //ignore the failure of not registering a socket
                    //it shouldn't affect the rest of the program
                    this.logger().log(e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Does the selection of any ready sockets
     *
     * @throws IOException if the selector is closed or an I/O exception occurs
     */
    private void doSelection() throws IOException {

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!now waiting for selection");
        int selected = selector.select();

        //else the selector what probably woken up for registration
        if (selected > 0) {
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();

                if (!key.isValid()) {
                    continue;
                }
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!got a valid key");

                int ops = key.interestOps();
                Runnable handler = null;

                if (key.isWritable()) {
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!which is writable");
                    handler = writable.apply(key, ops);

                } else if (key.isReadable()) {
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!which is readable");
                    handler = readable.apply(key, ops);
                }
                if (handler != null) {
                    //take the ops so that only one worker thread could work with the selection key
                    //and it doesn't get selected again
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!removing ops and executing handle");
                    key.interestOps(0);
                    handlers.execute(handler);
                }

                iterator.remove();
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!The key is till valid " + key.isValid());
            }
        }
    }

    /**
     * Provide functionality for when a socket has been selected for reading.
     * Have in mind that once the key is selected and validated all of it's ops are removed.
     * So, it's up to the provided functionality to cancel the key or respecify the ops
     *
     * @param function functionality selecting a socket from which it could be read
     */
    public void onReading(BiFunction<SelectionKey, Integer, Runnable> function) {
        this.readable = function;
    }

    /**
     * Provide functionality for when a socket has been selected for writing.
     * Have in mind that once the key is selected and validated all of it's ops are removed.
     * So, it's up to the provided functionality to cancel the key or respecify the ops
     *
     * @param function functionality selecting a socket from which it could be read
     */
    public void onWriting(BiFunction<SelectionKey, Integer, Runnable> function) {
        this.writable = function;
    }
}
