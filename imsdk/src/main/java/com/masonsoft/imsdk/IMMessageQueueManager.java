package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.message.MessageWrapper;

/**
 * 消息收发队列
 *
 * @since 1.0
 */
public class IMMessageQueueManager {

    private static final Singleton<IMMessageQueueManager> INSTANCE = new Singleton<IMMessageQueueManager>() {
        @Override
        protected IMMessageQueueManager create() {
            return new IMMessageQueueManager();
        }
    };

    @NonNull
    public static IMMessageQueueManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private IMMessageQueueManager() {
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
