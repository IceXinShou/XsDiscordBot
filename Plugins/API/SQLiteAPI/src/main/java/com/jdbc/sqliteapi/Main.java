package com.jdbc.sqliteapi;

import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;

public class Main extends Event {
    private static final String TAG = "SQLiteAPI";
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