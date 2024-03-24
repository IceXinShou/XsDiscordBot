package tw.xserver.serverinfo;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class ServerInfo extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerInfo.class);
    private static final String PATH_FOLDER_NAME = "plugins/ServerInfo";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    public ServerInfo() {
        super(true);

        reloadAll();
        LOGGER.info("loaded ServerInfo");
    }


    @Override
    public void unload() {
        LOGGER.info("unLoaded ServerInfo");
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
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, ServerInfo.class);
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("info", "show server info")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
        };
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (checkCommand(event, "info")) return;


        Member member = event.getMember();
        notNull(member, "Member Executor");

        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale locale = event.getUserLocale();

        event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.loading.get(locale), 0x00FFFF)).queue();
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
            builder.addField(lang.runtime.fields.members.title.get(locale), String.format(lang.runtime.fields.members.total.get(locale) + " `%d`\n" +
                            lang.runtime.fields.members.human.get(locale) + " `%d`\n" +
                            lang.runtime.fields.members.bot.get(locale) + " `%d`\n" +
                            lang.runtime.fields.members.admin.get(locale) + " `%d`",
                    memberCount, realMemberCount, botCount, adminCount), true);

            builder.addField(lang.runtime.fields.members_status.title.get(locale), String.format(lang.runtime.fields.members_status.online.get(locale) + " `%d`\n" +
                            lang.runtime.fields.members_status.working.get(locale) + " `%d`\n" +
                            lang.runtime.fields.members_status.idle.get(locale) + " `%d`\n" +
                            lang.runtime.fields.members_status.offline.get(locale) + " `%d`",
                    online, working, idle, offline), true);

            builder.addBlankField(false);

            builder.addField(lang.runtime.fields.channels.title.get(locale), String.format(lang.runtime.fields.channels.total.get(locale) + " `%d`\n" +
                            lang.runtime.fields.channels.text.get(locale) + " `%d`\n" +
                            lang.runtime.fields.channels.voice.get(locale) + " `%d`\n" +
                            lang.runtime.fields.channels.stage.get(locale) + " `%d`",
                    textCount + voiceCount + stageCount, textCount, voiceCount, stageCount), true);

            builder.addField(lang.runtime.fields.channels_status.title.get(locale), String.format(lang.runtime.fields.channels_status.connect.get(locale) + " `%d`\n" +
                            lang.runtime.fields.channels_status.voice.get(locale) + " `%d`\n" +
                            lang.runtime.fields.channels_status.stage.get(locale) + " `%d`",
                    inVoice + inStage, inVoice, inStage), true);

            builder.addBlankField(false);

            builder.addField(lang.runtime.fields.roles.get(locale),
                    "`" + guild.getRoles().size() + "`", true);

            builder.addField(lang.runtime.fields.emoji.get(locale),
                    "`" + guild.retrieveEmojis().complete().size() + "`", true);

            builder.addField(lang.runtime.fields.sticker.get(locale),
                    "`" + guild.retrieveStickers().complete().size() + "`", true);

            builder.addBlankField(false);

            builder.addField(lang.runtime.fields.boost.title.get(locale), lang.runtime.fields.boost.level.get(locale) + " `" + guild.getBoostTier().getKey() + "`\n" +
                    lang.runtime.fields.boost.amount.get(locale) + " `" + guild.getBoostCount() + "`", true);

            builder.addField(lang.runtime.fields.language.get(locale),
                    "`" + guild.getLocale().getNativeName() + "`", true);

            builder.addBlankField(true);


            Member owner = event.getGuild().retrieveOwner().complete();
            event.getHook().editOriginalEmbeds(
                    builder
                            .setAuthor(owner.getUser().getName(), null, owner.getEffectiveAvatarUrl())
                            .setTitle(event.getGuild().getName())
                            .setThumbnail(guild.getIconUrl())
                            .setFooter(lang.runtime.footer.get(locale))
                            .setColor(0x00FFFF)
                            .build()).queue();
        }).onError(i -> {
            LOGGER.error("ERROR");
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.error.get(locale), 0xFF0000)).queue();
        });
    }
}