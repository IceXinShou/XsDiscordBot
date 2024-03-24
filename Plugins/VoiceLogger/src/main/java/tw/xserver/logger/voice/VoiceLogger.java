package tw.xserver.logger.voice;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class VoiceLogger extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceLogger.class);
    private static final String PATH_FOLDER_NAME = "plugins/VoiceLogger";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;
    private final JsonManager jsonManager = new JsonManager();
    private final DiscordLocale defaultLocal = DiscordLocale.CHINESE_TAIWAN;
    private ButtonSystem buttonSystem;

    public VoiceLogger() {
        super(true);

        reloadAll();
        LOGGER.info("loaded VoiceLogger");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded VoiceLogger");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, VoiceLogger.class);
    }

    @Override
    public void reloadLang() {
        try {
            lang = new LangManager<>(getter, PATH_FOLDER_NAME, CHINESE_TAIWAN, Language.class).get();
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("voice-logger", "commands about voice logger")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("setting", "set voice log in this channel")
                                .setNameLocalizations(lang.register.subcommand.setting.name)
                                .setDescriptionLocalizations(lang.register.subcommand.setting.description)
                )
        };
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        jsonManager.init();
        buttonSystem = new ButtonSystem(lang, jsonManager);
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (checkCommand(event, "voice-logger")) return;
        if (event.getSubcommandName() == null) return;

        if (event.getSubcommandName().equals("setting")) {
            buttonSystem.setting(event);
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("voice-logger")) return;
        DiscordLocale locale = event.getUserLocale();


        switch (args[2]) {
            case "white":
            case "black": {
                buttonSystem.select(event, args, locale);
                break;
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("voice-logger")) return;
        DiscordLocale locale = event.getUserLocale();

        switch (args[2]) {
            case "toggle": {
                buttonSystem.toggle(event, args, locale);
                break;
            }

            case "black":
            case "white": {
                buttonSystem.createSel(event, args, locale);
                break;
            }

            case "delete": {
                buttonSystem.delete(event, args, locale);
                break;
            }
        }
    }

    @Override
    public void onChannelUpdateVoiceStatus(ChannelUpdateVoiceStatusEvent event) {
        DiscordLocale locale;
        if (!event.getGuild().getFeatures().contains("COMMUNITY"))
            locale = defaultLocal;
        else
            locale = event.getGuild().getLocale();

        VoiceChannel channel = event.getChannel().asVoiceChannel();
        String oldStr = event.getOldValue();
        String newStr = event.getNewValue();
        long guildID = event.getGuild().getIdLong();
        List<AuditLogEntry> entryList = event.getGuild().retrieveAuditLogs()
                .limit(1)
                .type(ActionType.VOICE_CHANNEL_STATUS_UPDATE)
                .complete();

        if (entryList.isEmpty()) {
            LOGGER.info("why empty ??");
            return;
        }

        Member member = event.getGuild().retrieveMemberById(entryList.get(0).getUserIdLong()).complete();

        if (newStr.isEmpty()) {
            // clear status
            jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                    (i, j) -> {
                        if (j.notContains(channel.getIdLong())) return;

                        TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                        if (sendChannel == null) return;

                        String channelStr = channel.getParentCategory() == null ?
                                (channel.getName()) :
                                (channel.getParentCategory().getName() + " > " + channel.getName());

                        EmbedBuilder builder = new EmbedBuilder()
                                .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                        (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                .setDescription(lang.runtime.log.status_remove.title.get(locale)
                                        .replace("%channel%", channelStr)
                                        .replace("%text_remove%", oldStr))
                                .setFooter(lang.runtime.log.status_remove.footer.get(locale))
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
                            if (j.notContains(channel.getIdLong())) return;

                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel == null) return;

                            String channelStr = channel.getParentCategory() == null ?
                                    (channel.getName()) :
                                    (channel.getParentCategory().getName() + " > " + channel.getName());

                            EmbedBuilder builder = new EmbedBuilder()
                                    .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                            (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                    .setDescription(lang.runtime.log.status_add.title.get(locale)
                                            .replace("%channel%", channelStr)
                                            .replace("%text_add%", newStr))
                                    .setFooter(lang.runtime.log.status_add.footer.get(locale))
                                    .setTimestamp(OffsetDateTime.now())
                                    .setColor(0x34E000);

                            sendChannel.sendMessageEmbeds(builder.build()).queue();
                        }
                );
            } else {
                // change
                jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                        (i, j) -> {
                            if (j.notContains(channel.getIdLong())) return;

                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel == null) return;

                            String channelStr = channel.getParentCategory() == null ?
                                    (channel.getName()) :
                                    (channel.getParentCategory().getName() + " > " + channel.getName());

                            EmbedBuilder builder = new EmbedBuilder()
                                    .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                            (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                    .setDescription(lang.runtime.log.status_change.title.get(locale)
                                            .replace("%channel%", channelStr)
                                            .replace("%text_add%", newStr)
                                            .replace("%text_remove%", oldStr))
                                    .setFooter(lang.runtime.log.move.footer.get(locale))
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
        DiscordLocale locale;
        if (!event.getGuild().getFeatures().contains("COMMUNITY"))
            locale = defaultLocal;
        else
            locale = event.getGuild().getLocale();

        AudioChannelUnion joinChannel = event.getChannelJoined();
        AudioChannelUnion leftChannel = event.getChannelLeft();
        Member member = event.getMember();
        long guildID = event.getGuild().getIdLong();

        if (joinChannel == null) {
            // left
            jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                    (i, j) -> {
                        if (j.notContains(leftChannel.getIdLong())) return;

                        TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                        if (sendChannel == null) return;

                        String leftChannelStr = leftChannel.getParentCategory() == null ?
                                (leftChannel.getName()) :
                                (leftChannel.getParentCategory().getName() + " > " + leftChannel.getName());

                        EmbedBuilder builder = new EmbedBuilder()
                                .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                        (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                .setDescription(lang.runtime.log.left.title.get(locale).replace("%channel_name_left%", leftChannelStr))
                                .setFooter(lang.runtime.log.left.footer.get(locale))
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
                            if (j.notContains(joinChannel.getIdLong())) return;

                            TextChannel sendChannel = event.getGuild().getTextChannelById(i);
                            if (sendChannel == null) return;

                            String joinChannelStr = joinChannel.getParentCategory() == null ?
                                    (joinChannel.getName()) :
                                    (joinChannel.getParentCategory().getName() + " > " + joinChannel.getName());

                            EmbedBuilder builder = new EmbedBuilder()
                                    .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                            (" (" + member.getUser().getName() + ')') : ""), null, member.getEffectiveAvatarUrl())
                                    .setDescription(lang.runtime.log.join.title.get(locale).replace("%channel_name_join%", joinChannelStr))
                                    .setFooter(lang.runtime.log.join.footer.get(locale))
                                    .setTimestamp(OffsetDateTime.now())
                                    .setColor(0x34E000);

                            sendChannel.sendMessageEmbeds(builder.build()).queue();
                        }
                );
            } else {
                // move
                jsonManager.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                        (i, j) -> {
                            if (j.notContains(joinChannel.getIdLong())) return;

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
                                    .setDescription(lang.runtime.log.move.title.get(locale)
                                            .replace("%channel_name_join%", joinChannelStr)
                                            .replace("%channel_name_left%", leftChannelStr))
                                    .setFooter(lang.runtime.log.move.footer.get(locale))
                                    .setTimestamp(OffsetDateTime.now())
                                    .setColor(0xe0b03d);

                            sendChannel.sendMessageEmbeds(builder.build()).queue();
                        }
                );
            }
        }
    }
}