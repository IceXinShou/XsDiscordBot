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

    private final StringBuilder fullContent = new StringBuilder();
    private final Message replyMessage;
    private Message latestMessage;
    private final StringBuffer curStr = new StringBuffer();
    private final Logger logger = new Logger("ChatGPT MsgMgr");


    MessageManager(JsonFileManager manager, Message replyMessage, String msg, long id) {
        this.replyMessage = replyMessage;

        JsonObject obj = manager.getObj();

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
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(() -> {
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

                if (conn.getResponseCode() == 200) {
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
                        fullContent.append(newMsg);
                        curStr.append(newMsg);

                        // 每 2 秒更新文字
                        if (System.currentTimeMillis() - lastTime > 2000) {
                            if (lastTime == 0) { // 第一則訊息
                                latestMessage = replyMessage.reply(curStr.toString()).complete();
                            } else {
                                updateMessage(false);
                            }

                            lastTime = System.currentTimeMillis();
                        }
                    }

                    reader.close();
                } else {
                    // 例外情況
                    InputStreamReader reader = new InputStreamReader(conn.getErrorStream());
                    JsonObject rep = JsonParser.parseReader(reader).getAsJsonObject();
                    reader.close();

                    logger.warn(conn.getResponseCode() + " error on requesting...");
                    logger.warn(rep.toString());

                    replyMessage.reply("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();
                    return;
                }

                // 更新剩下文字
                updateMessage(true);
                ary.add(JsonParser.parseString("{\"role\":\"assistant\",\"content\":\"" + fullContent + "\"}").getAsJsonObject());
                System.out.println(obj);
                manager.save();
                waitingList.remove(id);
                executor.shutdown();
                System.out.println("SAVED");
            } catch (IOException e) {
                logger.warn(e.getMessage());
                replyMessage.reply("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();
            }
        });

    }

    private void updateMessage(boolean complete) {
        List<String> msgList = splitMessage(curStr.toString());

        if (complete)
            latestMessage.editMessage(msgList.get(0)).complete();
        else
            latestMessage.editMessage(msgList.get(0)).queue();

        if (msgList.size() > 1) {
            for (int i = 1; i < msgList.size(); i++) {
                if (i != msgList.size() - 1) { // multi reply
                    replyMessage.reply(msgList.get(i)).queue();
                } else {
                    latestMessage = replyMessage.reply(msgList.get(i)).complete();
                    curStr.setLength(0);
                    curStr.append(msgList.get(i));
                }
            }
        }
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