package com.company;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;

/**
 * Class for watching over many connections.<br>
 * New Sockets to be registered and monitored could use the registerSocket method.
 * <p>
 * The class provide the specification of handlers when a key is selected. Handling could be on
 * either reading or writing. There is no default handlers in place
 * <p>
 * Extensions of the class need to specify how the selection would be done
 */
public abstract class SelectionThread extends ShutDownThread {

    /**
     * Used for selecting channel sockets for reading an writing
     */
    private Selector selector;

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


    public SelectionThread(Selector selector) {
        this.selector = selector;
        this.registerQueue = new ConcurrentLinkedQueue<>();
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
    protected void registerSockets() {
        if (!registerQueue.isEmpty()) {
            SocketChannel chan;
            while ((chan = registerQueue.poll()) != null) {
                System.out.println(Thread.currentThread().getName() + ": registering a new socket");
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
     * Does the selection of the  keys
     *
     * @throws IOException if an error while selecting occurs
     */
    protected abstract void doSelection() throws IOException;

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

    /**
     * Returns the selector
     *
     * @return the selector
     */
    protected Selector selector() {
        return this.selector;
    }

    /**
     * Returns the set functionality on reading
     *
     * @return the set functionality on reading
     */
    protected BiFunction<SelectionKey, Integer, Runnable> onReading() {
        return readable;
    }

    /**
     * Returns the set functionality on writing
     *
     * @return the set functionality on writing
     */
    protected BiFunction<SelectionKey, Integer, Runnable> onWriting() {
        return writable;
    }

}
