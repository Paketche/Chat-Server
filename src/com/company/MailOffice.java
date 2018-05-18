package com.company;

import java.nio.channels.SelectionKey;
import java.util.*;

public class MailOffice {
    private final TreeMap<Integer, SelectionKey> boxNo_to_Key;

    public MailOffice() {
        this.boxNo_to_Key = new TreeMap<>();
    }


    public void newMailBox(int number, SelectionKey key) {
        boxNo_to_Key.put(number, key);
    }

    public boolean removeMailBox(int number) {
        return boxNo_to_Key.remove(number) != null;
    }

    public boolean removeMailBox(SelectionKey user) {

        //find the number of the selection key
        Optional<Integer> o = boxNo_to_Key.entrySet().stream()
                .filter(e -> user.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();

        //remove if present
        Integer k;
        return (k = o.orElse(null)) != null && boxNo_to_Key.remove(k) != null;

    }

    public boolean thereIsBoxOf(int numeber) {
        return boxNo_to_Key.get(numeber) != null;
    }

    public void putMessageInBoxes(Message message, ArrayList<Integer> receivers) {
        receivers.stream()
                .map(boxNo_to_Key::get)
                .filter(Objects::nonNull)
                .forEach(key ->
                        putMessageInBoxes(key, message)
                );
    }

    public void putMessageInBoxes(int boxNo, Message message) {
        putMessageInBoxes(boxNo_to_Key.get(boxNo), message);
    }

    private void putMessageInBoxes(SelectionKey key, Message message) {
        //add a message to the queue of the key and set it up for writing
        User u = (User) key.attachment();
        Queue<Message> mailBox = u.setInUse();
        mailBox.add(message);

        //turn on write interest
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        u.makeUsable();
    }
}
