package com.xs.emoji;

import com.wavjaby.json.JsonArray;
import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Main extends PluginEvent {

    public Map<String, Object> config = new HashMap<>();
//    public Map<String, String> lang;
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
    private JsonArray ids;
    public static Map<String, Map<Long, Emoji>> emojis = new HashMap<>();

    @Override
    public void initLoad() {
        getter = new FileGetter(TAG, PATH_FOLDER_NAME, Main.class.getClassLoader());
        logger = new Logger(TAG);
        loadConfigFile();
        loadVariables();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadConfigFile() {
        config = getter.readYml("config.yml", "plugins/" + PATH_FOLDER_NAME);
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadVariables() {
        ids = new JsonArray(config.get("GuildID").toString());
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (ids.content(event.getGuild().getIdLong())) {
            Guild guild = event.getGuild();
            for (Emoji i : guild.retrieveEmojis().complete()) {
                if (!emojis.containsKey(i.getName())) {
                    Map<Long, Emoji> tmp = new HashMap<>();
                    tmp.put(guild.getIdLong(), i);
                    emojis.put(i.getName(), tmp);
                } else {
                    emojis.get(i.getName()).put(guild.getIdLong(), i);
                }
                logger.log("Loaded " + i.getName());
            }
        }
    }

    @Override
    public void loadLang() {
    }
}