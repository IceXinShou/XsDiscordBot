package com.xs.loader;

import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;

import static com.xs.loader.MainLoader.guildCommands;
import static com.xs.loader.MainLoader.subGuildCommands;

public class ListenerManager extends ListenerAdapter {
    Logger logger;

    public ListenerManager() {
        logger = new Logger("CMD");
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (!guildCommands.isEmpty()) {
            event.getGuild().updateCommands().addCommands(guildCommands).queue();
        }
        if (!subGuildCommands.isEmpty()) {
            event.getGuild().updateCommands().addCommands(
                    new CommandDataImpl("setting", "設定").addSubcommands(subGuildCommands)).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        event.getInteraction().deferReply(true).queue();

        if (event.isFromGuild()) {
            logger.log(event.getMember().getEffectiveName() + ": " + event.getCommandString());
        }
    }
}
