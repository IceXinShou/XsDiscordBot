package tw.xserver.loader.plugin;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.base.Loader;

import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class ClassLoader extends URLClassLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassLoader.class);
    private final Map<String, URL> resourcePath = new HashMap<>();

    public ClassLoader() {
        super(new URL[0], Loader.class.getClassLoader());
    }

    public void addJar(File file, String main) {
        try {
            URL url = file.toURI().toURL();
            main = main.substring(0, main.lastIndexOf('.'));
            main = main.replace('.', '/');
            if (resourcePath.containsKey(main))
                throw new RuntimeException(main);
            resourcePath.put(main, url);
            addURL(url);
        } catch (MalformedURLException e) {
            LOGGER.error("add jar file failed");
            LOGGER.error(e.getMessage());
        }
    }

    @Nullable
    public Class<?> getClass(String name) {
        try {
            return loadClass(name, false);
        } catch (ClassNotFoundException e) {
            LOGGER.error("cannot found class: {}", name);
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public URL findResource(String name) {
        URL url = super.findResource(name);
        if (url == null) {
            int index = name.lastIndexOf('/');
            if (index == -1) return null;
            for (Map.Entry<String, URL> i : resourcePath.entrySet()) {
                if (name.startsWith(i.getKey() + '/')) {
                    try {
                        return new URL("jar:" + i.getValue() + "!/" + name.substring(i.getKey().length() + 1));
                    } catch (MalformedURLException e) {
                        LOGGER.error("cannot found resource: {}", name);
                        LOGGER.error(e.getMessage());
                        return null;
                    }
                }
            }
        }
        return url;
    }
}
