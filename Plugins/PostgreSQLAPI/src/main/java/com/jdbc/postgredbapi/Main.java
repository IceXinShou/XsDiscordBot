package com.jdbc.postgredbapi;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;

public class Main extends PluginEvent {
    Logger logger;
    private static final String TAG = "PostgreDBAPI";

    public Main() {
        super(false);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }
}