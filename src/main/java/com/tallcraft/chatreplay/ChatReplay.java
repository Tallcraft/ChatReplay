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

import java.util.logging.Logger;

public class ChatReplay extends JavaPlugin implements Listener {
    public static final Logger logger = Logger.getLogger("minecraft");
    private ChatBuffer chatBuffer;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        int bufferSize = getConfig().getInt("bufferSize");
        if (bufferSize <= 0) {
            logger.info(this.getName() + ": Invalid bufferSize " + bufferSize + "! Must be greater 0");
        }
        chatBuffer = new ChatBuffer(
                bufferSize,
                getConfig().getString("replayHeader"),
                getConfig().getString("replayFooter"),
                getConfig().getString("replayMsgFormat"),
                getConfig().getString("replayMsgHover"));

        getServer().getPluginManager().registerEvents(this, this);
    }


    @EventHandler
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        chatBuffer.addMessage(new ChatMessage(ChatColor.stripColor(event.getPlayer().getDisplayName()), ChatColor.stripColor(event.getMessage())));
    }


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        reloadConfig();

        chatBuffer.setBufferSize(getConfig().getInt("bufferSize"));
        chatBuffer.setReplayHeader(getConfig().getString("replayHeader"));
        chatBuffer.setReplayFooter(getConfig().getString("replayFooter"));
        chatBuffer.setReplayMsgFormat(getConfig().getString("replayMsgFormat"));
        chatBuffer.setReplayMsgHover(getConfig().getString("replayMsgHover"));

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
}




