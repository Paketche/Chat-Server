package com.company;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

/**
 * REpresents an implementation of a chat server message.
 * At the same time it's its own factory
 */
public class SimpleMessage implements Message, MessageFactory {

    private static final byte HEADER_SIZE = 26;
    private static final byte PASSWORD_SIZE = 8;
    private static final byte THREAD_NAME_SIZE = 16;


    //offsets from the begging of the header to the specific information
    private static final byte type = 0;
    private static final byte messageLen = 1;
    private static final byte senderID = 5;
    private static final byte threadID = 6;
    private static final byte sendDate = 8;
    //offsets in the body
    private static final byte pass = 0;
    private static final byte threadName = 0;

    //message related variables
    private ByteBuffer header;
    private ByteBuffer body;

    /**
     * Creates a new Simple message from the given head and buffer
     *
     * @param header of the message
     * @param body   of the message
     */
    private SimpleMessage(ByteBuffer header, ByteBuffer body) {
        this.header = header;
        this.body = body;
        body.mark();
    }

    public SimpleMessage(){

    }

    @Override
    public Message newInstance(MessageType type, int senderID, String pass, int threadID, String threadName, String contents) {
        // get the correct length of the body(thread name and password could be part of the body)
        int messSize = contents.length();
        switch (type) {
            case NEW_THREAD:
                messSize += THREAD_NAME_SIZE;
                break;
            case REGISTER:
            case CONNECT:
                messSize += PASSWORD_SIZE;
        }

        // fill the the head
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.put(translate(type))
                .putInt(messSize)
                .put((byte) senderID)
                .putShort((short) threadID)
                .putLong(System.currentTimeMillis());

        // fill in body
        ByteBuffer body = ByteBuffer.allocate(messSize);
        switch (type) {
            case NEW_THREAD:
                body.put(threadName.getBytes());
                break;
            case REGISTER:
            case CONNECT:
                body.put(pass.getBytes());
        }
        body.put(contents.getBytes());

        return new SimpleMessage(header, body);
    }


    /**
     * Reads a message from a sender
     *
     * @param sender the {@link SocketChannel} that is sending this message
     * @throws IOException if something occurs while receiving
     */
    @Override
    public Message readFrom(SocketChannel sender) throws IOException {

        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        //while there is something to read from the buffer continue reading
        if (sender.read(header) < 0) {
            throw new IOException("SimpleMessage header not received");
        }

        //now for the rest of the message (the length indicates how big the body is)
        ByteBuffer body = ByteBuffer.allocate(header.getInt(messageLen));
        if (sender.read(body) < 0) {
            throw new IOException("SimpleMessage body not received");
        }
        handleOptionals(getType());

        //get them ready for reading from
        header.flip();
        body.reset();
        return new SimpleMessage(body, header);
    }

    /**
     * If a message has any optional values the content of the message is marked in the body buffer
     *
     * @param type
     */
    private void handleOptionals(MessageType type) {
        //marks to where the content should be reset after every time the message is read
        switch (type) {
            case CONNECT:
            case REGISTER:
                body.position(PASSWORD_SIZE);
                break;
            case NEW_THREAD:
                body.position(THREAD_NAME_SIZE);
        }
        body.mark();
    }

    /**
     * Sends the message to a receiving socket.
     *
     * @param receiver the {@link SocketChannel} that is going to receive the message
     * @throws IOException if something occurs while sending
     */
    @Override
    public void sendTo(SocketChannel receiver) throws IOException {
        header.rewind();
        body.reset();
        receiver.write(new ByteBuffer[]{header, body});
    }

    @Override
    public MessageType getType() {
        return translate(header.get(type));
    }

    @Override
    public int getSenderID() {
        return header.get(senderID);
    }

    /**
     * If the message is of types CONNECT or REGISTER, the password would be returned. -1 otherwise.
     *
     * @return password in the form of a long; -1 otherwise
     */
    @Override
    public String getPassword() {
        MessageType t = getType();
        if (t == MessageType.CONNECT || t == MessageType.REGISTER)
            return stringFrom(pass, PASSWORD_SIZE);

        return null;
    }

    @Override
    public int getThreadID() {
        return threadID;
    }

    /**
     * Returns the name of the thread of the message, only if the message is of type NEW_THREAD.
     * Null is returned otherwise
     *
     * @return name if this is a NEW_THREAD message; null otherwise
     */
    @Override
    public String getThreadName() {
        if (getType() == MessageType.NEW_THREAD) {
            return stringFrom(threadName, THREAD_NAME_SIZE);
        }
        return null;
    }

    @Override
    public long getDate() {
        return header.getLong(sendDate);
    }

    @Override
    public String getContents() {
        body.reset();
        //extract the contents skipping passwords and thread names;
        int contentsSize = body.capacity() - body.position();

        return stringFrom(body.position(), contentsSize);
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
                return MessageType.REGISTER;
            case 2:
                return MessageType.SEND;
            case 3:
                return MessageType.NEW_THREAD;
            case 4:
                return MessageType.DISCONNECT;
            case 5:
                return MessageType.FAILURE;
            default:
                return MessageType.UNKNOWN;
        }
    }

    /**
     * Transforms a {@link MessageType} to its ordinal value;
     *
     * @param type to be translated
     * @return value of type( in bytes since the protocol requires it to be)
     */
    private byte translate(MessageType type) {
        return (byte) type.ordinal();
    }

    /**
     * Transforms a slice of the buffer into a string
     *
     * @param position from which the slice starts
     * @param size     of the slice
     * @return a String representation of the slice
     */
    private String stringFrom(int position, int size) {
        byte[] strInBytes = new byte[size];
        body.position(position);
        body.get(strInBytes);
        return new String(strInBytes);
    }
}
