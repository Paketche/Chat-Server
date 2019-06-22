package valchanov.georgi;

import valchanov.georgi.messages.Message;

import java.nio.channels.SelectionKey;
import java.util.*;

/**
 * Provides a mapping between an integer and a selection key.<br>
 * It is expected that to the selection key a queue of messages it attached.
 */
public class MailOffice {
    /**
     * Each mail box is used as a user id to it's selection key
     */
    private final TreeMap<Integer, SelectionKey> boxNo_to_Key;

    /**
     * Creates a new mail office
     */
    public MailOffice() {
        this.boxNo_to_Key = new TreeMap<>();
    }


    /**
     * Creates a new mail box and maps it to a key
     *
     * @param number of the mail box
     * @param key    to the mail box
     * @throws IllegalArgumentException if the selection key does not have a queue attached to it
     */
    public void newMailBox(int number, SelectionKey key) throws IllegalArgumentException {
        if (!(key.attachment() instanceof Queue))
            throw new IllegalArgumentException("The selection key doesn't have a queue attached to it");

        synchronized (this) {
            boxNo_to_Key.put(number, key);
        }
    }

    /**
     * Removes a mail box
     *
     * @param number used for finding the box that needs to be removed
     * @return true if there was a mail box before removal
     */
    public synchronized boolean removeMailBox(int number) {
        return boxNo_to_Key.remove(number) != null;
    }

    /**
     * Removes a mail box
     *
     * @param key used for finding the box that needs to be removed
     * @return true if there was a mail box before removal
     */
    public synchronized boolean removeMailBox(SelectionKey key) {

        //find the number of the selection key
        Optional<Integer> o = boxNo_to_Key.entrySet().stream()
                .filter(e -> key.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();

        //remove if present
        Integer k;
        return (k = o.orElse(null)) != null && boxNo_to_Key.remove(k) != null;
    }

    /**
     * Returns true if the provided number is mapped to mail box
     *
     * @param number of the mail box
     * @return true if there is a mail box for this number
     */
    public boolean thereIsBoxOf(int number) {
        return boxNo_to_Key.get(number) != null;
    }

    /**
     * Put a message in multiple boxes.The keys if the users' where the message was put have their interest
     * set to op_write
     *
     * @param message   that is to be put in the mail boxes
     * @param receivers the mail box numbers of the receivers
     */
    public void putMessageInBoxes(Message message, ArrayList<Integer> receivers) {
        receivers.stream()
                .map(boxNo_to_Key::get)
                .filter(Objects::nonNull)
                .forEach(key -> {

                            putMessageInBox(key, message);
                        }
                );
    }

    /**
     * Puts a message in a mail box. The keys if the users' where the message was put have their interest
     * set to op_write
     *
     * @param boxNo   of the box that the message is going to be put in
     * @param message that is to be put in a message box
     * @throws IllegalArgumentException If the {@link SelectionKey} does not have a Queue<Message> attached to it
     */
    public void putMessageInBox(int boxNo, Message message) throws IllegalArgumentException {
        putMessageInBox(boxNo_to_Key.get(boxNo), message);
    }

    private void putMessageInBox(SelectionKey key, Message message) throws IllegalArgumentException {
        //add a message to the queue of the key and set it up for writing
        Queue<Message> mailBox;
        try {
            mailBox = (Queue<Message>) key.attachment();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The attached queue doesn't hold instances of the Message class");
        }

        mailBox.add(message);

        System.out.println("setting ops to writable");
        //turn on write interest
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        System.out.println("The key is writable : " + key.interestOps());
    }
}
