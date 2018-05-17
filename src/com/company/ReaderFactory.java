package com.company;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ReaderFactory implements Runnable {
    private final TreeMap<Integer, SelectionKey> mailBoxes;
    private Connection conn;
    private final PreparedStatement getParticipants;
    private final PreparedStatement saveMessage;
    private final PreparedStatement getID;

    public ReaderFactory(String connection, TreeMap<Integer, SelectionKey> mailBoxes) throws SQLException, ClassNotFoundException {
        this.mailBoxes = mailBoxes;

        //TODO provide the String
        Class.forName("");
        this.conn = DriverManager.getConnection(connection);
        getParticipants = conn.prepareStatement("SELECT s_id FROM messages WHERE t_id = ? AND s_id != ?");
        saveMessage = conn.prepareStatement("INSERT INTO messages VALUES(?,?,?,?)");
        getID = conn.prepareStatement("SELECT u_id FROM usr= ? AND pas = ?");
    }


    /**
     * creates a new message reader
     *
     * @param key
     * @return
     */
    public Runnable readFrom(final SelectionKey key) {
        return () -> {
            synchronized (key) {
                //check if its not in use already
                if ((key.interestOps() & SelectionKey.OP_READ) == 0)
                    return;
                //turn off read interest so no other thread is able to read from this key
                key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
            }

            SocketChannel socket = (SocketChannel) key.channel();
            SimpleMessage message = new SimpleMessage();

            try {

                message.readFrom(socket);

                //get the proper handler
                switch (message.type()) {
                    case CONNECT:
                        connectUser(key, message);
                        break;
                    case SEND:
                        relayMessage(message);
                        break;
                    case NEW_THREAD:
                        createThread(message);
                        break;
                    case DISCONNECT:
                        break;
                    case UNKNOWN:
                }
            } catch (IOException e) {
                //TODO think if i'll be handling the exception here
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                //add the interest again
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            }
        };

    }


    private void connectUser(SelectionKey key, SimpleMessage message) throws SQLException {
        ResultSet rs;
        //get an id for the client
        try {
            synchronized (getID) {
                //TODO think of a way to read password and user name from the message
                rs = getID.executeQuery();
            }
        } catch (SQLException e) {
            sendFailingMessage(message.senderID(), "You failed to connect");
            e.printStackTrace();
            return;
        }

        //create a new mailbox for the client and put it with the mail boxes
        rs.next();
        key.attach(new ConcurrentLinkedQueue<Message>());
        mailBoxes.put(rs.getInt(1), key);
    }

    private void relayMessage(SimpleMessage message) throws SQLException {
        byte senderID = message.senderID();
        //check if the sender has identified himself
        if (mailBoxes.get(senderID) == null) {
            sendFailingMessage(senderID, "You haven't been connected yet");
            return;
        }

        //refer to the database so that you know who is in the thread of the message
        ArrayList<Integer> participantIds = new ArrayList<>();
        ResultSet rs;

        synchronized (getParticipants) {
            getParticipants.setInt(1, message.threadID());
            getParticipants.setInt(2, senderID);
            rs = getParticipants.executeQuery();
            //clear for the next one using the statement
            getParticipants.clearParameters();
        }

        //collect the ids of the part
        while (rs.next())
            participantIds.add(rs.getInt(1));


        //put the message in each receiver's mail box
        //synchronization is in case someone disconnects while doing messages
        synchronized (mailBoxes) {
            participantIds.stream()
                    .map(mailBoxes::get)
                    .filter(Objects::nonNull)
                    .forEach(d -> {
                        //add a message to the queue of the key and set it up for writing
                        ((Queue<Message>) d.attachment()).add(message);
                        d.interestOps(SelectionKey.OP_WRITE);
                    });
        }

        //save the message into the database
        synchronized (saveMessage) {
            saveMessage.setInt(1, message.senderID());
            saveMessage.setInt(2, message.threadID());
            saveMessage.setDate(3, new Date(message.getDate()));
            saveMessage.setString(4, message.toString());
            saveMessage.executeUpdate();
            saveMessage.clearParameters();
        }
    }

    private void createThread(SimpleMessage message) {


    }

    private void sendFailingMessage(byte senderID, String message) {

    }

    @Override
    public void run() {

    }
}
