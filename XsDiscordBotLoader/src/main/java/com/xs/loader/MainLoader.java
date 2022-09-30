package com.xs.loader;

import com.xs.loader.logger.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.fusesource.jansi.AnsiConsole;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.login.LoginException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;

public class MainLoader {
    public static ConfigSetting loader = new ConfigSetting();
    public static String noPermissionERROR = "You have no permission";
    public static JDA jdaBot;
    public static List<CommandData> guildCommands = new ArrayList<>();
    public static List<SubcommandData> subGuildCommands = new ArrayList<>();
    public static List<CommandData> globalCommands = new ArrayList<>();
    public static final String ROOT_PATH = new File(System.getProperty("user.dir")).toString();

    private final Deque<PluginEvent> listeners = new ArrayDeque<>();
    private final Logger logger = new Logger("Main");

    MainLoader() {
        defaultFileInit();
        JDABuilder builder = JDABuilder.createDefault(ConfigSetting.botToken)
                .setBulkDeleteSplittingEnabled(false)
                .setLargeThreshold(250)
                .enableCache(CacheFlag.ONLINE_STATUS, CacheFlag.ACTIVITY)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES);

        loadPlugins();

        try {
            jdaBot = builder.build();
        } catch (LoginException e) {
            logger.error("Token is Invalid");
            return;
        }

        ConfigSetting.botID = jdaBot.getSelfUser().getIdLong();
        ListenerManager listenerManager = new ListenerManager();
        jdaBot.addEventListener(listenerManager);

        while (!listeners.isEmpty()) {
            jdaBot.addEventListener(listeners.poll());
        }

        jdaBot.updateCommands().addCommands(globalCommands).queue();
        logger.log("Bot Started!");
    }

    void defaultFileInit() {
        logger.log("File Initializing...");
        new File("plugins").mkdir();
        logger.log("File Initialized Successfully");
    }

    String getExtensionName(String fileName) {
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
        for (var file : new File("plugins").listFiles()) {
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
                        .loadClass((String) (((Map<String, Object>) new Yaml().load(result.toString(StandardCharsets.UTF_8))).get("MainPath")))
                        .getDeclaredConstructor().newInstance();

                pluginMain.initLoad();

                if (pluginMain.guildCommands() != null) {
                    guildCommands.addAll(Arrays.stream(pluginMain.guildCommands()).toList());
                }

                if (pluginMain.subGuildCommands() != null) {
                    subGuildCommands.addAll(Arrays.stream(pluginMain.subGuildCommands()).toList());
                }

                if (pluginMain.globalCommands() != null) {
                    globalCommands.addAll(Arrays.stream(pluginMain.globalCommands()).toList());
                }

                listeners.add(pluginMain);

                result.close();
                inputStream.close();
                jarFile.close();
                ++count;
            } catch (Exception e) {
                ++fail;
                logger.error(file.getName() + '\n' + Arrays.toString(e.getStackTrace()));
            }
        }
        if (fail > 0)
            logger.error(fail + " Plugin(s) Loading Failed!");
        logger.log(count + " Plugin(s) Loading Successfully");
    }

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        new MainLoader();
        AnsiConsole.systemUninstall();
    }
}