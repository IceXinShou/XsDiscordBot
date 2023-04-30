package com.xs.loader;

import com.xs.loader.logger.Color;
import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

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

import static com.xs.loader.util.GlobalUtil.getExtensionName;

public class MainLoader {
    public static JDA jdaBot;
    public static long botID;
    private final List<CommandData> guildCommands = new ArrayList<>();
    private final List<CommandData> globalCommands = new ArrayList<>();
    public static final String ROOT_PATH = new File(System.getProperty("user.dir")).toString();
    private final Queue<PluginEvent> listeners = new ArrayDeque<>();
    private final Logger logger;
    private final String version = "v1.5";
    private String BOT_TOKEN;
    private MainConfig configFile;
    private final List<String> botStatus = new ArrayList<>();
    private final Map<String, PluginInfo> plugins = new HashMap<>();
    private final LinkedHashMap<String, PluginEvent> plugin_queue = new LinkedHashMap<>();
    private ScheduledExecutorService threadPool;
    private static boolean ignore_version_check = false;

    MainLoader() {
        logger = new Logger("Main");

        if (versionCheck()) {
            return;
        }

        defaultFileInit();
        loadConfigFile();
        loadVariables();
        loadPlugins();

        JDABuilder builder = JDABuilder.createDefault(BOT_TOKEN)
                .setBulkDeleteSplittingEnabled(false)
                .setLargeThreshold(250)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(
                        CacheFlag.ONLINE_STATUS,
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
        botID = jdaBot.getSelfUser().getIdLong();

        ListenerManager listenerManager = new ListenerManager(guildCommands);

        jdaBot.addEventListener(listenerManager);

        while (!listeners.isEmpty()) jdaBot.addEventListener(listeners.poll());

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
        if (ignore_version_check) {
            logger.log("You ignored the version check");
            return false;
        }
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
                logger.warn("Your current version: " + Color.RED + version + Color.RESET + ", latest version: " + Color.GREEN + latestVersion + Color.RESET);
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
            logger.warn(e.getMessage());
        }
        return true;
    }

    void defaultFileInit() {
        File pluginsFolder = new File("plugins");
        if (pluginsFolder.exists()) return;

        logger.log("File Initializing...");
        if (pluginsFolder.mkdirs())
            logger.log("File Initialized Successfully");
        else
            logger.warn("File Initialized failed");
    }

    boolean loadPlugin(PluginInfo pluginInfo) {
        if (pluginInfo.depend != null)
            for (String depend : pluginInfo.depend) {
                // plugin list have depend
                if (plugins.containsKey(depend)) {
                    // load its depend
                    if (!plugin_queue.containsKey(depend))
                        loadPlugin(plugins.get(depend));
                } else {
                    logger.warn("plugin: " + pluginInfo.name + " lost dependency: " + depend);
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

    void loadPlugins() {
        int count = 0;
        int fail = 0;
        String tmp;
        logger.log("Plugin(s) Loading...");

        ClassLoader loader = new ClassLoader();


        // load all jar files and depends into plugins map
        for (File file : new File("plugins").listFiles()) {
            try {
                if (file == null) continue;
                if ((tmp = getExtensionName(file.getName())) == null || !tmp.equals("jar")) continue;
                JarFile jarFile = new JarFile(file);
                InputStream inputStream = jarFile.getInputStream(jarFile.getEntry("info.yml"));
                PluginConfig config = new Yaml().loadAs(inputStream, PluginConfig.class);

                if (!plugins.containsKey(config.name)) {
                    loader.addJar(file, config.main);
                    plugins.put(config.name, new PluginInfo(
                                    config.name,

                                    // plugin
                                    (PluginEvent) loader.getClass(config.main)
                                            .getDeclaredConstructor().newInstance(),

                                    // dependencies
                                    config.depend,
                                    config.soft_depend
                            )
                    );
                } else {
                    logger.warn("same plugin name: " + file.getName());
                    ++fail;
                    continue;
                }
                inputStream.close();
                jarFile.close();
                ++count;
            } catch (Exception e) {
                ++fail;
                e.printStackTrace();
//                logger.warn(file.getName() + '\n' +
//                        e.getMessage() + '\n' +
//                        Arrays.toString(e.getStackTrace()).replace(',', '\n'));
            }
        }

        for (PluginInfo i : plugins.values()) {
            // if added
            if (plugin_queue.containsKey(i.name)) continue;

            // load depends
            if (loadPlugin(i)) {
                logger.warn("stop loading plugins...");
                return;
            }
        }

        for (PluginEvent plugin : plugin_queue.values()) {
            plugin.initLoad();

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

        for (PluginEvent plugin : plugin_queue.values()) plugin.finishLoad();

        if (fail > 0)
            logger.warn(fail + " Plugin(s) Loading Failed!");
        logger.log(count + " Plugin(s) Loading Successfully");
    }

    private void loadConfigFile() {
        InputStream inputStream = readOrDefaultYml("config_0A2F7C.yml", "config.yml", this.getClass().getClassLoader());
        configFile = new Yaml().loadAs(inputStream, MainConfig.class);
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.log("Setting file loaded");
    }


    private void loadVariables() {
        MainConfig.GeneralSettings general = configFile.GeneralSettings;
        BOT_TOKEN = general.botToken;
        if (general.activityMessage != null)
            botStatus.addAll(Arrays.asList(general.activityMessage));
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
                        logger.warn("can not find type: " + arg[0]);
                        return;
                    } catch (InterruptedException e) {
                        logger.warn(e.getMessage());
                    }
                }
            }
        });
    }

    void getInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                String[] cmd = scanner.nextLine().split(" ");

                switch (cmd[0].toLowerCase()) {
                    case "dm":
                        jdaBot.retrieveUserById(cmd[1]).onSuccess(i -> {
                            i.openPrivateChannel(
                            ).onSuccess((j) -> {
                                j.sendMessage(cmd[2]).queue();
                            }).onErrorFlatMap(j -> {
                                logger.warn(j.getMessage());
                                return null;
                            }).complete();
                        }).onErrorMap(i -> {
                            logger.warn(i.getMessage());
                            return null;
                        }).complete();
                        break;

                    case "say":
                        jdaBot.getGuildById(cmd[1]).getTextChannelById(cmd[2]).sendMessage(cmd[3]).queue();
                        break;

                    case "join":
                        Guild guild = jdaBot.getGuildById(cmd[1]);
                        guild.getAudioManager().openAudioConnection(guild.getVoiceChannelById(cmd[2]));
                        break;

                    case "leave":
                        jdaBot.getGuildById(cmd[1]).getAudioManager().closeAudioConnection();
                        break;

                    case "mute":
                        guild = jdaBot.getGuildById(cmd[1]);
                        if (guild != null)
                            guild.getAudioManager().setSelfMuted(!guild.getAudioManager().isSelfMuted());
                        else
                            logger.warn("cannot found guild by id: " + cmd[1]);
                        break;

                    case "deafen":
                        guild = jdaBot.getGuildById(cmd[1]);
                        if (guild != null)
                            guild.getAudioManager().setSelfDeafened(!guild.getAudioManager().isSelfDeafened());
                        else
                            logger.warn("cannot found guild by id: " + cmd[1]);
                        break;

                    case "deafenm":
                        guild = jdaBot.getGuildById(cmd[1]);
                        if (guild != null) {
                            Member member = guild.retrieveMemberById(cmd[2]).complete();
                            member.deafen(!member.getVoiceState().isDeafened()).queue();
                        } else {
                            logger.warn("cannot found guild by id: " + cmd[1]);
                        }
                        break;

                    case "stop":
                        for (Object listener : jdaBot.getRegisteredListeners()) {
                            jdaBot.removeEventListener(listener);
                        }

                        List<PluginEvent> stopPlugins = new ArrayList<>(plugin_queue.values());
                        Collections.reverse(stopPlugins);
                        for (PluginEvent plugin : stopPlugins) {
                            plugin.unload();
                        }

                        plugins.clear();
                        plugin_queue.clear();

                        threadPool.shutdown();
                        jdaBot.shutdown();
                        logger.log("Stopped");
                        return;

                    case "reload":
                        logger.log("Reloading...");

                        List<PluginEvent> reloadPlugins = new ArrayList<>(plugin_queue.values());
                        Collections.reverse(reloadPlugins);
                        for (PluginEvent plugin : reloadPlugins) {
                            plugin.unload();
                        }
                        for (PluginEvent plugin : plugin_queue.values()) {
                            plugin.initLoad();
                        }

                        threadPool.shutdown();
                        loadConfigFile();
                        loadVariables();
                        setStatus();

                        logger.log("Reloaded");
                        break;

                    case "":
                        break;
                    default:
                        logger.warn("Unknown Command");
                        break;
                }
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }
    }

    @Nullable
    public InputStream readOrDefaultYml(String name, String outName, java.lang.ClassLoader loader) {
        File settingFile = new File(System.getProperty("user.dir") + '/' + outName);
        if (!settingFile.exists()) {
            logger.warn(outName + " not found, create default " + outName);
            settingFile = exportResource(name, outName, "", loader);
            if (settingFile == null) {
                logger.warn("read " + name + " failed");
                return null;
            }
        }
        logger.log("load " + settingFile.getPath());
        try {
            return Files.newInputStream(settingFile.toPath());
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return null;
        }
    }

    @Nullable
    public File exportResource(String sourceFile, String outputName, String outputPath, java.lang.ClassLoader loader) {
        InputStream fileInJar = loader.getResourceAsStream(sourceFile);

        try {
            if (fileInJar == null) {
                logger.warn("can not find resource: " + sourceFile);
                return null;
            }
            Files.copy(fileInJar, Paths.get(MainLoader.ROOT_PATH + "/" + outputPath + "/" + outputName), StandardCopyOption.REPLACE_EXISTING);
            fileInJar.close();
            return new File(MainLoader.ROOT_PATH + "/" + outputPath + "/" + outputName);
        } catch (IOException e) {
            logger.warn("read resource failed");
            logger.warn(e.getMessage());
        }
        return null;
    }

//    public Map<String, Object> readOrDefaultYml(String name, String outName) {
//        File settingFile = new File(System.getProperty("user.dir") + '/' + outName);
//        if (!settingFile.exists()) {
//            logger.warn(outName + " not found, create default " + outName);
//            settingFile = getter.exportResource(name, outName, "");
//            if (settingFile == null) {
//                logger.warn("read " + name + " failed");
//                return null;
//            }
//        }
//        logger.log("load " + settingFile.getPath());
//        String settingText = null;
//        try {
//            settingText = streamToString(Files.newInputStream(settingFile.toPath()));
//        } catch (IOException e) {
//            logger.warn(e.getMessage());
//        }
//
//        return new Yaml().load(settingText);
//    }

    public static void main(String[] args) {
        for (String i : args) {
            switch (i) {
                case "-ignore-version-check": {
                    ignore_version_check = true;
                    break;
                }
            }
        }
        AnsiConsole.systemInstall();
        new MainLoader();
        AnsiConsole.systemUninstall();
    }
}