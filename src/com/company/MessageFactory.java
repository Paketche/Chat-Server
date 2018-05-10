package com.company;

public interface MessageFactory {
    /**
     * Creates new instances of the message
     *
     * @return a new message instance
     */
    Message newInstance();
}
