package com.jdbc.postgredbapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.plugin.Event;

public class PostgreAPI extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreAPI.class);

    public PostgreAPI() {
        super(false);

        reloadAll();
        LOGGER.info("loaded PostgreDBAPI");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded PostgreDBAPI");
    }
}