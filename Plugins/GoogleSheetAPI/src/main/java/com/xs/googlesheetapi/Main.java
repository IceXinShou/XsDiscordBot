package com.xs.googlesheetapi;

import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;

public class Main extends Event {
    private static final String TAG = "GoogleSheetAPI";
    public static MainConfig configFile;
    private final String PATH_FOLDER_NAME = "plugins/GoogleSheetAPI";
    private FileGetter getter;
    private Logger logger;

    public Main() {
        super(false);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadConfigFile();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadConfigFile() {
        InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml");
        if (inputStream == null) return;

        try {
            configFile = new Yaml(new Constructor(MainConfig.class)).load(inputStream);
            inputStream.close();
            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            e.printStackTrace();
        }
        logger.log("Setting File Loaded Successfully");
    }

}