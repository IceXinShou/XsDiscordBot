package com.xs.emoji;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Main extends PluginEvent {

    private JSONObject config;
//    private Map<String, String> lang;
//    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};
//    private final String[] LANG_PARAMETERS_DEFAULT = {
//            "REGISTER_NAME", "REGISTER_OPTION_MEMBER_YOU_CHOOSE", "REGISTER_OPTION_TIME_DAY",
//            "REGISTER_OPTION_REASON", "NO_PERMISSION", "PERMISSION_DENIED", "SUCCESS", "UNKNOWN_ERROR"
//    };

    private final String[] CONFIG_PARAMETERS_DEFAULT = {
            "Lang", "GuildID"
    };

    FileGetter getter;
    Logger logger;
    final String TAG = "Emoji";
    final String PATH_FOLDER_NAME = "Emoji";
    private JSONArray ids;
    public static Map<String, Map<Long, Emoji>> emojis = new HashMap<>();
    boolean noSet = false;

    @Override
    public void initLoad() {
        super.initLoad();
        getter = new FileGetter(TAG, PATH_FOLDER_NAME, null, null, Main.class.getClassLoader());
        logger = new Logger(TAG);
        loadConfigFile();
        loadVariables();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
        logger.log("UnLoaded");
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", "plugins/" + PATH_FOLDER_NAME));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadVariables() {
        ids = config.getJSONArray("GuildID");

        if (ids.isNull(0)) {
            logger.error("Please configure /plugins/" + PATH_FOLDER_NAME + "/config.yml");
            noSet = true;
        }

    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (!noSet) {
            for (int i = 0; i < ids.length(); ++i) {
                if (ids.getLong(i) == event.getGuild().getIdLong()) {
                    Guild guild = event.getGuild();
                    for (Emoji j : guild.retrieveEmojis().complete()) {
                        if (!emojis.containsKey(j.getName())) {
                            Map<Long, Emoji> tmp = new HashMap<>();
                            tmp.put(guild.getIdLong(), j);
                            emojis.put(j.getName(), tmp);
                        } else {
                            emojis.get(j.getName()).put(guild.getIdLong(), j);
                        }
                        logger.log("Loaded " + j.getName());
                    }
                }
            }
        }
    }

    @Override
    public void loadLang() {
    }
}