package com.company;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.sql.*;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class ReaderFactory extends ShutDownThread {
    private MailOffice mailBoxes;
    private final PreparedStatement getParticipants;
    private final PreparedStatement saveMessage;
    private final PreparedStatement getID;
    private final PreparedStatement createThread;
    private final PreparedStatement getThreadID;

    private ErrorConsumer onReadError;
    /**
     * used for creating a new instance of a {@link Message}
     */
    private MessageFactory messageFactory;

    public ReaderFactory(String connection, MessageFactory factory) throws SQLException, ClassNotFoundException {
        messageFactory = factory;


        //TODO provide the String
        Class.forName("");
        Connection conn = DriverManager.getConnection(connection);

        getParticipants = conn.prepareStatement("SELECT s_id FROM messages WHERE t_id = ? AND s_id != ?");
        saveMessage = conn.prepareStatement("INSERT INTO messages VALUES(?,?,?,?)");
        getID = conn.prepareStatement("SELECT u_id FROM usr= ? AND pass = ?");

        createThread = conn.prepareStatement("INSERT INTO threads VALUES (?)");
        getThreadID = conn.prepareStatement("SELECT t_id FROM threads WHERE t_name = ?");
    }

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
            Message message = messageFactory.newInstance();

            try {

                message.readFrom(socket);

                //get the proper handler
                switch (message.getType()) {
                    case CONNECT:
                        connectUser(key, message);
                        break;
                    case SEND:
                        relayMessage(message);
                        break;
                    case NEW_THREAD:
                        createThread(key, message);
                        break;
                    case DISCONNECT:
                        break;
                    case UNKNOWN:
                }
            } catch (IOException | SQLException e) {
                //TODO think if i'll be handling the exception here
                onReadError.accept(key, message, e);
                e.printStackTrace();
            } finally {
                //reset the ops
                key.interestOps(keyOps);
            }
        };

    }


    private void connectUser(SelectionKey key, Message message) throws SQLException {
        ResultSet rs;
        //get an id for the client
        try {
            synchronized (getID) {
                //TODO think of a way to read password and user name from the message
                rs = getID.executeQuery();
            }
        } catch (SQLException e) {
            sendFailingMessage(message.getSenderID(), "You failed to connect");
            onReadError.accept(key, message, e);
            e.printStackTrace();
            return;
        }


        rs.next();
        //create a new mailbox for the client and put it with the mail boxes
        mailBoxes.newMailBox(rs.getInt(1), key);

        //now create a new User class so that it get attached to the selection key
        //and provide concurrency mechanisms to the user's queue
        User newClient = new User(new ArrayDeque<>());
        key.attach(newClient);
    }

    private void relayMessage(Message message) throws SQLException {
        int senderID = message.getSenderID();
        //check if the sender has identified himself
        if (mailBoxes.thereIsBoxOf(senderID)) {
            sendFailingMessage(senderID, "You haven't been connected yet");
            return;
        }

        //refer to the database so that you know who is in the thread of the message
        ArrayList<Integer> participantIds = new ArrayList<>();
        ResultSet rs;

        synchronized (getParticipants) {
            getParticipants.setInt(1, message.getThreadID());
            getParticipants.setInt(2, senderID);
            rs = getParticipants.executeQuery();
            //clear for the next one using the statement
            getParticipants.clearParameters();
        }

        //collect the ids of the part
        while (rs.next()) {
            participantIds.add(rs.getInt(1));
        }

        //put the message in each receiver's mail box
        //synchronization is in case someone disconnects while doing messages
        synchronized (mailBoxes) {
            mailBoxes.putMessageInBoxes(message, participantIds);
        }

        //save the message into the database
        synchronized (saveMessage) {
            saveMessage.setInt(1, message.getSenderID());
            saveMessage.setInt(2, message.getThreadID());
            saveMessage.setDate(3, new Date(message.getDate()));
            saveMessage.setString(4, message.toString());
            saveMessage.executeUpdate();
            saveMessage.clearParameters();
        }
    }

    private void createThread(SelectionKey key, Message message) throws SQLException {
        //TODO get the thread name from the message
        synchronized (createThread) {
            try {
                createThread.executeUpdate();
            } catch (SQLException e) {
                sendFailingMessage(message.getSenderID(), "Failed to create the thread");
                onReadError.accept(key, message, e);
                e.printStackTrace();
            }
        }
        synchronized (getThreadID) {
            ResultSet rs = getThreadID.executeQuery();
            rs.next();
            int id = rs.getByte(1);
            //Todo send a message to the client for id of the thread
        }


    }

    public void onReadError(ErrorConsumer onError) {
        this.onReadError = onError;
    }

    private void sendFailingMessage(int senderID, String message) {
        Message m = messageFactory.newInstance(MessageType.FAILURE, senderID, 0, message);
        mailBoxes.putMessageInBoxes(senderID, m);
    }
}
