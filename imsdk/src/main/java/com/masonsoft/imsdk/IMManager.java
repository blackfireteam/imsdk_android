package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.idonans.core.manager.ProcessManager;

/**
 * MSIM 统一管理类, 核心类, 业务层使用 IM 功能的最外层入口。包括登录，消息，会话等 IM 强相关功能。
 * MSIM 运行在单一的进程中，如果业务上是一个多进程应用，需要注意，在所有调用 MSIM 相关的功能时都需要处于主进程中。
 *
 * @since 1.0
 */
public class IMManager {

    private static final Singleton<IMManager> INSTANCE = new Singleton<IMManager>() {
        @Override
        protected IMManager create() {
            return new IMManager();
        }
    };

    /**
     * 获取 MSIMManager 单例
     */
    @NonNull
    public static IMManager getInstance() {
        // 进程检查，如果不是主进程，抛出异常.
        if (!ProcessManager.getInstance().isMainProcess()) {
            throw new IllegalAccessError("current process is not main process");
        }

        return INSTANCE.get();
    }

    private IMManager() {
    }

    /**
     * 获取 MSIMConversationManager 单例
     */
    @NonNull
    public IMConversationManager getConversationManager() {
        return IMConversationManager.getInstance();
    }

    /**
     * 获取 MSIMSessionManager 单例
     */
    @NonNull
    public IMSessionManager getSessionManager() {
        return IMSessionManager.getInstance();
    }

    /**
     * 获取 MSIMMessageManager 单例
     */
    public IMMessageManager getMessageManager() {
        return IMMessageManager.getInstance();
    }

}
