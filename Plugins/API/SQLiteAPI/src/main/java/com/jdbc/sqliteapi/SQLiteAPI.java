package com.jdbc.sqliteapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.plugin.Event;

public class SQLiteAPI extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteAPI.class);

    public SQLiteAPI() {
        super(false);

        reloadAll();
        LOGGER.info("loaded SQLiteAPI");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded SQLiteAPI");
    }
}