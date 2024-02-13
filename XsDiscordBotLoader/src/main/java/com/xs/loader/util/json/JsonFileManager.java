package com.xs.loader.util.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.xs.loader.base.Loader;
import com.xs.loader.logger.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

abstract class JsonFileManager<T extends JsonElement> {
    protected final File FILE;
    protected final Logger logger;
    protected T data;

    public JsonFileManager(String FILE_PATH, String TAG) {
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
                        data = new Gson().fromJson(tmp, getDataType());
                        return;
                    }
                }
            } else {
                FILE.createNewFile();
            }

            data = createData();
            save();
        } catch (IOException e) {
            logger.warnln(e.getMessage());
        }
    }

    protected abstract Class<T> getDataType();

    protected abstract T createData();

    public T get() {
        return data;
    }

    public synchronized void save() {
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(FILE.toPath()), StandardCharsets.UTF_8)) {
                writer.write(data.toString());
                writer.flush();
            }
        } catch (IOException e) {
            logger.warnln("Cannot save file: " + e.getMessage());
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
