package com.xs.chatgpt;

import com.google.gson.GsonBuilder;
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
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

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
            JsonArray ary;
            if (!obj.has(String.valueOf(id))) {
                ary = defaultAry.deepCopy();
                obj.add(String.valueOf(id), ary);
            } else {
                ary = obj.get(String.valueOf(id)).getAsJsonArray();
            }


            JsonObject postData = JsonParser.parseString("{\"stream\":true,\"model\": \"" + module + "\",\"messages\":" + ary + "}").getAsJsonObject();

            try {
                String str = postData.toString();
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);

                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                    wr.write(str.getBytes(StandardCharsets.UTF_8));
                }

                StringBuilder fullRep = new StringBuilder();
                switch (conn.getResponseCode()) {
                    case 200: {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                        String line;
                        StringBuilder lastDCMsgContent = new StringBuilder();
                        Message lastReplyMessage = null;
                        String newMsg;
                        long lastTime = 0;
                        while ((line = reader.readLine()) != null) {
                            if (line.equals("data: [DONE]")) {
                                break;
                            }

                            JsonObject streamObj = JsonParser.parseString(line).getAsJsonObject();
                            newMsg = streamObj.get("choices").getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("delta").getAsJsonObject()
                                    .get("content").getAsString();

                            fullRep.append(newMsg);
                            lastDCMsgContent.append(newMsg);

                            if (System.currentTimeMillis() - lastTime > 3000) {
                                if (lastTime == 0) {
                                    lastReplyMessage = event.getMessage().reply(lastDCMsgContent.toString()).complete();
                                } else {
                                    List<String> msgList = splitMessage(lastDCMsgContent.toString());
                                    lastReplyMessage.editMessage(msgList.get(0)).queue();
                                    if (msgList.size() > 1) {
                                        for (int i = 1; i < msgList.size(); i++) {
                                            if (i == msgList.size() - 1) {
                                                lastReplyMessage = event.getMessage().reply(msgList.get(i)).complete();
                                            } else {
                                                event.getMessage().reply(msgList.get(i)).queue();
                                            }
                                        }
                                    }
                                }

                                lastTime = System.currentTimeMillis();
                            }
                        }
                        reader.close();
                        break;
                    }
                    default: {
                        InputStreamReader reader = new InputStreamReader(conn.getErrorStream());
                        JsonObject rep = JsonParser.parseReader(reader).getAsJsonObject();
                        reader.close();

                        logger.warn("error on requesting...");
                        logger.warn(rep.toString());

                        waitingList.remove(id);
                        event.getMessage().reply("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();
                        return;
                    }
                }

                logger.log(new GsonBuilder().setPrettyPrinting().create().toJson(fullRep.toString()));


                ary.add(JsonParser.parseString("{\"role\":\"user\",\"content\":\"" + msg + "\"}").getAsJsonObject());
                ary.add(JsonParser.parseString("{\"assistant\":\"user\",\"content\":\"" + fullRep + "\"}").getAsJsonObject());
                manager.save();


                waitingList.remove(id);
            } catch (IOException e) {
                waitingList.remove(id);
                event.getChannel().sendMessage("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        reqExecutors.shutdown();
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