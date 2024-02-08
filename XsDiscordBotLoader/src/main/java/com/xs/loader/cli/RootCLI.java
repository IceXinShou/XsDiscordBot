package com.xs.loader.cli;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;
import com.xs.loader.error.ClassType;
import com.xs.loader.error.Exceptions;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.fusesource.jansi.AnsiConsole;

import static com.xs.loader.Main.cliLogger;
import static com.xs.loader.Main.loader;
import static com.xs.loader.base.Loader.jdaBot;
import static com.xs.loader.error.Checks.notNull;

public class RootCLI {

    /* Bind Control */
    @Command(name = "bind-guild", abbrev = "bg", description = "bind to a specific Guild")
    public void bindGuild(
            @Param(name = "guildID", description = "specific guild") String guildID
    ) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        ShellFactory.createConsoleShell(guild.getName(), null, new GuildCLI(guild)).commandLoop();
    }

    @Command(name = "bind-guild-channel", abbrev = "bgc", description = "bind to a specific channel of Guild")
    public void bindGuildChannel(
            @Param(name = "guildID", description = "specific guild") String guildID,
            @Param(name = "channelID", description = "specific channel id") String channelID

    ) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        GuildChannel channel = guild.getGuildChannelById(channelID);
        notNull(ClassType.GuildChannel, channel);

        ShellFactory.createConsoleShell(guild.getName() + " | " + channel.getName()
                , null, new GuildChannelCLI(guild, channel)).commandLoop();
    }

    @Command(name = "bind-private-channel", abbrev = "bpc", description = "bind to a specific private channel of User")
    public void bindPrivateChannel(
            @Param(name = "userID", description = "specific User id") String userID
    ) throws Exception {
        User user = jdaBot.retrieveUserById(userID).complete();
        notNull(ClassType.User, user);

        ShellFactory.createConsoleShell(user.getName(), null, new PrivateChannelCLI(user)).commandLoop();
    }

    @Command(name = "direct-message", abbrev = "dm", description = "direct message to the User")
    public void dm(
            @Param(name = "userID", description = "User id") String userID,
            @Param(name = "content", description = "message content") String content
    ) throws Exception {
        User user = jdaBot.retrieveUserById(userID).complete();
        notNull(ClassType.User, user);

        PrivateChannel privateChannel = user.openPrivateChannel().complete();
        notNull(ClassType.PrivateChannel, privateChannel);

        privateChannel.sendMessage(content).queue();
    }

    /* Guild Channel Control */
    @Command(name = "say", abbrev = "sa", description = "send message to the Channel of Guild")
    public void say(
            @Param(name = "guildID", description = "your Guild id") String guildID,
            @Param(name = "channelID", description = "your TextChannel id") String channelID,
            @Param(name = "content", description = "message content") String content
    ) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        GuildChannel channel = guild.getGuildChannelById(channelID);
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

    /* Guild Control */
    @Command(name = "join", abbrev = "jo", description = "make the bot join the AudioChannel of Guild")
    public void join(
            @Param(name = "guildID", description = "your Guild id") String guildID,
            @Param(name = "channelID", description = "your AudioChannel id") String channelID
    ) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        AudioChannel channel = guild.getChannelById(AudioChannel.class, channelID);
        notNull(ClassType.Channel, channel);
        guild.getAudioManager().openAudioConnection(channel);
    }

    @Command(name = "leave", abbrev = "le", description = "make the bot leave the AudioChannel of Guild")
    public void leave(
            @Param(name = "guildID", description = "your Guild id") String guildID
    ) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        guild.getAudioManager().closeAudioConnection();
    }

    @Command(name = "mute", abbrev = "mu", description = "make the bot muted")
    public void mute(
            @Param(name = "guildID", description = "your Guild id") String guildID
    ) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        AudioManager manager = guild.getAudioManager();
        manager.setSelfMuted(!manager.isSelfMuted());
    }

    @Command(name = "deafen", abbrev = "de", description = "make the bot deafened")
    public void deafen(
            @Param(name = "guildID", description = "your Guild id") String guildID
    ) throws Exception {
        Guild guild = jdaBot.getGuildById(guildID);
        notNull(ClassType.Guild, guild);

        AudioManager manager = guild.getAudioManager();
        manager.setSelfDeafened(!manager.isSelfDeafened());
    }

    @Command(name = "stop", abbrev = "close", description = "shutdown the program")
    public void stop() {
        loader.stop();
        AnsiConsole.systemUninstall();
        cliLogger.log("Stopped");
        System.exit(0);
    }

    @Command()
    public void reload() throws Exception {
        loader.reload();
    }


}