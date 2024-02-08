package com.xs.voicelogger;

import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent;
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
import java.util.List;
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
    public void onChannelUpdateVoiceStatus(ChannelUpdateVoiceStatusEvent event) {
        DiscordLocale local;
        if (!event.getGuild().getFeatures().contains("COMMUNITY"))
            local = defaultLocal;
        else
            local = event.getGuild().getLocale();

        VoiceChannel channel = event.getChannel().asVoiceChannel();
        String oldStr = event.getOldValue();
        String newStr = event.getNewValue();
        long guildID = event.getGuild().getIdLong();
        List<AuditLogEntry> entryList = event.getGuild().retrieveAuditLogs()
                .limit(1)
                .type(ActionType.VOICE_CHANNEL_STATUS_UPDATE)
                .complete();

        if (entryList.isEmpty()) {
            logger.log("why empty ??");
            return;
        }

        Member member = event.getGuild().retrieveMemberById(entryList.get(0).getUserIdLong()).complete();

        if (newStr.isEmpty()) {
            // clear status
            jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                    (i, j) -> {
                        if (!j.contains(channel.getIdLong())) return;

                        TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                        if (sendChannel == null) return;

                        String channelStr = channel.getParentCategory() == null ?
                                (channel.getName()) :
                                (channel.getParentCategory().getName() + " > " + channel.getName());

                        EmbedBuilder builder = new EmbedBuilder()
                                .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                        (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                .setTitle(langManager.get("runtime;log;status_remove;title", local)
                                        .replace("%channel%", channelStr)
                                        .replace("%text_remove%", oldStr))
                                .setFooter(langManager.get("runtime;log;status_remove;footer", local))
                                .setTimestamp(OffsetDateTime.now())
                                .setColor(0xff5151);

                        sendChannel.sendMessageEmbeds(builder.build()).queue();
                    }
            );
        } else {
            if (oldStr.isEmpty()) {
                // new status
                jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                        (i, j) -> {
                            if (!j.contains(channel.getIdLong())) return;

                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel == null) return;

                            String channelStr = channel.getParentCategory() == null ?
                                    (channel.getName()) :
                                    (channel.getParentCategory().getName() + " > " + channel.getName());

                            EmbedBuilder builder = new EmbedBuilder()
                                    .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                            (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                    .setTitle(langManager.get("runtime;log;status_add;title", local)
                                            .replace("%channel%", channelStr)
                                            .replace("%text_add%", newStr))
                                    .setFooter(langManager.get("runtime;log;status_add;footer", local))
                                    .setTimestamp(OffsetDateTime.now())
                                    .setColor(0x34E000);

                            sendChannel.sendMessageEmbeds(builder.build()).queue();
                        }
                );
            } else {
                // change
                jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                        (i, j) -> {
                            if (!j.contains(channel.getIdLong())) return;

                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel == null) return;

                            String channelStr = channel.getParentCategory() == null ?
                                    (channel.getName()) :
                                    (channel.getParentCategory().getName() + " > " + channel.getName());

                            EmbedBuilder builder = new EmbedBuilder()
                                    .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                            (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                    .setTitle(langManager.get("runtime;log;status_change;title", local)
                                            .replace("%channel%", channelStr)
                                            .replace("%text_add%", newStr)
                                            .replace("%text_remove%", oldStr))
                                    .setFooter(langManager.get("runtime;log;move;footer", local))
                                    .setTimestamp(OffsetDateTime.now())
                                    .setColor(0xe0b03d);

                            sendChannel.sendMessageEmbeds(builder.build()).queue();
                        }
                );
            }
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        DiscordLocale local;
        if (!event.getGuild().getFeatures().contains("COMMUNITY"))
            local = defaultLocal;
        else
            local = event.getGuild().getLocale();

        AudioChannelUnion joinChannel = event.getChannelJoined();
        AudioChannelUnion leftChannel = event.getChannelLeft();
        Member member = event.getMember();
        long guildID = event.getGuild().getIdLong();

        if (joinChannel == null) {
            // left
            jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                    (i, j) -> {
                        if (!j.contains(leftChannel.getIdLong())) return;

                        TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                        if (sendChannel == null) return;

                        String leftChannelStr = leftChannel.getParentCategory() == null ?
                                (leftChannel.getName()) :
                                (leftChannel.getParentCategory().getName() + " > " + leftChannel.getName());

                        EmbedBuilder builder = new EmbedBuilder()
                                .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                        (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                .setTitle(langManager.get("runtime;log;left;title", local).replace("%channel_name_left%", leftChannelStr))
                                .setFooter(langManager.get("runtime;log;left;footer", local))
                                .setTimestamp(OffsetDateTime.now())
                                .setColor(0xff5151);

                        sendChannel.sendMessageEmbeds(builder.build()).queue();
                    }
            );
        } else {
            if (leftChannel == null) {
                // new join
                jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                        (i, j) -> {
                            if (!j.contains(joinChannel.getIdLong())) return;

                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel == null) return;

                            String joinChannelStr = joinChannel.getParentCategory() == null ?
                                    (joinChannel.getName()) :
                                    (joinChannel.getParentCategory().getName() + " > " + joinChannel.getName());

                            EmbedBuilder builder = new EmbedBuilder()
                                    .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                            (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                    .setTitle(langManager.get("runtime;log;join;title", local).replace("%channel_name_join%", joinChannelStr))
                                    .setFooter(langManager.get("runtime;log;join;footer", local))
                                    .setTimestamp(OffsetDateTime.now())
                                    .setColor(0x34E000);

                            sendChannel.sendMessageEmbeds(builder.build()).queue();
                        }
                );
            } else {
                // move
                jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                        (i, j) -> {
                            if (!j.contains(joinChannel.getIdLong())) return;

                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel == null) return;

                            String joinChannelStr = joinChannel.getParentCategory() == null ?
                                    (joinChannel.getName()) :
                                    (joinChannel.getParentCategory().getName() + " > " + joinChannel.getName());
                            String leftChannelStr = joinChannel.getParentCategory() == null ?
                                    (joinChannel.getName()) :
                                    (joinChannel.getParentCategory().getName() + " > " + leftChannel.getName());

                            EmbedBuilder builder = new EmbedBuilder()
                                    .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                            (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                    .setTitle(langManager.get("runtime;log;move;title", local)
                                            .replace("%channel_name_join%", joinChannelStr)
                                            .replace("%channel_name_left%", leftChannelStr))
                                    .setFooter(langManager.get("runtime;log;move;footer", local))
                                    .setTimestamp(OffsetDateTime.now())
                                    .setColor(0xe0b03d);

                            sendChannel.sendMessageEmbeds(builder.build()).queue();
                        }
                );
            }
        }
    }
}