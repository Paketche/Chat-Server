package com.company;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class WriterFactory{

    /**
     * A consumer that will be called when an error occurs while writing
     */
    private ErrorConsumer onWriteError;

    /**
     * Creates a runnable object that takes a selection key and writes all of its queued messages.
     * This runnable blocks the access to the selection key's queue once it is acquired.
     * The execution of the runnable is highly dependent on an {@link User} being attacked to the selector
     *
     * @param key    that is writable
     * @param keyOps operation of the selection key
     * @return Runnable that executes the writing operation
     */
    public Runnable writeTo(SelectionKey key, int keyOps) {
        return () -> {
            //get a user's queue
            User user = (User) key.attachment();
            Queue<Message> queue = user.setInUse();

            //if the user has been terminated do nothing more
            //other classes will take care of it
            if (queue != null) {
                return;
            }


            SocketChannel socketChannel = (SocketChannel) key.channel();
            Message m = null;

            //take every message from the queue and send it
            try {
                //stop if the queue is empty

                while ((m = queue.poll()) != null) {
                    m.sendTo(socketChannel);
                }
            } catch (IOException e) {
                onWriteError.accept(key, m, e);
                e.printStackTrace();
            } finally {
                //turn off the writing ops since there are no messages to be written
                key.interestOps(keyOps ^ SelectionKey.OP_WRITE);
                user.makeUsable();
            }
        };
    }

    /**
     * Specify how an error is going to be handled. The thread that the handle works on would still have the key's lock
     *
     * @param onError instructions executed in the case of a writing error
     */
    public void onWriteError(ErrorConsumer onError) {
        this.onWriteError = onError;
    }

}
