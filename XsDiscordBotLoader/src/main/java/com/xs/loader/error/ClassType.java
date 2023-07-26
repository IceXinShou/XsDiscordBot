package com.xs.loader.error;

public enum ClassType {
    Guild("Guild"),
    Channel("Channel"),
    GuildChannel("GuildChannel"),
    TextChannel("TextChannel"),
    VoiceChannel("VoiceChannel"),
    ForumChannel("ForumChannel"),
    User("User"),
    Member("Member"),
    PrivateChannel("PrivateChannel");

    private final String name;

    ClassType(String name) {
        this.name = name;

    }

    public String getName() {
        return name;
    }
}
