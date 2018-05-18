package com.company;

import java.nio.channels.SelectionKey;

@FunctionalInterface
public interface ErrorConsumer{

    void accept(SelectionKey key, Message message, Exception exception);
}
