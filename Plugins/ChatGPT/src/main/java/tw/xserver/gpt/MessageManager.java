package tw.xserver.gpt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.util.json.JsonObjFileManager;

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

public class MessageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageManager.class);
    public final StringBuilder fullContent = new StringBuilder();
    private final Message replyMessage;
    private final StringBuffer curStr = new StringBuffer();
    private final String msg;
    private final JsonObject postObj = new JsonObject();
    private final JsonArray dataAry;
    private final Status status;
    private Message latestMessage;


    MessageManager(JsonObjFileManager manager, Message replyMessage, String msg, long id) {
        this.replyMessage = replyMessage;
        this.msg = msg;
        this.status = new Status();

        // 初始化部分請求內容
        JsonObject obj = manager.get();
        if (!obj.has(String.valueOf(id))) {
            dataAry = ChatGPT.defaultAry.deepCopy();
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
                status.set(Type.ERROR);
                LOGGER.error(e.getMessage());
            }
        });

        long lastTime = 0;
        while (!status.isDone()) {
            switch (status.get()) {
                case READING: {
                    if (System.currentTimeMillis() - lastTime > 2000) {
                        if (lastTime == 0) { // 第一則訊息
                            latestMessage = replyMessage.reply(curStr + "...").complete();
                        } else {
                            updateMessage(false);
                        }

                        lastTime = System.currentTimeMillis();
                    }
                    break;
                }

                case FINISH_READING: {
                    updateMessage(true);
                    JsonObject newObj = new JsonObject();
                    newObj.addProperty("role", "assistant");
                    newObj.addProperty("content", fullContent.toString());
                    newObj.addProperty("token", TokenCounter.getToken(fullContent.toString()));
                    dataAry.add(newObj);
                    manager.save();
                    break;
                }

                case READ_FAILED: {
                    fullContent.append("很抱歉，出現了一些錯誤。請等待修復或通知開發人員");
                    status.set(Type.DONE);
                    break;
                }
            }
        }

        executor.shutdown();
    }

    private String buildRequest() {
        // 新增訊息至部分請求內容
        JsonObject msgObj = new JsonObject();
        msgObj.addProperty("role", "user");
        msgObj.addProperty("content", msg);
        msgObj.addProperty("token", TokenCounter.getToken(msg));
        dataAry.add(msgObj);

        JsonArray tmpPostAry = new JsonArray();
        final int LIMIT_TOKEN = ChatGPT.configFile.MaxToken - ChatGPT.configFile.MinReplyToken - ChatGPT.prompt_token;
        int tokens = 0;

        // add from last message
        for (int i = dataAry.size() - 1; i >= 0; --i) {
            JsonObject aryObj = dataAry.get(i).getAsJsonObject();
            JsonObject obj = new JsonObject();

            int curToken = aryObj.get("token").getAsInt();
            if (tokens + curToken > LIMIT_TOKEN) {
                aryObj = dataAry.get(0).getAsJsonObject();
                obj.addProperty("role", aryObj.get("role").getAsString());
                obj.addProperty("content", aryObj.get("content").getAsString());
                tmpPostAry.add(obj);
                break;
            }
            tokens += curToken;

            obj.addProperty("role", aryObj.get("role").getAsString());
            obj.addProperty("content", aryObj.get("content").getAsString());

            tmpPostAry.add(obj);
        }

        // reverse
        JsonArray postAry = new JsonArray();
        for (int i = tmpPostAry.size() - 1; i >= 0; --i)
            postAry.add(tmpPostAry.get(i));

        // 完整請求建構
        postObj.addProperty("stream", true);
        postObj.addProperty("model", ChatGPT.configFile.Module);
        postObj.add("messages", postAry);

        return postObj.toString();
    }

    private void getData(String postStr) throws IOException {
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + ChatGPT.configFile.API_KEY);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postStr.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() == 200) {
            status.set(Type.READING);

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
                status.set(Type.FINISH_READING);
            }
        } else {
            // 例外情況
            status.set(Type.READ_FAILED);
            replyMessage.reply("很抱歉，出現了一些錯誤。請等待修復或通知開發人員").queue();

            try (InputStreamReader reader = new InputStreamReader(conn.getErrorStream())) {
                JsonObject rep = JsonParser.parseReader(reader).getAsJsonObject();

//                String errorCode = rep.get("error").getAsJsonObject().get("code").getAsString();
//                switch (errorCode) {
//                    case "context_length_exceeded": {
//                        getData(reBuildRequest());
//                        return;
//                    }
//                }

                LOGGER.error(conn.getResponseCode() + " error on requesting...");
                LOGGER.error(rep.toString());
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

    private static class Status {
        private Type status = Type.INIT;

        private synchronized void set(Type newStatus) {
            this.status = newStatus;
        }

        private synchronized Type get() {
            return this.status;
        }

        private synchronized boolean isDone() {
            return status == Type.DONE;
        }

        @Override
        public synchronized boolean equals(Object obj) {
            if (obj instanceof Type)
                return this.status == obj;
            return false;
        }
    }

    private enum Type {
        INIT,
        READING,
        FINISH_READING,
        READ_FAILED,
        ERROR,
        DONE
    }
}