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
     * Path to log file
     */
    private Path logFile;
    /**
     * date time format of the exceptions logged into the logging file
     */
    private SimpleDateFormat dateFormat;

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

    /**
     * Designate a file to thread exceptions would be logged
     *
     * @param path       of the log file
     * @param dateFormat of the date of the crash
     */
    public void setCrashLogFile(String path, String dateFormat) {
        this.logFile = Paths.get(path);
        this.dateFormat = new SimpleDateFormat(dateFormat);
    }

    /**
     * @param e
     */
    public void log(Exception e) {
        if (logFile != null && Files.exists(logFile)) {
            StringBuilder builder = new StringBuilder();
            //get the time of the crash
            builder
                    .append(dateFormat.format(new Date()))
                    .append(" ")
                    .append(e.getMessage())
                    .append(System.getProperty("line.separator"));

            try {
                //append at the end of the file
                Files.write(logFile, builder.toString().getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e1) {
                // well i really don't know what to do here
                e1.printStackTrace();
            }
        }
    }


}
