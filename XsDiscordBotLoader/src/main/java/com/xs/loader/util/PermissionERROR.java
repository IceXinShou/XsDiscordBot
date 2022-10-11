package com.xs.loader.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;


public class PermissionERROR {

    public static MessageEmbed noPermissionERROREmbed(Permission permission) {
        return EmbedCreator.createEmbed(
                "You have no permission" + " `(" + permission.getName() + ")`", 0xFF0000);
    }

    public static boolean permissionCheck(Permission permission, SlashCommandInteractionEvent event) {
        if (event.getMember().hasPermission(permission))
            return true;
        else
            event.getHook().editOriginalEmbeds(EmbedCreator.createEmbed("You have no permission" + " `(" + permission.getName() + ")`", 0xFF0000)).queue();
        return false;
    }
}