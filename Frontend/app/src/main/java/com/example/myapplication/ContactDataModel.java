package com.example.myapplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ContactDataModel {
    enum Level {
        REGULAR,
        PRIORITY,
        SPAM
    }

    private final String number;
    private final String threadId;
    private String displayName = "";
    private String snippet = "";
    private long snippetTime;
    private Level priority = Level.REGULAR;
    private int trustScore;

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ContactDataModel(String number, String threadId, String snippet, long snippetTime) {
        this.number = number;
        this.threadId = threadId;
        this.snippet = snippet;
        this.snippetTime = snippetTime;
    }

    public void setPriority(Level priority) {
        this.priority = priority;
    }

    public void setTrustScore(int trustScore) {
        this.trustScore = trustScore;
    }

    public String getNumber() {
        return number;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getDisplayName() {
        return displayName.isEmpty() ? getNumber() : displayName;
    }

    public String getDisplayTime() {
        return new SimpleDateFormat("hh:mm a MMM dd, yyyy", Locale.US).format(new Date(Long.parseLong(String.valueOf(snippetTime))));
    }

    public String getSnippet() {
        return snippet;
    }

    public Level getPriority() {
        return priority;
    }

    public int getTrustScore() {
        return trustScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactDataModel that = (ContactDataModel) o;
        return number.equals(that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

}
