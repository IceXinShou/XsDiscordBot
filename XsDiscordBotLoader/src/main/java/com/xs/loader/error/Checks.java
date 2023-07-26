package com.xs.loader.error;

public class Checks {
    public static void notNull(ClassType type, Object obj) throws Exception {
        if (obj == null) {
            throw new Exceptions("the type of " + type.getName() + " is null");
        }
    }
}
