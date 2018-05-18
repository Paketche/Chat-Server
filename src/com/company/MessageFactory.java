package com.company;

public interface MessageFactory {
    /**
     * Creates new instances of the message
     *
     * @return a new message instance
     */
    Message newInstance(MessageType type, int senderID, int threadID, String contents);

    Message newInstance();
}
