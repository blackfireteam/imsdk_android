package com.masonsoft.imsdk.lang;

/**
 * @since 1.0
 */
public class ObjectWrapper {

    private final Object mObject;

    public ObjectWrapper(Object object) {
        mObject = object;
    }

    public Object getObject() {
        return mObject;
    }

}
