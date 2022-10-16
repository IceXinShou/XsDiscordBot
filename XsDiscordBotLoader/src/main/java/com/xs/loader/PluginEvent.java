package com.xs.loader;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.fusesource.jansi.AnsiConsole;

public abstract class PluginEvent extends ListenerAdapter {
    protected String name;
    protected String version;

    public PluginEvent(final String name, final String version) {
        this.name = name;
        this.version = version;
    }

    public void initLoad() {
        AnsiConsole.systemInstall();
    }

    public void unload() {
        AnsiConsole.systemUninstall();
    }

    public CommandData[] guildCommands() {
        return null;
    }

    public SubcommandData[] subGuildCommands() {
        return null;
    }

    public CommandData[] globalCommands() {
        return null;
    }


    public void loadConfigFile() {
    }

    public void loadVariables() {
    }

    public void loadLang() {
    }
}
