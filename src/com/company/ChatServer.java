package com.company;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer extends ShutDownThread {

    /**
     * Address and port of the server
     */
    private InetSocketAddress address;
    /**
     * Socket for accepting new connections
     */
    private ServerSocketChannel serverSocket;

    /**
     * Selector for receiving evens for being ready
     * for reading or writing
     */
    private SelectionThread selectionThread;

    /**
     * Number of simultaneous worker threads
     */
    private int workersNum;
    /**
     * Each active user will have an entry in this map
     * Each active user will have a queue of scheduled messages.
     * When a SocketChannel is ready to be written, the queue would
     * be attached to the selector and read from.
     * The key would be the user'd ID
     */
    private MailOffice activeUserToMessageQueue;


    /**
     *
     */
    private ReaderFactory readers;


    private WriterFactory writers;


    /**
     * creates a new NIO Chat server
     *
     * @param address (IP) of the server
     * @param port    of the server
     */
    public ChatServer(String address, int port, ReaderFactory read, WriterFactory write) {
        this.address = new InetSocketAddress(address, port);

        activeUserToMessageQueue = new MailOffice();
        readers = read;
        writers = write;

        //read it from an article
        workersNum = 2 * (Runtime.getRuntime().availableProcessors() - 1);
    }

    public void run() {

        //try-catch for any kind of exception; it logs that exception
        try {
            //init server socket
            try {
                serverSocket = ServerSocketChannel.open();
                serverSocket.bind(this.address);


                initSelector();


                SocketChannel client;
                while (isRunning()) {
                    //accept a new client connection and register it to the selector
                    client = serverSocket.accept();
                    selectionThread.registerSocket(client);
                }
                //TODO think of sending something like goodbye messages to all clients


            } finally {
                //tear down the selection thread
                selectionThread.interrupt();
                selectionThread.join();
            }
        } catch (Exception e) {
            this.logger().log(e);
        }
    }

    private void initSelector() throws IOException {
        //init executor service and selector thread
        Selector selector = Selector.open();
        ExecutorService messageHandlers = Executors.newFixedThreadPool(workersNum);
        selectionThread = new SelectionThread(selector, messageHandlers);

        //the selector would schedule a new thread only if the key is not taken(don't wait to take it)
        selectionThread.keyValidation(k ->
                ((User) k.attachment()).tryTaking() != null);

        // provide on read and on write commands for the selector
        selectionThread.onReading(readers::readFrom);
        selectionThread.onWriting(writers::writeTo);

        selectionThread.start();
    }

    private void initReaderFactory() {
        readers.setMailOffice(activeUserToMessageQueue);
        readers.onReadError((k, m, e) -> logger().log(e));
    }

    private void initWriterFactory() {
        writers.onWriteError(
                (key, message, exception) -> {
                        User user = ((User) key.attachment());
                        user.setInUse();

                        key.cancel();
                        activeUserToMessageQueue.removeMailBox(key);
                        user.terminate();

                        logger().log(exception);
                }
        );
    }
}
