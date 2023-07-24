package com.xs.loader;

import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

public class ListenerManager extends ListenerAdapter {
    private final Logger logger;
    private final List<CommandData> guildCommands;

    public ListenerManager(List<CommandData> guildCommands) {
        logger = new Logger("LM");
        this.guildCommands = guildCommands;
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
}
