package tw.xserver.loader.cli;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import tw.xserver.loader.error.Exceptions;

import static net.dv8tion.jda.internal.utils.Checks.notNull;

public class GuildCLI extends RootCLI {
    private final Guild guild;

    public GuildCLI(Guild guild) {
        this.guild = guild;
    }

    @Command(name = "bindc", abbrev = "bc", description = "bind to a specific channel of Guild")
    public void bindGuildChannel(
            @Param(name = "channelID", description = "specific channel id") String channelID

    ) throws Exception {
        GuildChannel channel = guild.getGuildChannelById(channelID);
        notNull(channel, "Guild Channel");

        ShellFactory.createConsoleShell(guild.getName() + " | " + channel.getName(), null, new GuildChannelCLI(guild, channel)).commandLoop();
    }

    @Command(name = "say", abbrev = "say", description = "send message to the Channel of Guild")
    public void say(
            @Param(name = "channelID", description = "your TextChannel id") String channelID,
            @Param(name = "content", description = "message content") String content
    ) throws Exception {
        GuildChannel channel = guild.getGuildChannelById(channelID);
        notNull(channel, "Guild Channel");

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
    public void join(
            @Param(name = "channelID", description = "your AudioChannel id") String channelID
    ) {
        AudioChannel channel = guild.getChannelById(AudioChannel.class, channelID);
        notNull(channel, "Guild Channel");

        guild.getAudioManager().openAudioConnection(channel);
    }

    @Command(name = "leave", abbrev = "le", description = "make the bot leave the AudioChannel of Guild")
    public void leave() {
        guild.getAudioManager().closeAudioConnection();
    }

    @Command(name = "mute", abbrev = "mu", description = "make the bot muted")
    public void mute() {
        AudioManager manager = guild.getAudioManager();
        manager.setSelfMuted(!manager.isSelfMuted());
    }

    @Command(name = "deafen", abbrev = "de", description = "make the bot deafened")
    public void deafen() {
        AudioManager manager = guild.getAudioManager();
        manager.setSelfDeafened(!manager.isSelfDeafened());
    }

    @Command(name = "delete", abbrev = "del", description = "delete a message")
    public void delete(
            @Param(name = "channelID", description = "your TextChannel id") String channelID,
            @Param(name = "messageID", description = "your Message id") String messageID
    ) throws Exception {
        GuildChannel channel = guild.getGuildChannelById(channelID);
        notNull(channel, "Guild Channel");

        if (channel instanceof TextChannel) {
            ((TextChannel) channel).retrieveMessageById(messageID).queue(msg -> msg.delete().queue());
        } else if (channel instanceof VoiceChannel) {
            ((VoiceChannel) channel).retrieveMessageById(messageID).queue(msg -> msg.delete().queue());
        } else if (channel instanceof ThreadChannel) {
            ((ThreadChannel) channel).retrieveMessageById(messageID).queue(msg -> msg.delete().queue());
        } else if (channel instanceof NewsChannel) {
            ((NewsChannel) channel).retrieveMessageById(messageID).queue(msg -> msg.delete().queue());
        } else {
            throw new Exceptions("unknown type of channel");
        }
    }
}
