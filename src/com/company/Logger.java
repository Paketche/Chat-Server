package com.company;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A class used for saving errors onto a log file
 */
public class Logger {
    /**
     * Path to log file
     */
    private final Path logFile;
    /**
     * Date time format of the exceptions logged into the logging file
     */
    private SimpleDateFormat dateFormat;

    /**
     * Create a new logger with a specified path to the log file and a date format for a log
     *
     * @param logFile's  path
     * @param dateFormat of the date that's going to be written next to the exception
     */
    public Logger(String logFile, String dateFormat) {
        this.logFile = Paths.get(logFile);
        this.dateFormat = new SimpleDateFormat(dateFormat);
    }

    /**
     * Logs an exception
     *
     * @param e the exception that is to be logged
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
                synchronized (logFile) {
                    Files.write(logFile, builder.toString().getBytes(), StandardOpenOption.APPEND);
                }
            } catch (IOException e1) {
                // well i really don't know what to do here
                e1.printStackTrace();
            }
        }
    }
}
