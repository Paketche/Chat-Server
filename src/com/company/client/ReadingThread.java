package com.company.client;

import com.company.Message;
import com.company.MessageFactory;
import com.company.ShutDownThread;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * A class used for receiving a read event on a socket.
 * The class handles the receiving of a few message type
 * which are specifiable.
 */
public class ReadingThread extends ShutDownThread {

    private Selector selector;

    /**
     * used to create a message by reading it
     */
    private final MessageFactory factory;

    //all of these are handler called on receiving a certain type of message
    private Consumer<Message> onConnect;

    private Consumer<Message> onNewThread;

    private Consumer<Message> onSend;

    private Consumer<Message> onFailing;

    private Consumer<Message> onRegister;

    /**
     * Creates a new thread for reading from a socket
     *
     * @param channel that is going to be read from
     * @param factory to create new messages
     * @throws IOException If an error occurs with the channel
     */
    ReadingThread(SocketChannel channel, MessageFactory factory) throws IOException {
        selector = Selector.open();

        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);

        this.factory = factory;
    }

    @Override
    public void run() {
        try {
            while (isRunning()) {

                int selected = selector.select();
                if (selected == 0) continue;// it case it is woken up

                Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();

                while (selectionKeyIterator.hasNext()) {
                    SelectionKey k = selectionKeyIterator.next();

                    if (!k.isValid())
                        continue;

                    if (k.isReadable()) {
                        read(k);
                    }

                    selectionKeyIterator.remove();
                }
            }
        } catch (IOException e) {
            System.out.println("Something happened: " + e.getMessage());
        }

    }

    /**
     * Reads a message form the channel of the selection key and uses the appropriate handler
     *
     * @param key of the channel that is going to be read
     */
    private void read(SelectionKey key) throws IOException {

        Message m = factory.readFrom((SocketChannel) key.channel());

        switch (m.getType()) {
            case NEW_THREAD:
                onNewThread.accept(m);
                break;
            case CONNECT:
                onConnect.accept(m);
                break;
            case SEND:
                onSend.accept(m);
                break;
            case REGISTER:
                onRegister.accept(m);
            case FAILURE:
                onFailing.accept(m);
        }
    }


    /**
     * Handle used on receiving a SEND message
     *
     * @param m
     */
    public void onReceivingASendMessage(Consumer<Message> m) {
        this.onSend = m;
    }

    /**
     * Handle used on receiving a NEW_THREAD message
     *
     * @param m
     */
    public void onReceivingNewThreadMessage(Consumer<Message> m) {
        this.onNewThread = m;
    }

    /**
     * Handle used on receiving a CONNECT message
     *
     * @param m
     */
    public void onReceivingConnectMessage(Consumer<Message> m) {
        this.onConnect = m;
    }

    /**
     * Handle used on receiving a FAILURE message
     *
     * @param m
     */
    public void onReceivingAFailureMessage(Consumer<Message> m) {
        this.onFailing = m;
    }

    /**
     * Handle used on receiving a REGISTER message
     *
     * @param onRegister
     */
    public void onReceivingARegisterMessage(Consumer<Message> onRegister) {
        this.onRegister = onRegister;
    }
}

