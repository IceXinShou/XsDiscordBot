package com.xs.emojiapi;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Main extends PluginEvent {

    private JSONObject config;
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "EmojiAPI";
    private final String PATH_FOLDER_NAME = "./plugins/EmojiAPI";
    private JSONArray ids;
    public static Map<String, Map<Long, Emoji>> emojis = new HashMap<>();
    private boolean setup = false;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
        logger.log("UnLoaded");
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));

        if (config.has("GuildID") && !config.getJSONArray("GuildID").isEmpty()) {
            ids = config.getJSONArray("GuildID");
            setup = true;
            logger.log("Setting File Loaded Successfully");
        } else {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (setup) {
            for (int i = 0; i < ids.length(); ++i) {
                if (ids.getLong(i) == event.getGuild().getIdLong()) {
                    Guild guild = event.getGuild();
                    for (Emoji j : guild.retrieveEmojis().complete()) {
                        emojis.getOrDefault(j.getName(), new HashMap<>()).put(guild.getIdLong(), j);
                        logger.log("Loaded " + j.getName());
                    }
                }
            }
        }
    }
}