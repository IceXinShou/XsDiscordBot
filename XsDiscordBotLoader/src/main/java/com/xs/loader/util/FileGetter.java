package com.xs.loader.util;

import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class FileGetter {
    private final String FOLDER_PATH;
    private final ClassLoader LOADER;
    private final Logger logger;
    private final String[] defaultLang;
    private final String[] parameters;

    public FileGetter(final String TAG, final String PATH_FOLDER_NAME, final String[] DEFAULT_LANG, final String[] PARAMETERS, final ClassLoader LOADER) {
        logger = new Logger(TAG);
        this.FOLDER_PATH = MainLoader.ROOT_PATH + "/plugins/" + PATH_FOLDER_NAME;
        this.LOADER = LOADER;
        this.parameters = PARAMETERS;
        this.defaultLang = DEFAULT_LANG;
    }

    public Map<String, Object> readFile(File f) {
        try {
            return new Yaml().load(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Nullable
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

    @Nullable
    public File exportResource(String sourceFileName, String outputPath) {
        InputStream fileInJar = LOADER.getResourceAsStream(sourceFileName);

        try {
            if (fileInJar == null) {
                logger.error("can not find resource: " + sourceFileName);
                return null;
            }
            Files.copy(fileInJar, Paths.get(MainLoader.ROOT_PATH + "/" + outputPath + "/" + sourceFileName), StandardCopyOption.REPLACE_EXISTING);
            fileInJar.close();
            return new File(MainLoader.ROOT_PATH + "/" + outputPath + "/" + sourceFileName);
        } catch (IOException e) {
            logger.error(e.getMessage());
            logger.error("read resource failed");
        }
        return null;
    }

    public void exportResource(String sourceFile, String outputName, String outputPath) {
        InputStream fileInJar = LOADER.getResourceAsStream(sourceFile);

        try {
            if (fileInJar == null) {
                logger.error("can not find resource: " + sourceFile);
                return;
            }
            Files.copy(fileInJar, Paths.get(FOLDER_PATH + "/" + outputPath + "/" + outputName), StandardCopyOption.REPLACE_EXISTING);
            fileInJar.close();
        } catch (IOException e) {
            logger.error("read resource failed");
            logger.error(e.getMessage());
        }
    }

    public void exportDefaultLang() {
        // init folder
        new File(FOLDER_PATH + "/Lang").mkdirs();

        for (String l : defaultLang) {
            String fileName = l + ".yml";
            File lang_file;
            if ((lang_file = new File(FOLDER_PATH + "/Lang/" + fileName)).exists()) {
                Map<String, Object> fileData = readFile(lang_file);
                if (checkFileParameter(fileData, fileName)) {
                    logger.error("Create default lang: " + fileName);
                    try {
                        copyFile(lang_file, FOLDER_PATH + "/Lang/-" + fileName);
                        exportResource("lang/" + fileName, fileName, "Lang");
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                }
                continue;
            }

            // export is not exist
            exportResource("lang/" + fileName, fileName, "Lang");
        }
    }

    public Map<String, String> getLangFileData(final String LANG) {
        File f;
        Map<String, String> langMap = new HashMap<>();
        if (LANG == null || (!(f = new File(FOLDER_PATH + "/Lang/" + LANG + ".yml")).exists())) {
            f = new File(FOLDER_PATH + "/Lang/en_US.yml");
        }

        try {
            readFile(f).forEach((i, j) -> langMap.put(i, (String) j));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return langMap;
    }

    public boolean checkFileParameter(Map<String, Object> data, String fileName) {
        boolean error = false;
        for (String parameter : parameters) {
            if (!data.containsKey(parameter)) {
                logger.error("file " + fileName + " lost " + parameter + " parameter!");
                error = true;
            }
        }
        return error;
    }

    private void copyFile(@NotNull File source, @NotNull File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath());
    }

    private void copyFile(@NotNull File source, @NotNull String dest) throws IOException {
        Files.copy(source.toPath(), Paths.get(dest));
    }
}
