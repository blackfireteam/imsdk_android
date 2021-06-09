package com.masonsoft.imsdk.util;

import com.masonsoft.imsdk.core.IMLog;

import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class RxJavaUtil {

    private RxJavaUtil() {
    }

    public static void setErrorHandler() {
        RxJavaPlugins.setErrorHandler(e -> {
            e.printStackTrace();
            IMLog.e(new RuntimeException("RxJavaUtil", e));
        });
    }

}
