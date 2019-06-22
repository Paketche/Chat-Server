package valchanov.georgi;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

/**
 * Represents a immutable message
 */
public interface Message {

    /**
     * Sends the message to a receiving socket
     *
     * @param receiver the {@link SocketChannel} that is going to receive the message
     * @throws IOException if something occurs while sending
     */
    void sendTo(SocketChannel receiver) throws IOException;

    /**
     * Returns the {@link MessageType} of the message
     *
     * @return type of the message
     */
    MessageType getType();

    /**
     * Returns a time stamp of the message
     *
     * @return a time stamp of the message
     */
    long getDate();

    /**
     * Returns an id of the sender
     *
     * @return
     */
    int getSenderID();

    /**
     * Returns the password of the client
     *
     * @return the password of the client
     */
    String getPassword();

    /**
     * Returns an id of the message's thread
     *
     * @return an id of the message's thread
     */
    int getThreadID();

    /**
     * Returns the name of a thread
     *
     * @return the name of a thread
     */
    String getThreadName();

    /**
     * Retrieves the contents of the message
     *
     * @return the contents of the message
     */
    String getContents();
}
