package com.example.myapplication;

public class SMSMessage {
    private final String message;
    private final long time;
    private final boolean sent;
    private final boolean firstMessageAtDate;

    public SMSMessage(String message, long time, Boolean sent, Boolean firstMessageAtDate) {
        this.message = message;
        this.time = time;
        this.sent = sent;
        this.firstMessageAtDate = firstMessageAtDate;
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

    public boolean isFirstMessageAtDate() {
        return firstMessageAtDate;
    }
}
