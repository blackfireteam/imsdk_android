package com.masonsoft.imsdk;

/**
 * MSIM 统一管理类, 核心类, 业务层使用 IM 功能的最外层入口。包括登录，消息，会话等 IM 强相关功能。
 *
 * @since 1.0
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

    /**
     * 获取 MSIMConversationManager 单例
     */
    public MSIMConversationManager getConversationManager() {
        return MSIMConversationManager.getInstance();
    }

}
