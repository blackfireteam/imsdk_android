package com.masonsoft.imsdk.core;

import android.util.Log;

import com.idonans.core.manager.ProcessManager;
import com.masonsoft.imsdk.IMLog;

public class IMProcessValidator {

    private IMProcessValidator() {
    }

    /**
     * 校验 IM 当前运行的进程是否合法
     */
    public static void validateProcess() {
        if (!ProcessManager.getInstance().isMainProcess()) {
            final Throwable e = new IllegalAccessError("current process is not main process");
            IMLog.e(e);

            // 如果运行在 debug 模式下，则直接抛出异常
            if (IMLog.getLogLevel() <= Log.DEBUG) {
                throw new RuntimeException(e);
            }
        }
    }

}