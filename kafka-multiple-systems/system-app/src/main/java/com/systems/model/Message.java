package com.systems.model;

public class Message {
    private final String from;
    private final String content;
    private final long timestamp;

    public Message(String from, String content, long timestamp) {
        this.from = from;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getFrom() { return from; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}
