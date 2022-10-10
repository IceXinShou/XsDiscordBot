package com.xs.loader.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;

import java.util.List;

public class Tag {
    public static <T> String tagUserID(T ID) {
        return "<@!" + ID + '>';
    }

    public static <T> String tagChannelID(T ID) {
        return "<#" + ID + '>';
    }

    public static String tagChannel(GuildChannel channel) {
        return "<#" + channel.getId() + '>';
    }

    public static String getNameByID(Guild guild, long id) {
        return guild.retrieveMemberById(id).complete().getEffectiveName();
    }

    public static <T> String tagRoleID(T ID) {
        return "<@&" + ID + '>';
    }

    public static String tagUsersID(List<Object> ID) {
        StringBuilder n = new StringBuilder();
        for (Object str : ID)
            n.append("<@&").append(str).append('>');

        return n.toString();
    }

    public static String tagChannelsID(List<Object> ID) {
        StringBuilder n = new StringBuilder();
        for (Object str : ID)
            n.append("<@&").append(str).append('>');

        return n.toString();
    }

    public static String tagRolesID(List<Object> ID) {
        StringBuilder n = new StringBuilder();
        for (Object str : ID)
            n.append("<@&").append(str).append('>');

        return n.toString();
    }


    public static String getMemberNick(GenericInteractionCreateEvent event) {
        return (event.getMember().getNickname() == null ?
                event.getUser().getAsTag() : event.getMember().getNickname());
    }

    public static String getMemberName(GenericInteractionCreateEvent event) {
        return (event.getMember().getNickname() == null ?
                event.getUser().getAsTag() : String.format("%s (%s)", event.getMember().getNickname(), event.getUser().getAsTag()));
    }

    public static String getMemberName(GenericGuildVoiceEvent event) {
        return (event.getMember().getNickname() == null ?
                event.getMember().getUser().getAsTag() : String.format("%s (%s)", event.getMember().getNickname(), event.getMember().getUser().getAsTag()));
    }

    public static String getMemberName(GenericComponentInteractionCreateEvent event) {
        return (event.getMember().getNickname() == null ?
                event.getMember().getUser().getAsTag() : String.format("%s (%s)", event.getMember().getNickname(), event.getMember().getUser().getAsTag()));
    }

    public static String getMemberName(MessageUpdateEvent event) {
        return (event.getMember().getNickname() == null ?
                event.getMember().getUser().getAsTag() : String.format("%s (%s)", event.getMember().getNickname(), event.getMember().getUser().getAsTag()));
    }
}
