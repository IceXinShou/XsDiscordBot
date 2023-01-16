package com.xs.emoji;

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
    private static final String TAG = "Emoji";
    private static final String VERSION = "1.0";
    private final String PATH_FOLDER_NAME = "plugins/Emoji";
    private JSONArray ids;
    public static Map<String, Map<Long, Emoji>> emojis = new HashMap<>();
    private boolean setup = false;

    public Main() {
        super(TAG, VERSION);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        loadVariables();
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
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadVariables() {
        if (config.has("GuildID") && !config.getJSONArray("GuildID").isEmpty()) {
            ids = config.getJSONArray("GuildID");
            setup = true;
        } else {
            logger.error("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
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