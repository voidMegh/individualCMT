package com.marketdata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    public String type;
    public Object data;
    public long timestamp;

    public Message() {
    }

    public Message(String type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String type, Object data, long timestamp) {
        this.type = type;
        this.data = data;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("Message{type='%s', timestamp=%d}", type, timestamp);
    }
}