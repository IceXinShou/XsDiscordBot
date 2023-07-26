package com.xs.loader;

import asg.cliche.Command;
import com.xs.loader.error.ClassType;
import com.xs.loader.error.Exceptions;
import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import static com.xs.loader.Loader.jdaBot;
import static com.xs.loader.Main.loader;
import static com.xs.loader.error.Checks.notNull;

public class CLICommands {
    private static final Logger logger = new Logger("CLI");

    @Command
    public void dm(String userID, String msg) throws Exception {
        User user = jdaBot.retrieveUserById(userID).complete();
        notNull(ClassType.User, user);

        PrivateChannel privateChannel = user.openPrivateChannel().complete();
        notNull(ClassType.PrivateChannel, privateChannel);

        privateChannel.sendMessage(msg).queue();
    }

    @Command
    public void say(String guildID, String channelID, String msg) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        Channel channel = guild.getChannelById(Channel.class, channelID);
        notNull(ClassType.Channel, channel);

        if (channel instanceof TextChannel) {
            ((TextChannel) channel).sendMessage(msg).queue();
        } else if (channel instanceof VoiceChannel) {
            ((VoiceChannel) channel).sendMessage(msg).queue();
        } else if (channel instanceof ThreadChannel) {
            ((ThreadChannel) channel).sendMessage(msg).queue();
        } else if (channel instanceof NewsChannel) {
            ((NewsChannel) channel).sendMessage(msg).queue();
        } else {
            throw new Exceptions("unknown type of channel");
        }
    }

    @Command
    public void join(String guildID, String channelID) throws Exception {
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

    @Command
    public void leave(String guildID) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        guild.getAudioManager().closeAudioConnection();
    }

    @Command
    public void mute(String guildID) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        AudioManager manager = guild.getAudioManager();
        manager.setSelfMuted(!manager.isSelfMuted());
    }

    @Command
    public void deafen(String guildID) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        AudioManager manager = guild.getAudioManager();
        manager.setSelfDeafened(!manager.isSelfDeafened());
    }

    @Command
    public void stop() {
        loader.stop();
        logger.log("Stopped");
    }

    @Command
    public void reload() throws Exception {
        loader.reload();
        logger.log("Stopped");
    }
}