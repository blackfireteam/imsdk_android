package com.masonsoft.imsdk;

/**
 * 处理会话相关内容。包括查询会话列表，监听会话更新等。
 */
public class MSIMConversationManager {

    private static final class InstanceHolder {
        private static final MSIMConversationManager INSTANCE = new MSIMConversationManager();
    }

    /**
     * 获取 MSIMConversationManager 单例
     */
    static MSIMConversationManager getInstance() {
        return MSIMConversationManager.InstanceHolder.INSTANCE;
    }

    private MSIMConversationManager() {
    }

}
