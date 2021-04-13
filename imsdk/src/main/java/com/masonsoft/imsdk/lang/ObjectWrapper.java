package com.masonsoft.imsdk.lang;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class ObjectWrapper {

    @Nullable
    private Object mObject;

    public ObjectWrapper(@Nullable Object object) {
        mObject = object;
    }

    @Nullable
    public Object getObject() {
        return mObject;
    }

    public void setObject(@Nullable Object object) {
        mObject = object;
    }

}
