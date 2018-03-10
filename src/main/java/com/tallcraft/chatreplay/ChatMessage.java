package com.tallcraft.chatreplay;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;

public class ChatMessage {
    public static String timestampFormat = "yyyy-MM-dd HH:mm";
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

    public ChatMessage(String playerName, String message, OffsetDateTime offsetTimestamp) {
        this(playerName, message, new Date(offsetTimestamp.toInstant().toEpochMilli()));
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestampFormatted() {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(timestampFormat);
            return formatter.format(this.timestamp);
        } catch (NullPointerException | IllegalArgumentException ex) {
            // In case invalid formatStr is provided, fall back to native toString
            return this.timestamp.toString();
        }
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
