package com.xs.loader.plugin;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.Nullable;

public abstract class Event extends ListenerAdapter {
    public final boolean listener;

    public Event(boolean listener) {
        this.listener = listener;
    }

    public void initLoad() {
    }

    public void finishLoad() {
    }

    public void unload() {
    }

    @Nullable
    public CommandData[] guildCommands() {
        return null;
    }

    @Nullable
    public CommandData[] globalCommands() {
        return null;
    }

    public void loadConfigFile() {
    }

    public void loadLang() {
    }
}
