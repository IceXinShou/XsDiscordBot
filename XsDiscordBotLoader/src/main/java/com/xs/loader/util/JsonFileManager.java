package com.xs.loader.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xs.loader.base.Loader;
import com.xs.loader.logger.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JsonFileManager {
    private final File FILE;
    private final Logger logger;
    private final boolean isObject;
    private JsonObject data_obj;
    private JsonArray data_ary;

    public JsonFileManager(String FILE_PATH, String TAG, boolean isObject) {
        this.FILE = new File(Loader.ROOT_PATH + '/' + FILE_PATH);
        this.logger = new Logger(TAG);
        this.isObject = isObject;
        initData();
    }

    public static String streamToString(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            for (int length; (length = inputStream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        }
    }

    private void initData() {
        String tmp;
        try {
            if (!FILE.exists() || (tmp = streamToString(Files.newInputStream(FILE.toPath()))).length() == 0) {
                FILE.createNewFile();
                if (isObject) {
                    data_obj = new JsonObject();
                } else {
                    data_ary = new JsonArray();
                }
                save();
                return;
            }

            if (isObject) {
                data_obj = JsonParser.parseString(tmp).getAsJsonObject();
            } else {
                data_ary = JsonParser.parseString(tmp).getAsJsonArray();
            }
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    public JsonObject getObj() {
        return data_obj;
    }

    @Nullable
    public JsonObject getObj(String key) {
        if (data_obj.has(key))
            return data_obj.get(key).getAsJsonObject();
        return null;
    }

    public JsonArray getAry() {
        return data_ary;
    }

    @Nullable
    public JsonObject getOrDefault(String key) {
        if (isObject) {
            if (data_obj.has(key)) return data_obj.get(key).getAsJsonObject();
            else {
                JsonObject tmp = new JsonObject();
                data_obj.add(key, tmp);
                return tmp;
            }
        }
        return null;
    }

    @Nullable
    public JsonObject getOrDefault(String key, JsonObject object) {
        if (isObject) {
            if (data_obj.has(key)) return data_obj.get(key).getAsJsonObject();
            else {
                data_obj.add(key, object);
                return object;
            }
        }
        return null;
    }

    public JsonArray getOrDefaultArray(String key) {
        if (data_obj.has(key)) return data_obj.get(key).getAsJsonArray();
        else {
            JsonArray tmp = new JsonArray();
            data_obj.add(key, tmp);
            return tmp;
        }
    }

    public JsonArray getOrDefaultArray(String key, JsonArray array) {
        if (data_obj.has(key)) return data_obj.get(key).getAsJsonArray();
        else {
            data_obj.add(key, array);
            return array;
        }
    }

    public JsonFileManager removeObj(String id) {
        if (data_obj.has(id)) {
            data_obj.remove(id);
        } else {
            logger.warn("Cannot remove data by id: " + id);
        }
        return this;
    }

    public void save() {
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(FILE.toPath()), StandardCharsets.UTF_8)) {
                if (isObject)
                    writer.write(data_obj.toString());
                else
                    writer.write(data_ary.toString());
                writer.flush();
//                FileWriter writer = new FileWriter(FILE);
            }
        } catch (IOException e) {
            logger.warn("Cannot save file: " + e.getMessage());
        }
    }
}
