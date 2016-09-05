package com.tallcraft.chatreplay;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
        //TODO: sanity checks buffer-size (client limit etc)
        chatBuffer = new ChatBuffer(bufferSize);
        getServer().getPluginManager().registerEvents(this, this);
    }


    @EventHandler
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        chatBuffer.addMessage(new ChatMessage(event.getPlayer().getDisplayName(), event.getMessage()));
    }


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        //TODO commands
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        chatBuffer.playTo(event.getPlayer());
    }
}




