package com.xs.loader;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public abstract class PluginEvent extends ListenerAdapter {
    public abstract void initLoad();

    abstract public void unload();

    public CommandData[] guildCommands() {
        return null;
    }

    public SubcommandData[] subGuildCommands() {
        return null;
    }

    public CommandData[] globalCommands() {
        return null;
    }

    public abstract void loadConfigFile();

    public abstract void loadVariables();

    public abstract void loadLang();
}
