package com.jdbc.sqliteapi;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;

public class Main extends PluginEvent {
    Logger logger;
    private static final String TAG = "SQLiteAPI";

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
        logger.log("UnLoaded");
    }
}