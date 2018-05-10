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

        SocketChannel socket = null;
        try {
            socket = SocketChannel.open();
//            System.out.println("enter hostname");
//            String hostname = input.nextLine();
//            System.out.println("enter port number");
//            int port = input.nextInt();
            socket.connect(new InetSocketAddress("127.0.0.1", 8085));

            System.out.println("Allocating buffer");
            ByteBuffer buffer;
            while (true) {
                String line = input.nextLine();
                if (!socket.isOpen() || line.equals("quit"))
                    break;
                byte[] lineB = line.getBytes();
                //System.out.println(line);
                buffer = ByteBuffer.allocate(headerSize + lineB.length);


                buffer.putInt(lineB.length);
                buffer.put((byte) 1);
                buffer.putShort((short) 2);
                buffer.put(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()).getBytes());
                buffer.put(lineB);
                buffer.flip();
                // System.out.println("buffer was loaded and flipped");

                while (buffer.hasRemaining())
                    socket.write(buffer);

                System.out.println("message sent. waiting for next message");
                buffer.clear();
                System.out.println("going to sleep");
                Thread.sleep(1000);
                System.out.println("woke up");
                socket.configureBlocking(false);
                int read = socket.read(buffer);

                System.out.println("going to read now: " + read);

                if (read > 0) {
                    buffer.flip();

                    while (buffer.hasRemaining()) {
                        System.out.print((char) buffer.get());
                    }
                    buffer.clear();
                }
            }
        } catch (IOException e) {
            System.out.println("Sorry something happened: " + e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
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
