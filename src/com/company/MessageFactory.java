package com.company;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Creates new instances of a {@link Message}
 */
public interface MessageFactory {
    /**
     * Creates new instances of the message
     *
     * @return a new message instance
     */
    Message newInstance(MessageType type, int senderID, String password, int threadID, String threadName, String contents);

    /**
     * Reads a message from a sender
     *
     * @param sender the {@link SocketChannel} that is sending this message
     * @throws IOException if something occurs while receiving
     */
    Message readFrom(SocketChannel sender) throws IOException;
}
