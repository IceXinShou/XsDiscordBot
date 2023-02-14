package com.xs.loader;

import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
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
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if (message.equals("")) return;
        logger.log(event.getAuthor().getAsTag() + ": " + message);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        logger.log(event.getUser().getAsTag() + ": " + event.getCommandString());

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        logger.log(event.getUser().getAsTag() + ": " + event.getComponentId());
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        event.deferReply(true).queue();

        logger.log(event.getUser().getAsTag() + ": " + event.getCommandString());
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        logger.log(event.getUser().getAsTag() + ": " + event.getValues());
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        logger.log(event.getUser().getAsTag() + ": " + event.getValues());
    }
}
