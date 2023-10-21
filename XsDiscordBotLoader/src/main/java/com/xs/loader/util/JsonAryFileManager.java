package com.xs.loader.util;

import com.google.gson.*;
import com.xs.loader.base.Loader;
import com.xs.loader.logger.Logger;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JsonAryFileManager {
    private final File FILE;
    private final Logger logger;
    private JsonArray data;


    public JsonAryFileManager(String FILE_PATH, String TAG) {
        this.FILE = new File(Loader.ROOT_PATH + '/' + FILE_PATH);
        this.logger = new Logger(TAG);
        initData();
    }

    private synchronized void initData() {
        try {
            if (FILE.exists()) {
                try (InputStream inputStream = Files.newInputStream(FILE.toPath())) {
                    String tmp = streamToString(inputStream);
                    if (!tmp.isEmpty()) {
                        data = new Gson().fromJson(tmp, JsonArray.class);
                        return;
                    }
                }
            } else {
                FILE.createNewFile();
            }

            data = new JsonArray();
            save();
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    public JsonArray get() {
        return data;
    }

    public JsonElement get(int index) {
        return data.get(index);
    }

    public String getAsString(int index) {
        return data.get(index).getAsString();
    }

    public byte getByte(int index) {
        return data.get(index).getAsByte();
    }

    public short getShort(int index) {
        return data.get(index).getAsShort();
    }

    public int getInt(int index) {
        return data.get(index).getAsInt();
    }

    public long getLong(int index) {
        return data.get(index).getAsLong();
    }

    public Double getDouble(int index) {
        return data.get(index).getAsDouble();
    }

    public boolean getBoolean(int index) {
        return data.get(index).getAsBoolean();
    }

    public JsonObject getJsonObject(int index) {
        return data.get(index).getAsJsonObject();
    }

    public JsonArray getJsonArray(int index) {
        return data.get(index).getAsJsonArray();
    }

    public BigInteger getBigInteger(int index) {
        return data.get(index).getAsBigInteger();
    }

    public BigDecimal getBigDecimal(int index) {
        return data.get(index).getAsBigDecimal();
    }

    public JsonPrimitive getJsonPrimitive(int index) {
        return data.get(index).getAsJsonPrimitive();
    }

    public Number getNumber(int index) {
        return data.get(index).getAsNumber();
    }

    public boolean remove(int index) {
        try {
            data.remove(index);
        } catch (IndexOutOfBoundsException e) {
            return true;
        }
        return false;
    }

    public synchronized void save() {
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(FILE.toPath()), StandardCharsets.UTF_8)) {
                writer.write(data.toString());
                writer.flush();
            }
        } catch (IOException e) {
            logger.warn("Cannot save file: " + e.getMessage());
        }
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
}
