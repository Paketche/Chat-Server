package com.company;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        int headerSize = 31;
        Scanner input = new Scanner(System.in);
        MessageFactory factory = new SimpleMessage();

        int senderID = 0;
        int threadID = 0;

        SocketChannel socket = null;
        try {
            socket = SocketChannel.open();
//            System.out.println("enter hostname");
//            String hostname = input.nextLine();
//            System.out.println("enter port number");
//            int port = input.nextInt();
            socket.connect(new InetSocketAddress("10.32.163.191", 8085));
            System.out.println("connected");

            System.out.println("Allocating buffer");
            ByteBuffer buffer;

            Message r = factory.newInstance(MessageType.REGISTER, senderID, "smtp", threadID, "", "");
            r.sendTo(socket);

            Message uid = factory.readFrom(socket);
            if (uid.getType() == MessageType.REGISTER) {
                senderID = uid.getSenderID();
            }

            System.out.println("Client id is:" + senderID);

            System.out.println("joining thread");
            Message t = factory.newInstance(MessageType.NEW_THREAD, senderID, "smtp", 0, "some twat", "");
            if (t.getType() == MessageType.NEW_THREAD) {
                threadID = t.getThreadID();
            }
            System.out.println("thread id: " + threadID);
//
//            while (true) {
//                String line = input.nextLine();
//                if (!socket.isOpen() || line.equals("quit"))
//                    break;
//
//                Message m = factory.newInstance(MessageType.REGISTER, threadID, "smtp", threadID, "", line);
//                m.sendTo(socket);
//
//
//                //System.out.println(line);
////                buffer = ByteBuffer.allocate(headerSize + lineB.length);
////
////
////                buffer.putInt(lineB.length);
////                buffer.put((byte) 1);
////                buffer.putShort((short) 2);
////                buffer.put(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()).getBytes());
////                buffer.put(lineB);
////                buffer.flip();
////                // System.out.println("buffer was loaded and flipped");
////
////                while (buffer.hasRemaining())
////                    socket.write(buffer);
////
////                System.out.println("message sent. waiting for next message");
////                buffer.clear();
////                System.out.println("going to sleep");
////                Thread.sleep(1000);
////                System.out.println("woke up");
////                socket.configureBlocking(false);
////                int read = socket.read(buffer);
////
////                System.out.println("going to read now: " + read);
////
////                if (read > 0) {
////                    buffer.flip();
////
////                    while (buffer.hasRemaining()) {
////                        System.out.print((char) buffer.get());
////                    }
////                    buffer.clear();
////                }
//            }
        } catch (IOException e) {
            System.out.println("Sorry something happened: " + e.getMessage());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Bye.");
    }
}