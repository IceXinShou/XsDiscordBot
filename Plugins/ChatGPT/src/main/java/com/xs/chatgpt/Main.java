package com.xs.chatgpt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.xs.loader.base.Loader.ROOT_PATH;

public class Main extends Event {
    private static final String TAG = "ChatGPT";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/ChatGPT";
    private final List<Long> allowUserID = new ArrayList<>();
    private final HashSet<Long> waitingList = new HashSet<>();
    private final ExecutorService reqExecutors = Executors.newFixedThreadPool(8);
    private String apiKey;
    private String module;
    private JsonArray defaultAry;
    private MainConfig configFile;
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private JsonFileManager manager;
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
        apiKey = configFile.API_KEY;
        module = configFile.Module;
        defaultAry = JsonParser.parseString(
                "[{\"role\": \"system\", \"content\": \"" + configFile.SystemPrompt + "\"}]"
        ).getAsJsonArray();


        new File(ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonFileManager("/" + PATH_FOLDER_NAME + "/data/data.json", TAG, true);

        logger.log("Setting File Loaded Successfully");
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Only from private channel
        if (!event.isFromType(ChannelType.PRIVATE)) return;

        // Bot Filter
        User user = event.getAuthor();
        if (user.isBot()) return;

        // AllowList Filter
        long id = event.getAuthor().getIdLong();
        if (!allowUserID.contains(id)) return;

        String msg = event.getMessage().getContentRaw();
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
            event.getMessage().reply("請等待上一個訊息完成...").queue();
            return;
        }

        waitingList.add(id);
        event.getChannel().sendTyping().queue();

        reqExecutors.submit(() -> {

            // 初始化部分請求內容
            JsonArray ary;
            if (!obj.has(String.valueOf(id))) {
                ary = defaultAry.deepCopy();
                obj.add(String.valueOf(id), ary);
            } else {
                ary = obj.get(String.valueOf(id)).getAsJsonArray();
            }

            // 新增訊息至部分請求內容
            ary.add(JsonParser.parseString("{\"role\":\"user\",\"content\":\"" + msg + "\"}").getAsJsonObject());

            // 完整請求建構
            JsonObject postObj = JsonParser.parseString("{\"stream\":true,\"model\": \"" + module + "\",\"messages\":" + ary + "}").getAsJsonObject();

            try {
                String postStr = postObj.toString();
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);

                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                    wr.write(postStr.getBytes(StandardCharsets.UTF_8));
                }

                StringBuilder fullRep = new StringBuilder();
                Message lastReplyMessage = null;
                StringBuffer lastDCMsgContent = new StringBuffer();
                switch (conn.getResponseCode()) {
                    case 200: {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                        String line;
                        String newMsg;
                        long lastTime = 0;
                        while ((line = reader.readLine()) != null) {
                            // 過濾系統通知與空回覆
                            if (line.contains("assistant") || line.isEmpty()) {
                                continue;
                            }
                            JsonObject streamObj = JsonParser.parseString(line.substring(6)).getAsJsonObject();

                            // 傳輸結束
                            if (!streamObj.get("choices").getAsJsonArray().get(0).getAsJsonObject().get("finish_reason").isJsonNull()) {
                                break;
                            }

                            // 取得文字內容
                            newMsg = streamObj.get("choices").getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("delta").getAsJsonObject()
                                    .get("content").getAsString();
                            fullRep.append(newMsg);
                            lastDCMsgContent.append(newMsg);

                            // 每 2 秒更新文字
                            if (System.currentTimeMillis() - lastTime > 2000) {
                                if (lastTime == 0) {
                                    lastReplyMessage = event.getMessage().reply(lastDCMsgContent.toString()).complete();
                                } else {
                                    StringBuffer buffer = updateMessage(event.getMessage(), lastReplyMessage, lastDCMsgContent.toString(), false);
                                    if (buffer != null)
                                        lastDCMsgContent = buffer;
                                }

                                lastTime = System.currentTimeMillis();
                            }
                        }

                        reader.close();
                        break;
                    }

                    // 例外情況
                    default: {
                        InputStreamReader reader = new InputStreamReader(conn.getErrorStream());
                        JsonObject rep = JsonParser.parseReader(reader).getAsJsonObject();
                        reader.close();

                        logger.warn(conn.getResponseCode() + " error on requesting...");
                        logger.warn(rep.toString());

                        waitingList.remove(id);
                        event.getMessage().reply("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();
                        return;
                    }
                }

                // 更新剩下文字
                updateMessage(event.getMessage(), lastReplyMessage, lastDCMsgContent.toString(), true);
                ary.add(JsonParser.parseString("{\"role\":\"user\",\"content\":\"" + fullRep + "\"}").getAsJsonObject());

                logger.log("<- " + user.getName() + ": " + msg);
                logger.log("-> " + user.getName() + ": " + fullRep);

                manager.save();
                waitingList.remove(id);
            } catch (IOException e) {
                logger.warn(e.getMessage());
                waitingList.remove(id);
                event.getChannel().sendMessage("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();
            }
        });
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        reqExecutors.shutdown();
    }

    @Nullable
    private StringBuffer updateMessage(Message originMsg, Message latestMsg, String msg, boolean complete) {
        List<String> msgList = splitMessage(msg);

        if (msgList.size() != 1) {
            for (int i = 1; i < msgList.size(); i++) {
                if (i != msgList.size() - 1) { // multi reply
                    originMsg.reply(msgList.get(i)).queue();

                } else {
                    originMsg.reply(msgList.get(i)).complete();
                    return new StringBuffer(msgList.get(i));
                }
            }
        }

        if (complete)
            latestMsg.editMessage(msg).complete();
        else
            latestMsg.editMessage(msg).queue();
        return null;
    }

    private List<String> splitMessage(String msg) {
        List<String> msgs = new ArrayList<>();
        int maxMessageLength = 1999;

        while (msg.length() > maxMessageLength) {
            String part = msg.substring(0, maxMessageLength);

            int lastCodeEndIndex = part.lastIndexOf("```\n");
            if (lastCodeEndIndex > 0) {
                part = part.substring(0, lastCodeEndIndex + 4);
            } else {
                int lastNewlineIndex = part.lastIndexOf('\n');
                if (lastNewlineIndex > 0) {
                    part = part.substring(0, lastNewlineIndex);
                }
            }

            msgs.add(part);
            msg = msg.substring(part.length());
        }

        if (!msg.isEmpty()) {
            msgs.add(msg);
        }

        return msgs;
    }
}