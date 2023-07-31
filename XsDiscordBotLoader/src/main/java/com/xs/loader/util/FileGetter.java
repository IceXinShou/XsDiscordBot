package com.xs.loader.util;

import com.xs.loader.base.Loader;
import com.xs.loader.logger.Logger;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class FileGetter {
    private final String FOLDER_PATH;
    private final Class<?> CLASS;
    private final Logger logger;

    public FileGetter(final Logger logger, final String PATH_FOLDER_NAME, final Class<?> CLASS) {
        this.logger = logger;
        this.FOLDER_PATH = Loader.ROOT_PATH + "/" + PATH_FOLDER_NAME;
        this.CLASS = CLASS;

        new File(FOLDER_PATH).mkdirs();
    }

    @Nullable
    public Map<String, Object> readFileMapByPath(Path f) {
        try {
            return new Yaml().load(Files.newInputStream(f));
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
        return null;
    }

    @Nullable
    public Map<String, Object> readFileMapByPathOrDefaultFromSource(String fileName) {
        File settingFile = new File(FOLDER_PATH + "/" + fileName);
        if (!settingFile.exists()) {
            logger.warn(fileName + " not found, create default " + fileName);
            settingFile = exportResource(fileName);
            if (settingFile == null) {
                logger.warn("read " + fileName + " failed");
                return null;
            }
        }
        logger.log("load " + settingFile.getPath());

        return readFileMapByPath(settingFile.toPath());
    }

    @Nullable
    public InputStream readInputStreamOrDefaultFromSource(String fileName) {
        File settingFile = new File(FOLDER_PATH + '/' + fileName);
        if (!settingFile.exists()) {
            logger.warn(fileName + " not found, create default " + fileName);
            settingFile = exportResource(fileName);
            if (settingFile == null) {
                logger.warn("read " + fileName + " failed");
                return null;
            }
        }
        logger.log("load " + settingFile.getPath());

        try {
            return Files.newInputStream(settingFile.toPath());
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }

        return null;
    }

    /**
     * @param sourceFilePath ex. "/info.yml"
     */
    @Nullable
    public InputStream getResource(String sourceFilePath) {
        return CLASS.getResourceAsStream(sourceFilePath);
    }

    /**
     * @param sourceFilePath ex. "/info.yml"
     */
    @Nullable
    public File exportResource(String sourceFilePath) {
        try (InputStream fileInJar = CLASS.getResourceAsStream(sourceFilePath)) {
            if (fileInJar == null) {
                logger.warn("can not find resource: " + sourceFilePath);
                return null;
            }

            Files.copy(fileInJar, Paths.get(FOLDER_PATH + '/' + sourceFilePath), StandardCopyOption.REPLACE_EXISTING);
            return new File(FOLDER_PATH + '/' + sourceFilePath);
        } catch (IOException e) {

            logger.warn(e.getMessage());
            logger.warn("read resource failed");
        }
        return null;
    }

    /**
     * @param sourceFilePath ex. "/info.yml"
     * @param outputPath     ex."/data/info.yml"
     */
    @Nullable
    public File exportResource(String sourceFilePath, String outputPath) {
        try (InputStream fileInJar = CLASS.getResourceAsStream(sourceFilePath)) {
            if (fileInJar == null) {
                logger.warn("can not find resource: " + sourceFilePath);
                return null;
            }

            Files.copy(fileInJar, Paths.get(FOLDER_PATH + '/' + outputPath), StandardCopyOption.REPLACE_EXISTING);
            return new File(FOLDER_PATH + '/' + outputPath);
        } catch (IOException e) {
            logger.warn("read resource failed");
            logger.warn(e.getMessage());
        }

        return null;
    }
}
