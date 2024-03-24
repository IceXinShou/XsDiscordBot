package tw.xserver.botinfo;

import net.dv8tion.jda.api.EmbedBuilder;
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
import java.time.OffsetDateTime;

import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class BotInfo extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotInfo.class);
    private static final String PATH_FOLDER_NAME = "plugins/BotInfo";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    public BotInfo() {
        super(true);

        reloadAll();
        LOGGER.info("loaded BotInfo");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded BotInfo");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, BotInfo.class);
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
    public CommandData[] globalCommands() {
        return new SlashCommandData[]{
                Commands.slash("bot-info", "show about the bot data")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
        };
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (checkCommand(event, "bot-info")) return;

        DiscordLocale locale = event.getUserLocale();

        int members = 0;
        for (int i = 0; i < event.getJDA().getGuilds().size(); i++)
            members = members + event.getJDA().getGuilds().get(i).getMemberCount();

        EmbedBuilder builder = new EmbedBuilder();
        builder.addField(
                lang.runtime.successes.guild_count.get(locale),
                String.valueOf((long) event.getJDA().getGuilds().size()), false
        );
        builder.addField(
                lang.runtime.successes.member_count.get(locale),
                String.valueOf(members), false
        );

        event.getHook().editOriginalEmbeds(builder
                .setTitle(lang.runtime.successes.title.get(locale))
                .setTimestamp(OffsetDateTime.now())
                .setColor(0x00FFFF)
                .build()
        ).queue();
    }
}