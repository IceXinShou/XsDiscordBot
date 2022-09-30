package com.xs.loader;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;

import static com.xs.loader.MainLoader.guildCommands;
import static com.xs.loader.MainLoader.subGuildCommands;

public class ListenerManager extends ListenerAdapter {
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (!subGuildCommands.isEmpty()) {
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
    }
}
