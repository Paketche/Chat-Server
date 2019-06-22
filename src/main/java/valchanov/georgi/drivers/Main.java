package valchanov.georgi.drivers;

import valchanov.georgi.ChatServer;
import valchanov.georgi.ReaderFactory;
import valchanov.georgi.WriterFactory;
import valchanov.georgi.messages.SimpleMessage;

import java.sql.SQLException;

public class Main {

    /**
     * Starts a new chat server instance.
     * the arguments of the function are as followed:
     * <ol>
     * <ul>log file path</ul>
     *
     * <ul>local server address</ul>
     * <ul>local server port</ul>
     *
     * <ul>driver name</ul>
     * <ul>database url</ul>
     *
     * <ul>user</ul>
     * <ul>password</ul
     * </ol>
     * <p>
     * The user and password arguments are optional depending on the database.
     *
     * @param args to the process
     */
    public static void main(String[] args) {

        Thread.currentThread().setName("Driver thread");

        try {
            if (args.length < 5) {
                Main.usage();
            }

            SimpleMessage m = new SimpleMessage();

            ReaderFactory rf = null;
            if (args.length == 5) {
                rf = ReaderFactory.newInstance(args[3], args[4], m);
            } else if (args.length == 7) {
                rf = ReaderFactory.newInstance(args[3], args[4], args[5], args[6], m);
            } else {
                usage();
            }

//            "jdbc:sqlite:" + Paths.get("./db/messages.db").toAbsolutePath();


//            ReaderFactory rf = ReaderFactory.newInstance("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/messages", "root", "SNTmgl134", m);
            WriterFactory wf = new WriterFactory();
            ChatServer cs = new ChatServer(args[1], Integer.parseInt(args[2]), rf, wf);
            cs.setCrashLogFile(args[0], "yyyy.MM.dd G 'at' HH:mm:ss z");

            cs.start();

        } catch (Exception e) {
            System.out.println("A fatal error occurred: "+ e.getMessage());
        }

    }

    private static void usage() {
        System.out.println();
        System.exit(1);
    }
}
