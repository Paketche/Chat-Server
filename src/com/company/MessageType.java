package com.company;

/**
 * Used to identify what type a message is
 */
public enum MessageType {
    /**
     * Used when a client is identifying itself to the server
     */
    CONNECT,
    /**
     * Used when ether a client is sending a standard message
     */
    SEND,
    /**
     * Used in the creation of a new chat thread(both request and reply have this type)
     */
    NEW_THREAD,
    /**
     * Used when a clients is disconnecting(both request and reply have this type)
     */
    DISCONNECT,
    /**
     * Used to indicate that a client request was unsuccessful
     */
    FAILURE,
    /**
     * Used when a client send a message with an unknown type
     */
    UNKNOWN
}
