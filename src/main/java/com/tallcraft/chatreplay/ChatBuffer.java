package com.tallcraft.chatreplay;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.*;

public class ChatBuffer {
    private List buffer;
    private int bufferSize; // How many messages are currently stored
    private int maxBufferSize; // Max size, How many messages to store + replay

    private HashMap<UUID, Integer> playerIndex; // Store pos of last shown messages to player
    private int viewSize; // How many messages to show user at once

    private String replayHeader;
    private String replayFooter;
    private String replayMsgFormat;
    private String replayMsgHover;
    private String navigateHistoryButtonText;


    public ChatBuffer(int maxBufferSize, int viewSize, String timestampFormat, String replayHeader, String replayFooter, String replayMsgFormat, String replayMsgHover, String navigateHistoryButtonText) {
        setReplayHeader(replayHeader);
        setReplayFooter(replayFooter);
        setReplayMsgFormat(replayMsgFormat);
        setReplayMsgHover(replayMsgHover);
        setNavigateHistoryButtonText(navigateHistoryButtonText);
        setTimestampFormat(timestampFormat);

        this.maxBufferSize = maxBufferSize;
        this.viewSize = viewSize;

        this.bufferSize = 0;
        this.playerIndex = new HashMap<>();
        this.buffer = Collections.synchronizedList(new ArrayList<ChatMessage>());
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            while (bufferSize > maxBufferSize) {
                buffer.remove(0);
                bufferSize--;
                updatePlayerIndexes(-1);
            }
            buffer.add(message);
            bufferSize++;
        }
    }


    public void playTo(Player player) {
        if (bufferSize == 0) {//Only replay if there is data
            return;
        }

        TextComponent header = new TextComponent(
                TextComponent.fromLegacyText(
                        replayHeader.replace("{{msgCount}}", Integer.toString(viewSize))) // FIXME: should be replayedCounter instead
        );
        TextComponent footer = new TextComponent(
                TextComponent.fromLegacyText(
                        replayFooter.replace("{{msgCount}}", Integer.toString(viewSize))) // FIXME: Should be replayedCounter instead
        );

        TextComponent formattedMessage;
        String replacedMsgFormat;
        String replacedMsgHover;

        player.sendMessage(header.toLegacyText());

        int playerIndex = getPlayerIndex(player);
        int startIndex = playerIndex - (viewSize - 1);
        if (startIndex < 0) {
            startIndex = 0;
        }

//        logger.info("Replaying player '" + player.getDisplayName() + "' playerIndex: " + playerIndex +  " startIndex: " + startIndex + " bufferSize: " + bufferSize);


        int replayedCounter = 0;
        for (int i = startIndex; i < bufferSize && replayedCounter < viewSize; i++, replayedCounter++) {
            ChatMessage msg = (ChatMessage) buffer.get(i);

            replacedMsgFormat = new String(replayMsgFormat);
            replacedMsgHover = new String(replayMsgHover);
            try {
                replacedMsgFormat = replaceVariables(new String[]{msg.getPlayerName(), msg.getMessage(),
                        msg.getTimestampFormatted()}, replacedMsgFormat);
                replacedMsgHover = replaceVariables(new String[]{msg.getPlayerName(), msg.getMessage(),
                        msg.getTimestampFormatted()}, replacedMsgHover);
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

        // Only show button when there are messages left
        if (startIndex != 0) {
            TextComponent showOlder = new TextComponent(TextComponent.fromLegacyText(navigateHistoryButtonText));
            showOlder.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatreplay more"));
            player.spigot().sendMessage(showOlder);
        }

        // Update player index for pagination
        modifyPlayerIndex(player.getUniqueId(), playerIndex, -replayedCounter);

//        logger.info("Replayed player '" + player.getDisplayName() + "' playerIndex: " + getPlayerIndex(player.getUniqueId()) + " replayedCounter: " + replayedCounter);
    }

    // TODO: move variable replacement to ChatMessage class
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

    private int getPlayerIndex(UUID uuid) {
        if (playerIndex.containsKey(uuid)) {
            return playerIndex.get(uuid);
        }
        return bufferSize - 1;
    }

    private int getPlayerIndex(Player player) {
        return getPlayerIndex(player.getUniqueId());
    }


    private void updatePlayerIndexes(int modifier) {
        playerIndex.forEach((uuid, index) -> modifyPlayerIndex(uuid, index, modifier));
    }

    private void modifyPlayerIndex(UUID uuid, int index, int modifier) {
        int result = index + modifier;

        if (result < 0) {
            // If index becomes invalid remove entry
            playerIndex.remove(uuid);
        } else {
            // Otherwise apply modifier
            playerIndex.put(uuid, result);
        }
    }

    public void resetPlayer(Player player) {
        playerIndex.remove(player.getUniqueId());
    }

    public int getViewSize() {
        return viewSize;
    }

    public void setViewSize(int viewSize) {
        this.viewSize = viewSize;
    }

    public int getBufferSize() {
        return maxBufferSize;
    }

    public void setBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
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

    public void setNavigateHistoryButtonText(String navigateHistoryButtonText) {
        this.navigateHistoryButtonText = ChatColor.translateAlternateColorCodes('&',
                navigateHistoryButtonText);
    }

    public void setTimestampFormat(String format) {
        ChatMessage.timestampFormat = format;
    }
}
