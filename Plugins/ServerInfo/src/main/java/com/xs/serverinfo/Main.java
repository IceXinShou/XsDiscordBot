package com.xs.serverinfo;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    FileGetter getter;
    Logger logger;
    private static final String TAG = "ServerInfo";
    private final String PATH_FOLDER_NAME = "./plugins/ServerInfo";
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content

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
                Commands.slash("info", "show server info")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("info")) return;
        if (!permissionCheck(ADMINISTRATOR, event))
            return;

        DiscordLocale local = event.getUserLocale();

        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;loading").get(local), 0x00FFFF)).queue();
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


            List<MessageEmbed.Field> fields = new ArrayList<>();
            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;members;title").get(local), String.format("" +
                            lang.get("runtime;fields;members;total").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;members;human").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;members;bot").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;members;admin").get(local) + " `%d`",
                    memberCount, realMemberCount, botCount, adminCount), true)
            );
            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;members_status;title").get(local), String.format("" +
                            lang.get("runtime;fields;members_status;online").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;members_status;working").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;members_status;idle").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;members_status;offline").get(local) + " `%d`",
                    online, working, idle, offline), true)
            );

            fields.add(new MessageEmbed.Field("\u200e", "\u200e", true));
            fields.add(new MessageEmbed.Field("\u200e", "\u200e", false));

            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;channels;title").get(local), String.format("" +
                            lang.get("runtime;fields;channels;total").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;channels;text").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;channels;voice").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;channels;stage").get(local) + " `%d`",
                    textCount + voiceCount + stageCount, textCount, voiceCount, stageCount), true)
            );
            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;channels_status;title").get(local), String.format("" +
                            lang.get("runtime;fields;channels_status;connect").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;channels_status;voice").get(local) + " `%d`\n" +
                            lang.get("runtime;fields;channels_status;stage").get(local) + " `%d`",
                    inVoice + inStage, inVoice, inStage), true)
            );

            fields.add(new MessageEmbed.Field("\u200e", "\u200e", true));
            fields.add(new MessageEmbed.Field("\u200e", "\u200e", false));

            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;roles").get(local),
                    "`" + guild.getRoles().size() + "`", true)
            );

            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;emoji").get(local),
                    "`" + guild.retrieveEmojis().complete().size() + "`", true)
            );

            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;sticker").get(local),
                    "`" + guild.retrieveStickers().complete().size() + "`", true)
            );

            fields.add(new MessageEmbed.Field("\u200e", "\u200e", false));

            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;boost;title").get(local), "" +
                    lang.get("runtime;fields;boost;level").get(local) + " `" + guild.getBoostTier().getKey() + "`\n" +
                    lang.get("runtime;fields;boost;amount").get(local) + " `" + guild.getBoostCount() + "`", true)
            );

            fields.add(new MessageEmbed.Field(lang.get("runtime;fields;language").get(local),
                    "`" + guild.getLocale().getNativeName() + "`", true)
            );

            fields.add(new MessageEmbed.Field("\u200e", "\u200e", true));

            Member owner = event.getGuild().retrieveOwner().complete();
            event.getHook().editOriginalEmbeds(
                    createEmbed(event.getGuild().getName(), guild.getIconUrl(), "", fields, lang.get("runtime;footer").get(local), owner.getUser().getAsTag(), owner.getEffectiveAvatarUrl(), 0xff0000)).queue();
        }).onError(i -> {
            logger.warn("ERROR");
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;error").get(local), 0xFF0000)).queue();
        });
    }
}