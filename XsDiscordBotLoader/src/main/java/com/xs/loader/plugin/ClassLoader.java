package com.xs.loader.plugin;


import com.xs.loader.base.Loader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class ClassLoader extends URLClassLoader {
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
                throw new RuntimeException();
            resourcePath.put(main, url);
            addURL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public Class<?> getClass(String name) {
        try {
            return loadClass(name, false);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

//    @Override
//    public InputStream getResourceAsStream(String name) {
//        System.out.println("getResourceAsStream: " + name);
//        return super.getResourceAsStream(name);
//    }

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
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }
//        System.out.println("findResource: " + url);
        return url;
    }

}