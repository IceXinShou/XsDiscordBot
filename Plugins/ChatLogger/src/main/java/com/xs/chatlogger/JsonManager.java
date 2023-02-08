package com.xs.chatlogger;

import com.xs.loader.util.JsonFileManager;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonManager {
    private final String TAG = "ChatLogger";
    private final String PATH_FOLDER_NAME = "./plugins/ChatLogger";
    private final JsonFileManager fileManager = new JsonFileManager(PATH_FOLDER_NAME + "/Data/data.json", TAG, true);

    public void toggle(String id) {
        JSONObject obj = fileManager.getObj(String.valueOf(id));
        if (obj == null) return;
        obj.put("whitelist", !obj.getBoolean("whitelist"));
        save();
    }

    public void addChannels(String rootID, List<String> channelIDs, String type) {
        JSONObject obj = fileManager.getObj(rootID).getJSONObject(type.toString().toLowerCase());
        for (String i : channelIDs) {
            JSONObject tmp = new JSONObject();
            tmp.put("receive", true);
            tmp.put("update", true);
            tmp.put("delete", true);
            obj.put(i, tmp);
        }
        fileManager.save();
    }

    public void delete(String id) {
        fileManager.removeObj(id);
        fileManager.save();
    }

    public ChannelSetting getOrDefault(long id) {
        JSONObject obj = fileManager.getObj(String.valueOf(id));
        ChannelSetting setting = new ChannelSetting();

        if (obj == null) {
            setting.whitelist = true;
            setting.white = new HashSet<>();
            setting.black = new HashSet<>();

            JSONObject tmp = new JSONObject();
            tmp.put("whitelist", true);
            tmp.put("white", new JSONObject());
            tmp.put("black", new JSONObject());
            fileManager.getObj().put(String.valueOf(id), tmp);
            save();
        } else {
            setting.whitelist = obj.getBoolean("whitelist");
            setting.white = getChannels(obj.getJSONObject("white"));
            setting.black = getChannels(obj.getJSONObject("black"));
        }

        return setting;
    }

    private Set<ChannelSetting.ListData> getChannels(JSONObject rootObj) {
        Set<ChannelSetting.ListData> channelsList = new HashSet<>();

        for (String i : rootObj.keySet()) {
            JSONObject obj = rootObj.getJSONObject(i);
            ChannelSetting.ListData channel = new ChannelSetting.ListData();

            channel.id = Long.parseLong(i);
            channel.receive = obj.getBoolean("receive");
            channel.update = obj.getBoolean("update");
            channel.delete = obj.getBoolean("delete");
            channelsList.add(channel);
        }

        return channelsList;
    }

    public void save() {
        fileManager.save();
    }
}
