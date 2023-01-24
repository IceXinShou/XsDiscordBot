package com.xs.loader.util;

import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;

public class JsonFileManager {
    private final File FILE;
    private JSONObject data;
    private final Logger logger;

    public JsonFileManager(String FILE_PATH, String TAG) {
        this.FILE = new File(MainLoader.ROOT_PATH + '/' + FILE_PATH);
        this.logger = new Logger(TAG);
        initData();
    }

    public static String streamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    private void initData() {
        try {
            if (!FILE.exists()) {
                FILE.createNewFile();
                FileWriter writer = new FileWriter(FILE);
                writer.write("{}");
                writer.flush();
                writer.close();
            }

            data = new JSONObject(streamToString(Files.newInputStream(FILE.toPath())));
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }


    public JSONObject get() {
        return data;
    }

    public JSONObject getOrDefault(String key) {
        if (data.has(key))
            return data.getJSONObject(key);
        else {
            JSONObject tmp = new JSONObject();
            data.put(key, tmp);
            return tmp;
        }
    }

    public JSONArray getOrDefaultArray(String key) {
        if (data.has(key))
            return data.getJSONArray(key);
        else {
            JSONArray tmp = new JSONArray();
            data.put(key, tmp);
            return tmp;
        }
    }


    public void removeGuild(long id) {
        if (data.has(String.valueOf(id))) {
            data.remove(String.valueOf(id));
        } else {
            logger.warn("Cannot remove guild data by id: " + id);
        }
    }

    public void save() {
        try {
            FileWriter writer = new FileWriter(FILE);
            writer.write(data.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger.warn("Cannot save file: " + e.getMessage());
        }
    }
}
