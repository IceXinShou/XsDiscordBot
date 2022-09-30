package com.xs.loader.util;

import com.wavjaby.json.JsonArray;
import com.wavjaby.json.JsonObject;
import com.xs.loader.MainLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonFileManager {
    private final String TAG;
    private final String PLUGIN_NAME;
    public Map<Long, File> guildData;

    public JsonFileManager(String dirPath, String TAG, String PLUGIN_NAME) {
        initALlFileData(new File(dirPath));
        this.TAG = TAG;
        this.PLUGIN_NAME = PLUGIN_NAME;
    }

    public JsonObject getDataObject(long id) {
        return new JsonObject(readFile(guildData.get(id)));
    }

    public JsonArray getDataArray(long id) {
        return new JsonArray(readFile(guildData.get(id)));
    }

    public void addGuild(long id) {
        try {
            new File(MainLoader.ROOT_PATH + "\\plugins\\" + PLUGIN_NAME + "\\" + id + ".json").createNewFile();
            File file = new File(MainLoader.ROOT_PATH + "\\plugins\\" + PLUGIN_NAME + "\\" + id + ".json");
            guildData.put(id, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeGuild(long id) {
        if (guildData.get(id).delete()) {
            System.err.println(TAG + " can not remove file");
        }
        guildData.remove(id);
    }

    public void saveGuildData(long id, JsonObject object) {
        try {
            FileOutputStream fileWriter = new FileOutputStream(guildData.get(id));
            ByteArrayInputStream in = new ByteArrayInputStream(object.toString().getBytes(StandardCharsets.UTF_8));
            byte[] buff = new byte[1024];
            int length;
            while ((length = in.read(buff)) > 0) {
                fileWriter.write(buff, 0, length);
            }
            in.close();
            fileWriter.close();
        } catch (IOException e) {
            System.err.println(TAG + " can not save file");
        }
    }

    private void initALlFileData(final File folder) {
        try {
            folder.mkdirs();
            for (final File file : folder.listFiles()) {
                guildData.put(Long.parseLong(file.getName()), file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFile(String filepath) {
        File file = new File(filepath);
        return readFile(file);
    }

    private String readFile(File file) {
        if (!file.exists())
            return null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            InputStream in = new FileInputStream(file);
            byte[] buff = new byte[1024];
            int length;
            while ((length = in.read(buff)) > 0) {
                out.write(buff, 0, length);
            }
            in.close();
            out.close();
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
