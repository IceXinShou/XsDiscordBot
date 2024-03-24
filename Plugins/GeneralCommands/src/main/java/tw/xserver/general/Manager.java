package tw.xserver.general;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.general.commands.Ban;
import tw.xserver.general.commands.Kick;
import tw.xserver.general.commands.Language;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static tw.xserver.loader.base.Loader.ROOT_PATH;

public class Manager extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(Manager.class);
    private static final String PATH_FOLDER_NAME = "plugins/GeneralCommands";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    private final Ban ban;
    private final Kick kick;

    public Manager() {
        super(true);

        reloadAll();
        ban = new Ban(lang.ban);
        kick = new Kick(lang.kick);
        LOGGER.info("loaded GeneralCommands");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded GeneralCommands");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, Manager.class);
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
        return ArrayUtils.addAll(ban.guildCommands(), kick.guildCommands());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ban": {
                ban.process(event);
                break;
            }

            case "kick": {
                kick.process(event);
                break;
            }
        }
    }
}