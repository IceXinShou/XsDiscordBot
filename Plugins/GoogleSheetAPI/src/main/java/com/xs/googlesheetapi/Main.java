package com.xs.googlesheetapi;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

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
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadConfigFile() {
        configFile = new Yaml(new Constructor(MainConfig.class))
                .load(getter.readYmlInputStream("config.yml", this.getClass(), PATH_FOLDER_NAME));
        logger.log("Setting File Loaded Successfully");
    }

}