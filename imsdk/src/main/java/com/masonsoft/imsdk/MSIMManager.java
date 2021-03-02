package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;

/**
 * MSIM 统一管理类, 核心类, 业务层使用 IM 功能的最外层入口。包括登录，消息，会话等 IM 强相关功能。
 *
 * @since 1.0
 */
public class MSIMManager {

    private static final Singleton<MSIMManager> INSTANCE = new Singleton<MSIMManager>() {
        @Override
        protected MSIMManager create() {
            return new MSIMManager();
        }
    };

    /**
     * 获取 MSIMManager 单例
     */
    @NonNull
    public static MSIMManager getInstance() {
        return INSTANCE.get();
    }

    private MSIMManager() {
    }

    /**
     * 获取 MSIMConversationManager 单例
     */
    @NonNull
    public MSIMConversationManager getConversationManager() {
        return MSIMConversationManager.getInstance();
    }

    /**
     * 获取 MSIMSessionManager 单例
     */
    @NonNull
    public MSIMSessionManager getSessionManager() {
        return MSIMSessionManager.getInstance();
    }

}
