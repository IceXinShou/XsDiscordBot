package com.xs.loader.cli;

import asg.cliche.Command;
import asg.cliche.Param;
import com.xs.loader.error.Exceptions;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

public class GuildChannelCLI extends GuildCLI {
    private final GuildChannel channel;

    public GuildChannelCLI(Guild guild, GuildChannel channel) {
        super(guild);
        this.channel = channel;
    }


    @Command(name = "say", abbrev = "say", description = "send message to the Channel of Guild")
    public void say(
            @Param(name = "content", description = "message content") String content
    ) throws Exception {
        if (channel instanceof TextChannel) {
            ((TextChannel) channel).sendMessage(content).queue();
        } else if (channel instanceof VoiceChannel) {
            ((VoiceChannel) channel).sendMessage(content).queue();
        } else if (channel instanceof ThreadChannel) {
            ((ThreadChannel) channel).sendMessage(content).queue();
        } else if (channel instanceof NewsChannel) {
            ((NewsChannel) channel).sendMessage(content).queue();
        } else {
            throw new Exceptions("unknown type of channel");
        }
    }
}
