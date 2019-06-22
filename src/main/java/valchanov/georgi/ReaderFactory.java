package valchanov.georgi;


import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Creates new {@link Runnable} objects that handle incoming messages from {@link SocketChannel}s
 */
public class ReaderFactory extends ShutDownThread {

    /**
     * Used when a new thread is created
     */
    private static String helloMess = "Hello";
    /**
     * Represents a mapping between a sender's id an mail box allocated for an identified user
     */
    private MailOffice mailBoxes;

    private final PreparedStatement registerUser;

    /**
     * Used to retrieve a client id from the database
     */
    private final PreparedStatement getID;

    /**
     * Used to retrieve the client id's of participants in a chat thread
     */
    private final PreparedStatement getParticipants;
    /**
     * Used to save a new message to the database
     */
    private final PreparedStatement saveMessage;

    /**
     * Used to create a new chat thread in the database
     */
    private final PreparedStatement createThread;
    /**
     * Used to retrieve a thread's id
     */
    private final PreparedStatement getThreadID;

    /**
     * Called when a problem with serving the message occurs
     */
    private ErrorConsumer onReadError;

    /**
     * used for creating a new instance of a {@link Message}
     */
    private MessageFactory messageFactory;

    /**
     * Creates a new reader factory
     *
     * @param drivers    of the JDBC connector
     * @param connection specifying the URL of the database
     * @param user       name for identification
     * @param password   for identification
     * @param factory    that creates new messages
     * @throws SQLException           if a database update could not be done
     * @throws ClassNotFoundException if the database drivers are not present
     */
    public ReaderFactory(String drivers, String connection, String user, String password, MessageFactory factory) throws SQLException, ClassNotFoundException {
        messageFactory = factory;

        System.out.println("Connecting to database");
        Class.forName(drivers);
        Connection conn = DriverManager.getConnection(connection, user, password);
        System.out.println("Got connection");


        // create the prepared statements
        System.out.println("Preparing statements");
        getParticipants = conn.prepareStatement("SELECT DISTINCT uid FROM messages WHERE tid = ? AND uid != ?");
        saveMessage = conn.prepareStatement("INSERT INTO messages VALUES(?,?,?,?)");
        getID = conn.prepareStatement("SELECT uid FROM users where uid= ? AND password = ?");

        createThread = conn.prepareStatement("INSERT INTO threads (`tname`) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
        getThreadID = conn.prepareStatement("SELECT tid FROM threads WHERE tname = ?");

        registerUser = conn.prepareStatement("INSERT INTO  users (`password`) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    }

    /**
     * Provides a mail office to be used by the readers
     *
     * @param office to be used by the readers
     */
    void setMailOffice(MailOffice office) {
        this.mailBoxes = office;
    }

    /**
     * Creates a new {@link Runnable} that acts as a message reader
     *
     * @param key    that is ready for reading
     * @param keyOps of the key
     * @return a new {@link Runnable} that acts as a message reader
     */
    public Runnable readFrom(final SelectionKey key, final int keyOps) {
        return () -> {
            Thread.currentThread().setName("Reading message:");

            SocketChannel socket = (SocketChannel) key.channel();
            Message message = null;

            System.out.println("Reading from a channel");
            try {
                message = messageFactory.readFrom(socket);
                //get the proper handler
                switch (message.getType()) {
                    case CONNECT:
                        connectUser(key, message);
                        break;
                    case REGISTER:
                        registerUser(key, message);
                        break;
                    case SEND:
                        relayMessage(message);
                        break;
                    case NEW_THREAD:
                        createThread(message);
                        break;
                    case DISCONNECT:
                        disconnect(key, message);
                        break;
                    case UNKNOWN:
                }
            } catch (IOException | SQLException e) {
                if (onReadError != null) onReadError.accept(key, message, e);

                //terminate the faulty socket
                if (key.isValid()) {
                    try {
                        disconnect(key, e);
                    } catch (IOException e1) {
                        if (onReadError != null) onReadError.accept(key, message, e);
                    }
                }
            }

            //reset the ops
            if (key.isValid()) {
                System.out.println("Reinserting ops in the key");
                key.interestOps(key.interestOps() | keyOps);
                key.selector().wakeup();
            }
        };
    }

    /**
     * Used when a client requests a disconnection from the selector and
     *
     * @param message that was received by a socket
     */
    private void disconnect(SelectionKey key, Message message) throws IOException {
        System.out.println("Disconnecting a client");
        //deallocate resources
        mailBoxes.removeMailBox(message.getSenderID());
        key.channel().close();
    }

    /**
     * Used when reading from a client error occurs so we deallocate their resources
     */
    private void disconnect(SelectionKey key, Exception e) throws IOException {
        System.out.println("Disconnecting a client due to failure: " + e.getMessage());
        //deallocate resources
        mailBoxes.removeMailBox(key);
        key.channel().close();
    }

    /**
     * Registers users by putting their name in the database
     *
     * @param message of registration
     * @throws SQLException if a database update could not be done
     */
    private void registerUser(SelectionKey key, Message message) throws SQLException {
        System.out.println("Registering a new user");
        int id;
        //create an entry of the user in the database
        synchronized (registerUser) {
            registerUser.setString(1, message.getPassword());
            registerUser.executeUpdate();

            //get the auto incremented id
            try (ResultSet rs = registerUser.getGeneratedKeys()) {
                rs.next();
                id = rs.getInt(1);
            }
        }
        //now create a new ArrayDeque so that it gets attached to the selection key
        key.attach(new ConcurrentLinkedQueue<>());
        //create a new mailbox for the client and put it with the mail boxes
        mailBoxes.newMailBox(id, key);

        //sends a message to the client with their id
        Message m = messageFactory.newInstance(message.getType(), id, message.getPassword(), message.getThreadID(), message.getThreadName(), message.getContents());
        mailBoxes.putMessageInBox(id, m);
    }

    /**
     * After validation it allocated resources to a newly connected client.
     * If the validation fails the channel is closed.
     * (keeping a connection alive until validation has a major risk of Dos attacks)
     *
     * @param key     of the connecting client
     * @param message connection message
     * @throws SQLException If a database related error occurs
     */
    private void connectUser(SelectionKey key, Message message) throws SQLException {
        System.out.println("Connecting a user");
        int id;

        //get an id for the client
        try {
            synchronized (getID) {
                System.out.println("id: " + message.getSenderID() + " password: " + message.getPassword());
                getID.setInt(1, message.getSenderID());
                getID.setString(2, message.getPassword());

                //querying the database to see if the combination exist
                try (ResultSet rs = getID.executeQuery()) {
                    rs.next();
                    id = rs.getInt(1);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            //In case the query fails or the client hasn't passed in the correct number of arguments
            throw new SQLException();
        }

        //now create a new ArrayDeque so that it gets attached to the selection key
        key.attach(new ConcurrentLinkedQueue<>());
        //create a new mailbox for the client and put it with the mail boxes
        mailBoxes.newMailBox(id, key);


        //send the message with the id to the new thread
        Message m = messageFactory.newInstance(MessageType.CONNECT, message.getSenderID(), message.getPassword(), message.getThreadID(), message.getThreadName(), "");
        mailBoxes.putMessageInBox(message.getSenderID(), m);
    }

    /**
     * Takes in a message and puts it in the receivers' mail boxes
     *
     * @param message to be dispatched
     * @throws SQLException If a database related error occurs
     */
    private void relayMessage(Message message) throws SQLException {
        System.out.println("Relaying a message");
        int senderID = message.getSenderID();
        //check if the sender has identified himself
        if (!mailBoxes.thereIsBoxOf(senderID)) {
            sendFailingMessage(senderID, "You haven't been connected yet");
            return;
        }

        ArrayList<Integer> participantIds = new ArrayList<>();
        //refer to the database so that you know who is in the thread of the message
        synchronized (getParticipants) {
            getParticipants.setInt(1, message.getThreadID());
            getParticipants.setInt(2, senderID);

            //get the ids of the clients that are in the same thread
            try (ResultSet rs = getParticipants.executeQuery()) {
                getParticipants.clearParameters();

                //collect the ids of the participants
                while (rs.next()) {
                    participantIds.add(rs.getInt(1));
                }
            }
        }

        try {
            //save the message into the database
            saveMessage(message.getSenderID(), message.getThreadID(), message.getDate(), message.getContents());
            //put the message in each receiver's mail box
            mailBoxes.putMessageInBoxes(message, participantIds);
        } catch (SQLException e) {
            // in case the client send a message with an unknown sender id or thread id this break database strains
            sendFailingMessage(message.getSenderID(), "the message could not be delivered");
        }
    }

    /**
     * Creates a new cat thread requested by a client
     *
     * @param message specifying the thread creation
     * @throws SQLException If a database related error occurs
     */
    private void createThread(Message message) throws SQLException {

        String threadName = message.getThreadName();
        boolean tryCreating = false;
        int threadID = 0;

        //first we see if the thread exists
        synchronized (getThreadID) {
            try {
                System.out.println("searching for thread");

                getThreadID.setString(1, message.getThreadName());
                try (ResultSet rs = getThreadID.executeQuery();) {
                    rs.next();
                    threadID = rs.getInt(1);
                }

                System.out.println("Retrieving a chat thread id for a client");
            } catch (SQLException e) {
                // the thread does not exist in the database so create it
                tryCreating = true;
            }
        }

        //if the requested thread is non-existent yet
        if (tryCreating) {
            try {
                synchronized (createThread) {
                    //create it
                    createThread.setString(1, threadName);
                    createThread.executeUpdate();
                    createThread.clearParameters();

                    //get the newly generated id
                    try(ResultSet rs = createThread.getGeneratedKeys()){
                        rs.next();
                        threadID = rs.getInt(1);
                    }
                }
                System.out.println("Creating a new chat thread for a user");
            } catch (SQLException e) {
                sendFailingMessage(message.getSenderID(), "Failed to create the thread");
            }
        }

        //save the message in the database
        int senderIDid = message.getSenderID();

        //send the message with the id to the new thread
        Message m = messageFactory.newInstance(MessageType.NEW_THREAD, senderIDid, "", threadID, threadName, "");
        mailBoxes.putMessageInBox(senderIDid, m);

        //if a new thread was created join the user to it
        if (tryCreating)
            saveMessage(senderIDid, threadID, message.getDate(), helloMess);
    }

    /**
     * Specifies a functionality that would be called when an error occurs while servicing a received message
     *
     * @param onError description fo what is done on an error
     */
    public void onReadError(ErrorConsumer onError) {
        this.onReadError = onError;
    }

    /**
     * Creates a new error message to a related failed attempt
     *
     * @param senderID of the sender
     * @param message  to be sent to the to the client
     */
    private void sendFailingMessage(int senderID, String message) {
        System.out.println("Sending a failure message to user. ID:" + senderID + " message:" + message);

        Message m = messageFactory.newInstance(MessageType.FAILURE, senderID, "", 0, "", message);
        mailBoxes.putMessageInBox(senderID, m);
    }

    /**
     * Saves a new message to the database
     *
     * @param senderID of the sender
     * @param threadID of the thread
     * @param date     of the message
     * @param contents of the message
     * @throws SQLException If a database related error occurs
     */
    private void saveMessage(int senderID, int threadID, long date, String contents) throws SQLException {
        synchronized (saveMessage) {
            Timestamp sqlDate = new Timestamp(date);
            saveMessage.setInt(1, threadID);
            saveMessage.setInt(2, senderID);
            saveMessage.setString(3, contents);
            saveMessage.setTimestamp(4, sqlDate);
            saveMessage.executeUpdate();
            saveMessage.clearParameters();
        }
    }

}
