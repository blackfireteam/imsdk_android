package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;

/**
 * 处理会话相关内容。包括查询会话列表，监听会话更新等。
 *
 * @since 1.0
 */
public class MSIMConversationManager {

    private static final Singleton<MSIMConversationManager> INSTANCE = new Singleton<MSIMConversationManager>() {
        @Override
        protected MSIMConversationManager create() {
            return new MSIMConversationManager();
        }
    };

    /**
     * 获取 MSIMConversationManager 单例
     *
     * @see MSIMManager#getConversationManager()
     */
    @NonNull
    static MSIMConversationManager getInstance() {
        return INSTANCE.get();
    }

    private MSIMConversationManager() {
    }

}
