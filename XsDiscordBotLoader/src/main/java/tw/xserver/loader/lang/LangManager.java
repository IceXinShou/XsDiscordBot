package tw.xserver.loader.lang;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.base.Loader;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class LangManager<E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LangManager.class);
    private final E langData;
    private final String FOLDER_PATH;
    private final DiscordLocale DEFAULT_LOCALE;
    private final FileGetter getter;


    public LangManager(FileGetter getter, String pathFolderName, DiscordLocale defaultLocale, Class<E> clazz)
            throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.getter = getter;
        this.FOLDER_PATH = Loader.ROOT_PATH + '/' + pathFolderName + "/lang/";
        this.DEFAULT_LOCALE = defaultLocale;
        this.langData = clazz.getConstructor().newInstance();

        new File(FOLDER_PATH).mkdirs();
        exportDefaultLang();

        boolean hasDefaultLocale = false;
        for (File i : new File(FOLDER_PATH).listFiles()) {
            DiscordLocale locale = DiscordLocale.from(i.getName().replaceAll("\\.\\w+$", ""));
            if (locale == DiscordLocale.UNKNOWN) {
                LOGGER.warn("cannot find discord locate by file: " + i.getAbsolutePath());
                continue;
            }

            if (locale == defaultLocale)
                hasDefaultLocale = true;

            readLang(getter.readMap(i.toPath()), langData, locale);
        }

        if (!hasDefaultLocale)
            throw new NoSuchFieldError();
    }

    public E get() {
        return langData;
    }

    private void exportDefaultLang() throws IOException {
        String[] langFiles = getter.getResources("/lang/");

        if (langFiles.length == 0) {
            LOGGER.error("cannot find default languages");
            throw new FileNotFoundException();
        }

        for (String langFilename : langFiles) {
            File old_lang_file = new File(FOLDER_PATH + '/' + langFilename);
            if (old_lang_file.exists()) continue;

            // export is not exist
            getter.exportResource("lang/" + langFilename);
        }
    }

    private void readLang(Object json, Object obj, DiscordLocale locale) throws IllegalAccessException {
        if (!(json instanceof Map)) {
            return;
        }

        //noinspection unchecked
        Map<String, Object> map = (Map<String, Object>) json;

        for (final Map.Entry<String, Object> i : map.entrySet()) {
            Field field = tryGetField(obj.getClass(), i.getKey());
            if (field == null) {
                LOGGER.error("cannot find field: {}, {}", langData.getClass().getName(), i.getKey());
                return;
            }


            if (!(i.getValue() instanceof String)) {
                Object nextObj = field.get(obj);
                readLang(i.getValue(), nextObj, locale);
                continue;
            }

            LocaleData localeData = (LocaleData) field.get(obj);
            if (localeData == null) {
                localeData = new LocaleData();
                field.set(obj, localeData);
            }

            localeData.setDefaultLocale(DEFAULT_LOCALE);
            localeData.put(locale, (String) i.getValue());
        }
    }

    @Nullable
    private Field tryGetField(Class<?> clazz, String fieldName) {
        Field f = null;
        try {
            f = clazz.getField(fieldName);
            f.setAccessible(true);
        } catch (NoSuchFieldException e1) {
            try {
                f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
            } catch (NoSuchFieldException e2) {
                LOGGER.error("cannot find fieldName: {}", fieldName);
                LOGGER.error(e2.getMessage());
            }
        }

        return f;
    }
}