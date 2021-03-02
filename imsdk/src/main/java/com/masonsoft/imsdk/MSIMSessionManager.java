package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;

/**
 * 处理登录状态相关内容，当登录状态发生变更时，将强制断开长连接然后发起新的长连接(如果有登录信息)。
 * 并且在读取任意数据时，都将校验当前长连接上通过认证的登陆信息是否与最新的登录信息一致，如果不一致，将强制中断长连接然后发起新的长连接(如果有需要)。
 *
 * @since 1.0
 */
public class MSIMSessionManager {

    private static final Singleton<MSIMSessionManager> INSTANCE = new Singleton<MSIMSessionManager>() {
        @Override
        protected MSIMSessionManager create() {
            return new MSIMSessionManager();
        }
    };

    /**
     * 获取 MSIMSessionManager 单例
     *
     * @see MSIMManager#getSessionManager()
     */
    @NonNull
    static MSIMSessionManager getInstance() {
        return INSTANCE.get();
    }

    private MSIMSessionManager() {
    }

}
