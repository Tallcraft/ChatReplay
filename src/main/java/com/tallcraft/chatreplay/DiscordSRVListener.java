package com.tallcraft.chatreplay;

import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;

public class DiscordSRVListener {
    private ChatBuffer chatBuffer;
    private boolean enabled;

    public DiscordSRVListener(ChatBuffer chatBuffer) {
        this(chatBuffer, true);
    }

    public DiscordSRVListener(ChatBuffer chatBuffer, boolean enabled) {
        this.chatBuffer = chatBuffer;
        this.enabled = enabled;
    }

    // TODO: Currently records all channels, allow user to configure, do not record console channels
    @Subscribe(priority = ListenerPriority.MONITOR)
    public void discordMessageReceived(DiscordGuildMessageReceivedEvent event) {
        if(enabled) {
            chatBuffer.addMessage(
                    new ChatMessage(
                            event.getAuthor().getName(),
                            event.getMessage().getStrippedContent(),
                            event.getMessage().getCreationTime()
                    ));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
