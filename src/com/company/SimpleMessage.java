package com.company;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Represents an implementation of a chat server message.
 * At the same time it's its own factory.
 * <p>
 * A simple implantation of the message interface.
 * the message has a header that contains:
 * <pre>
 * message type(byte)
 * message Length(int)
 * senderID(byte)
 * threadID(short)
 * send date(contain as a timestamp (long))
 * </pre>
 * <p>
 * the body contains optional field like a password or a chat thread name
 * for when a thread is created or when a CONNECT message is sent
 * <p>
 * <p>
 * The Class also doubles as it's own factory being able to create a message by reading from a socket channel
 * or by filling in the fields in the new instance method. The method takes care of the optional fields
 * if they are not needed
 */
public class SimpleMessage implements Message, MessageFactory {

    private static final byte HEADER_SIZE = 16;
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
    }

    /**
     * Use for creating a factory for this message type
     */
    public SimpleMessage() {

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
                threadName = padString(threadName, THREAD_NAME_SIZE);
                body.put(threadName.getBytes());
                break;
            case REGISTER:
            case CONNECT:
                pass = padString(pass, PASSWORD_SIZE);
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

        return new SimpleMessage(header, body);
    }

    /**
     * Sends the message to a receiving socket.
     *
     * @param receiver the {@link SocketChannel} that is going to receive the message
     * @throws IOException if something occurs while sending
     */
    @Override
    public void sendTo(SocketChannel receiver) throws IOException {
        header.flip();
        body.flip();

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
            //remove the padding of the password
            return stringFrom(pass, PASSWORD_SIZE).trim();

        return null;
    }

    @Override
    public int getThreadID() {
        return header.getShort(threadID);
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
            //remove the padding of the thread name
            return stringFrom(threadName, THREAD_NAME_SIZE).trim();
        }
        return null;
    }

    @Override
    public long getDate() {
        return header.getLong(sendDate);
    }

    /**
     * Returns a string only if the message is a SEND message; Null otherwise
     *
     * @return a string if the message is of type SEND; null otherwise
     */
    @Override
    public String getContents() {
        if (!(getType() == MessageType.SEND)) {
            return "";
        }

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
        return (byte) (type.ordinal());
    }

    /**
     * Transforms a slice of the buffer into a string
     *
     * @param position from which the slice starts
     * @param size     of the slice
     * @return a String representation of the slice
     */
    private String stringFrom(int position, int size) {
        body.position(position);

        byte[] strInBytes = new byte[size];
        body.get(strInBytes);

        return new String(strInBytes);
    }

    /**
     * used for padding of field of variable length
     *
     * @param tobepaded string to be padded
     * @param size      to which it should be padded
     * @return padded string
     */
    private String padString(String tobepaded, int size) {
        StringBuilder b = new StringBuilder(tobepaded);

        while (b.length() < size) b.append(" ");

        return b.toString();
    }
}
