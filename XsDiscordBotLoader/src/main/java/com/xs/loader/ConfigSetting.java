package com.xs.loader;

import com.xs.loader.logger.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;


public class ConfigSetting {
    public static Map<String, Object> settings;
    private final Logger logger;
    public static String botToken;
    public static long botID;

    public ConfigSetting() {
        logger = new Logger("MainCS");
        loadConfigFile();
        loadVariables();
    }

    private void loadConfigFile() {
        settings = readYml("config_0A2F7C.yml", "config.yml");
        logger.log("Setting file loaded");
    }

    private void loadVariables() {
        Map<String, Object> general = (Map<String, Object>) settings.get("GeneralSettings");
        botToken = (String) general.get("botToken");
    }

    /* ------ Util ------ */

    public Map<String, Object> readYml(String name, String outName) {
        File settingFile = new File(System.getProperty("user.dir") + '/' + outName);
        if (!settingFile.exists()) {
            logger.error(outName + " not found, create default " + outName);
            settingFile = exportResource(name, outName);
            if (settingFile == null) {
                logger.error("read " + name + " failed");
                return null;
            }
        }
        logger.log("load " + settingFile.getPath());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            FileInputStream in = new FileInputStream(settingFile);
            int length;
            byte[] buff = new byte[1024];
            while ((length = in.read(buff)) > 0)
                out.write(buff, 0, length);

        } catch (IOException e) {
            logger.error("read " + name + " failed");
            return null;
        }
        String settingText = out.toString(StandardCharsets.UTF_8);

        return new Yaml().load(settingText);
    }

    public File exportResource(String name, String outName) {
        InputStream fileInJar = ConfigSetting.class.getClassLoader().getResourceAsStream(name);

        try {
            if (fileInJar == null) {
                logger.error("can not find resource: " + outName);
                return null;
            }
            Files.copy(fileInJar, Paths.get(System.getProperty("user.dir") + "/" + outName), StandardCopyOption.REPLACE_EXISTING);
            return new File(System.getProperty("user.dir") + "/" + outName);
        } catch (IOException e) {
            logger.error("read resource failed");
        }
        return null;
    }
}