package com.xs.voicelogger;

import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;

public class Main extends Event {
    private static final String TAG = "VoiceLogger";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/VoiceLogger";
    private final JsonManager jsonManager = new JsonManager();
    private final DiscordLocale defaultLocal = DiscordLocale.CHINESE_TAIWAN;
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content
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
    public void reload() {
        loadLang();
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        langMap = langManager.getMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("voice_logger", "commands about voice logger")
                        .setNameLocalizations(langMap.get("register;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("setting", "set voice log in this channel")
                                .setNameLocalizations(langMap.get("register;subcommand;setting;cmd"))
                                .setDescriptionLocalizations(langMap.get("register;subcommand;setting;description"))
                )
        };
    }

    @Override
    public void onReady(ReadyEvent event) {
        jsonManager.init();
        buttonSystem = new ButtonSystem(langManager, jsonManager);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("voice_logger")) return;
        if (event.getSubcommandName() == null) return;

        if (event.getSubcommandName().equals("setting")) {
            buttonSystem.setting(event);
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
                                                (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                        .setTitle(langManager.get("runtime;log;left;title", local).replace("%channel_name%", title))
                                        .setFooter(langManager.get("runtime;log;left;footer", local))
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
                                                (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                        .setTitle(langManager.get("runtime;log;join;title", local).replace("%channel_name%", title))
                                        .setFooter(langManager.get("runtime;log;join;footer", local))
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