package com.jdbc.postgredbapi;

import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;

public class Main extends Event {
    private static final String TAG = "PostgreDBAPI";
    Logger logger;

    public Main() {
        super(false);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        logger.logln("Loaded");
    }

    @Override
    public void unload() {
        logger.logln("UnLoaded");
    }
}