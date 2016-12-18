package com.tallcraft.chatreplay;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatBuffer {

    private ConcurrentLinkedQueue<ChatMessage> queue;
    private int queueSize; //We store it ourselves for efficiency
    private int bufferSize; //How many messages to store + replay

    private String replayHeader;
    private String replayFooter;
    private String replayMsgFormat;
    private String replayMsgHover;


    public ChatBuffer(int bufferSize, String replayHeader, String replayFooter, String replayMsgFormat, String replayMsgHover) {
        setReplayHeader(replayHeader);
        setReplayFooter(replayFooter);
        setReplayMsgFormat(replayMsgFormat);
        setReplayMsgHover(replayMsgHover);

        this.bufferSize = bufferSize;
        queue = new ConcurrentLinkedQueue<ChatMessage>();
        queueSize = 0;
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            while (queueSize > bufferSize) {
                queue.remove();
                queueSize--;
            }
            queue.add(message);
            queueSize++;
        }
    }

    private ConcurrentLinkedQueue<ChatMessage> getQueue() {
        return this.queue;
    }

    public void playTo(Player player) {
        if (queueSize > 0) { //Only replay if there is data
            int displaySize = bufferSize;
            if (queueSize < bufferSize) {
                displaySize = queueSize;
            }

            TextComponent header = new TextComponent(
                    TextComponent.fromLegacyText(
                            replayHeader.replace("{{msgCount}}", Integer.toString(displaySize)))
            );
            TextComponent footer = new TextComponent(
                    TextComponent.fromLegacyText(
                            replayFooter.replace("{{msgCount}}", Integer.toString(displaySize)))
            );

            TextComponent formattedMessage;
            String replacedMsgFormat;
            String replacedMsgHover;

            player.spigot().sendMessage(header);

            for (ChatMessage msg : queue) {
                replacedMsgFormat = new String(replayMsgFormat);
                replacedMsgHover = new String(replayMsgHover);

                try {
                    replacedMsgFormat = replaceVariables(new String[]{msg.getPlayerName(), msg.getMessage(),
                            msg.getTimestamp().toString()}, replacedMsgFormat);
                    replacedMsgHover = replaceVariables(new String[]{msg.getPlayerName(), msg.getMessage(),
                            msg.getTimestamp().toString()}, replacedMsgHover);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return;
                }

                formattedMessage = new TextComponent(TextComponent.fromLegacyText(replacedMsgFormat));
                formattedMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(replacedMsgHover).create()));

                player.spigot().sendMessage(formattedMessage);
            }


            player.spigot().sendMessage(footer);
        }
    }

    private String replaceVariables(String[] values, String str) throws IllegalArgumentException {
        return replaceVariables(new String[]{"{{player}}", "{{message}}", "{{timestamp}}"}, values, str);
    }

    private String replaceVariables(String[] variables, String[] values, String str) throws IllegalArgumentException {
        if (variables.length != values.length) {
            throw new IllegalArgumentException("Arguments variables and values have to match in array-length");
        }
        for (int i = 0; i < variables.length; i++) {
            str = str.replace(variables[i], values[i]);
        }

        return str;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getReplayHeader() {
        return replayHeader;
    }

    public void setReplayHeader(String replayHeader) {
        this.replayHeader = ChatColor.translateAlternateColorCodes('&', replayHeader);
    }

    public String getReplayFooter() {
        return replayFooter;
    }

    public void setReplayFooter(String replayFooter) {
        this.replayFooter = ChatColor.translateAlternateColorCodes('&', replayFooter);
    }

    public String getReplayMsgFormat() {
        return replayMsgFormat;
    }

    public void setReplayMsgFormat(String replayMsgFormat) {
        this.replayMsgFormat = ChatColor.translateAlternateColorCodes('&', replayMsgFormat);
    }

    public String getReplayMsgHover() {
        return replayMsgHover;
    }

    public void setReplayMsgHover(String replayMsgHover) {
        this.replayMsgHover = ChatColor.translateAlternateColorCodes('&', replayMsgHover);
    }
}
