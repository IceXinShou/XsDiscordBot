package com.xs.loader.util;

import com.xs.loader.MainLoader;
import com.xs.loader.logger.Logger;
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
        this.FOLDER_PATH = MainLoader.ROOT_PATH + "/" + PATH_FOLDER_NAME;
        this.CLASS = CLASS;
    }

    public Map<String, Object> readFileMap(Path f) {
        try {
            return new Yaml().load(Files.newInputStream(f));
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
        return null;
    }

    public Map<String, Object> readYml(String fileName, String path) {
        new File(MainLoader.ROOT_PATH + "/" + path).mkdirs();
        File settingFile = new File(MainLoader.ROOT_PATH + "/" + path + "/" + fileName);
        if (!settingFile.exists()) {
            logger.warn(fileName + " not found, create default " + fileName);
            settingFile = exportResource(fileName, path);
            if (settingFile == null) {
                logger.warn("read " + fileName + " failed");
                return null;
            }
        }
        logger.log("load " + settingFile.getPath());

        return readFileMap(settingFile.toPath());
    }

    public InputStream readYmlInputStream(String fileName, String path) {
        new File(MainLoader.ROOT_PATH + "/" + path).mkdirs();
        File settingFile = new File(MainLoader.ROOT_PATH + "/" + path + "/" + fileName);
        if (!settingFile.exists()) {
            logger.warn(fileName + " not found, create default " + fileName);
            settingFile = exportResource(fileName, path);
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

    public File exportResource(String sourceFileName, String outputPath) {
        InputStream fileInJar = CLASS.getResourceAsStream(sourceFileName);
        try {
            if (fileInJar == null) {
                logger.warn("can not find resource: " + sourceFileName);
                return null;
            }
            Files.copy(fileInJar, Paths.get(MainLoader.ROOT_PATH + "/" + outputPath + "/" + sourceFileName), StandardCopyOption.REPLACE_EXISTING);
            fileInJar.close();
            return new File(MainLoader.ROOT_PATH + "/" + outputPath + "/" + sourceFileName);
        } catch (IOException e) {
            logger.warn(e.getMessage());
            logger.warn("read resource failed");
        }
        return null;
    }

    public File exportResource(String sourceFile, String outputName, String outputPath) {
        InputStream fileInJar = CLASS.getResourceAsStream(sourceFile);

        try {
            if (fileInJar == null) {
                logger.warn("can not find resource: " + sourceFile);
                return null;
            }
            Files.copy(fileInJar, Paths.get(FOLDER_PATH + "/" + outputPath + "/" + outputName), StandardCopyOption.REPLACE_EXISTING);
            fileInJar.close();
            return new File(FOLDER_PATH + "/" + outputPath + "/" + outputName);
        } catch (IOException e) {
            logger.warn("read resource failed");
            logger.warn(e.getMessage());
        }
        return null;
    }

    private void copyFile(File source, File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath());
    }

    public void copyFile(File source, String dest) throws IOException {
        Files.copy(source.toPath(), Paths.get(dest));
    }
}
