package valchanov.georgi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A chat server that provides the functionality of reading from and writing to multiple clients
 */
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
    private ServerSelectionThread selectionThread;

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
     * Provides functionality when a socket is ready to be read from
     */
    private ReaderFactory readers;

    /**
     * Provides functionality when a socket is ready to be written to
     */
    private WriterFactory writers;

    /**
     * Creates a new NIO Chat server
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
        super.setName("Chat Server");
    }

    public void run() {

        System.out.println("starting server");
        //try-catch for any kind of exception; it logs that exception
        try {
            try {
                //init server socket
                serverSocket = ServerSocketChannel.open();
                serverSocket.bind(this.address);

                initReaderFactory();
                initWriterFactory();
                initSelector();

                SocketChannel client;
                System.out.println("Waiting for connections");
                while (isRunning()) {
                    //accept a new client connection and register it to the selector
                    client = serverSocket.accept();
                    System.out.println("A new client was connected");
                    selectionThread.registerSocket(client);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            } finally {
                //tear down the selection thread
                selectionThread.shutDown();
                selectionThread.join();
                serverSocket.close();
            }
        } catch (Exception e) {
            this.logger().log(e);
        }
    }

    /**
     * Initializes the thread that is going to be doing the selection
     *
     * @throws IOException if the selector cannot be opened
     */
    private void initSelector() throws IOException {
        //init executor service and selector thread
        Selector selector = Selector.open();
        ExecutorService messageHandlers = Executors.newFixedThreadPool(workersNum);
        selectionThread = new ServerSelectionThread(selector, messageHandlers);

        // provide on read and on write commands for the selector
        selectionThread.onReading(readers::readFrom);
        selectionThread.onWriting(writers::writeTo);

        selectionThread.start();
        System.out.println("starting selector");
    }

    /**
     * Initializes the reader factory
     */
    private void initReaderFactory() {
        readers.setMailOffice(activeUserToMessageQueue);
        readers.onReadError((k, m, e) ->
                logger().log(e)
        );
    }

    /**
     * Initializes the writer factory
     */
    private void initWriterFactory() {
        writers.onWriteError(
                //when a socket could not be written to it is assumed that the client is disconnected
                // so we deallocate its resources
                (key, message, exception) -> {
                    System.out.println(exception.getMessage());
                    try {
                        key.channel().close();
                    } catch (IOException e) {
                       logger().log(e);
                    }
                    activeUserToMessageQueue.removeMailBox(key);
                    logger().log(exception);
                }
        );
    }
}
