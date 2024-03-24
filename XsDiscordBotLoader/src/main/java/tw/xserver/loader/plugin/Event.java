package tw.xserver.loader.plugin;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class Event extends ListenerAdapter {
    public final boolean listener;
    public FileGetter getter;

    public Event(boolean listener) {
        this.listener = listener;
    }

    public void onReady() {
    }

    public void unload() {
    }

    public void reloadConfigFile() {
    }

    public void reloadLang() {
    }

    public void reloadAll() {
        reloadConfigFile();
        reloadLang();
    }

    @Nullable
    public CommandData[] guildCommands() {
        return null;
    }

    @Nullable
    public CommandData[] globalCommands() {
        return null;
    }
}
