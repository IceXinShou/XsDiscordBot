package com.xs.dynamicvc;

public class TrackedChannel {
    long guildID;
    long categoryID;
    String name;
    int bitrate;
    int memberLimit;

    public TrackedChannel(long guildID, long categoryID, String name, int bitrate, int memberLimit) {
        this.guildID = guildID;
        this.categoryID = categoryID;
        this.name = name;
        this.bitrate = bitrate;
        this.memberLimit = memberLimit;
    }
}