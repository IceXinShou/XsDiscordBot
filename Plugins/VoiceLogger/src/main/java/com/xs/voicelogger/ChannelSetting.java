package com.xs.voicelogger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;

import java.util.HashSet;
import java.util.Set;

public class ChannelSetting {
    public boolean whitelistStat; // changeable
    public final Set<Long> white = new HashSet<>();
    public final Set<Long> black = new HashSet<>();

    public ChannelSetting() {
        this.whitelistStat = true;
    }

    public ChannelSetting(boolean whitelist, @Nullable JSONArray white, @Nullable JSONArray black) {
        this.whitelistStat = whitelist;
        if (white != null) add(white, true);
        if (black != null) add(black, false);
    }

    public ChannelSetting toggle() {
        whitelistStat = !whitelistStat;
        return this;
    }

    public ChannelSetting add(@NotNull JSONArray ids, boolean white) {
        if (white) {
            for (Object i : ids) {
                if (i instanceof Long)
                    this.white.add((Long) i);
            }

        } else {
            for (Object i : ids) {
                if (i instanceof Long)
                    this.black.add((Long) i);
            }
        }

        return this;
    }

    public boolean contains(long channelID) {
        if (whitelistStat) {
            return white.stream().anyMatch(i -> i == channelID);
        } else {
            return black.stream().noneMatch(i -> i == channelID);
        }
    }
}
