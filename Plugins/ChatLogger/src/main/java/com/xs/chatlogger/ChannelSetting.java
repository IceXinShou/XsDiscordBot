package com.xs.chatlogger;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class ChannelSetting {
    public final Set<ListData> white = new HashSet<>();
    public final Set<ListData> black = new HashSet<>();
    public boolean whitelistStat; // changeable

    public ChannelSetting() {
        this.whitelistStat = true;
    }

    public ChannelSetting(boolean whitelist, @Nullable JSONObject white, @Nullable JSONObject black) {
        this.whitelistStat = whitelist;
        if (white != null) add(white, true);
        if (black != null) add(black, false);
    }

    public ChannelSetting toggle() {
        whitelistStat = !whitelistStat;
        return this;
    }

    public ChannelSetting add(JSONObject obj, boolean white) {
        for (String detectID : obj.keySet()) {
            JSONObject setting = obj.getJSONObject(detectID);
            ListData data = new ListData(
                    Long.parseLong(detectID),
                    setting.getBoolean("update"),
                    setting.getBoolean("delete")
            );

            if (white)
                this.white.add(data);
            else
                this.black.add(data);
        }
        return this;
    }

    public boolean contains(long channelID, DetectType type) {
        if (whitelistStat) {
            return white.stream().anyMatch(i -> i.contains(channelID, type));
        } else {
            return black.stream().noneMatch(i -> i.contains(channelID, type));
        }
    }


    public enum DetectType {
        UPDATE,
        DELETE
    }

    public static class ListData {
        public final long detectID;
        public final boolean update;
        public final boolean delete;

        public ListData(long detectID, boolean update, boolean delete) {
            this.detectID = detectID;
            this.update = update;
            this.delete = delete;
        }

        public boolean contains(long channelID, DetectType type) {
            if (detectID == channelID) {
                return ((update && type == DetectType.UPDATE) || (delete && type == DetectType.DELETE));
            }

            return false;
        }

        @Override
        public int hashCode() {
            return String.valueOf(detectID).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ListData) return (detectID == ((ListData) obj).detectID);

            return false;
        }
    }
}
