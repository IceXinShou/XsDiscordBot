package com.xs.chatgpt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.Message;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.xs.chatgpt.Main.*;

public class MessageManager {

    public final StringBuilder fullContent = new StringBuilder();
    private final Message replyMessage;
    private final StringBuffer curStr = new StringBuffer();
    private final Logger logger;
    private Message latestMessage;
    private Status status = Status.INIT;
    private final String msg;
    private final JsonObject postObj = new JsonObject();
    private final JsonArray dataAry;


    MessageManager(JsonFileManager manager, Message replyMessage, String msg, long id, Logger logger) {
        this.replyMessage = replyMessage;
        this.msg = msg;
        this.logger = logger;

        // 初始化部分請求內容
        JsonObject obj = manager.getObj();
        if (!obj.has(String.valueOf(id))) {
            dataAry = defaultAry.deepCopy();
            obj.add(String.valueOf(id), dataAry);
        } else {
            dataAry = obj.get(String.valueOf(id)).getAsJsonArray();
        }

        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(() -> {
            try {
                getData(buildRequest());
            } catch (IOException e) {
                replyMessage.reply("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();
                status = Status.ERROR;
                e.printStackTrace();
            }
        });

        long lastTime = 0;
        while (status == Status.INIT || curStr.length() == 0) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        while (status == Status.READING) {
            // 每 2 秒更新文字
            if (System.currentTimeMillis() - lastTime > 2000) {
                if (lastTime == 0) { // 第一則訊息
                    latestMessage = replyMessage.reply(curStr + "...").complete();
                } else {
                    updateMessage(false);
                }

                lastTime = System.currentTimeMillis();
            }
        }

        if (status == Status.DONE) {
            updateMessage(true);
            JsonObject newObj = new JsonObject();
            newObj.addProperty("role", "assistant");
            newObj.addProperty("content", fullContent.toString());
            dataAry.add(newObj);
            manager.save();
        }
        waitingList.remove(id);
        executor.shutdown();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildRequest() {
        // 新增訊息至部分請求內容
        JsonObject msgObj = new JsonObject();
        msgObj.addProperty("role", "user");
        msgObj.addProperty("content", msg);
        dataAry.add(msgObj);

        // 完整請求建構
        postObj.addProperty("stream", true);
        postObj.addProperty("model", module);
        postObj.add("messages", dataAry);

        return postObj.toString();
    }

    private String reBuildRequest() {
        System.out.println(postObj);
        dataAry.remove(1); // index 0 is system command

        // 更新請求建構
        postObj.add("messages", dataAry);
        System.out.println(postObj);
        return postObj.toString();
    }

    private void getData(String postStr) throws IOException {
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

        status = Status.READING;
        if (conn.getResponseCode() == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                String newMsg;
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
                    fullContent.append(newMsg);
                    curStr.append(newMsg);
                }
                status = Status.DONE;
            }
        } else {
            // 例外情況
            try (InputStreamReader reader = new InputStreamReader(conn.getErrorStream())) {
                JsonObject rep = JsonParser.parseReader(reader).getAsJsonObject();

                String errorCode = rep.get("error").getAsJsonObject().get("code").getAsString();
                switch (errorCode) {
                    case "context_length_exceeded": {
                        System.out.println("SORT");
                        // TODO: add gpt3-tokenizer
                        getData(reBuildRequest());
                        return;
                    }
                }

                logger.warn(conn.getResponseCode() + " error on requesting...");
                logger.warn(rep.toString());

                replyMessage.reply("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();
                status = Status.READ_FAILED;
            }
        }
    }

    private void updateMessage(boolean complete) {
        List<String> msgList;

        if (curStr.length() < 1950) {
            if (complete)
                latestMessage.editMessage(curStr.toString()).complete();
            else
                latestMessage.editMessage(curStr + "...").queue();
        } else {
            msgList = splitMessage(curStr.toString());
            for (int i = 1; i < msgList.size(); i++) {
                if (i != msgList.size() - 1) { // multi reply
                    replyMessage.reply(msgList.get(i)).queue();
                } else {
                    latestMessage = replyMessage.reply(msgList.get(i) + "...").complete();
                    curStr.setLength(0);
                    curStr.append(msgList.get(i));
                }
            }
        }
    }

    private List<String> splitMessage(String msg) {
        List<String> msgs = new ArrayList<>();
        int maxMessageLength = 1950;

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

    private enum Status {
        INIT,
        READING,
        DONE,
        READ_FAILED,
        ERROR,
    }
}