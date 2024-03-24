package tw.xserver.logger.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class ChannelSetting {
    public final Set<Long> white = new HashSet<>();
    public final Set<Long> black = new HashSet<>();
    public boolean whitelistStat; // changeable

    public ChannelSetting() {
        this.whitelistStat = true;
    }

    public ChannelSetting(boolean whitelist, @Nullable JsonArray white, @Nullable JsonArray black) {
        this.whitelistStat = whitelist;
        if (white != null) add(white, true);
        if (black != null) add(black, false);
    }

    public ChannelSetting toggle() {
        whitelistStat = !whitelistStat;
        return this;
    }

    public ChannelSetting add(@Nonnull JsonArray ids, boolean white) {
        if (white) {
            for (JsonElement i : ids) {
                this.white.add(i.getAsLong());
            }

        } else {
            for (JsonElement i : ids) {
                this.black.add(i.getAsLong());
            }
        }

        return this;
    }

    public boolean notContains(long channelID) {
        if (whitelistStat)
            return white.stream().noneMatch(i -> i == channelID);

        return black.stream().anyMatch(i -> i == channelID);
    }
}