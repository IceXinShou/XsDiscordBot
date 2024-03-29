package tw.xserver.logger.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Guild;
import tw.xserver.loader.util.json.JsonObjFileManager;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tw.xserver.loader.base.Loader.jdaBot;

public class JsonManager {
    public final Map<Long, Map<Long, ChannelSetting>> channelSettings = new HashMap<>();
    private final String PATH_FOLDER_NAME = "plugins/VoiceLogger";
    private final Map<Long, JsonObjFileManager> fileManager = new HashMap<>();

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
            JsonObjFileManager manager = new JsonObjFileManager(PATH_FOLDER_NAME + "/setting/" + file.getName());
            long guildID = Long.parseLong(file.getName().substring(0, file.getName().indexOf('.')));
            Guild guild = jdaBot.getGuildById(guildID);
            if (guild == null) continue;
            fileManager.put(guildID, manager);

            // put data from json files to channelSettings map
            for (String channelID : manager.get().keySet()) {
                // if channel cannot access, skip it
                if (guild.getGuildChannelById(channelID) == null) continue;

                JsonObject settingObj = manager.getAsJsonObject(channelID);
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
        ChannelSetting setting = channelSettings
                .computeIfAbsent(guildID, k -> new HashMap<>())
                .computeIfAbsent(channelID, k -> new ChannelSetting())
                .toggle();

        // update json file
        JsonObjFileManager manager = fileManager.get(guildID);
        JsonObject obj = manager.getAsJsonObject(String.valueOf(channelID));
        if (obj == null) return null; // WTF

        obj.addProperty("whitelist", setting.whitelistStat);
        manager.save();

        return setting;
    }

    @Nullable
    public ChannelSetting addChannels(long guildID, long rootID, List<Long> channelIDs, boolean whitelist) {
        JsonObjFileManager manager = fileManager.get(guildID);
        JsonObject obj = manager.getAsJsonObject(String.valueOf(rootID));
        if (obj == null) return null; // WTF

        JsonArray channelsObj = obj.get(whitelist ? "white" : "black").getAsJsonArray();

        for (Long channelID : channelIDs) {
            channelsObj.add(channelID);
        }

        manager.save();
        return channelSettings
                .computeIfAbsent(guildID, k -> new HashMap<>())
                .computeIfAbsent(rootID, k -> new ChannelSetting())
                .add(channelsObj, whitelist);
    }

    public void delete(long guildID, long channelID) {
        channelSettings.get(guildID).remove(channelID);
        fileManager.get(guildID).remove(String.valueOf(channelID)).save();
    }

    public ChannelSetting getOrDefault(long guildID, long channelID) {
        if (!channelSettings.containsKey(guildID)) channelSettings.put(guildID, new HashMap<>());
        Map<Long, ChannelSetting> settingMap = channelSettings.get(guildID);
        if (settingMap.containsKey(channelID)) {
            // setting exist
            return settingMap.get(channelID);
        }

        // setting not exist
        JsonObjFileManager manager = new JsonObjFileManager(PATH_FOLDER_NAME + "/setting/" + guildID + ".json");
        fileManager.put(guildID, manager);

        ChannelSetting setting = new ChannelSetting();
        settingMap.put(channelID, setting);


        JsonObject tmp = new JsonObject();
        tmp.addProperty("whitelist", true);
        tmp.add("white", new JsonArray());
        tmp.add("black", new JsonArray());
        manager.add(String.valueOf(channelID), tmp);
        manager.save();

        return setting;
    }
}
