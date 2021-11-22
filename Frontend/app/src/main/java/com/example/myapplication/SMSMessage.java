package com.example.myapplication;

public class SMSMessage {
    private final String message;
    private final long time;
    private final boolean sent;

    public SMSMessage(String message, long time, Boolean sent) {
        this.message = message;
        this.time = time;
        this.sent = sent;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }

    public boolean isSent() {
        return sent;
    }
}
