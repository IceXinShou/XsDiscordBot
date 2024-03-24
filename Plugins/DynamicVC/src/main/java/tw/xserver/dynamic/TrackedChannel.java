package tw.xserver.dynamic;

public class TrackedChannel {
    final long guildID;
    final long categoryID;
    final String name;
    final int bitrate;
    final int memberLimit;

    public TrackedChannel(long guildID, long categoryID, String name, int bitrate, int memberLimit) {
        this.guildID = guildID;
        this.categoryID = categoryID;
        this.name = name;
        this.bitrate = bitrate;
        this.memberLimit = memberLimit;
    }
}