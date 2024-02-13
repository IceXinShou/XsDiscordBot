package com.xs.placeholderapi;

import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;

import java.util.HashMap;
import java.util.Map;

public class Main extends Event {
    protected static final Map<String, ValueGetter> placeholders = new HashMap<>();
    private static final String TAG = "PAPI";
    private Logger logger;

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