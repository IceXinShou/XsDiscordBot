package tw.xserver.api.emoji;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.base.Loader.jdaBot;

public class EmojiAPI extends Event {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmojiAPI.class);
    private static final String PATH_FOLDER_NAME = "plugins/EmojiAPI";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private MainConfig configFile;
    private final boolean setup = false;
    public static final Map<String, Map<Long, Emoji>> emojis = new HashMap<>();

    public EmojiAPI() {
        super(true);

        reloadAll();
        LOGGER.info("loaded EmojiAPI");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded EmojiAPI");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, EmojiAPI.class);

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        if (setup) {
            for (long i : configFile.GuildID) {
                Guild guild = jdaBot.getGuildById(i);
                if (guild == null) continue;
                guild.retrieveEmojis().queue(e -> {
                    for (Emoji j : e) {
                        emojis.computeIfAbsent(j.getName(), k -> new HashMap<>()).put(guild.getIdLong(), j);
                        LOGGER.info("loaded " + j.getName());
                    }
                });
            }
        }
    }
}