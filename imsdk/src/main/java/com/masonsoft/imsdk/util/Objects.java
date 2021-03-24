package com.masonsoft.imsdk.util;

public class Objects {

    public static String defaultObjectTag(Object object) {
        if (object == null) {
            return "null";
        }
        return object.getClass().getName() + "@" + System.identityHashCode(object);
    }

}
