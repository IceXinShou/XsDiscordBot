package com.xs.loader;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.fusesource.jansi.AnsiConsole;

public abstract class PluginEvent extends ListenerAdapter {
    protected final boolean listener;

    public PluginEvent(boolean listener) {
        this.listener = listener;
    }

    public void initLoad() {
        AnsiConsole.systemInstall();
    }

    public void finishLoad() {
    }

    public void unload() {
        AnsiConsole.systemUninstall();
    }

    public CommandData[] guildCommands() {
        return null;
    }

    public CommandData[] globalCommands() {
        return null;
    }

    public void loadConfigFile() {
    }

    public void loadLang() {
    }
}
