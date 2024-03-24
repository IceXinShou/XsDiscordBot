package tw.xserver.api.google.sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static tw.xserver.loader.base.Loader.ROOT_PATH;

public class SheetAPI extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetAPI.class);
    private static final String PATH_FOLDER_NAME = "plugins/GoogleSheetAPI";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    public static MainConfig configFile;

    public SheetAPI() {
        super(false);

        reloadAll();
        LOGGER.info("loaded SheetAPI");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded SheetAPI");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, SheetAPI.class);

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        LOGGER.info("setting file loaded successfully");
    }

}