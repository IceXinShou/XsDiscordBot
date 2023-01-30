package com.xs.loader;

import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

public class ListenerManager extends ListenerAdapter {
    private final Logger logger;
    private final List<CommandData> guildCommands;

    public ListenerManager(List<CommandData> guildCommands) {
        logger = new Logger("CMD");
        this.guildCommands = guildCommands;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (!guildCommands.isEmpty()) {
            event.getGuild().updateCommands().addCommands(guildCommands).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        if (event.isFromGuild()) {
            logger.log(event.getMember().getEffectiveName() + ": " + event.getCommandString());
        } else {
            logger.log(event.getUser().getAsTag() + ": " + event.getCommandString());
        }
    }
}
