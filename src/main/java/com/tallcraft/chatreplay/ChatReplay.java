package com.tallcraft.chatreplay;

import github.scarsz.discordsrv.DiscordSRV;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class ChatReplay extends JavaPlugin implements Listener {
    private final Logger logger = Logger.getLogger(this.getName());
    private ChatBuffer chatBuffer;
    private FileConfiguration config;
    private DiscordSRVListener discordsrvListener;
    private boolean discordInstalled = isClass("github.scarsz.discordsrv.DiscordSRV");

    @Override
    public void onEnable() {

        //Metrics powered by bstats.org
        Metrics metrics = new Metrics(this);

        // Set up configuration
        initConfig(); //  Update config file if options are missing

        // Load config options and initialize buffer
        configureBuffer();

        // Listen for DiscordSRV Messages if the plugin is installed
        if (discordInstalled) {
            boolean enabled = config.getBoolean("recordDiscordSRV");

            if(enabled) logger.info("DiscordSRV installed. Recording messages.");

            // Initialize listener for DiscordSRV messages
            discordsrvListener = new DiscordSRVListener(chatBuffer, enabled);

            // Subscribe to DiscordSRV messages
            DiscordSRV.api.subscribe(discordsrvListener);
        }

        // Register events to listen for chat, command and player join
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (discordInstalled) {
            // Unsubscribe from DiscordSRV messages
            DiscordSRV.api.unsubscribe(discordsrvListener);
        }
    }


    private void initConfig() {
        // Get config object for "config.yml"
        config = this.getConfig();

        // Read config header from external file and set it
        String header = new Scanner(this.getResource("configHeader"))
                .useDelimiter("\\Z").next();
        config.options().header(header);

        MemoryConfiguration defaultConfig = new MemoryConfiguration();
        defaultConfig.set("replayOnLogin", true);
        defaultConfig.set("recordDiscordSRV", false);
        defaultConfig.set("bufferSize", 500);
        defaultConfig.set("viewSize", 70);
        defaultConfig.set("timestampFormat", "yyyy-MM-dd HH:mm");
        defaultConfig.set("replayHeader", "&7&lReplaying last {{msgCount}} messages");
        defaultConfig.set("replayFooter", "&7&lReplayed last {{msgCount}} messages");
        defaultConfig.set("replayMsgFormat", "&7[{{player}}]: {{message}}");
        defaultConfig.set("replayMsgHover", "{{timestamp}}");
        defaultConfig.set("navigateHistoryButtonText", "&7&nSHOW OLDER");

        config.setDefaults(defaultConfig);
        config.options().copyDefaults(true);
        saveConfig();
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        if(event.isCancelled()) {
            return;
        }
        chatBuffer.addMessage(
                new ChatMessage(
                        ChatColor.stripColor(event.getPlayer().getDisplayName()),
                        ChatColor.stripColor(event.getMessage())
                ));
    }


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0) {
            return false;
        }

        // Chat replay commands are only supported for players
        if (sender instanceof Player) {

            if (args[0].equalsIgnoreCase("play")) {
                if (!sender.hasPermission("chatreplay.play")) {
                    sender.sendMessage(cmd.getPermissionMessage());
                    return true;
                }
                chatBuffer.resetPlayer((Player) sender);
                chatBuffer.playTo((Player) sender);
                return true;
            }

            if (args[0].equalsIgnoreCase("more")) {
                if (!sender.hasPermission("chatreplay.more")) {
                    sender.sendMessage(cmd.getPermissionMessage());
                    return true;
                }
                chatBuffer.playTo((Player) sender);
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chatreplay.reload")) {
                sender.sendMessage(cmd.getPermissionMessage());
                return true;
            }
            reloadConfig();
            initConfig();
            configureBuffer();

            // If the DiscordSRV option has been disabled stop recording messages from it
            if (discordInstalled) {
                discordsrvListener.setEnabled(config.getBoolean("recordDiscordSRV"));
            }

            logger.info("Reloaded configuration");
            if (sender instanceof Player) {
                sender.sendMessage(this.getName() + ": Reloaded configuration!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission("chatreplay.clear")) {
                sender.sendMessage(cmd.getPermissionMessage());
                return true;
            }

            chatBuffer.clear();
            logger.info("Cleared chat buffer.");
            if (sender instanceof Player) {
                sender.sendMessage(this.getName() + ": Cleared chat buffer.");
            }
            return true;
        }

        return false;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        // the user is typing the desired subcommand
        if(args.length == 1) {
            // subcommands the user has permission to use
            List<String> subcommands = new LinkedList<>();
            // player-only subcommands
            if(sender instanceof Player) {
                if(sender.hasPermission("chatreplay.play")) 
                    subcommands.add("play");
                if(sender.hasPermission("chatreplay.more"))
                    subcommands.add("more");
            }
            if(sender.hasPermission("chatreplay.reload"))
                subcommands.add("reload");
            if(sender.hasPermission("chareplat.clear"))
                subcommands.add("clear");
            List<String> matches = new LinkedList<>();
            // partial matches to make life easier
            StringUtil.copyPartialMatches(args[0], subcommands, matches);
            return matches;
        }
        return null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        chatBuffer.resetPlayer(player);

        if (config.getBoolean("replayOnLogin") && player.hasPermission("chatreplay.playlogin")) {
            chatBuffer.playTo(event.getPlayer());
        }
    }

    /**
     * Update existing chatBuffer or create a new one with config options
     */
    private void configureBuffer() {
        int bufferSize = config.getInt("bufferSize");
        int viewSize = config.getInt("viewSize");

        if (bufferSize <= 0) {
            throw new IllegalArgumentException(this.getName() + ": Invalid bufferSize " + bufferSize + "! Must be greater 0");
        }

        if (viewSize > bufferSize) {
            throw new IllegalArgumentException(this.getName() + ": Invalid viewSize. Can not be smaller than total buffer size");
        }

        String timestampFormat = config.getString("timestampFormat");
        String replayHeader = config.getString("replayHeader");
        String replayFooter = config.getString("replayFooter");
        String replayMsgFormat = config.getString("replayMsgFormat");
        String replayMsgHover = config.getString("replayMsgHover");
        String navigateHistoryButtonText = config.getString("navigateHistoryButtonText");

        if (chatBuffer == null) {
            chatBuffer = new ChatBuffer(
                    bufferSize,
                    viewSize,
                    timestampFormat,
                    replayHeader,
                    replayFooter,
                    replayMsgFormat,
                    replayMsgHover,
                    navigateHistoryButtonText);
        } else {
            chatBuffer.setBufferSize(bufferSize);
            chatBuffer.setViewSize(viewSize);
            chatBuffer.setTimestampFormat(timestampFormat);
            chatBuffer.setReplayHeader(replayHeader);
            chatBuffer.setReplayFooter(replayFooter);
            chatBuffer.setReplayMsgFormat(replayMsgFormat);
            chatBuffer.setReplayMsgHover(replayMsgHover);
            chatBuffer.setNavigateHistoryButtonText(navigateHistoryButtonText);
        }
    }

    private boolean isClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}




