package com.xs.chatgpt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.xs.loader.base.Loader.ROOT_PATH;

public class Main extends Event {
    private static final String TAG = "ChatGPT";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/ChatGPT";
    private final List<Long> allowUserID = new ArrayList<>();
    private final List<Long> allowForumChannelID = new ArrayList<>();
    public static final Set<Long> waitingList = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    public static String apiKey;
    public static String module;
    public static JsonArray defaultAry;
    private MainConfig configFile;
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private JsonFileManager dmManager;
    private final Map<Long, JsonFileManager> guildsManager = new HashMap<>();
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content

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
    public void loadLang() {
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        langMap = langManager.getMap();
    }

    @Override
    public void loadConfigFile() {
        InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml");
        if (inputStream == null) return;

        try {
            configFile = new Yaml(new Constructor(MainConfig.class)).load(inputStream);
            inputStream.close();
            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            e.printStackTrace();
        }
        allowUserID.addAll(Arrays.asList(configFile.AllowUserID));
        allowForumChannelID.addAll(Arrays.asList(configFile.AllowForumChannelID));
        apiKey = configFile.API_KEY;
        module = configFile.Module;
        defaultAry = JsonParser.parseString(
                "[{\"role\": \"system\", \"content\": \"" + configFile.SystemPrompt + "\"}]"
        ).getAsJsonArray();


        new File(ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data").mkdirs();
        dmManager = new JsonFileManager("/" + PATH_FOLDER_NAME + "/data/dm.json", TAG, true);

        logger.log("Setting File Loaded Successfully");
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        dmListener(event);
        forumListener(event);
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        executor.shutdown();
    }


    private void dmListener(MessageReceivedEvent event) {

        // Only from private channel
        if (!event.isFromType(ChannelType.PRIVATE)) return;

        // Bot Filter
        User user = event.getAuthor();
        if (user.isBot()) return;

        // AllowList Filter
        long id = event.getAuthor().getIdLong();
        if (!allowUserID.contains(id)) return;

        String msg = event.getMessage().getContentRaw();
        JsonObject obj = dmManager.getObj();

        if (msg.equals("結束對話") || msg.equalsIgnoreCase("end")) {
            obj.add(String.valueOf(id), defaultAry.deepCopy());
            dmManager.save();
            waitingList.remove(id);
            event.getChannel().sendMessage("對話已結束，可以開始新的話題了~").queue();
            return;
        }

        // WaitingList Filter
        if (waitingList.contains(id)) {
            event.getMessage().reply("請等待上一個訊息完成...").queue(i -> i.delete().queueAfter(3, TimeUnit.SECONDS));
            return;
        }

        waitingList.add(id);
        event.getChannel().sendTyping().queue();
        new MessageManager(dmManager, event.getMessage(), msg, id);
    }

    private void forumListener(MessageReceivedEvent event) {
        // Only from forum channel
        if (!event.isFromType(ChannelType.GUILD_PUBLIC_THREAD)) return;

        // Bot Filter
        User user = event.getAuthor();
        if (user.isBot()) return;

        // AllowList Filter
        ThreadChannel channel = event.getChannel().asThreadChannel();
        if (!allowForumChannelID.contains(channel.getParentChannel().getIdLong())) return;

        String msg = event.getMessage().getContentRaw();
        long id = channel.getIdLong();
        long guildID = event.getGuild().getIdLong();
        JsonFileManager manager;
        if (guildsManager.containsKey(guildID)) {
            manager = guildsManager.get(guildID);
        } else {
            manager = new JsonFileManager("/" + PATH_FOLDER_NAME + "/data/" + guildID + ".json", TAG, true);
            guildsManager.put(guildID, manager);
        }
        JsonObject obj = manager.getObj();
        if (msg.equals("結束對話") || msg.equalsIgnoreCase("end")) {
            obj.add(String.valueOf(id), defaultAry.deepCopy());
            manager.save();
            waitingList.remove(id);
            event.getChannel().sendMessage("對話已結束，可以開始新的話題了~").queue();
            return;
        }

        // WaitingList Filter
        if (waitingList.contains(id)) {
            event.getMessage().reply("請等待上一個訊息完成...").queue(i -> i.delete().queueAfter(3, TimeUnit.SECONDS));
            return;
        }

        waitingList.add(id);
        event.getChannel().sendTyping().queue();
        new MessageManager(manager, event.getMessage(), msg, id);
    }
}