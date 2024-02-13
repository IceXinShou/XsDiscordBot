package com.xs.emojiapi;

import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.xs.loader.base.Loader.jdaBot;

public class Main extends Event {

    private static final String TAG = "EmojiAPI";
    public static Map<String, Map<Long, Emoji>> emojis = new HashMap<>();
    private final String PATH_FOLDER_NAME = "plugins/EmojiAPI";
    private MainConfig configFile;
    private FileGetter getter;
    private Logger logger;
    private boolean setup = false;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadConfigFile();
        logger.logln("Loaded");
    }

    @Override
    public void unload() {
        logger.logln("UnLoaded");
    }

    @Override
    public void loadConfigFile() {
        try (InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            logger.logln("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warnln("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (setup) {
            for (long i : configFile.GuildID) {
                Guild guild = jdaBot.getGuildById(i);
                if (guild == null) continue;
                guild.retrieveEmojis().queue(e -> {
                    for (Emoji j : e) {
                        emojis.computeIfAbsent(j.getName(), k -> new HashMap<>()).put(guild.getIdLong(), j);
                        logger.logln("Loaded " + j.getName());
                    }
                });
            }
        }
    }
}