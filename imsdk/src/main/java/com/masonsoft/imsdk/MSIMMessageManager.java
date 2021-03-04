package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;

/**
 * 长连接上的消息收发池
 */
public class MSIMMessageManager {

    private static final Singleton<MSIMMessageManager> INSTANCE = new Singleton<MSIMMessageManager>() {
        @Override
        protected MSIMMessageManager create() {
            return new MSIMMessageManager();
        }
    };

    /**
     * 获取 MSIMMessageManager 单例
     *
     * @see MSIMManager#getMessageManager()
     */
    @NonNull
    static MSIMMessageManager getInstance() {
        return INSTANCE.get();
    }

    private MSIMMessageManager() {
    }

}
