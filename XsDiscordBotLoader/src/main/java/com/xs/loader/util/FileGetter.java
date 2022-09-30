package com.xs.loader.util;

import com.wavjaby.json.JsonArray;
import com.wavjaby.json.JsonObject;
import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class FileGetter {
    private final String TAG;
    private final String PATH_FOLDER_NAME;
    private final ClassLoader LOADER;
    private final Logger logger;

    public FileGetter(final String TAG, final String PATH_FOLDER_NAME, final ClassLoader LOADER) {
        logger = new Logger(TAG);
        this.TAG = TAG;
        this.PATH_FOLDER_NAME = PATH_FOLDER_NAME;
        this.LOADER = LOADER;
    }

    public static JsonObject getOrDefault(JsonObject input, String key) {
        if (input.containsKey(key))
            return input.getJson(key);
        else {
            JsonObject data = new JsonObject();
            input.put(key, data);
            return data;
        }
    }

    public static JsonArray getOrDefaultArray(JsonObject input, String key) {
        if (input.containsKey(key))
            return input.getArray(key);
        else {
            JsonArray data = new JsonArray();
            input.put(key, data);
            return data;
        }
    }

    public Map<String, Object> readFile(File flie) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(flie);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return new Yaml().load(inputStream);
    }

    public Map<String, Object> readYml(String fileName, String path) {
        new File(MainLoader.ROOT_PATH + "/" + path).mkdirs();
        File settingFile = new File(MainLoader.ROOT_PATH + "/" + path + "/" + fileName);
        if (!settingFile.exists()) {
            logger.error(fileName + " not found, create default " + fileName);
            settingFile = exportResource(fileName, path);
            if (settingFile == null) {
                logger.error("read " + fileName + " failed");
                return null;
            }
        }
        logger.log("load " + settingFile.getPath());

        return readFile(settingFile);
    }
//    public Map<String, Object> readYml(String originFileName, String outputFileName, String path) {
//        new File(ROOT_PATH + "/" + path).mkdirs();
//        File settingFile = new File(ROOT_PATH + "/" + path + "/" + outputFileName);
//        if (!settingFile.exists()) {
//            System.err.println(TAG + ' ' + outputFileName + " not found, create default " + outputFileName);
//            settingFile = exportResource(originFileName, outputFileName, path);
//            if (settingFile == null) {
//                System.err.println(TAG + " read " + outputFileName + " failed");
//                return null;
//            }
//        }
//        System.out.println(TAG + " load " + settingFile.getPath());
//
//        return readFile(settingFile);
//    }

    public File exportResource(String fileName, String path) {
        InputStream fileInJar = LOADER.getResourceAsStream(fileName);

        try {
            if (fileInJar == null) {
                logger.error("can not find resource: " + fileName);
                return null;
            }
            Files.copy(fileInJar, Paths.get(MainLoader.ROOT_PATH + "/" + path + "/" + fileName), StandardCopyOption.REPLACE_EXISTING);
            return new File(MainLoader.ROOT_PATH + "/" + path + "/" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("read resource failed");
        }
        return null;
    }

    public File exportResource(String originFileName, String outputName, String path) {
        InputStream fileInJar = LOADER.getResourceAsStream(originFileName);

        try {
            if (fileInJar == null) {
                logger.error("can not find resource: " + originFileName);
                return null;
            }
            Files.copy(fileInJar, Paths.get(MainLoader.ROOT_PATH + "/" + path + "/" + outputName), StandardCopyOption.REPLACE_EXISTING);
            return new File(MainLoader.ROOT_PATH + "/" + path + "/" + outputName);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("read resource failed");
        }
        return null;
    }

    private void copyFile(File source, File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath());
    }

    private void copyFile(File source, String dest) throws IOException {
        Files.copy(source.toPath(), Path.of(dest));
    }

    public void exportLang(String[] lang_name, String[] parameters) {
        new File(MainLoader.ROOT_PATH + "/plugins/" + PATH_FOLDER_NAME + "/Lang").mkdirs();
        for (var lang : lang_name) {
            File lang_file;
            if (!(lang_file = new File(MainLoader.ROOT_PATH + "/plugins/" + PATH_FOLDER_NAME + "/Lang/" + lang + ".yml")).exists()) {
                exportResource("lang/" + lang + ".yml", lang + ".yml", "plugins/" + PATH_FOLDER_NAME + "/Lang");
            } else {
                var fileData = readFile(lang_file);
                for (var parameter : parameters) {
                    if (!fileData.containsKey(parameter)) {
                        logger.error("file " + lang + ".yml lost " + parameter + " parameter!");
                        try {
                            copyFile(lang_file, MainLoader.ROOT_PATH + "/plugins/" + PATH_FOLDER_NAME + "/Lang/-" + lang + ".yml");
                            //exportResource(prefix + lang + ".yml", lang + ".yml", "plugins/Lang/" + PLUGIN_NAME);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public Map<String, String> getLangFileData(String langCode) {
        File f;
        Map<String, String> lang = new HashMap<>();
        if (langCode == null || !(f = new File(MainLoader.ROOT_PATH + "/plugins/" + PATH_FOLDER_NAME + "/Lang/" + langCode + ".yml")).exists()) {
            f = new File(MainLoader.ROOT_PATH + "/plugins/" + PATH_FOLDER_NAME + "/Lang/en_US.yml");
        }

        try {
            readFile(f).forEach((i, j) -> lang.put(i, (String) j));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lang;
    }
}
