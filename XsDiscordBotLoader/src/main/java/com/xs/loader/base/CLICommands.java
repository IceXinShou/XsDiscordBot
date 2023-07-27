package com.xs.loader.base;

import asg.cliche.Command;
import asg.cliche.Param;
import com.xs.loader.error.ClassType;
import com.xs.loader.error.Exceptions;
import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import static com.xs.loader.Main.loader;
import static com.xs.loader.base.Loader.jdaBot;
import static com.xs.loader.error.Checks.notNull;
import static java.lang.System.exit;

public class CLICommands {
    private static final Logger logger = new Logger("CLI");

    @Command(name = "dm", description = "direct message to the user")
    public void dm(
            @Param(name = "userID", description = "user id") String userID,
            @Param(name = "content", description = "message content") String content) throws Exception {
        User user = jdaBot.retrieveUserById(userID).complete();
        notNull(ClassType.User, user);

        PrivateChannel privateChannel = user.openPrivateChannel().complete();
        notNull(ClassType.PrivateChannel, privateChannel);

        privateChannel.sendMessage(content).queue();
    }

    @Command(name = "say", abbrev = "say", description = "send message to the Channle of Guild")
    public void say(@Param(name = "guildID", description = "your Guild id") String guildID,
                    @Param(name = "channelID", description = "your TextChannel id") String channelID,
                    @Param(name = "content", description = "message content") String content) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        Channel channel = guild.getChannelById(Channel.class, channelID);
        notNull(ClassType.Channel, channel);

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

    @Command(name = "join", abbrev = "jo", description = "make the bot join the AudioChannel of Guild")
    public void join(@Param(name = "guildID", description = "your Guild id") String guildID,
                     @Param(name = "channelID", description = "your AudioChannel id") String channelID) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        Channel channel = guild.getChannelById(Channel.class, channelID);
        notNull(ClassType.Channel, channel);

        if (channel instanceof AudioChannel) {
            guild.getAudioManager().openAudioConnection((AudioChannel) channel);
        } else {
            throw new Exceptions("unknown type of channel");
        }
    }

    @Command(name = "leave", abbrev = "le", description = "make the bot leave the AudioChannel of Guild")
    public void leave(@Param(name = "guildID", description = "your Guild id") String guildID) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        guild.getAudioManager().closeAudioConnection();
    }

    @Command(name = "mute", abbrev = "mu", description = "make the bot muted")
    public void mute(@Param(name = "guildID", description = "your Guild id") String guildID) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        AudioManager manager = guild.getAudioManager();
        manager.setSelfMuted(!manager.isSelfMuted());
    }

    @Command(name = "deafen", abbrev = "de", description = "make the bot deafened")
    public void deafen(@Param(name = "guildID", description = "your Guild id") String guildID) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        AudioManager manager = guild.getAudioManager();
        manager.setSelfDeafened(!manager.isSelfDeafened());
    }

    @Command(name = "stop", abbrev = "close", description = "shutdown the program")
    public void stop() {
        loader.stop();
        logger.log("Stopped");
        exit(0);
    }

    @Command()
    public void reload() throws Exception {
        loader.reload();
    }
}