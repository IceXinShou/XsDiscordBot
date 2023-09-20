package com.xs.chatgpt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.xs.chatgpt.TokenCounter.getToken;
import static com.xs.loader.base.Loader.ROOT_PATH;

public class Main extends Event {
    public static final Set<Long> waitingList = ConcurrentHashMap.newKeySet();
    private static final String TAG = "ChatGPT";
    public static JsonArray defaultAry = new JsonArray();
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/ChatGPT";
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    private final Map<Long, JsonFileManager> guildsManager = new HashMap<>();
    public static MainConfig configFile;
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private JsonFileManager dmManager;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content

    public static int prompt_token;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadConfigFile();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void reload() {
        loadConfigFile();
        loadLang();
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        langMap = langManager.getMap();
    }

    @Override
    public void loadConfigFile() {
        try (InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()))
                    .loadAs(inputStream, MainConfig.class);
            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        prompt_token = getToken(configFile.SystemPrompt);

        JsonObject defaultObj = new JsonObject();
        defaultObj.addProperty("role", "system");
        defaultObj.addProperty("content", configFile.SystemPrompt);
        defaultObj.addProperty("token", getToken(configFile.SystemPrompt));

        defaultAry.add(defaultObj);

        new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data").mkdirs();
        dmManager = new JsonFileManager('/' + PATH_FOLDER_NAME + "/data/dm.json", TAG, true);

        logger.log("Setting File Loaded Successfully");
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Bot Filter
        User user = event.getAuthor();
        if (user.isBot()) return;


        switch (event.getChannelType()) {
            case PRIVATE: {
                dmListener(event);
                break;
            }
            case GUILD_PUBLIC_THREAD: {
                forumListener(event);
                break;
            }
        }
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        executor.shutdown();
    }


    private void dmListener(MessageReceivedEvent event) {
        // AllowList Filter
        if (!Arrays.asList(configFile.AllowUserID).contains(event.getAuthor().getIdLong())) return;

        String msg = event.getMessage().getContentRaw();
        if (!msg.startsWith(configFile.Prefix)) return;

        process(event, msg.substring(1), dmManager);
    }

    private void forumListener(MessageReceivedEvent event) {
        // AllowList Filter
        if (!Arrays.asList(configFile.AllowForumChannelID).contains(
                event.getChannel().asThreadChannel().getParentChannel().getIdLong())) return;

        String msg = event.getMessage().getContentRaw();
        if (!msg.startsWith(configFile.Prefix)) return;

        long guildID = event.getGuild().getIdLong();
        JsonFileManager manager;
        if (guildsManager.containsKey(guildID)) {
            manager = guildsManager.get(guildID);
        } else {
            manager = new JsonFileManager('/' + PATH_FOLDER_NAME + "/data/" + guildID + ".json", TAG, true);
            guildsManager.put(guildID, manager);
        }

        process(event, msg.substring(1), manager);
    }

    private void process(MessageReceivedEvent event, String msg, JsonFileManager manager) {
        JsonObject obj = manager.getObj();
        long id = event.getAuthor().getIdLong();

        if (msg.equals("結束對話") || msg.equalsIgnoreCase("end")) {
            obj.add(String.valueOf(id), defaultAry.deepCopy());
            manager.save();
            waitingList.remove(id);
            event.getChannel().sendMessage("對話已結束，可以開始新的話題了~").queue();
            return;
        }

        // WaitingList Filter
        if (waitingList.contains(id)) {
            event.getMessage().reply("請等待上一個訊息完成!").queue(i -> i.delete().queueAfter(3, TimeUnit.SECONDS));
            return;
        }

        waitingList.add(id);
        event.getChannel().sendTyping().queue();
        String name = event.getAuthor().getName();
        executor.submit(() -> {
            logger.log("<- " + name + ": " + msg.replace("\n", "\\n"));

            MessageManager messageManager = new MessageManager(manager, event.getMessage(), msg, event.getChannel().getIdLong(), logger);
            logger.log("-> " + name + ": "
                    + messageManager.fullContent.toString().replace("\n", "\\n"));
            waitingList.remove(id);
        });
    }
}