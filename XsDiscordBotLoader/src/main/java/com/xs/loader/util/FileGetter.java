package com.xs.loader.util;

import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class FileGetter {
    private final String FOLDER_PATH;
    private final ClassLoader LOADER;
    private final Logger logger;

    public FileGetter(final Logger logger, final String PATH_FOLDER_NAME, final ClassLoader LOADER) {
        this.logger = logger;
        this.FOLDER_PATH = MainLoader.ROOT_PATH + "/" + PATH_FOLDER_NAME;
        this.LOADER = LOADER;
    }

    public Map<String, Object> readFile(File f) {
        try {
            return new Yaml().load(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        }
        return null;
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

    public File exportResource(String sourceFile, String outputName, String outputPath) {
        InputStream fileInJar = LOADER.getResourceAsStream(sourceFile);

        try {
            if (fileInJar == null) {
                logger.error("can not find resource: " + sourceFile);
                return null;
            }
            Files.copy(fileInJar, Paths.get(FOLDER_PATH + "/" + outputPath + "/" + outputName), StandardCopyOption.REPLACE_EXISTING);
            fileInJar.close();
            return new File(FOLDER_PATH + "/" + outputPath + "/" + outputName);
        } catch (IOException e) {
            logger.error("read resource failed");
            logger.error(e.getMessage());
        }
        return null;
    }

    public boolean checkFileParameter(Map<String, Object> data, String[] parameters, String fileName) {
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

    public void copyFile(@NotNull File source, @NotNull String dest) throws IOException {
        Files.copy(source.toPath(), Paths.get(dest));
    }
}
