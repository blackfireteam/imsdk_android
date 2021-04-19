package com.masonsoft.imsdk.core;

import android.util.Log;

public class RuntimeMode {

    /**
     * 当前 im 是否处于 debug 模式运行
     */
    public static boolean isDebug() {
        return IMLog.getLogLevel() <= Log.DEBUG;
    }

    public static void fixme(Throwable e) {
        if (isDebug()) {
            final Throwable error = new RuntimeException("fix me !!!!!!!!!", e);
            error.printStackTrace();
        }
    }

}
