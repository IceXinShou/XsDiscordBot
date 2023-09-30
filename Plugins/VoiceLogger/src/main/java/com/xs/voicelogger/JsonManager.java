package com.xs.voicelogger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xs.loader.base.Loader.jdaBot;

public class JsonManager {
    public final Map<Long, Map<Long, ChannelSetting>> channelSettings = new HashMap<>();
    private final String TAG = "VoiceLogger";
    private final String PATH_FOLDER_NAME = "plugins/VoiceLogger";
    private final Map<Long, JsonFileManager> fileManager = new HashMap<>();

    public void init() {
        File f = new File(PATH_FOLDER_NAME + "/setting");
        f.mkdirs();

        File[] files = f.listFiles();
        if (files == null) return;

        // delete unable access guild json files
        Arrays.stream(files).forEach(i -> {
            if (jdaBot.getGuildById(i.getName().substring(0, i.getName().indexOf('.'))) == null)
                i.delete();
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

                JsonObject settingObj = manager.getObjByKey(channelID).getAsJsonObject();
                Map<Long, ChannelSetting> tmp = new HashMap<>();
                tmp.put(Long.parseLong(channelID), new ChannelSetting(
                        settingObj.get("whitelist").getAsBoolean(),
                        settingObj.get("white").getAsJsonArray(),
                        settingObj.get("black").getAsJsonArray()
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
        JsonObject obj = manager.getObjByKey(String.valueOf(channelID));
        if (obj == null) return null; // WTF

        obj.addProperty("whitelist", setting.whitelistStat);
        manager.save();

        return setting;
    }

    @Nullable
    public ChannelSetting addChannels(long guildID, long rootID, List<Long> channelIDs, boolean whitelist) {
        JsonFileManager manager = fileManager.get(guildID);
        JsonObject obj = manager.getObjByKey(String.valueOf(rootID));
        if (obj == null) return null; // WTF

        JsonArray channelsObj = obj.get(whitelist ? "white" : "black").getAsJsonArray();

        for (Long channelID : channelIDs) {
            channelsObj.add(channelID);
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


        JsonObject tmp = new JsonObject();
        tmp.addProperty("whitelist", true);
        tmp.add("white", new JsonArray());
        tmp.add("black", new JsonArray());
        manager.getObj().add(String.valueOf(channelID), tmp);
        manager.save();

        return setting;
    }
}
