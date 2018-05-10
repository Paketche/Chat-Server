package com.company;

import java.nio.file.Path;

/**
 * A Thread with the ability to receive shutdown signals.<br>
 * Any class inheriting this thread could have
 * this construction in it's code:<br/>
 * <BLOCKQUOTE>
 * {@code while(isRunning()){ //do something }}
 * </BLOCKQUOTE>
 * When the execution comes back around the loop would be exited meaning
 * that the thread is shutting down
 */
public class ShutDownThread extends Thread {
    /**
     * Boolean indicating the state of the server
     */
    private volatile boolean isRunning;

    /**
     * Returns true if the server is operational
     *
     * @return true for a working server
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Sends a shutdown signal to the server
     */
    public void shutDown() {
        isRunning = false;
    }


}
