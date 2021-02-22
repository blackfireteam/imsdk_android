package com.masonsoft.imsdk;

/**
 * MSIM 统一管理类(单例 {@linkplain #getInstance() MSIMManager.getInstance()}).
 */
public class MSIMManager {

    private static final class InstanceHolder {
        private static final MSIMManager INSTANCE = new MSIMManager();
    }

    /**
     * 获取 MSIMManager 单例
     */
    public static MSIMManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private MSIMManager() {
    }

}
