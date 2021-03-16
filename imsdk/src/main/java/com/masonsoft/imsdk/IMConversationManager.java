package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;

/**
 * 处理会话相关内容。包括查询会话列表，监听会话更新等。
 *
 * @since 1.0
 */
public class IMConversationManager {

    private static final Singleton<IMConversationManager> INSTANCE = new Singleton<IMConversationManager>() {
        @Override
        protected IMConversationManager create() {
            return new IMConversationManager();
        }
    };

    /**
     * 获取 IMConversationManager 单例
     *
     * @see IMManager#getConversationManager()
     */
    @NonNull
    static IMConversationManager getInstance() {
        return INSTANCE.get();
    }

    private IMConversationManager() {
    }

}
