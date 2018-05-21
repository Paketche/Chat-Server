package com.company;

import com.company.client.SelectionThread;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ServerSelectionThread extends SelectionThread {


    /**
     * Used for dispatching each socket handle to new thread
     * while keeping the active threads to a certain number
     */
    private ExecutorService handlers;


    /**
     * create a new Selection thread that selects {@link SocketChannel}s and hands the selected keys off
     * to specified readers and riders
     *
     * @param selector
     * @param handlers
     */
    public ServerSelectionThread(Selector selector, ExecutorService handlers) {
        super(selector);
        this.setName("Selector thread");
        this.handlers = handlers;
    }


    public void run() {
        System.out.println("Selection thread started");
        try {
            while (isRunning()) {
                registerSockets();
                doSelection();
            }

            handlers.shutdown();
            selector().close();
        } catch (IOException e) {
            //if the selector is broken
            this.logger().log(e);
            e.printStackTrace();
        }
    }


    /**
     * Does the selection of any ready sockets
     *
     * @throws IOException if the selector is closed or an I/O exception occurs
     */
    protected void doSelection() throws IOException {

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!now waiting for selection");
        int selected = selector().select();

        //else the selector what probably woken up for registration
        if (selected > 0) {
            Iterator<SelectionKey> iterator = selector().selectedKeys().iterator();
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
                    if (onWriting() != null)
                        handler = onWriting().apply(key, ops);

                } else if (key.isReadable()) {
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!which is readable");

                    if (onReading() != null)
                        handler = onReading().apply(key, ops);
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
}
