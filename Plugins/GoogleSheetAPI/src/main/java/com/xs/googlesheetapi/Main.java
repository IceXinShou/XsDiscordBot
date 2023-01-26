package com.xs.googlesheetapi;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Main extends PluginEvent {
    private MainConfig configFile;
    private FileGetter getter;
    private Logger logger;
    public static SheetRequest sheet;
    private static final String TAG = "GoogleSheetAPI";
    private final String PATH_FOLDER_NAME = "./plugins/GoogleSheetAPI";

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        loginService();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
        logger.log("UnLoaded");
    }

    @Override
    public void loadConfigFile() {
        configFile = new Yaml(new Constructor(MainConfig.class))
                .load(getter.readYmlInputStream("config.yml", PATH_FOLDER_NAME));
        logger.log("Setting File Loaded Successfully");
    }

    private void loginService() {
        try {
            sheet = new SheetRequest(logger, configFile);
        } catch (IOException | GeneralSecurityException e) {
            logger.warn(e.getMessage());
        }
    }
}