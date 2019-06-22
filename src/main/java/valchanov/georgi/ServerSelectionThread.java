package valchanov.georgi;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

/**
 * Class for watching over many connections.<br>
 * New Sockets to be registered and monitored could use the registerSocket method.
 * <p>
 * When a socket is selected a handler for it is executed on a new thread. before passing it to the thread.
 * The selection key has all of it's interests removed so that it doesn't get handled by a second thread.
 * So, it is up to the handler to wake up the key's selector and re insert the ops
 * <p>
 * The class provide the specification of handlers when a key is selected. Handling could be on
 * either reading or writing. There is no default handlers in place
 */
public class ServerSelectionThread extends SelectionThread {


    /**
     * Used for dispatching each socket handle to new thread
     * while keeping the active threads to a certain number
     */
    private ExecutorService handlers;


    /**
     * create a new Selection thread that selects {@link SocketChannel}s and hands the selected keys off
     * to specified readers and riders
     *
     * @param selector which will be used for selecting sockets
     * @param handlers thread pool on which the handler are going to be executed
     */
    public ServerSelectionThread(Selector selector, ExecutorService handlers) {
        super(selector);
        this.setName("Selector thread");
        this.handlers = handlers;
    }


    public void run() {
        System.out.println("Selection thread started");
        try {
            while (isRunning()) {
                registerSockets();
                doSelection();
            }

            handlers.shutdown();
            selector().close();
        } catch (IOException e) {
            //if the selector is broken
            this.logger().log(e);
        }
    }


    /**
     * Does the selection of any ready sockets
     *
     * @throws IOException if the selector is closed or an I/O exception occurs
     */
    protected void doSelection() throws IOException {


        int selected = selector().select();

        //else the selector what probably woken up for registration
        if (selected > 0) {
            System.out.println(selected + " keys were selected");
            Iterator<SelectionKey> iterator = selector().selectedKeys().iterator();
            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();

                if (!key.isValid()) {
                    continue;
                }

                int ops = key.interestOps();
                Runnable handler = null;


                if (key.isWritable()) {
                    if (onWriting() != null)
                        handler = onWriting().apply(key, ops);

                } else if (key.isReadable()) {
                    if (onReading() != null)
                        handler = onReading().apply(key, ops);
                }
                if (handler != null) {
                    //take the ops so that only one worker thread could work with the selection key
                    //and it doesn't get selected again
                    key.interestOps(0);
                    System.out.println("Running handler");
                    handlers.execute(handler);
                }

                iterator.remove();
            }
        }
    }
}
