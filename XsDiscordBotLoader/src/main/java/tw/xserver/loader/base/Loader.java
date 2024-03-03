package tw.xserver.loader.base;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import tw.xserver.loader.Main;
import tw.xserver.loader.logger.Color;
import tw.xserver.loader.plugin.ClassLoader;
import tw.xserver.loader.plugin.Config;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.plugin.Info;
import tw.xserver.loader.util.Setting;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.jar.JarFile;

import static tw.xserver.loader.util.GlobalUtil.getExtensionByName;

public class Loader {
    public static final String ROOT_PATH = new File(System.getProperty("user.dir")).toString();
    public static JDA jdaBot;
    public static User bot;
    public static long botID;
    private final List<CommandData> guildCommands = new ArrayList<>();
    private final List<CommandData> globalCommands = new ArrayList<>();
    private final Queue<Event> listeners = new ArrayDeque<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class);
    private final List<String> botStatus = new ArrayList<>();
    private final Map<String, Info> plugins = new HashMap<>();
    private final LinkedHashMap<String, Event> plugin_queue = new LinkedHashMap<>();
    private String BOT_TOKEN;
    private Setting configFile;
    private ScheduledExecutorService threadPool;

    public Loader() throws IOException {
        if (versionCheck()) {
            return;
        }

        defaultFileInit();
        loadSettingFile();
        loadVariables();
        loadPlugins();

        JDABuilder builder = JDABuilder.createDefault(BOT_TOKEN)
                .setBulkDeleteSplittingEnabled(false)
                .setLargeThreshold(250)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(
                        CacheFlag.ONLINE_STATUS,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.ACTIVITY
                )
                .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_PRESENCES,
                        GatewayIntent.MESSAGE_CONTENT
                );

        jdaBot = builder.build();
        bot = jdaBot.getSelfUser();
        botID = bot.getIdLong();

        jdaBot.addEventListener(new ListenerManager(guildCommands, plugin_queue));
        while (!listeners.isEmpty())
            jdaBot.addEventListener(listeners.poll());

        jdaBot.updateCommands().addCommands(globalCommands).queue();
        setStatus();

        LOGGER.info("bot initialized");
    }

    private boolean versionCheck() {
        String latestVersion;
        String fileName;
        URL downloadURL;
        URL url;
        if (Main.arg.ignore_version_check) {
            LOGGER.info("you ignored the version check");
            return false;
        }
        LOGGER.info("checking version...");

        try {
            url = new URL("https://github.com/IceXinShou/XsDiscordBot/releases/latest");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            String tmp = conn.getURL().toString();
            latestVersion = tmp.substring(tmp.lastIndexOf('/') + 1);
            fileName = "XsDiscordBotLoader_" + latestVersion + ".jar";
            downloadURL = new URL("https://github.com/IceXinShou/XsDiscordBot/releases/download/" + latestVersion + '/' + fileName);
            conn.disconnect();


            final String version = "v1.6";
            if (version.equals(latestVersion)) {
                LOGGER.info("you are running on the latest version: " + Color.GREEN + version + Color.RESET);
                return false;
            } else {
                LOGGER.warn("your current version: " + Color.RED + version + Color.RESET + ", latest version: " + Color.GREEN + latestVersion + Color.RESET);
                LOGGER.info("downloading latest version file...");
                LOGGER.info("please wait...");

                try (FileOutputStream fos = new FileOutputStream(ROOT_PATH + '/' + fileName)) {
                    fos.getChannel().transferFrom(Channels.newChannel(downloadURL.openStream()), 0, Long.MAX_VALUE);
                }

                LOGGER.info("download Successfully");
                LOGGER.info("please change to the latest version");
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("error on version checking {}", e.getMessage());
        }
        return true;
    }

    private void defaultFileInit() {
        File pluginsFolder = new File("plugins");
        if (!pluginsFolder.exists())
            pluginsFolder.mkdirs();
    }

    private boolean loadPlugin(Info pluginInfo) {
        if (pluginInfo.depend != null)
            for (String depend : pluginInfo.depend) {
                // plugin list have depend
                if (plugins.containsKey(depend)) {
                    // load its depend
                    if (!plugin_queue.containsKey(depend))
                        loadPlugin(plugins.get(depend));
                } else {
                    LOGGER.error("plugin: " + pluginInfo.name + " lost dependency: " + depend);
                    return true;
                }
            }
        if (pluginInfo.softDepend != null)
            for (String depend : pluginInfo.softDepend) {
                // plugin list have depend
                if (plugins.containsKey(depend)) {
                    // load its depend
                    if (!plugin_queue.containsKey(depend))
                        loadPlugin(plugins.get(depend));
                }
            }

        plugin_queue.put(pluginInfo.name, pluginInfo.pluginInstance);

        return false;
    }


    private void loadSettingFile() throws IOException {
        try (InputStream inputStream = readOrDefaultYml("config_0A2F7C.yml", "config.yml", this.getClass().getClassLoader())) {
            configFile = new Yaml().loadAs(inputStream, Setting.class);
        }

        LOGGER.info("setting file loaded");
    }

    private void loadVariables() {
        Setting.GeneralSettings general = configFile.GeneralSettings;
        BOT_TOKEN = general.botToken;
        if (general.activityMessage != null)
            botStatus.addAll(Arrays.asList(general.activityMessage));
    }

    private void loadPlugins() {
        int count = 0;
        int fail = 0;
        String tmp;
        LOGGER.info("plugin(s) loading...");

        ClassLoader loader = new ClassLoader();

        // load all jar files and depends into plugins map
        for (File file : new File("plugins").listFiles()) {
            try {
                if (file == null) continue;
                if ((tmp = getExtensionByName(file.getName())) == null || !tmp.equals("jar")) continue;
                try (JarFile jarFile = new JarFile(file)) {
                    try (InputStream inputStream = jarFile.getInputStream(jarFile.getEntry("info.yml"))) {
                        Config config = new Yaml().loadAs(inputStream, Config.class);

                        if (!plugins.containsKey(config.name)) {
                            loader.addJar(file, config.main);

                            plugins.put(config.name, new Info(
                                            config.name,

                                            // plugin
                                            (Event) loader.getClass(config.main)
                                                    .getDeclaredConstructor().newInstance(),

                                            // dependencies
                                            config.depend,
                                            config.soft_depend
                                    )
                            );
                            ++count;
                        } else {
                            LOGGER.error("same plugin name: " + file.getName());
                            ++fail;
                            continue;
                        }
                    } catch (Exception e) {
                        ++fail;
                        LOGGER.error("occur file: {}", file.getName());
                    }
                }
            } catch (Exception e) {
                ++fail;
                e.printStackTrace();
            }
        }

        for (Info i : plugins.values()) {
            // if added
            if (plugin_queue.containsKey(i.name)) continue;

            // load depends
            if (loadPlugin(i)) {
                LOGGER.error("stop loading plugins...");
                return;
            }
        }

        for (Event plugin : plugin_queue.values()) {
            CommandData[] guildCommandsTmp;
            if ((guildCommandsTmp = plugin.guildCommands()) != null) {
                Collections.addAll(guildCommands, guildCommandsTmp);
            }

            CommandData[] globalCommandsTmp;
            if ((globalCommandsTmp = plugin.globalCommands()) != null) {
                Collections.addAll(globalCommands, globalCommandsTmp);
            }

            if (plugin.listener) listeners.add(plugin);
        }

        if (fail > 0) LOGGER.error(fail + " plugin(s) loading FAILED!");
        LOGGER.info(count + " plugin(s) loading successfully");
    }

    private void setStatus() {
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
                        LOGGER.error("cannot find type: " + arg[0]);
                        return;
                    } catch (InterruptedException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        });
    }

    public void reload() throws IOException {
        for (Event plugin : plugin_queue.values()) {
            plugin.reloadAll();
        }

        threadPool.shutdown();
        loadSettingFile();
        loadVariables();
        setStatus();
    }

    public void stop() {
        for (Object listener : jdaBot.getRegisteredListeners()) {
            jdaBot.removeEventListener(listener);
        }

        List<Event> stopPlugins = new ArrayList<>(plugin_queue.values());
        Collections.reverse(stopPlugins);
        for (Event plugin : stopPlugins) {
            plugin.unload();
        }

        plugins.clear();
        plugin_queue.clear();

        threadPool.shutdown();
        jdaBot.shutdown();
    }


    @Nullable
    private InputStream readOrDefaultYml(String name, String outName, java.lang.ClassLoader loader) {
        File settingFile = new File(System.getProperty("user.dir") + '/' + outName);
        if (!settingFile.exists()) {
            LOGGER.info(outName + " not found, create default " + outName);
            settingFile = exportResource(name, outName, "", loader);
            if (settingFile == null) {
                LOGGER.error("Read {} failed", name);
                return null;
            }
        }
        LOGGER.info("Load " + settingFile.getPath());
        try {
            return Files.newInputStream(settingFile.toPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    @Nullable
    private File exportResource(String sourceFile, String outputName, String outputPath, java.lang.ClassLoader loader) {
        try (InputStream fileInJar = loader.getResourceAsStream(sourceFile)) {
            if (fileInJar == null) {
                LOGGER.error("Cannot find resource: {}", sourceFile);
                return null;
            }
            Files.copy(fileInJar, Paths.get(Loader.ROOT_PATH + '/' + outputPath + '/' + outputName), StandardCopyOption.REPLACE_EXISTING);
            return new File(Loader.ROOT_PATH + '/' + outputPath + '/' + outputName);
        } catch (IOException e) {
            LOGGER.error("Read resource failed: {}", e.getMessage());
        }
        return null;
    }
}