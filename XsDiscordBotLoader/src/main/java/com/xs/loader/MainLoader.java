package com.xs.loader;

import com.xs.loader.logger.Color;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import javax.net.ssl.HttpsURLConnection;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;

import static com.xs.loader.util.JsonFileManager.streamToString;

public class MainLoader {
    public static String noPermissionERROR = "You have no permission";
    public static JDA jdaBot;
    public static List<CommandData> guildCommands = new ArrayList<>();
    public static List<SubcommandData> subGuildCommands = new ArrayList<>();
    public static List<CommandData> globalCommands = new ArrayList<>();
    public static final String ROOT_PATH = new File(System.getProperty("user.dir")).toString();
    private final Queue<PluginEvent> listeners = new ArrayDeque<>();
    private final Logger logger;
    private final String version = "v1.2";
    private String BOT_TOKEN;
    private long botID;
    private JSONObject settings;
    private FileGetter getter;
    private final List<String> botStatus = new ArrayList<>();

    MainLoader() {
        logger = new Logger("Main");

        if (versionCheck()) {
            return;
        }

        getter = new FileGetter(logger, "", MainLoader.class.getClassLoader());

        defaultFileInit();
        loadConfigFile();
        loadVariables();

        JDABuilder builder = JDABuilder.createDefault(BOT_TOKEN)
                .setBulkDeleteSplittingEnabled(false)
                .setLargeThreshold(250)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.ONLINE_STATUS, CacheFlag.ACTIVITY)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES);

        loadPlugins();

        try {
            jdaBot = builder.build();
        } catch (LoginException e) {
            logger.error("Token is Invalid");
            return;
        }

        botID = jdaBot.getSelfUser().getIdLong();
        ListenerManager listenerManager = new ListenerManager();
        jdaBot.addEventListener(listenerManager);

        while (!listeners.isEmpty()) {
            jdaBot.addEventListener(listeners.poll());
        }

        jdaBot.updateCommands().addCommands(globalCommands).queue();
        setStatus();
        logger.log("Bot Initialized");
    }

    private boolean versionCheck() {
        String latestVersion;
        String fileName;
        URL downloadURL;
        URL url;
        try {
            url = new URL("https://github.com/IceLeiYu/XsDiscordBot/releases/latest ");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            String tmp = conn.getURL().toString();
            latestVersion = tmp.substring(tmp.lastIndexOf('/') + 1);
            fileName = "XsDiscordBotLoader_" + latestVersion + ".jar";
            downloadURL = new URL("https://github.com/IceLeiYu/XsDiscordBot/releases/download/" + latestVersion + '/' + fileName);
            conn.disconnect();


            if (version.equals(latestVersion)) {
                logger.log("You are running on the latest version: " + Color.GREEN + version + Color.RESET);
                return false;
            } else {
                logger.error("Your current version: " + Color.RED + version + Color.RESET + ", latest version: " + Color.GREEN + latestVersion + Color.RESET);
                logger.log("Downloading latest version file...");
                logger.log("Please wait...");

                FileOutputStream fos = new FileOutputStream(ROOT_PATH + '/' + fileName);
                fos.getChannel().transferFrom(Channels.newChannel(downloadURL.openStream()), 0, Long.MAX_VALUE);

                logger.log("Download Successfully");

                Process proc = Runtime.getRuntime().exec("java -jar " + fileName);
                InputStream in = proc.getInputStream();
                OutputStream err = proc.getOutputStream();

                return true;
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return true;
    }

    void defaultFileInit() {
        logger.log("File Initializing...");
        new File("plugins").mkdir();
        logger.log("File Initialized Successfully");
    }

    String getExtensionName(@NotNull String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        }
        return null;
    }

    void loadPlugins() {
        int count = 0;
        int fail = 0;
        logger.log("Plugin(s) Loading...");
        String tmp;
        File f = new File("plugins");
        for (File file : f.listFiles()) {
            try {
                if (file == null) continue;
                if ((tmp = getExtensionName(file.getName())) == null || !tmp.equals("jar")) continue;
                JarFile jarFile = new JarFile(file);
                InputStream inputStream = jarFile.getInputStream(jarFile.getEntry("info.yml"));
                ByteArrayOutputStream result = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1)
                    result.write(buffer, 0, length);

                URLClassLoader loader = new URLClassLoader(
                        new URL[]{file.toURI().toURL()},
                        MainLoader.class.getClassLoader()
                );

                PluginEvent pluginMain = (PluginEvent) loader
                        .loadClass((String) (((Map<String, Object>) new Yaml().load(result.toString("UTF-8"))).get("MainPath")))
                        .getDeclaredConstructor().newInstance();

                pluginMain.initLoad();

                if (pluginMain.guildCommands() != null) {
                    Collections.addAll(guildCommands, pluginMain.guildCommands());
                }

                if (pluginMain.subGuildCommands() != null) {
                    Collections.addAll(subGuildCommands, pluginMain.subGuildCommands());
                }

                if (pluginMain.globalCommands() != null) {
                    Collections.addAll(globalCommands, pluginMain.globalCommands());
                }

                listeners.add(pluginMain);

                result.close();
                inputStream.close();
                jarFile.close();
                ++count;
            } catch (Exception e) {
                ++fail;
                logger.error(file.getName() + '\n' + e.getMessage());
            }
        }
        if (fail > 0)
            logger.error(fail + " Plugin(s) Loading Failed!");
        logger.log(count + " Plugin(s) Loading Successfully");
    }

    private void loadConfigFile() {
        settings = new JSONObject(readOrDefaultYml("config_0A2F7C.yml", "config.yml"));
        logger.log("Setting file loaded");
    }

    private void loadVariables() {
        JSONObject general = settings.getJSONObject("GeneralSettings");
        BOT_TOKEN = general.getString("botToken");
        if (general.has("activityMessage") && !general.getJSONArray("activityMessage").isEmpty()) {
            for (Object i : general.getJSONArray("activityMessage")) {
                botStatus.add((String) i);
            }
        }
    }

    void setStatus() {
        if (botStatus.isEmpty()) return;

        new Thread(() -> {
            while (true) {
                for (String i : botStatus) {
                    String[] arg = i.split(";");
                    try {
                        if (arg[0].equals("STREAMING")) {
                            // name, url
                            jdaBot.getPresence().setActivity(Activity.of(Activity.ActivityType.STREAMING, arg[1], arg[2]));
                            Thread.sleep(Long.parseLong(arg[3]));
                        } else {
                            jdaBot.getPresence().setActivity(Activity.of(Activity.ActivityType.valueOf(arg[0]), arg[1]));
                            Thread.sleep(Long.parseLong(arg[2]));
                        }
                    } catch (IllegalArgumentException e) {
                        logger.error("can not find type: " + arg[0]);
                        return;
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        }).start();
    }

    public Map<String, Object> readOrDefaultYml(String name, String outName) {
        File settingFile = new File(System.getProperty("user.dir") + '/' + outName);
        if (!settingFile.exists()) {
            logger.error(outName + " not found, create default " + outName);
            settingFile = getter.exportResource(name, outName, "");
            if (settingFile == null) {
                logger.error("read " + name + " failed");
                return null;
            }
        }
        logger.log("load " + settingFile.getPath());
        String settingText = null;
        try {
            settingText = streamToString(Files.newInputStream(settingFile.toPath()));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return new Yaml().load(settingText);
    }

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        new MainLoader();
        AnsiConsole.systemUninstall();
    }
}