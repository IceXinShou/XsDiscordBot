package com.xs.placeholderapi;

import com.xs.loader.plugin.Event;
import com.xs.loader.logger.Logger;

import java.util.HashMap;
import java.util.Map;

public class Main extends Event {
    protected static final Map<String, ValueGetter> placeholders = new HashMap<>();
    private Logger logger;
    private static final String TAG = "PAPI";

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