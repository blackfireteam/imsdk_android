package com.masonsoft.imsdk.core;

import android.util.Log;

public class RuntimeMode {

    /**
     * 当前 im 是否处于 debug 模式运行. debug 模式下，当出现意外的异常或错误时，会终止程序运行。
     */
    public static boolean isDebug() {
        return IMLog.getLogLevel() <= Log.DEBUG;
    }

    public static void throwIfDebug(Throwable e) {
        if (isDebug()) {
            throw new RuntimeException(e);
        }
    }

}
