package com.xs.chatlogger;

import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xs.loader.MainLoader.jdaBot;

public class JsonManager {
    private final String TAG = "ChatLogger";
    private final String PATH_FOLDER_NAME = "./plugins/ChatLogger";
    private final Map<Long, JsonFileManager> fileManager = new HashMap<>();
    public final Map<Long, Map<Long, ChannelSetting>> channelSettings = new HashMap<>();

    public void init() {
        File f = new File(PATH_FOLDER_NAME + "/setting");
        f.mkdirs();

        File[] files = f.listFiles();
        if (files == null) return;

        // delete unable access guild json files
        Arrays.stream(files).forEach(i -> {
            if (jdaBot.getGuildById(i.getName().substring(0, i.getName().indexOf('.'))) == null) i.delete();
        });

        for (File file : files) {
            JsonFileManager manager = new JsonFileManager(PATH_FOLDER_NAME + "/setting/" + file.getName(), TAG, true);
            long guildID = Long.parseLong(file.getName().substring(0, file.getName().indexOf('.')));
            Guild guild = jdaBot.getGuildById(guildID);
            if (guild == null) continue;
            fileManager.put(guildID, manager);

            // put data from json files to channelSettings map
            for (String channelID : manager.getObj().keySet()) {
                // if channel cannot access, skip it
                if (guild.getGuildChannelById(channelID) == null) continue;

                JSONObject settingObj = manager.getObj().getJSONObject(channelID);
                Map<Long, ChannelSetting> tmp = new HashMap<>();
                tmp.put(Long.parseLong(channelID), new ChannelSetting(
                        settingObj.getBoolean("whitelist"),
                        settingObj.getJSONObject("white"),
                        settingObj.getJSONObject("black")
                ));
                channelSettings.put(guildID, tmp);
            }
        }
    }


    public ChannelSetting toggle(long guildID, long channelID) {
        // update map
        ChannelSetting setting = channelSettings.getOrDefault(guildID, new HashMap<>()).getOrDefault(channelID, new ChannelSetting()).toggle();

        // update json file
        JsonFileManager manager = fileManager.get(guildID);
        JSONObject obj = manager.getObj(String.valueOf(channelID));
        if (obj == null) return null; // WTF

        obj.put("whitelist", setting.whitelistStat);
        manager.save();

        return setting;
    }

    @Nullable
    public ChannelSetting addChannels(long guildID, long rootID, List<Long> channelIDs, boolean whitelist) {
        JsonFileManager manager = fileManager.get(guildID);
        JSONObject obj = manager.getObj(String.valueOf(rootID));
        if (obj == null) return null; // WTF

        JSONObject channelsObj = obj.getJSONObject(whitelist ? "white" : "black");

        for (Long channelID : channelIDs) {
            JSONObject tmp = new JSONObject();
            tmp.put("update", true);
            tmp.put("delete", true);
            channelsObj.put(String.valueOf(channelID), tmp);
        }

        manager.save();
        return channelSettings.getOrDefault(guildID, new HashMap<>()).getOrDefault(rootID, new ChannelSetting()).add(channelsObj, whitelist);
    }

    public void delete(long guildID, long channelID) {
        channelSettings.get(guildID).remove(channelID);
        fileManager.get(guildID).removeObj(String.valueOf(channelID)).save();
    }

    public ChannelSetting getOrDefault(long guildID, long channelID) {
        if (!channelSettings.containsKey(guildID)) channelSettings.put(guildID, new HashMap<>());
        Map<Long, ChannelSetting> settingMap = channelSettings.get(guildID);
        if (settingMap.containsKey(channelID)) {
            // setting exist
            return settingMap.get(channelID);
        }

        // setting not exist
        JsonFileManager manager = new JsonFileManager(PATH_FOLDER_NAME + "/setting/" + guildID + ".json", TAG, true);
        fileManager.put(guildID, manager);

        ChannelSetting setting = new ChannelSetting();
        settingMap.put(channelID, setting);


        JSONObject tmp = new JSONObject();
        tmp.put("whitelist", true);
        tmp.put("white", new JSONObject());
        tmp.put("black", new JSONObject());
        manager.getObj().put(String.valueOf(channelID), tmp);
        manager.save();

        return setting;
    }
}
