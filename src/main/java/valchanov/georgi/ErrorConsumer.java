package valchanov.georgi;

import valchanov.georgi.messages.Message;

import java.nio.channels.SelectionKey;


/**
 * Used for when a reading/writing of a message error occurs
 */
@FunctionalInterface
public interface ErrorConsumer {

    void accept(SelectionKey key, Message message, Exception exception);
}
