package com.company;

import com.sun.xml.internal.bind.v2.TODO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
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
     * SimpleMessage creator, reader and writer
     */
    private Message messageGenerator;
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
    private TreeMap<Integer, Deque<SimpleMessage>> activeUserToMessageQueue;


    /**
     * Path to the log file used to log crashes
     */
    private Path logFile;

    /**
     * Format of the date with which the server is going to work
     */
    private DateFormat dateFormat;


    /**
     * creates a new NIO Chat server
     *
     * @param address (IP) of the server
     * @param port    of the server
     * @param message used for generating new messages, reading incoming messages and writing scheduled ones
     */
    public ChatServer(String address, int port, Message message) {
        this.address = new InetSocketAddress(address, port);
        this.messageGenerator = message;
        //read it from an article
        workersNum = 2 * (Runtime.getRuntime().availableProcessors() - 1);
    }

    /**
     * Designate a file to which server crashes would be logged
     *
     * @param path       of the log file
     * @param dateFormat of the date of the crash
     */
    public void setCrashLogFile(String path, String dateFormat) {
        this.logFile = Paths.get(path);
        this.dateFormat = new SimpleDateFormat(dateFormat);
    }

    /**
     * @throws IOException if an problem occurs when opening the selector
     */
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
                selectionThread.start();

                SocketChannel client;
                while (isRunning()) {
                    //accept a new client connection and register it to the selector
                    client = serverSocket.accept();
                    client.configureBlocking(false);

                    //wake up the selector so that a new socket could be registered
                    //call the suspend/resume Selection methods so that the selector
                    //doesn't go into selecting between the wake up and register
                    //the part where the
                    synchronized (selector) {
                        selectionThread.suspendSelection();
                        selector.wakeup();
                        client.register(selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
                        selectionThread.resumeSelection();
                        notifyAll();
                    }
                }
                //TODO think of sending something like goodbye messages to all clients


            } finally {
                //tear down the selection thread
                selectionThread.interrupt();
                selectionThread.join();
            }
        } catch (Exception e) {
            //log server crashes if a log file is specified
            if (logFile != null && Files.exists(logFile)) {
                StringBuilder builder = new StringBuilder();
                //get the time of the crash
                builder
                        .append(dateFormat.format(new Date()) + " ")
                        .append(e.getMessage())
                        .append(System.getProperty("line.separator"));

                try {
                    //append at the end of the file
                    Files.write(logFile, builder.toString().getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e1) {
                    // well i really don't know what to do here
                    e1.printStackTrace();
                }
            }
        }
    }


}
