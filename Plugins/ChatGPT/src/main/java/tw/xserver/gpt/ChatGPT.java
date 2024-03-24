package tw.xserver.gpt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;
import tw.xserver.loader.util.json.JsonObjFileManager;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static tw.xserver.gpt.TokenCounter.getToken;
import static tw.xserver.loader.base.Loader.ROOT_PATH;

public class ChatGPT extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatGPT.class);
    private static final String PATH_FOLDER_NAME = "plugins/ChatGPT";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + '/');
    private JsonObjFileManager dmManager;

    public static final Set<Long> processingList = ConcurrentHashMap.newKeySet();
    public static final JsonArray defaultAry = new JsonArray();
    public static MainConfig configFile;
    public static int prompt_token;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    private final Map<Long, JsonObjFileManager> guildsManager = new HashMap<>();
    private final Set<Long> waitList = new HashSet<>();

    public ChatGPT() {
        super(true);

        reloadAll();
        LOGGER.info("loaded ChatGPT");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded ChatGPT");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, ChatGPT.class);

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions())).loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        prompt_token = getToken(configFile.SystemPrompt);

        JsonObject defaultObj = new JsonObject();
        defaultObj.addProperty("role", "system");
        defaultObj.addProperty("content", configFile.SystemPrompt);
        defaultObj.addProperty("token", getToken(configFile.SystemPrompt));

        defaultAry.add(defaultObj);

        if (new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data").mkdirs()) {
            LOGGER.info("default data folder created");
        }

        dmManager = new JsonObjFileManager('/' + PATH_FOLDER_NAME + "/data/dm.json");

        LOGGER.info("setting file loaded successfully");
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
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        if (!event.getEmoji().getName().equals("\uD83E\uDD16") || !waitList.contains(event.getMessageIdLong())) return;
        waitList.remove(event.getMessageIdLong());

        Message message = event.retrieveMessage().complete();

        process(event.retrieveUser().complete(), event.getChannel().asThreadChannel(), message, message.getContentRaw(), dmManager);
    }

    @Override
    public void onShutdown(@Nonnull ShutdownEvent event) {
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }


    private void dmListener(MessageReceivedEvent event) {
        // AllowList Filter
        if (!Arrays.asList(configFile.AllowUserID).contains(event.getAuthor().getIdLong())) return;

        Message message = event.getMessage();
        String raw = message.getContentRaw();
        if (!raw.isEmpty() && !raw.startsWith(configFile.Prefix)) return;
        process(event.getAuthor(), event.getChannel().asPrivateChannel(), message, raw.substring(1), dmManager);
    }

    private void forumListener(MessageReceivedEvent event) {
        // AllowList Filter
        if (!Arrays.asList(configFile.AllowForumChannelID).contains(event.getChannel().asThreadChannel().getParentChannel().getIdLong()))
            return;

        Message message = event.getMessage();
        String raw = message.getContentRaw();
        if (!raw.startsWith(configFile.Prefix)) {
            event.getMessage().addReaction(Emoji.fromUnicode("🤖")).queue();
            waitList.add(event.getMessageIdLong());
            return;
        }

        long guildID = event.getGuild().getIdLong();
        JsonObjFileManager manager;
        if (guildsManager.containsKey(guildID)) {
            manager = guildsManager.get(guildID);
        } else {
            manager = new JsonObjFileManager('/' + PATH_FOLDER_NAME + "/data/" + guildID + ".json");
            guildsManager.put(guildID, manager);
        }

        process(event.getAuthor(), event.getChannel().asThreadChannel(), message, raw.substring(1), manager);
    }

    private void process(User author, MessageChannel channel, Message message, String msg, JsonObjFileManager manager) {
        if (msg.isEmpty()) return;

        JsonObject obj = manager.get();
        long id = author.getIdLong();

        if (msg.equals("結束對話") || msg.equalsIgnoreCase("end")) {
            obj.add(String.valueOf(id), defaultAry.deepCopy());
            manager.save();
            processingList.remove(id);
            channel.sendMessage("對話已結束，可以開始新的話題了~").queue();
            return;
        }

        // WaitingList Filter
        if (processingList.contains(id)) {
            message.reply("請等待上一個訊息完成!").queue(i -> i.delete().queueAfter(3, TimeUnit.SECONDS));
            return;
        }

        processingList.add(id);
        channel.sendTyping().queue();
        String name = author.getName();
        executor.submit(() -> {
            LOGGER.info("<- " + name + ": " + msg.replace("\n", "\\n"));

            MessageManager messageManager = new MessageManager(manager, message, msg, channel.getIdLong());
            LOGGER.info("-> " + name + ": " + messageManager.fullContent.toString().replace("\n", "\\n"));
            processingList.remove(id);
        });
    }
}