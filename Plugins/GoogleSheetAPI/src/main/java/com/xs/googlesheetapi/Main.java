package com.xs.googlesheetapi;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;

public class Main extends PluginEvent {
    public static MainConfig configFile;
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "GoogleSheetAPI";
    private final String PATH_FOLDER_NAME = "./plugins/GoogleSheetAPI";

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
        InputStream inputStream = getter.readYmlInputStream("config.yml", PATH_FOLDER_NAME);
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