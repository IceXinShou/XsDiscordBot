package com.xs.messagemanagerapi;

import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.Permission.MESSAGE_MANAGE;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main extends Event {
    private static final String TAG = "MessageManagerAPI";
    Logger logger;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        logger = new Logger(TAG);
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }


    /*
    /msg say [content]
    /msg remove [id]
    /msg embed
    /msg modify [msgID]
     */
    @Override
    public CommandData[] globalCommands() {
        return new SlashCommandData[]{
                Commands.slash("say", "make the bot send a message")
                        .addOption(STRING, "content", "message content", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("remove", "make the bot remove a message")
                        .addOption(INTEGER, "id", "message ID")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(MESSAGE_MANAGE)),

                Commands.slash("embed", "make the bot send a embed message")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("modify", "make the bot remove a message")
                        .addOption(INTEGER, "id", "message ID")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(MESSAGE_MANAGE)),
        };
    }

    @Override
    public void onReady(ReadyEvent event) {
//        Guild g = event.getJDA().getGuildById(858672865355890708L);
    }
}