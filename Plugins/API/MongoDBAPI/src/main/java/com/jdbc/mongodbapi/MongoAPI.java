package com.jdbc.mongodbapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.plugin.Event;

public class MongoAPI extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoAPI.class);

    public MongoAPI() {
        super(false);

        reloadAll();
        LOGGER.info("loaded MongoDBAPI");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded MongoDBAPI");
    }
}