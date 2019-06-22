package valchanov.georgi.client;

import valchanov.georgi.messages.Message;
import valchanov.georgi.messages.MessageFactory;
import valchanov.georgi.messages.MessageType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class Client implements Runnable {

    private ReadingThread readingThread;

    private CountDownLatch awaitConnection;
    private CountDownLatch awaitThread;

    private static String noContent = "";
    private static int unknownThread = 0;

    private volatile int senderID = -1;
    private volatile int threadID = -1;

    private SocketChannel socket;
    private InetSocketAddress serverAddress;
    private MessageFactory factory;
    private boolean disconnected;

    private Scanner input;

    private boolean loggedin;


    public Client(InetSocketAddress serverAddr, MessageFactory mFact) {

        this.serverAddress = serverAddr;
        this.factory = mFact;

        disconnected = true;
        input = new Scanner(System.in);
        awaitConnection = new CountDownLatch(1);
        awaitThread = new CountDownLatch(1);
    }

    public void start() throws IOException {
        socket = SocketChannel.open();
        if (disconnected) {
            socket.connect(serverAddress);
            disconnected = false;
        }
        readingThread = new ReadingThread(socket, factory);

        readingThread.onReceivingASendMessage(m ->
                System.out.println(m.getSenderID() + "." + m.getThreadID() + ": " + m.getContents())
        );

        readingThread.onReceivingConnectMessage(m -> {
            // signal that you've got a connect message
            System.out.println("waking up connection latch");
            awaitConnection.countDown();
            this.senderID = m.getSenderID();
        });

        readingThread.onReceivingNewThreadMessage(m -> {
            //signal that you've got a thread message
            awaitThread.countDown();
            this.threadID = m.getThreadID();
        });

        readingThread.onReceivingAFailureMessage(m -> {
            // in cases of failed connection or thread obtaining
            awaitThread.countDown();
            awaitConnection.countDown();
            System.out.println(m.getContents());
        });

        readingThread.onReceivingARegisterMessage(m -> {
            //when the we're waing for a registration
            awaitConnection.countDown();
            this.senderID = m.getSenderID();
        });

        readingThread.start();
    }

    public void connect(int id, String password) throws IOException, InterruptedException {
        //reuse latch
        awaitConnection = new CountDownLatch(1);

        Message message = factory.newInstance(MessageType.CONNECT, id, password, unknownThread, noContent, noContent);
        message.sendTo(socket);
        //wait for the connect message
        awaitConnection.await();
        System.out.println("woken up");
    }

    public void createThread(String threadName) throws IOException, InterruptedException {
        //reuse latch
        awaitThread = new CountDownLatch(1);

        Message message = factory.newInstance(MessageType.NEW_THREAD, senderID, noContent, unknownThread, threadName, noContent);
        message.sendTo(socket);
        //wait for the thread message
        awaitThread.await();
    }

    public void register(String password) throws IOException, InterruptedException {
        //reuse latch
        awaitConnection = new CountDownLatch(1);
        Message message = factory.newInstance(MessageType.REGISTER, senderID, password, 0, "", "");
        message.sendTo(socket);

        //wait for the connect message
        awaitConnection.await();
    }

    public boolean isStillWorking() {
        return socket.isOpen();
    }

    public void disconnect() throws IOException {
        Message message = factory.newInstance(MessageType.DISCONNECT, senderID, noContent, unknownThread, noContent, noContent);
        message.sendTo(socket);
        disconnected = true;
    }

    public void stop() throws IOException, InterruptedException {
        readingThread.shutDown();
        readingThread.join();
        socket.close();
    }

    public void sendMessage(String contents) throws IOException {
        Message message = factory.newInstance(MessageType.SEND, senderID, noContent, threadID, noContent, contents);
        message.sendTo(socket);
    }


    public void run() {
        String line;
        boolean connecting;

        try {
            this.start();
            while (!loggedin) {
                System.out.println("Would you like to register or connect(type: 'register' or 'connect')");
                line = input.nextLine();
                switch (line) {
                    case "connect":
                        connecting = true;
                        break;
                    case "register":
                        connecting = false;
                        break;
                    default:
                        System.out.println("Unknown command");
                        continue;
                }

                int id;
                String pass;

                if (!connecting) {
                    pass = requestAndValidate("Enter a password( a new id would be given to you)", 8);
                    if (pass == null) {
                        continue;
                    }

                    this.register(pass);
                    //depending or registration success(on successful login the id is set)
                    if (senderID < 0) continue;
                    else id = senderID;
                } else {
                    try {
                        System.out.println("Enter user id:");
                        id = input.nextInt();

                        pass = requestAndValidate("Enter a password", 8);
                        if (pass == null) {
                            continue;
                        }

                    } catch (NoSuchElementException e) {
                        System.out.println("Bad input");
                        continue;
                    }
                }
                this.connect(id, pass);
                if (this.senderID < 0) {
                    System.out.println("Connection failed");
                    continue;
                }
                System.out.println("Connection successful");
                loggedin = true;
            }
            while (true) {
                String name = requestAndValidate("Name the thread you want to join", 16);
                if (name != null) {
                    this.createThread(name);
                    //successful thread request
                    if (threadID > 0) break;
                }
            }
            System.out.println("Chat await (to quit just type 'quit')");

            while (true) {
                line = input.nextLine();
                if (!isStillWorking() || line.equals("quit"))
                    break;

                sendMessage(line);
            }

            System.out.println("disconnecting");
            disconnect();
            stop();
        } catch (IOException | InterruptedException e) {
            System.out.println("Sorry something happened: " + e.getMessage());
        }
    }

    private String requestAndValidate(String request, int length) {
        String pass;
        System.out.println(request + "(must not be longer than " + length + " characters)");
        pass = input.next();
        if (pass.length() > length) {
            System.out.println("input too long");
            pass = null;
        }
        return pass;
    }
}