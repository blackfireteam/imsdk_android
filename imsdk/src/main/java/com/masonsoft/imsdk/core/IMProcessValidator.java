package com.masonsoft.imsdk.core;

import io.github.idonans.core.manager.ProcessManager;

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
            RuntimeMode.fixme(e);
        }
    }

}
