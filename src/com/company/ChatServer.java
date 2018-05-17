package com.company;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.TreeMap;
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
     * A pool of message handler threads that will read/write
     * multiple messages from the sockets
     */
    private ExecutorService messageHandlers;
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
    private TreeMap<Integer, SelectionKey> activeUserToMessageQueue;

    /**
     *
     */
    private ReaderFactory readers;


    /**
     * creates a new NIO Chat server
     *
     * @param address (IP) of the server
     * @param port    of the server
     * @param message used for generating new messages, reading incoming messages and writing scheduled ones
     */
    public ChatServer(String address, int port, ReaderFactory factory) {
        this.address = new InetSocketAddress(address, port);
        readers = factory;
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

                //init executor service and selector thread
                Selector selector = Selector.open();
                messageHandlers = Executors.newFixedThreadPool(workersNum);
                selectionThread = new SelectionThread(selector, messageHandlers);

                // provide on read and on write commands for the selector
                selectionThread.onReading(readers::readFrom);
                //TODO add a writing function
                selectionThread.onWriting(null);

                selectionThread.start();

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
            this.log(e);
        }
    }
}
