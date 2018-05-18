package com.company;

import java.util.Queue;

public class User {
    private volatile boolean terminated;
    private boolean inUse;
    private Thread holder;
    private Queue<Message> messages;


    public User(Queue<Message> queue) {

        this.messages = queue;
        inUse = false;
        terminated = false;
    }


    /**
     * Checks if the key is free. If so it returns it, else it return {@code null}
     *
     * @return the key if it's free to use; {@code null} otherwise
     */
    public Queue<Message> tryTaking() {
        synchronized (this) {
            return inUse ? null : messages;
        }
    }

    public Queue<Message> setInUse() {
        synchronized (this) {
            while (inUse) {
                if (terminated)
                    return null;
                try {
                    wait();
                } catch (InterruptedException e) {
                    //could be terminated by another thread
                }
            }
        }

        return messages;
    }

    public void makeUsable() {
        synchronized (this) {
            holder = null;
            notifyAll();
        }
    }

    public void terminate() {
        synchronized (this) {
            terminated = true;
            notifyAll();
        }
    }

    public boolean isTerminated() {
        synchronized (this) {
            return terminated;
        }
    }

}
