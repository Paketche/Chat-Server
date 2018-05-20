package com.company;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client {

    private static String noContent = "";
    private static int unknownThread = 0;

    private int senderID = 0;
    private int threadID = 0;
    private SocketChannel socket;
    private InetSocketAddress serverAddress;
    private MessageFactory factory;
    private boolean disconnected;


    public Client(int id, InetSocketAddress serverAddr, MessageFactory mFact) {
        this.senderID = id;
        this.serverAddress = serverAddr;
        this.factory = mFact;

    }

    public void start() throws IOException {
        socket = SocketChannel.open();
    }

    public void connect(String password) throws IOException {
        socket.connect(serverAddress);
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
        socket.connect(serverAddress);
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
    }

    public void stop() throws IOException {
        socket.close();
    }

    public void sendMeesage(String contents) throws IOException {
        Message message = factory.newInstance(MessageType.SEND, senderID, noContent, threadID, noContent, contents);
        message.sendTo(socket);
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        MessageFactory factory = new SimpleMessage();

        Client client = new Client(47, new InetSocketAddress("localhost", 8080), factory);

        try {
            client.start();
            System.out.println("connecting client");
            client.connect("smtp");

            System.out.println("creating a thread");
            client.createThread("some dumbo");

            while (true) {
                System.out.println("Enter message:");
                String line = input.nextLine();
                if (!client.isStillWorking() || line.equals("quit"))
                    break;

                client.sendMeesage(line);
            }

            System.out.println("disconnecting");
            client.disconnect();
            client.stop();
        } catch (IOException e) {
            System.out.println("Sorry something happened: " + e.getMessage());
        }
        System.out.println("Bye.");
    }


}