package com.company;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface Message {


    /**
     * Reads a message from a sender
     *
     * @param sender the {@link SocketChannel} that is sending this message
     * @throws IOException if something occurs while receiving
     */
    void readFrom(SocketChannel sender) throws IOException;

    /**
     * Sends the message to a receiving socket
     *
     * @param receiver the {@link SocketChannel} that is going to receive the message
     * @throws IOException if something occurs while sending
     */
    void sendTo(SocketChannel receiver) throws IOException;

    MessageType getType();

    long getDate();

    int getSenderID();

    int getThreadID();
}
