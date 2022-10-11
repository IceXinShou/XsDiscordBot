package com.xs.loader.lang;

import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class LangGetter {
    private final String FOLDER_PATH;
    private final Logger logger;
    private final String[] defaultLang;
    private final String[] defaultParameter;
    private final String lang;
    private final FileGetter getter;

    public LangGetter(final String TAG, final FileGetter GETTER, final String PATH_FOLDER_NAME, final String[] DEFAULT_LANG, final String[] DEFAULT_PARAMETER, final String LANG) {
        logger = new Logger(TAG);
        this.FOLDER_PATH = MainLoader.ROOT_PATH + "/" + PATH_FOLDER_NAME;
        this.getter = GETTER;
        this.defaultLang = DEFAULT_LANG;
        this.lang = LANG;
        this.defaultParameter = DEFAULT_PARAMETER;
    }


    public void exportDefaultLang() {
        // init folder
        new File(FOLDER_PATH + "/Lang").mkdirs();

        for (String l : defaultLang) {
            String fileName = l + ".yml";
            File lang_file;
            if ((lang_file = new File(FOLDER_PATH + "/Lang/" + fileName)).exists()) {
                Map<String, Object> fileData = getter.readFile(lang_file);
                if (getter.checkFileParameter(fileData, defaultParameter, fileName)) {
                    logger.error("Create default lang: " + fileName);
                    try {
                        getter.copyFile(lang_file, FOLDER_PATH + "/Lang/-" + fileName);
                        getter.exportResource("lang/" + fileName, fileName, "Lang");
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                }
                continue;
            }

            // export is not exist
            getter.exportResource("lang/" + fileName, fileName, "Lang");
        }
    }

    public JSONObject getLangFileData() {
        File f;
        if (lang == null || (!(f = new File(FOLDER_PATH + "/Lang/" + lang + ".yml")).exists())) {
            f = new File(FOLDER_PATH + "/Lang/en_US.yml");
        }

        try {
            return new JSONObject(getter.readFile(f));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return null;
    }
}
