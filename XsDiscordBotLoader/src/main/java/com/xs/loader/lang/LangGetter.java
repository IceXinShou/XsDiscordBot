package com.xs.loader.lang;

import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LangGetter {
    private final Map<String, Map<DiscordLocale, String>> lang = new HashMap<>();
    private final String FOLDER_PATH;
    private final Logger logger;
    private final String[] defaultLang;
    private final FileGetter getter;

    public LangGetter(final String TAG, final FileGetter GETTER, final String PATH_FOLDER_NAME, final String[] DEFAULT_LANG) {
        logger = new Logger(TAG);
        this.FOLDER_PATH = MainLoader.ROOT_PATH + "/" + PATH_FOLDER_NAME;
        this.getter = GETTER;
        this.defaultLang = DEFAULT_LANG;
    }


    public void exportDefaultLang() {
        // init folder
        new File(FOLDER_PATH + "/Lang").mkdirs();

        for (String l : defaultLang) {
            String fileName = l + ".yml";
            File lang_file = new File(FOLDER_PATH + "/Lang/" + fileName);

            if (lang_file.exists()) {
//                logger.error("Create default lang: " + fileName);
//                getter.exportResource("lang/" + fileName, fileName, "Lang");

//                try {
//                    getter.copyFile(lang_file, FOLDER_PATH + "/Lang/-" + fileName);
//                    getter.exportResource("lang/" + fileName, fileName, "Lang");
//                } catch (IOException e) {
//                    logger.error(e.getMessage());
//                }

                continue;
            }

            // export is not exist
            getter.exportResource("lang/" + fileName, fileName, "Lang");
        }
    }

    public Map<String, Map<DiscordLocale, String>> readLangFileData() {
        for (File i : new File(FOLDER_PATH + "/Lang/").listFiles()) {
            DiscordLocale local = DiscordLocale.from(i.getName().replaceAll("\\.\\w+$", ""));
            if (local == DiscordLocale.UNKNOWN) {
                logger.error("Cannot find discord locate by file: " + i.getAbsolutePath() + "");
                continue;
            }

            try {
                readLang(new JSONObject(getter.readFile(i.toPath())), "", local);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return lang;
    }

    private void readLang(final Object origin_json, final String level, final DiscordLocale locale) {
        if (origin_json instanceof JSONObject) {
            JSONObject json = (JSONObject) origin_json;
            for (final String key : json.keySet()) {
                Object i = json.get(key);
                if (level.equals("")) {
                    readLang(i, key, locale);
                } else {
                    readLang(i, level + ';' + key, locale);
                }
            }
        } else if (origin_json instanceof JSONArray) {
            for (final Object key : (JSONArray) origin_json) {
                readLang(key, level, locale);
            }
        } else if (origin_json instanceof String) {
            Map<DiscordLocale, String> tmp = lang.getOrDefault(level, new HashMap<>());
            if (tmp.containsKey(locale)) {
                tmp.put(locale, tmp.get(locale) + ';' + origin_json);
            } else {
                tmp.put(locale, origin_json.toString());
            }
            lang.put(level, tmp);
        }
    }
}