package com.company.client;


import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;


public class ClientSelectionThread extends SelectionThread {


    /**
     * create a new Selection thread that selects {@link java.nio.channels.SocketChannel}s and hands the selected keys off
     * to specified readers and riders
     *
     * @param selector
     */
    public ClientSelectionThread(Selector selector) {
        super(selector);
    }

    @Override
    protected void doSelection() throws IOException {

        Selector selector = super.selector();

        if (selector.select() > 0) {
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                if (onWriting() != null && key.isWritable()) {

                    super.onWriting().apply(key, key.interestOps()).run();
                }

                if (onReading() != null && key.isReadable()) {
                    super.onReading().apply(key, key.interestOps()).run();
                }

                keyIterator.remove();
            }
        }
    }
}
