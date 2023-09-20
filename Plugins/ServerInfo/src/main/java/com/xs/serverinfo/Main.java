package com.xs.serverinfo;

import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Map;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;

public class Main extends Event {
    private static final String TAG = "ServerInfo";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/ServerInfo";
    FileGetter getter;
    Logger logger;
    private LangManager langManager;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content

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
                Commands.slash("info", "show server info")
                        .setNameLocalizations(langMap.get("register;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("info")) return;
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale local = event.getUserLocale();

        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;loading", local), 0x00FFFF)).queue();
        Guild guild = event.getGuild();
        int voiceCount = guild.getVoiceChannels().size();
        int textCount = guild.getTextChannels().size();
        int stageCount = guild.getStageChannels().size();
        int memberCount = guild.getMemberCount();

        guild.loadMembers().onSuccess(members -> {
            int realMemberCount = 0;
            int botCount = 0;
            int online = 0;
            int working = 0;
            int idle = 0;
            int offline = 0;
            int adminCount = 0;

            int inVoice = 0;
            int inStage = 0;

            for (VoiceChannel i : guild.getVoiceChannels()) {
                inVoice += i.getMembers().size();
            }

            for (StageChannel i : guild.getStageChannels()) {
                inStage += i.getMembers().size();
            }

            for (Member i : members) {
                if (i.getUser().isBot()) {
                    botCount++;
                } else {
                    realMemberCount++;
                }
                if (i.hasPermission(Permission.ADMINISTRATOR)) adminCount++;
                switch (i.getOnlineStatus()) {
                    case ONLINE: {
                        online++;
                        break;
                    }
                    case DO_NOT_DISTURB: {
                        working++;
                        break;
                    }
                    case IDLE: {
                        idle++;
                        break;
                    }
                    case OFFLINE: {
                        offline++;
                        break;
                    }
                }
                guild.unloadMember(i.getIdLong());
            }

            EmbedBuilder builder = new EmbedBuilder();
            builder.addField(langManager.get("runtime;fields;members;title", local), String.format(langManager.get("runtime;fields;members;total", local) + " `%d`\n" +
                            langManager.get("runtime;fields;members;human", local) + " `%d`\n" +
                            langManager.get("runtime;fields;members;bot", local) + " `%d`\n" +
                            langManager.get("runtime;fields;members;admin", local) + " `%d`",
                    memberCount, realMemberCount, botCount, adminCount), true);

            builder.addField(langManager.get("runtime;fields;members_status;title", local), String.format(langManager.get("runtime;fields;members_status;online", local) + " `%d`\n" +
                            langManager.get("runtime;fields;members_status;working", local) + " `%d`\n" +
                            langManager.get("runtime;fields;members_status;idle", local) + " `%d`\n" +
                            langManager.get("runtime;fields;members_status;offline", local) + " `%d`",
                    online, working, idle, offline), true);

            builder.addBlankField(false);

            builder.addField(langManager.get("runtime;fields;channels;title", local), String.format(langManager.get("runtime;fields;channels;total", local) + " `%d`\n" +
                            langManager.get("runtime;fields;channels;text", local) + " `%d`\n" +
                            langManager.get("runtime;fields;channels;voice", local) + " `%d`\n" +
                            langManager.get("runtime;fields;channels;stage", local) + " `%d`",
                    textCount + voiceCount + stageCount, textCount, voiceCount, stageCount), true);

            builder.addField(langManager.get("runtime;fields;channels_status;title", local), String.format(langManager.get("runtime;fields;channels_status;connect", local) + " `%d`\n" +
                            langManager.get("runtime;fields;channels_status;voice", local) + " `%d`\n" +
                            langManager.get("runtime;fields;channels_status;stage", local) + " `%d`",
                    inVoice + inStage, inVoice, inStage), true);

            builder.addBlankField(false);

            builder.addField(langManager.get("runtime;fields;roles", local),
                    "`" + guild.getRoles().size() + "`", true);

            builder.addField(langManager.get("runtime;fields;emoji", local),
                    "`" + guild.retrieveEmojis().complete().size() + "`", true);

            builder.addField(langManager.get("runtime;fields;sticker", local),
                    "`" + guild.retrieveStickers().complete().size() + "`", true);

            builder.addBlankField(false);

            builder.addField(langManager.get("runtime;fields;boost;title", local), langManager.get("runtime;fields;boost;level", local) + " `" + guild.getBoostTier().getKey() + "`\n" +
                    langManager.get("runtime;fields;boost;amount", local) + " `" + guild.getBoostCount() + "`", true);

            builder.addField(langManager.get("runtime;fields;language", local),
                    "`" + guild.getLocale().getNativeName() + "`", true);

            builder.addBlankField(true);


            Member owner = event.getGuild().retrieveOwner().complete();
            event.getHook().editOriginalEmbeds(
                    builder
                            .setAuthor(owner.getUser().getName(), null, owner.getEffectiveAvatarUrl())
                            .setTitle(event.getGuild().getName())
                            .setThumbnail(guild.getIconUrl())
                            .setFooter(langManager.get("runtime;footer", local))
                            .setColor(0x00FFFF)
                            .build()).queue();
        }).onError(i -> {
            logger.warn("ERROR");
            event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;error", local), 0xFF0000)).queue();
        });
    }
}