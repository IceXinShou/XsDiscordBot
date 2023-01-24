package com.xs.loader;

public class PluginInfo {
    final String name;
    final PluginEvent pluginInstance;
    final String[] depend;
    final String[] softDepend;

    public PluginInfo(String name, PluginEvent pluginInstance, String[] depend, String[] softDepend) {
        this.name = name;
        this.pluginInstance = pluginInstance;
        this.depend = depend;
        this.softDepend = softDepend;
    }
}
