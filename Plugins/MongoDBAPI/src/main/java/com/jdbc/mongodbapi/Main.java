package com.jdbc.mongodbapi;

import com.xs.loader.plugin.Event;
import com.xs.loader.logger.Logger;

public class Main extends Event {
    Logger logger;
    private static final String TAG = "MongoDBAPI";

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