package com.xs.loader.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class EmbedCreator {
    public static MessageEmbed createEmbed(String title, String description, int color) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .build();
    }
    public static MessageEmbed createEmbed(String title, int color) {
        return new EmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .build();
    }
}

