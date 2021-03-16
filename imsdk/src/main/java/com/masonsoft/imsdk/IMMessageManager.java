package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.masonsoft.imsdk.message.MessageWrapper;

/**
 * 长连接上的消息收发池
 */
public class IMMessageManager {

    private static final Singleton<IMMessageManager> INSTANCE = new Singleton<IMMessageManager>() {
        @Override
        protected IMMessageManager create() {
            return new IMMessageManager();
        }
    };

    /**
     * 获取 IMMessageManager 单例
     *
     * @see IMManager#getMessageManager()
     */
    @NonNull
    static IMMessageManager getInstance() {
        return INSTANCE.get();
    }

    private IMMessageManager() {
    }

    /**
     * 收到服务器发送的消息
     *
     * @param sessionUserId 收到消息的长连接上认证成功的用户 id
     */
    public void enqueueReceivedMessage(long sessionUserId, @NonNull MessageWrapper messageWrapper) {
        // TODO
    }

}
