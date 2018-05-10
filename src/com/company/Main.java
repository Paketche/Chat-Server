package com.company;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            //bind server
            server.bind(new InetSocketAddress("127.0.0.1", 8085));
            System.out.println("Bound to " + server.getLocalAddress());


            Selector selector = Selector.open();
            //accepting and selecting for now are on the same thread
            // so if there is nothing to accept proceed to select
            server.configureBlocking(false);

            //TODO move selecting to a different thread and make accepting blocking
            System.out.println("Accepting now");
            while (true) {

                //accept without blocking
                SocketChannel client = server.accept();
                if (client != null) {
                    client.configureBlocking(false);

                    System.out.println("Connected to " + client.getRemoteAddress());
                    System.out.println("Now registering to a selector");
                    //register for reading
                    client.register(selector, SelectionKey.OP_READ);
                }

                // get anything
                if (selector.selectNow() > 0) {

                    //System.out.println("Found some boys to read from");
                    Set<SelectionKey> readReady = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = readReady.iterator();

                    //for each readable channel
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if (key.isReadable()) {
                            key.interestOps(0);
                            //System.out.println("got a readable selection key");
                            SocketChannel readyClient = (SocketChannel) key.channel();
                            //set the ops to 0 so when the server while(true) loop comes around
                            // it doesn't get picket up again and shoved into another thread

                            Runnable handler = () -> {
                                //TODO get a distinct buffer for every socket
                                //ByteBuffer buffer = ByteBuffer.allocate(140);

                                Message incomingMessage = new Message(26);
                                try {
                                    //supposed that this try should handle a single message
                                    //System.out.println("starting to read from it");
                                    incomingMessage.readFrom(readyClient);

                                    System.out.println("Length: " + incomingMessage.messageLen);
                                    System.out.println("Sender ID: " + incomingMessage.senderID());
                                    System.out.println("Thread ID: " + incomingMessage.threadID());
                                    System.out.println("Date: " + incomingMessage.getDate());
                                    ByteBuffer b = (ByteBuffer) incomingMessage.body.flip();
                                    System.out.println("Message: ");
                                    while (b.hasRemaining()) {
                                        System.out.print((char) b.get());
                                    }

                                    incomingMessage.sendTo(readyClient);

//                                    int read = readyClient.read(buffer);
                                    //readyClient.read
                                    //System.out.println("read " + read);

//                                    while (read > 0) {
//                                        //socket was closed
//                                        if (read == -1) {
//                                            readyClient.close();
//                                            key.cancel();
//                                            System.out.println("Done reading for " + readyClient.getRemoteAddress());
//                                            return;
//                                        }
//
//                                        buffer.flip();
//                                        while (buffer.hasRemaining())
//                                            System.out.print((char) buffer.get());
//
//                                       // System.out.println("\nClearing a buffer");
//                                        buffer.clear();
//
//                                        //say that the client make small pauses between messages
//                                        //instead of terminating the thread and instantly creating
//                                        //a new one for the message after the pause
//                                        //I'd wait a bit so that there is not need to terminate
//                                        //the read
//
//                                        //if a read had filled up the buffer we assume that the message
//                                        //will be bigger than the buffer so no need to sleep but instead
//                                        //instantly fetch the next bytes from the channel
//                                        if (read < buffer.capacity())
//                                            Thread.sleep(2000);
//
//                                        read = readyClient.read(buffer);

                                    //TODO find a way to not kill the thread if there's a small interval
                                    // between messages
                                } catch (IOException e) {
                                    System.out.println("this is in the worker thread");
                                    e.printStackTrace();
//                            } catch(InterruptedException e){
//                                e.printStackTrace();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                } finally {
                                    System.out.println("\ndestroying thread");
                                    //set the key ops again so that it get's picked up by the server loop
                                    key.interestOps(SelectionKey.OP_READ);
                                }
                            };
                            new Thread(handler).start();
                            iterator.remove();
                        }

                    }
                }
            }
        } catch (
                IOException e)

        {
            System.out.println("this is in the whole server");
            e.printStackTrace();
        }
    }
}
