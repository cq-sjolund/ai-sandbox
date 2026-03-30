package com.systems.store;

import com.systems.model.Message;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class MessageStore {
    private static final int MAX_SIZE = 100;
    private final Deque<Message> messages = new LinkedList<>();

    public synchronized void add(Message message) {
        if (messages.size() >= MAX_SIZE) {
            messages.pollFirst();
        }
        messages.addLast(message);
    }

    public synchronized List<Message> getAll() {
        return new ArrayList<>(messages);
    }
}
