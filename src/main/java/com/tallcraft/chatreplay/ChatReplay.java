package com.tallcraft.chatreplay;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.util.logging.Logger;

public class ChatReplay extends JavaPlugin implements Listener {
    private static final Logger logger = Logger.getLogger("minecraft");
    private ChatBuffer chatBuffer;

    @Override
    public void onEnable() {

        //Metrics powered by bstats.org
        Metrics metrics = new Metrics(this);

        this.saveDefaultConfig();

        configureBuffer();

        getServer().getPluginManager().registerEvents(this, this);
    }


    @EventHandler
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        chatBuffer.addMessage(new ChatMessage(ChatColor.stripColor(event.getPlayer().getDisplayName()), ChatColor.stripColor(event.getMessage())));
    }


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        reloadConfig();

        configureBuffer();

        logger.info(this.getName() + ": Reloaded configuration");
        if (sender instanceof Player) {
            sender.sendMessage(this.getName() + ": Reloaded configuration!");
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("chatreplay.view")) {
            chatBuffer.playTo(event.getPlayer());
        }
    }

    /**
     * Update existing chatBuffer or create a new one with config options
     */
    private void configureBuffer() {
        int bufferSize = getConfig().getInt("bufferSize");
        int viewSize = getConfig().getInt("bufferSize");

        if (bufferSize <= 0) {
            throw new IllegalArgumentException(this.getName() + ": Invalid bufferSize " + bufferSize + "! Must be greater 0");
        }

        if (viewSize > bufferSize) {
            throw new IllegalArgumentException(this.getName() + ": Invalid viewSize. Can not be smaller than total buffer size");
        }

        if(chatBuffer == null) {
            chatBuffer = new ChatBuffer(
                    bufferSize,
                    getConfig().getString("replayHeader"),
                    getConfig().getString("replayFooter"),
                    getConfig().getString("replayMsgFormat"),
                    getConfig().getString("replayMsgHover"));
        } else {
            chatBuffer.setBufferSize(getConfig().getInt("bufferSize"));
            chatBuffer.setReplayHeader(getConfig().getString("replayHeader"));
            chatBuffer.setReplayFooter(getConfig().getString("replayFooter"));
            chatBuffer.setReplayMsgFormat(getConfig().getString("replayMsgFormat"));
            chatBuffer.setReplayMsgHover(getConfig().getString("replayMsgHover"));
        }
    }
}




