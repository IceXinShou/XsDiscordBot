package com.xs.loader.lang;

import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LangManager {
    private final Map<String, Map<DiscordLocale, String>> langMap = new HashMap<>();
    private final String FOLDER_PATH;
    private final Logger LOGGER;
    private final String[] DEFAULT_LANGS;
    private final DiscordLocale DEFAULT_LOCAL;
    private final FileGetter GETTER;
    private final Class<?> FROM_CLASS;

    public LangManager(String tag, FileGetter getter, String pathFolderName, String[] defaultLangs, DiscordLocale defaultLocal, final Class<?> fromClass) {
        LOGGER = new Logger(tag);
        this.FOLDER_PATH = MainLoader.ROOT_PATH + "/" + pathFolderName + "/Lang";
        this.GETTER = getter;
        this.DEFAULT_LANGS = defaultLangs;
        DEFAULT_LOCAL = defaultLocal;
        this.FROM_CLASS = fromClass;

        new File(FOLDER_PATH).mkdirs();
        exportDefaultLang();
    }

    public Map<String, Map<DiscordLocale, String>> readLangFileDataMap() {
        for (File i : new File(FOLDER_PATH).listFiles()) {
            DiscordLocale local = DiscordLocale.from(i.getName().replaceAll("\\.\\w+$", ""));
            if (local == DiscordLocale.UNKNOWN) {
                LOGGER.warn("Cannot find discord locate by file: " + i.getAbsolutePath() + "");
                continue;
            }

            try {
                readLang(GETTER.readFileMapByPath(i.toPath()), "", local);
            } catch (Exception e) {
                LOGGER.warn(e.getMessage());
            }
        }
        return langMap;
    }

    public String get(String key, DiscordLocale local) {
        Map<DiscordLocale, String> first = langMap.get(key);

        if (first == null) // cannot get string by key
            return null;

        String second = first.get(local);
        if (second == null) // if local not support, return default language
            return first.get(DEFAULT_LOCAL);

        return second;
    }

    private void exportDefaultLang() {
        for (String lang : DEFAULT_LANGS) {
            String fileName = lang + ".yml";
            File lang_file = new File(FOLDER_PATH + fileName);
            if (lang_file.exists()) continue;

            // export is not exist
            GETTER.exportResource("lang/" + fileName, "/Lang/" + fileName);
        }
    }

    private void readLang(Object origin_json, String level, DiscordLocale locale) {
        if (origin_json instanceof Map) {
            Map<String, Object> json = (Map<String, Object>) origin_json;
            for (final String key : json.keySet()) {
                Object i = json.get(key);
                if (level.equals("")) {
                    readLang(i, key, locale);
                } else {
                    readLang(i, level + ';' + key, locale);
                }
            }
        } else if (origin_json instanceof ArrayList) {
            for (final Object key : (ArrayList<Object>) origin_json) {
                readLang(key, level, locale);
            }
        } else if (origin_json instanceof String) {
            Map<DiscordLocale, String> tmp = langMap.getOrDefault(level, new HashMap<>());
            if (tmp.containsKey(locale)) {
                tmp.put(locale, tmp.get(locale) + ';' + origin_json);
            } else {
                tmp.put(locale, origin_json.toString());
            }
            langMap.put(level, tmp);
        }
    }
}