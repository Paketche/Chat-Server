package com.company;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class WriterFactory {

    /**
     * A consumer that will be called when an error occurs while writing
     */
    private ErrorConsumer onWriteError;

    /**
     * Creates a runnable object that takes a selection key and writes all of its queued messages.
     * This runnable blocks the access to the selection key's queue once it is acquired.
     * The execution of the runnable is highly dependent on an {@link Queue} being attacked to the selector
     *
     * @param key    that is writable
     * @param keyOps operation of the selection key
     * @return Runnable that executes the writing operation
     */
    public Runnable writeTo(SelectionKey key, int keyOps) {
        return () -> {
            Thread.currentThread().setName("Writing message");
            System.out.println("Got a key to write to " + Thread.currentThread().getName() + ". And it is valid" + key.isValid());

            //get a user's queue
            Queue<Message> queue = (Queue<Message>) key.attachment();

            //if the user has been terminated do nothing more
            //other classes will take care of it
            if (queue == null) {
                System.out.println("there is no queue");
                return;
            }


            System.out.println("getting channel");
            SocketChannel socketChannel = (SocketChannel) key.channel();
            Message m = null;

            //take every message from the queue and send it
            try {
                //stop if the queue is empty

                System.out.println("trying to poll");
                while ((m = queue.poll()) != null) {

                    System.out.println("Message is of type: " + m.getType() + ": " + m.getContents());
                    System.out.println("sending to channel");
                    synchronized (m) {
                        m.sendTo(socketChannel);
                    }
                    System.out.println("just wrote a message");
                }
            } catch (IOException e) {
                onWriteError.accept(key, m, e);
                e.printStackTrace();
            } finally {
                System.out.println("just wrote all messages and turning on ops except 'write'");
                //turn off the writing ops since there are no messages to be written
                synchronized (key) {
                    if (key.isValid()) {
                        key.interestOps(keyOps & ~SelectionKey.OP_WRITE);
                        System.out.println("Key ops: " + key.interestOps());
                        key.selector().wakeup();
                    }
                }
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
