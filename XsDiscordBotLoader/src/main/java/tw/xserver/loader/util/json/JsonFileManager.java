package tw.xserver.loader.util.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.base.Loader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

abstract class JsonFileManager<T extends JsonElement> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(JsonFileManager.class);
    protected final File FILE;
    protected T data;

    public JsonFileManager(String FILE_PATH) {
        this.FILE = new File(Loader.ROOT_PATH + '/' + FILE_PATH);
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
            LOGGER.error(e.getMessage());
        }
    }

    protected abstract Class<T> getDataType();

    protected abstract T createData();

    public T get() {
        return data;
    }

    public JsonObject getAsJsonObject() {
        return data.getAsJsonObject();
    }

    public JsonArray getAsJsonArray() {
        return data.getAsJsonArray();
    }

    public synchronized void save() {
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(FILE.toPath()), StandardCharsets.UTF_8)) {
                writer.write(data.toString());
                writer.flush();
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot save file: {}", e.getMessage());
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
