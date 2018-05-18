package com.company;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleMessage implements Message, MessageFactory {

    public static final int HEADER_SIZE = 26;

    private ByteBuffer header;
    private ByteBuffer body;

    private MessageType messageType;
    private int messageLen;
    private byte senderID;
    private short threadID;
    private long sendDate;

    public SimpleMessage() {
    }


    private SimpleMessage(ByteBuffer header, ByteBuffer body) {
        this.header = header;
        this.body = body;
    }

    @Override
    public Message newInstance(MessageType type, int senderID, int threadID, String contents) {
        //type + length + senderID + threadID + date = size of message header in bytes

        int messSize = contents.length();
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        ByteBuffer body = ByteBuffer.allocate(messSize);

        body.put(contents.getBytes());

        header.put(translate(type))
                .putInt(messSize)
                .put((byte) senderID)
                .putShort((short) threadID)
                .putLong(System.currentTimeMillis());


        return new SimpleMessage(header, body);
    }

    @Override
    public Message newInstance() {
        return new SimpleMessage();
    }


    /**
     * Reads a message from a sender
     *
     * @param sender the {@link SocketChannel} that is sending this message
     * @throws IOException    if something occurs while receiving
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

    @Override
    public MessageType getType() {
        return messageType;
    }

    @Override
    public int getSenderID() {
        return senderID;
    }

    @Override
    public int getThreadID() {
        return threadID;
    }

    @Override
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

    /**
     * Transform an ordinal type value to an enum
     *
     * @param code to be translated
     * @return a corresponding enum value; {@link MessageType}.UNKNOWN if it's an unknown value;
     */
    private MessageType translate(byte code) {
        switch (code) {
            case 0:
                return MessageType.CONNECT;
            case 1:
                return MessageType.SEND;
            case 2:
                return MessageType.NEW_THREAD;
            case 3:
                return MessageType.DISCONNECT;
            case 4:
                return MessageType.FAILURE;
            default:
                return MessageType.UNKNOWN;
        }
    }

    /**
     * transforms a {@link MessageType} to its ordinal value;
     *
     * @param type to be translated
     * @return value of type( in bytes since the protocol requires it to be)
     */
    private byte translate(MessageType type) {
        return (byte) type.ordinal();
    }
}
