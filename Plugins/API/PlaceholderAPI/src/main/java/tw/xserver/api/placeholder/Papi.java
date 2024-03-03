package tw.xserver.api.placeholder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.plugin.Event;

import java.util.HashMap;
import java.util.Map;

public class Papi extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(Papi.class);
    protected static final Map<String, ValueGetter> placeholders = new HashMap<>();


    public Papi() {
        super(false);

        reloadAll();
        LOGGER.info("loaded PlaceholderAPI");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded PlaceholderAPI");
    }
}