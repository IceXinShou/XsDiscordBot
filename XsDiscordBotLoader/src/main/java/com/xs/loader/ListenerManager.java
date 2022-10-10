package com.xs.loader;

import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListenerManager extends ListenerAdapter {
    private final Logger logger;
    private final List<CommandData> guildCommands;
    private final List<SubcommandData> subGuildCommands;

    public ListenerManager(List<CommandData> guildCommands, List<SubcommandData> subGuildCommands) {
        logger = new Logger("CMD");
        this.guildCommands = guildCommands;
        this.subGuildCommands = subGuildCommands;
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
