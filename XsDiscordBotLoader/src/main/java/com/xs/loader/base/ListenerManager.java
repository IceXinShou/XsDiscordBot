package com.xs.loader.base;

import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.LinkedHashMap;
import java.util.List;

public class ListenerManager extends ListenerAdapter {
    private final Logger logger;
    private final List<CommandData> guildCommands;
    private final LinkedHashMap<String, Event> plugin_queue;

    public ListenerManager(List<CommandData> guildCommands, LinkedHashMap<String, Event> plugin_queue) {
        logger = new Logger("LM");
        this.guildCommands = guildCommands;
        this.plugin_queue = plugin_queue;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        Guild guild = event.getGuild();
        if (!guildCommands.isEmpty()) {
            guild.updateCommands().addCommands(guildCommands).queue();
        }

        logger.log("Load Guild: " + guild.getName() + " (" + guild.getId() + ')');
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        logger.log(event.getUser().getName() + ": " + event.getCommandString());
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        event.deferReply(true).queue();

        logger.log(event.getUser().getName() + ": " + event.getCommandString());
    }

    @Override
    public void onReady(ReadyEvent event) {
        for (Event plugin : plugin_queue.values()) plugin.onReady();
    }
}
