package com.company;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Paths;
import java.util.Scanner;

public class Testing {


    public static void main(String[] args) {


        try {
            Selector sel = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);

            client.register(sel, SelectionKey.OP_READ);

            sel.select();

            Runnable reader = () -> {


            };


            Runnable clientSide = () -> {
                try {
                    FileChannel fs = FileChannel.open(Paths.get("/logs/ChatServer.txt"));
                    ByteBuffer buff = ByteBuffer.allocate(1024);
                    fs.read(buff);


                    SocketChannel src = SocketChannel.open();
                    src.connect(new InetSocketAddress("localhost", 8085));


                    src.write(buff);

                    buff.clear();

                    src.read(buff);


                    System.out.println(new String(buff.array()));


                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class Server implements Runnable {
        Selector sel = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        Server(InetSocketAddress addr) throws IOException {
            serverSocketChannel.bind(addr);
        }

        @Override
        public void run() {
            try {
                SocketChannel client = serverSocketChannel.accept();
                client.configureBlocking(false);
                client.register(sel,SelectionKey.OP_READ);

                String line = "";
                while (!line.equals("quit")) {

                    sel.select();

                    for(SelectionKey key: sel.selectedKeys()){
                        if (key.isReadable()){

                        }





                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class Client implements Runnable {

        private SocketChannel socket;

        public Client(InetSocketAddress addr) throws IOException {
            this.socket = SocketChannel.open();
            this.socket.connect(addr);
        }

        @Override
        public void run() {

            ByteBuffer buff = ByteBuffer.allocate(1024);

            Scanner reader = new Scanner(System.in);
            String line = "";
            while (!line.equals("quit")) {

                line = reader.nextLine();

                buff.put(line.getBytes());

                try {
                    buff.flip();
                    socket.write(buff);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                buff.clear();

                try {
                    socket.read(buff);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println(new String(buff.array()));
                buff.clear();
            }
        }
    }


}

