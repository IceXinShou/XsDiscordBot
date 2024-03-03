package tw.xserver.logger.chat;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.util.json.JsonObjFileManager;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

import static tw.xserver.loader.base.Loader.jdaBot;

public class JsonManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonManager.class);
    public final Map<Long, Map<Long, ChannelSetting>> channelSettings = new HashMap<>();
    private final String PATH_FOLDER_NAME = "plugins/ChatLogger";
    private final Map<Long, JsonObjFileManager> fileManager = new HashMap<>();

    public void init() {
        File f = new File(PATH_FOLDER_NAME + "/setting");
        if (f.mkdirs()) {
            LOGGER.info("default data folder created");
        }

        File[] files = f.listFiles();
        if (files == null) return;

        // delete unable access guild json files
        Arrays.stream(files).forEach(file -> {
            long guildID = Long.parseLong(file.getName().substring(0, file.getName().indexOf('.')));
            Guild guild = jdaBot.getGuildById(guildID);
            if (guild == null) {
                file.delete();
                return;
            }

            JsonObjFileManager manager = new JsonObjFileManager(PATH_FOLDER_NAME + "/setting/" + file.getName());

            fileManager.put(guildID, manager);

            // put data from json files to channelSettings map

            Iterator<String> iterator = manager.get().keySet().iterator();
            while (iterator.hasNext()) {
                String channelID = iterator.next();

                // if channel cannot access, remove and skip it
                if (guild.getGuildChannelById(channelID) == null) {
                    iterator.remove();
                    continue;
                }

                JsonObject settingObj = manager.getAsJsonObject(channelID);
                Map<Long, ChannelSetting> tmp = new HashMap<>();
                tmp.put(Long.parseLong(channelID), new ChannelSetting(
                        settingObj.get("whitelist").getAsBoolean(),
                        settingObj.get("white").getAsJsonObject(),
                        settingObj.get("black").getAsJsonObject()
                ));
                channelSettings.put(guildID, tmp);
            }

            manager.save();
        });
    }


    public ChannelSetting toggle(long guildID, long channelID) {
        // update map
        ChannelSetting setting = channelSettings
                .computeIfAbsent(guildID, k -> new HashMap<>())
                .computeIfAbsent(channelID, k -> new ChannelSetting()).toggle();

        // update json file
        JsonObjFileManager manager = fileManager.get(guildID);
        JsonObject obj = manager.getAsJsonObject(String.valueOf(channelID));
        if (obj == null) return null; // impossible

        obj.addProperty("whitelist", setting.whitelistStat);
        manager.save();

        return setting;
    }

    @Nullable
    public ChannelSetting addChannels(long guildID, long rootID, List<Long> channelIDs, boolean whitelist) {
        JsonObjFileManager manager = fileManager.get(guildID);
        JsonObject obj = manager.getAsJsonObject(String.valueOf(rootID));
        if (obj == null) return null; // WTF

        JsonObject channelsObj = obj.get(whitelist ? "white" : "black").getAsJsonObject();

        for (Long channelID : channelIDs) {
            JsonObject tmp = new JsonObject();
            tmp.addProperty("update", true);
            tmp.addProperty("delete", true);
            channelsObj.add(String.valueOf(channelID), tmp);
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
        Map<Long, ChannelSetting> settingMap = channelSettings.computeIfAbsent(guildID, k -> new HashMap<>());

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
        tmp.add("white", new JsonObject());
        tmp.add("black", new JsonObject());
        manager.add(String.valueOf(channelID), tmp);
        manager.save();

        return setting;
    }
}
