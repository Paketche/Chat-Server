package com.company;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleMessage implements Message {

    public static final int HEADER_SIZE = 26;

    private ByteBuffer header;
    private ByteBuffer body;

    private MessageType messageType;
    private int messageLen;
    private byte senderID;
    private short threadID;
    private long sendDate;


    /**
     * Reads a message from a sender
     *
     * @param sender the {@link SocketChannel} that is sending this message
     * @return something dk right now
     * @throws IOException    if something occurs while receiving
     * @throws ParseException if the sender send a datetime with the wrong format
     */
    @Override
    public void readFrom(SocketChannel sender) throws IOException {

        header = ByteBuffer.allocate(HEADER_SIZE);
        //while there is something to read from the buffer continue reading
        if (sender.read(header) < 0) {
            throw new IOException("SimpleMessage header not received");
        }

        //get data from header
        header.flip();
        messageType = translate(header.get());
        messageLen = header.getInt();
        senderID = header.get();
        threadID = header.getShort();
        sendDate = header.getLong();

        //now for the rest of the message
        body = ByteBuffer.allocate(this.messageLen);
        if (sender.read(body) < 0) {
            throw new IOException("SimpleMessage body not received");
        }
    }

    /**
     * Sends the message to a receiving socket
     *
     * @param receiver the {@link SocketChannel} that is going to receive the message
     * @throws IOException if something occurs while sending
     */
    @Override
    public void sendTo(SocketChannel receiver) throws IOException {
        header.flip();
        body.flip();
        receiver.write(new ByteBuffer[]{header, body});
        System.out.println("message sent");
    }

    public MessageType type() {
        return messageType;
    }

    public byte senderID() {
        return senderID;
    }

    public short threadID() {
        return threadID;
    }

    public long getDate() {
        return this.sendDate;
    }

    /**
     * Returns a string representation of the contents of the message
     *
     * @return the message's contents
     */
    @Override
    public String toString() {
        return new String(body.array());
    }

    public MessageType translate(byte code) {
        switch (code) {
            case 0:
                return MessageType.CONNECT;
            case 1:
                return MessageType.SEND;
            case 2:
                return MessageType.NEW_THREAD;
            case 3:
                return MessageType.DISCONNECT;
            default:
                return MessageType.UNKNOWN;
        }
    }
}
