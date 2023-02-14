package com.xs.loader.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

import static com.xs.loader.MainLoader.jdaBot;

public class GlobalUtil {
    public static User getUserById(Long id) {
        return jdaBot.retrieveUserById(id).complete();
    }

    @Nullable
    public static String getExtensionName(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        }
        return null;
    }

    public static String getNickOrTag(User user, Guild guild) {
        Member member;
        if ((member = guild.retrieveMemberById(user.getIdLong()).complete()) == null || member.getNickname() == null) {
            return user.getAsTag();
        }

        return member.getNickname() + " (" + user.getAsTag() + ')';
    }
}
