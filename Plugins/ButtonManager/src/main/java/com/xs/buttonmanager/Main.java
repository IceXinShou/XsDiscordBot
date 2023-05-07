package com.xs.buttonmanager;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;

public class Main extends PluginEvent {
    private Logger logger;
    private FileGetter getter;
    private static final String TAG = "ButtonManager";
    private final String PATH_FOLDER_NAME = "plugins/ButtonManager";

    public Main() {
        super(false);
    }

    @Override
    public void initLoad() {
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        logger = new Logger(TAG);
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }


    public void test() {

    }
}