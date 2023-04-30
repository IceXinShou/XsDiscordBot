package com.xs.voicelogger;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.xs.loader.MainLoader.ROOT_PATH;
import static com.xs.loader.util.GlobalUtil.getNickOrTag;
import static com.xs.loader.util.GlobalUtil.getUserById;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "VoiceLogger";
    private final String PATH_FOLDER_NAME = "plugins/VoiceLogger";
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private final JsonManager jsonManager = new JsonManager();
    private ButtonSystem buttonSystem;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadLang();

        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, this.getClass());

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("voice_logger", "commands about voice logger")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("setting", "set voice log in this channel")
                                .setNameLocalizations(lang.get("register;subcommand;setting;cmd"))
                                .setDescriptionLocalizations(lang.get("register;subcommand;setting;description"))
                )
        };
    }

    @Override
    public void onReady(ReadyEvent event) {
        jsonManager.init();
        buttonSystem = new ButtonSystem(lang, jsonManager);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("voice_logger")) return;
        if (event.getSubcommandName() == null) return;
        DiscordLocale local = event.getUserLocale();

        switch (event.getSubcommandName()) {
            case "setting": {
                buttonSystem.setting(event, local);
                break;
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("voicelogger")) return;
        DiscordLocale local = event.getUserLocale();


        switch (args[2]) {
            case "white":
            case "black": {
                buttonSystem.select(event, args, local);
                break;
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("voicelogger")) return;
        DiscordLocale local = event.getUserLocale();

        switch (args[2]) {
            case "toggle": {
                buttonSystem.toggle(event, args, local);
                break;
            }

            case "black":
            case "white": {
                buttonSystem.createSel(event, args, local);
                break;
            }

            case "delete": {
                buttonSystem.delete(event, args, local);
                break;
            }
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        DiscordLocale local = event.getGuild().getLocale();
        AudioChannelUnion joinChannel = event.getChannelJoined();
        AudioChannelUnion leftChannel = event.getChannelLeft();
        Member member = event.getMember();
        long guildID = event.getGuild().getIdLong();

        if (leftChannel != null) { // left
            jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                    (i, j) -> {
                        if (j.contains(leftChannel.getIdLong())) {
                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel != null) {
                                String title = leftChannel.getParentCategory() == null ?
                                        (leftChannel.getName()) :
                                        (leftChannel.getParentCategory().getName() + " > " + leftChannel.getName());

                                EmbedBuilder builder = new EmbedBuilder()
                                        .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                                (" (" + member.getUser().getAsTag() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                        .setTitle(lang.get("runtime;log;left;title").get(local).replace("%channel_name%", title))
                                        .setFooter(lang.get("runtime;log;left;footer").get(local))
                                        .setTimestamp(OffsetDateTime.now())
                                        .setColor(0xff5151);

                                sendChannel.sendMessageEmbeds(builder.build()).queue();
                            }
                        }
                    }
            );
        }

        if (joinChannel != null) { // join
            jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                    (i, j) -> {
                        if (j.contains(joinChannel.getIdLong())) {
                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel != null) {
                                String title = joinChannel.getParentCategory() == null ?
                                        (joinChannel.getName()) :
                                        (joinChannel.getParentCategory().getName() + " > " + joinChannel.getName());

                                EmbedBuilder builder = new EmbedBuilder()
                                        .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                                (" (" + member.getUser().getAsTag() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                        .setTitle(lang.get("runtime;log;join;title").get(local).replace("%channel_name%", title))
                                        .setFooter(lang.get("runtime;log;join;footer").get(local))
                                        .setTimestamp(OffsetDateTime.now())
                                        .setColor(0x34E000);

                                sendChannel.sendMessageEmbeds(builder.build()).queue();
                            }
                        }
                    }
            );
        }
    }
}