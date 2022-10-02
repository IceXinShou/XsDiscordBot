package com.xs.loader.util;

import com.xs.loader.logger.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

public class JsonFileManager {
    private final String TAG;
    private final File FILE;
    private JSONObject data;
    private final Logger logger;

    public JsonFileManager(String FILE_PATH, String TAG) {
        this.TAG = TAG;
        this.FILE = new File(FILE_PATH);
        this.logger = new Logger(TAG);
        initData();
    }

    private void initData() {
        try {
            data = new JSONObject(new FileReader(FILE));
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        }
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
            logger.error("Cannot remove guild data by id: " + id);
        }
    }

    public void save() {
        try {
            FileWriter writer = new FileWriter(FILE);
            writer.write(data.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger.error("Cannot save file");
        }
    }
}
