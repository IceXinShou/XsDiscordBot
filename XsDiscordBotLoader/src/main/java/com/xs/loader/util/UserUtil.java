package com.xs.loader.util;

import net.dv8tion.jda.api.entities.User;

import static com.xs.loader.MainLoader.jdaBot;

public class UserUtil {
    public static User getUserById(Long id) {
        return jdaBot.retrieveUserById(id).complete();
    }
}
