package tw.xserver.loader.plugin;

public class Info {
    public final String name;
    public final Event pluginInstance;
    public final String[] depend;
    public final String[] softDepend;

    public Info(String name, Event pluginInstance, String[] depend, String[] softDepend) {
        this.name = name;
        this.pluginInstance = pluginInstance;
        this.depend = depend;
        this.softDepend = softDepend;
    }
}
