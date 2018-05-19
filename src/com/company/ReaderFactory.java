package com.company;


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

        Class.forName(drivers);
        Connection conn = DriverManager.getConnection(connection, user, password);

        getParticipants = conn.prepareStatement("SELECT s_id FROM messages WHERE t_id = ? AND s_id != ?");
        saveMessage = conn.prepareStatement("INSERT INTO messages VALUES(?,?,?,?)");
        getID = conn.prepareStatement("SELECT u_id FROM usr= ? AND pass = ?");

        createThread = conn.prepareStatement("INSERT INTO threads VALUES (?)", Statement.RETURN_GENERATED_KEYS);
        getThreadID = conn.prepareStatement("SELECT t_id FROM threads WHERE t_name = ?");

        registerUser = conn.prepareStatement("INSERT INTO  users VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    }

    /**
     * Provides a mail office to be used by the readers
     *
     * @param office to be used by the readers
     */
    public void setMailOffice(MailOffice office) {
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
            SocketChannel socket = (SocketChannel) key.channel();
            Message message = null;

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
                onReadError.accept(key, message, e);
                e.printStackTrace();
            }
            //reset the ops
            key.interestOps(keyOps);

        };
    }

    /**
     * Used when a client requests a disconnection from the selector and
     *
     * @param key     readable
     * @param message that was received by a socket
     */
    private void disconnect(SelectionKey key, Message message) throws IOException {
//        Message m = messageFactory.newInstance(message.getType(), message.getSenderID(), message.getPassword(), message.getThreadID(), message.getThreadName(), "Goodbye");
//        mailBoxes.putMessageInBox(m.getSenderID(), m);
        mailBoxes.removeMailBox(message.getSenderID());
        key.channel().close();
        key.cancel();
    }

    /**
     * Registers users by putting their name in the database
     *
     * @param key     of the new client
     * @param message of registration
     * @throws SQLException if a database update could not be done
     */
    private void registerUser(SelectionKey key, Message message) throws SQLException {
        int id;
        //create an entry of the user in the database
        synchronized (registerUser) {
            registerUser.setString(1, message.getPassword());
            ResultSet rs = registerUser.getGeneratedKeys();
            rs.next();
            id = rs.getInt(1);
            rs.close();
        }
        //sends a message to the client with their id
        Message m = messageFactory.newInstance(message.getType(), id, message.getPassword(), message.getThreadID(), message.getThreadName(), message.getContents());
        mailBoxes.newMailBox(id, key);
        mailBoxes.putMessageInBox(id, m);
        key.attach(new ConcurrentLinkedQueue<>());
    }

    /**
     * After validation it allocated resources to a newly connected client.
     * If the validation fails the channel is closed.
     * (keeping a connection alive until validation has a major risk of Dos attacks)
     *
     * @param key     of the connecting client
     * @param message connection message
     * @throws SQLException If a database related error occurs
     * @throws IOException  If an I/O error occur with the socket
     */
    private void connectUser(SelectionKey key, Message message) throws SQLException, IOException {
        int id;
        ResultSet rs;
        //get an id for the client
        try {
            synchronized (getID) {
                getID.setInt(1, message.getSenderID());
                getID.setString(2, message.getPassword());
                rs = getID.executeQuery();

                rs.next();
                id = rs.getInt(1);

                rs.close();
            }
        } catch (SQLException | IndexOutOfBoundsException e) {
            //In case the query fails or the client hasn't passed in the correct number of arguments
            key.channel().close();
            key.cancel();
            e.printStackTrace();
            throw e;
        }

        //create a new mailbox for the client and put it with the mail boxes
        mailBoxes.newMailBox(id, key);
        //now create a new ArrayDeque so that it gets attached to the selection key
        key.attach(new ConcurrentLinkedQueue<>());
    }

    /**
     * Takes in a message and puts it in the receivers' mail boxes
     *
     * @param message to be dispatched
     * @throws SQLException If a database related error occurs
     */
    private void relayMessage(Message message) throws SQLException {
        int senderID = message.getSenderID();
        //check if the sender has identified himself
        if (mailBoxes.thereIsBoxOf(senderID)) {
            sendFailingMessage(senderID, "You haven't been connected yet");
            return;
        }

        ArrayList<Integer> participantIds = new ArrayList<>();
        //refer to the database so that you know who is in the thread of the message
        synchronized (getParticipants) {
            getParticipants.setInt(1, message.getThreadID());
            getParticipants.setInt(2, senderID);

            ResultSet rs = getParticipants.executeQuery();
            getParticipants.clearParameters();

            //collect the ids of the participants
            while (rs.next()) {
                participantIds.add(rs.getInt(1));
            }
            rs.close();
        }

        //put the message in each receiver's mail box
        mailBoxes.putMessageInBoxes(message, participantIds);


        //save the message into the database
        saveMessage(message.getSenderID(), message.getThreadID(), message.getDate(), message.getContents());
    }

    /**
     * Creates a new cat thread requested by a client
     *
     * @param message specifying the thread creation
     * @throws SQLException If a database related error occurs
     */
    private void createThread(Message message) throws SQLException {
        String threadName = message.getContents();
        boolean tryCreating = false;
        int threadID = 0;

        //first we see if the thread exists
        synchronized (getThreadID) {
            try {
                ResultSet rs = getThreadID.executeQuery();

                rs.next();
                threadID = rs.getByte(1);
                rs.close();
            } catch (SQLException e) {
                // the thread does not exist in the database
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
                    ResultSet rs = createThread.getGeneratedKeys();
                    rs.next();
                    threadID = rs.getInt(1);
                    rs.close();
                }
            } catch (SQLException e) {
                sendFailingMessage(message.getSenderID(), "Failed to create the thread");
                e.printStackTrace();
                throw e;
            }
        }

        //save the message in the database
        int senderIDid = message.getSenderID();
        saveMessage(senderIDid, threadID, message.getDate(), helloMess);

        //send the message with the id to the new thread
        Message m = messageFactory.newInstance(MessageType.NEW_THREAD, senderIDid, "", threadID, threadName, "");
        mailBoxes.putMessageInBox(senderIDid, m);

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
            saveMessage.setInt(1, senderID);
            saveMessage.setInt(2, threadID);
            saveMessage.setDate(3, new Date(date));
            saveMessage.setString(4, contents);
            saveMessage.executeUpdate();
            saveMessage.clearParameters();
        }
    }

}
