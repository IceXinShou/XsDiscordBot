package com.xs.loader.util;

import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;

public class JsonFileManager {
    private final File FILE;
    private JSONObject data_obj;
    private JSONArray data_ary;
    private final Logger logger;
    private final boolean isObject;

    public JsonFileManager(String FILE_PATH, String TAG, boolean isObject) {
        this.FILE = new File(MainLoader.ROOT_PATH + '/' + FILE_PATH);
        this.logger = new Logger(TAG);
        this.isObject = isObject;
        initData();
    }

    private void initData() {
        String tmp;
        try {
            if (!FILE.exists() || (tmp = streamToString(Files.newInputStream(FILE.toPath()))).length() == 0) {
                FILE.createNewFile();
                FileWriter writer = new FileWriter(FILE);
                if (isObject) {
                    writer.write("{}");
                    data_obj = new JSONObject("{}");
                } else {
                    writer.write("[]");
                    data_ary = new JSONArray("[]");
                }
                writer.flush();
                writer.close();
                return;
            }

            if (isObject) {
                data_obj = new JSONObject(tmp);
            } else
                data_ary = new JSONArray(tmp);
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }


    public JSONObject getObj() {
        return data_obj;
    }

    public JSONArray getAry() {
        return data_ary;
    }

    public JSONObject getOrDefault(String key) {
        if (isObject) {
            if (data_obj.has(key)) return data_obj.getJSONObject(key);
            else {
                JSONObject tmp = new JSONObject();
                data_obj.put(key, tmp);
                return tmp;
            }
        }
        return null;
    }

    public JSONArray getOrDefaultArray(String key) {
        if (data_obj.has(key)) return data_obj.getJSONArray(key);
        else {
            JSONArray tmp = new JSONArray();
            data_obj.put(key, tmp);
            return tmp;
        }
    }


    public void removeObj(long id) {
        if (data_obj.has(String.valueOf(id))) {
            data_obj.remove(String.valueOf(id));
        } else {
            logger.warn("Cannot remove guild data by id: " + id);
        }
    }

    public void save() {
        try {
            FileWriter writer = new FileWriter(FILE);
            if (isObject)
                writer.write(data_obj.toString());
            else
                writer.write(data_obj.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger.warn("Cannot save file: " + e.getMessage());
        }
    }

    public static String streamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString();
    }
}
