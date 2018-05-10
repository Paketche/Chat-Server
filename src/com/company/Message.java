package com.company;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {

    public static final String DATE_FIELD_FORMAT = "yyyy-MM-dd hh:mm:ss";
    public static final int DATE_FIELD_LENGTH = 19;

    private int headersize;
    private ByteBuffer header;
    ByteBuffer body;

    int messageLen;
    private byte senderID;
    private short threadID;


    private DateFormat dateFormat;
    private Date sendDate;


    /**
     * Creates a new message
     *
     * @param headerSize
     */
    public Message(int headerSize) {
        //TODO find a way to extract a a length of the message from the message header as well as date and thread
        this.headersize = headerSize;
        this.dateFormat = new SimpleDateFormat(DATE_FIELD_FORMAT);
    }

    /**
     * Reads a message from a sender
     *
     * @param sender the {@link SocketChannel} that is sending this message
     * @return something dk right now
     * @throws IOException    if something occurs while receiving
     * @throws ParseException if the sender send a datetime with the wrong format
     */
    public int readFrom(SocketChannel sender) throws IOException, ParseException {

        header = ByteBuffer.allocate(headersize);
        //while there is something to read from the buffer continue reading
        if (sender.read(header) < 0) {
            throw new IOException("Message header not received");
        }

        //get data from header
        header.flip();
        messageLen = header.getInt();
        senderID = header.get();
        threadID = header.getShort();

        //get the date time
        byte[] datebytes = new byte[DATE_FIELD_LENGTH];
        header.get(datebytes);
        sendDate = dateFormat.parse(new String(datebytes));

        //now for the rest of the message
        body = ByteBuffer.allocate(this.messageLen);
        if (sender.read(body) < 0) {
            throw new IOException("Message body not received");
        }
        return 0;
    }

    /**
     * Sends the message to a receiving socket
     *
     * @param receiver the {@link SocketChannel} that is going to receive the message
     * @throws IOException if something occurs while sending
     */
    public void sendTo(SocketChannel receiver) throws IOException {
        header.flip();
        body.flip();
        receiver.write(new ByteBuffer[]{header, body});
        System.out.println("message sent");
    }

    public byte senderID() {
        return senderID;
    }

    public short threadID() {
        return threadID;
    }

    public String getDate() {
        return this.dateFormat.format(this.sendDate);
    }
}