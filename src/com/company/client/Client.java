package com.company.client;

import com.company.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client {

    private Selector selector;
    private ClientSelectionThread selectorThread;

    private static String noContent = "";
    private static int unknownThread = 0;

    private int senderID = 0;
    private int threadID = 0;
    private SocketChannel socket;
    private InetSocketAddress serverAddress;
    private MessageFactory factory;
    private boolean disconnected;

    private ConcurrentLinkedQueue<Message> outQueue;


    public Client(int id, InetSocketAddress serverAddr, MessageFactory mFact) throws IOException {
        this.senderID = id;
        this.serverAddress = serverAddr;
        this.factory = mFact;

        disconnected = true;
        selector = Selector.open();
        selectorThread = new ClientSelectionThread(selector);
        this.outQueue = new ConcurrentLinkedQueue<>();

        selectorThread.onReading((key, o) -> () -> {
            try {
                Message m = factory.readFrom((SocketChannel) key.channel());
                System.out.println(m.getSenderID() + "." + m.getThreadID() + ": " + m.getContents());
            } catch (IOException e) {
                System.out.println("Sorry something happened: " + e.getMessage());
            }
        });

        selectorThread.onWriting((k, ops) -> () -> {
            try {
                Message m;
                while ((m = outQueue.poll()) != null) {
                    m.sendTo(socket);
                }
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    System.out.println("Sorry something happened: " + e.getMessage());
                }
                System.out.println("Sorry something happened: " + e.getMessage());
            } finally {
                if (k.isValid()) k.interestOps(ops & ~SelectionKey.OP_WRITE);
            }
        });
    }

    public void start() throws IOException {
        socket = SocketChannel.open();
        selectorThread.registerSocket(socket);
        selectorThread.start();
    }

    public void connect(String password) throws IOException {
        if (disconnected) {
            socket.connect(serverAddress);
            disconnected = false;
        }
        Message message = factory.newInstance(MessageType.CONNECT, senderID, password, unknownThread, noContent, noContent);
        message.sendTo(socket);

        message = factory.readFrom(socket);
        System.out.println("IDs" + (message.getSenderID() == senderID ? "" : "don't") + " match");
    }

    public void createThread(String threadName) throws IOException {
        Message message = factory.newInstance(MessageType.NEW_THREAD, senderID, noContent, unknownThread, threadName, noContent);
        message.sendTo(socket);

        //get the id of the thread
        message = factory.readFrom(socket);
        threadID = message.getThreadID();
        System.out.println("New thread id is: " + threadID);
    }

    public void register(String password) throws IOException {
        if (disconnected) {
            socket.connect(serverAddress);
            disconnected = false;
        }
        Message message = factory.newInstance(MessageType.REGISTER, senderID, password, 0, "", "");
        message.sendTo(socket);

        //get response with id
        message = factory.readFrom(socket);
        senderID = message.getSenderID();
        System.out.println("Sender ID: " + senderID);
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
        socket.close();
        selectorThread.join();
    }

    public void sendMessage(String contents) throws IOException {
        Message message = factory.newInstance(MessageType.SEND, senderID, noContent, threadID, noContent, contents);
        outQueue.add(message);

        SelectionKey k = socket.keyFor(selector);
        k.interestOps(k.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        MessageFactory factory = new SimpleMessage();
        System.out.println("enter user id");
        String userID = input.nextLine();

        System.out.println("enter user password");
        String pass = input.nextLine();


        try {
            Client client = new Client(Integer.parseInt(userID), new InetSocketAddress("localhost", 8080), factory);

            client.start();
            System.out.println("enter register or connect");
            String line = input.nextLine();

            if (line.equals("connect")) {
                System.out.println("connecting client");
                client.connect(pass);
            } else if (line.equals("register")) {
                System.out.println("Registering user");
                client.register(pass);
                System.out.println("Connecting user");
                client.connect(pass);
            }

            System.out.println("creating a thread");
            client.createThread("some dumbo");

            while (true) {
                System.out.println("Enter message:");
                line = input.nextLine();
                if (!client.isStillWorking() || line.equals("quit"))
                    break;

                client.sendMessage(line);
            }

            System.out.println("disconnecting");
            client.disconnect();
            client.stop();
        } catch (IOException | InterruptedException e) {
            System.out.println("Sorry something happened: " + e.getMessage());
        }
        System.out.println("Bye.");
    }
}