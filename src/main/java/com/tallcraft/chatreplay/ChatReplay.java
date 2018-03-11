package com.tallcraft.chatreplay;

import github.scarsz.discordsrv.DiscordSRV;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class ChatReplay extends JavaPlugin implements Listener {
    private static final Logger logger = Logger.getLogger(this.getName());
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

        // Insert messages for testing
//        for (int i = 0; i < 500; i++) {
//            chatBuffer.addMessage(new ChatMessage("Notch", Integer.toString(i + 1)));
//        }

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

        config.options().header("ChatReplay Configuration\n" +
                "\n" +
                " Should messages be automatically re-played on login?\n" +
                "   replayOnLogin\n" +
                "\n" +
                " If installed, should DiscordSRV messages be recorded?\n" +
                "   recordDiscordSRV\n" +
                "\n" +
                " Chat Buffer Options:\n" +
                "\n" +
                "   bufferSize: How many of the most recent chat messages to store in total\n" +
                "   viewSize: How many messages to show players at once. Vanilla Minecraft Client has a history limit of around 100\n" +
                "\n" +
                " Messages\n" +
                "\n" +
                "   Format of timestamp string. Do not use color / formatting codes.\n" +
                "   Format instructions: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html\n" +
                "     timestampFormat\n" +
                "\n" +
                "   Header and footer message of replay output\n" +
                "   Available variables: {{msgCount}}\n" +
                "     replayHeader\n" +
                "     replayFooter\n" +
                "\n" +
                "   Formatting for single messages replayed\n" +
                "   Available variables: {{player}}, {{message}}, {{timestamp}}\n" +
                "     replayMsgFormat\n" +
                "     replayMsgHover\n" +
                "\n" +
                "   Button to navigate chat history\n" +
                "      navigateHistoryButtonText");

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


    @EventHandler
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
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

    public boolean isClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}




