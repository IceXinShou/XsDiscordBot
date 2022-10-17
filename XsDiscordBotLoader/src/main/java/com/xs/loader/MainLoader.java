package com.xs.loader;

import com.xs.loader.logger.Color;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.Pair;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.jar.JarFile;

import static com.xs.loader.util.JsonFileManager.streamToString;

public class MainLoader {
    public static JDA jdaBot;
    public static long botID;
    public final List<CommandData> guildCommands = new ArrayList<>();
    public final List<SubcommandData> subGuildCommands = new ArrayList<>();
    public final List<CommandData> globalCommands = new ArrayList<>();
    public static final String ROOT_PATH = new File(System.getProperty("user.dir")).toString();
    private final Queue<PluginEvent> listeners = new ArrayDeque<>();
    private final Logger logger;
    private final String version = "v1.4";
    private String BOT_TOKEN;
    private JSONObject configFile;
    private FileGetter getter;
    private final List<String> botStatus = new ArrayList<>();
    private final Map<String, Pair<PluginEvent, JSONArray>> plugins = new HashMap<>();
    private final List<PluginEvent> queue = new ArrayList<>();
    private ScheduledExecutorService threadPool;

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

        jdaBot = builder.build();

        botID = jdaBot.getSelfUser().getIdLong();
        ListenerManager listenerManager = new ListenerManager(guildCommands, subGuildCommands);
        jdaBot.addEventListener(listenerManager);

        while (!listeners.isEmpty()) {
            jdaBot.addEventListener(listeners.poll());
        }

        jdaBot.updateCommands().addCommands(globalCommands).queue();
        setStatus();
        logger.log("Bot Initialized");
        getInput();
    }

    private boolean versionCheck() {
        String latestVersion;
        String fileName;
        URL downloadURL;
        URL url;

        logger.log("Version checking...");

        try {
            url = new URL("https://github.com/IceLeiYu/XsDiscordBot/releases/latest");
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
                fos.close();
                logger.log("Download Successfully");
                logger.log("Please change to the latest version");
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

    void loadDependencies(String currentPlugin, JSONArray dependencies) {
        if (dependencies != null && !dependencies.isEmpty()) {
            for (Object depend : dependencies) {
                if (plugins.containsKey((String) depend)) {
                    loadDependencies((String) depend, plugins.get((String) depend).getValue());
                    queue.add(plugins.get(currentPlugin).getKey());
                } else {
                    logger.error("plugin: " + currentPlugin + " lost dependency: " + depend);
                }
            }
        } else {
            queue.add(plugins.get(currentPlugin).getKey());
        }
    }

    @SuppressWarnings("unchecked")
    void loadPlugins() {
        int count = 0;
        int fail = 0;
        String tmp;
        logger.log("Plugin(s) Loading...");

        for (File file : new File("plugins").listFiles()) {
            try {
                if (file == null) continue;
                if ((tmp = getExtensionName(file.getName())) == null || !tmp.equals("jar")) continue;
                JarFile jarFile = new JarFile(file);
                InputStream inputStream = jarFile.getInputStream(jarFile.getEntry("info.yml"));
                JSONObject data = new JSONObject((Map<String, Object>) new Yaml().load(streamToString(inputStream)));

                if (!plugins.containsKey(data.getString("Name"))) {
                    plugins.put(data.getString("Name"), new Pair<>((PluginEvent)
                            new URLClassLoader(
                                    new URL[]{file.toURI().toURL()},
                                    MainLoader.class.getClassLoader())
                                    .loadClass(data.getString("MainPath"))
                                    .getDeclaredConstructor().newInstance(),
                            data.has("Dependencies") ? data.getJSONArray("Dependencies") : null));
                } else {
                    logger.error("same plugin!: " + file.getName());
                    ++fail;
                    continue;
                }

                inputStream.close();
                jarFile.close();
                ++count;
            } catch (Exception e) {
                ++fail;
                logger.error(file.getName() + '\n' + e.getMessage());
            }
        }

        plugins.forEach((i, j) -> {
            if (queue.contains(j.getKey())) {
                return;
            }
            loadDependencies(i, j.getValue());
        });

        for (PluginEvent plugin : queue) {
            plugin.initLoad();

            if (plugin.guildCommands() != null) {
                Collections.addAll(guildCommands, plugin.guildCommands());
            }

            if (plugin.subGuildCommands() != null) {
                Collections.addAll(subGuildCommands, plugin.subGuildCommands());
            }

            if (plugin.globalCommands() != null) {
                Collections.addAll(globalCommands, plugin.globalCommands());
            }

            listeners.add(plugin);
        }

        if (fail > 0)
            logger.error(fail + " Plugin(s) Loading Failed!");
        logger.log(count + " Plugin(s) Loading Successfully");
    }

    private void loadConfigFile() {
        configFile = new JSONObject(readOrDefaultYml("config_0A2F7C.yml", "config.yml"));
        logger.log("Setting file loaded");
    }

    private void loadVariables() {
        JSONObject general = configFile.getJSONObject("GeneralSettings");
        BOT_TOKEN = general.getString("botToken");
        if (general.has("activityMessage") && !general.getJSONArray("activityMessage").isEmpty()) {
            for (Object i : general.getJSONArray("activityMessage")) {
                botStatus.add((String) i);
            }
        }
    }

    void setStatus() {
        if (botStatus.isEmpty()) return;
        if (threadPool != null && !threadPool.isShutdown())
            threadPool.shutdown();

        threadPool = Executors.newSingleThreadScheduledExecutor();

        threadPool.execute(() -> {
            while (true) {
                for (String i : botStatus) {
                    String[] arg = i.split(";");
                    try {
                        if (arg[0].equals("STREAMING")) {
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
        });
    }

    void getInput() {
        Scanner scanner = new Scanner(System.in);
        String command;
        while (true) {
            command = scanner.nextLine();
            switch (command.toLowerCase()) {
                case "stop":
                    for (Object listener : jdaBot.getRegisteredListeners()) {
                        jdaBot.removeEventListener(listener);
                    }

                    for (PluginEvent plugin : queue) {
                        plugin.unload();
                    }

                    plugins.clear();
                    queue.clear();

                    threadPool.shutdown();
                    jdaBot.shutdown();
                    logger.log("Stopped");
                    return;

                case "reload":
                    logger.log("Reloading...");

                    for (PluginEvent plugin : queue) {
                        plugin.unload();
                        plugin.initLoad();
                    }

                    threadPool.shutdown();
                    loadConfigFile();
                    loadVariables();
                    setStatus();

                    logger.log("Reloaded");
                    break;
                default:
                    if (command.length() != 0)
                        logger.error("Unknown Command");
                    break;
            }
        }
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