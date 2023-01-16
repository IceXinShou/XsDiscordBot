package com.xs.loader;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class test {
    static Map<String, Map<DiscordLocale, String>> data = new HashMap<>();

    public static void main(String[] args) throws IOException {
        File file = new File("C:\\Users\\icexi\\IdeaProjects\\XsDiscordBot\\Plugins\\Ban\\src\\main\\resources\\lang\\en-US.yml");
        Map<String, Object> object;

        object = new Yaml().load(Files.newInputStream(file.toPath()));

//        System.out.println(object.toString());
        System.out.println(new JSONObject(object));
        read(new JSONObject(object), "", DiscordLocale.ENGLISH_US);
        System.out.println(data);
    }


    static void read(final Object origin_json, final String level, final DiscordLocale locale) {
        if (origin_json instanceof JSONObject) {
            JSONObject json = (JSONObject) origin_json;
            for (final String key : json.keySet()) {
                Object i = json.get(key);
                if (level.equals("")) {
                    read(i, key, locale);
                } else {
                    read(i, level + ';' + key, locale);
                }
            }
        } else if (origin_json instanceof JSONArray) {
            for (final Object key : (JSONArray) origin_json) {
                read(key, level, locale);
            }
        } else if (origin_json instanceof String) {
            Map<DiscordLocale, String> tmp = data.getOrDefault(level, new HashMap<>());
            if (tmp.containsKey(locale)) {
                tmp.put(locale, tmp.get(locale) + ';' + origin_json);
            } else {
                tmp.put(locale, origin_json.toString());
            }
            data.put(level, tmp);
        }
    }
}
