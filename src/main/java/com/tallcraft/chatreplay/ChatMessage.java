package com.tallcraft.chatreplay;

import java.util.Date;

/**
 * Created by paul on 05.09.16.
 */
public class ChatMessage {
    private String playerName;
    private String message;
    private Date timestamp;

    public ChatMessage(String playerName, String message) {
        this.playerName = playerName;
        this.message = message;
        this.timestamp = new Date();
    }

    public ChatMessage(String playerName, String message, Date timestamp) {
        this.playerName = playerName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int compareTo(ChatMessage other) {
        return this.getTimestamp().compareTo(other.getTimestamp());
    }

    public int compareTo(Date date) {
        return this.getTimestamp().compareTo(date);
    }

    public String toString() {
        return playerName + ": " + message;
    }
}
