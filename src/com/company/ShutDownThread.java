package com.company;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A Thread with the ability to receive shutdown signals.<br>
 * Any class inheriting this thread could have
 * this construction in it's code:<br/>
 * <pre>
 * {@code while(isRunning()){ //do something }}
 * </pre>
 * When the execution comes back around the loop would be exited meaning
 * that the thread is shutting down
 */
public class ShutDownThread extends Thread {
    /**
     * Boolean indicating the state of the server
     */
    private volatile boolean isRunning;

    /**
     * This would log any errors that occur within the execution of the thread
     */
    private Logger logger;


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
        this.interrupt();
        isRunning = false;
    }

    /**
     * Designate a file to thread exceptions would be logged
     *
     * @param path       of the log file
     * @param dateFormat of the date of the crash
     */
    public void setCrashLogFile(String path, String dateFormat) {
        logger = new Logger(path, dateFormat);
        isRunning = false;
    }

    public void start() {
        isRunning = true;
        super.start();
    }

    protected Logger logger() {
        return logger;
    }
}
