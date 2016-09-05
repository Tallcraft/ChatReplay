package com.tallcraft.chatreplay;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class ChatBuffer {

    public static final Logger logger = Logger.getLogger("minecraft");
    private ConcurrentLinkedQueue<ChatMessage> queue;
    private int queueSize; //We store it ourselves for efficiency
    private int bufferSize; //How many messages to store + replay

    public ChatBuffer(int bufferSize) {
        this.bufferSize = bufferSize;
        queue = new ConcurrentLinkedQueue<ChatMessage>();
        queueSize = 0;
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            queue.add(message);
            if (queueSize >= bufferSize) {
                queue.remove();
            } else {
                queueSize++;
            }
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
            TextComponent header = new TextComponent("Replaying last " + displaySize + " messages ========");
            TextComponent footer = new TextComponent("Replay end ========================");
            header.setColor(ChatColor.GRAY);
            header.setBold(true);
            footer.setColor(ChatColor.GRAY);
            footer.setBold(true);

            player.spigot().sendMessage(header);
            TextComponent formattedMessage;
            for (ChatMessage msg : queue) {
                formattedMessage = new TextComponent("[" + msg.getPlayerName() + "]: " + msg.getMessage());
                formattedMessage.setColor(ChatColor.GRAY);
                formattedMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(msg.getTimestamp().toString()).create()));

                player.spigot().sendMessage(formattedMessage);
            }


            player.spigot().sendMessage(footer);
        }
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
