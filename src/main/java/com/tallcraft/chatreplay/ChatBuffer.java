package com.tallcraft.chatreplay;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class ChatBuffer {

    private ConcurrentLinkedQueue<ChatMessage> queue;
    private int queueSize; // How many messages are currently stored
    private int bufferSize; // Max size, How many messages to store + replay

    private HashMap<UUID, Integer> playerIndex; // Store pos of last shown messages to player

    private String replayHeader;
    private String replayFooter;
    private String replayMsgFormat;
    private String replayMsgHover;

    private static final Logger logger = Logger.getLogger("minecraft"); //FIXME remove


    public ChatBuffer(int bufferSize, String replayHeader, String replayFooter, String replayMsgFormat, String replayMsgHover) {
        setReplayHeader(replayHeader);
        setReplayFooter(replayFooter);
        setReplayMsgFormat(replayMsgFormat);
        setReplayMsgHover(replayMsgHover);

        this.bufferSize = bufferSize;
        queue = new ConcurrentLinkedQueue<>();
        queueSize = 0;

        playerIndex = new HashMap<>();
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            while (queueSize > bufferSize) {
                queue.remove();
                queueSize--;
                modifyPlayerIndex(-1);
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

            player.sendMessage(header.toLegacyText());

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


            player.sendMessage(footer.toLegacyText());
            player.sendMessage("Your index: " + Integer.toString(getPlayerIndex(player)));
            player.sendMessage("MORE (TODO CLICK)");

            setPlayerIndex(player, 5); // TODO: testing
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

    private int getPlayerIndex(Player player) {
        UUID uuid = player.getUniqueId();
        if(playerIndex.containsKey(uuid)) {
            return playerIndex.get(uuid);
        }
        return 0;
    }

    private void setPlayerIndex(Player player, int index) {
        playerIndex.put(player.getUniqueId(), index);
    }

    private void modifyPlayerIndex(int modifier) {
        for(Map.Entry<UUID, Integer> entry : playerIndex.entrySet()) {
            int index = entry.getValue();
            int result = index + modifier;
            if(result < 0) {
                // If index becomes invalid remove entry
                playerIndex.remove(entry.getKey());
            } else {
                // Otherwise apply modifier
                entry.setValue(result);
            }
        }
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
