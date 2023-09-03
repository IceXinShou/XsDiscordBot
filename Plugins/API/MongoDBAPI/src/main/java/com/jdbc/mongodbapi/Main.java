package com.jdbc.mongodbapi;

import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;

public class Main extends Event {
    private static final String TAG = "MongoDBAPI";
    Logger logger;

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