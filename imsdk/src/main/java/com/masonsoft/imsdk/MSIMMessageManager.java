package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.lang.GeneralResult;

import io.github.idonans.core.Singleton;

/**
 * @since 1.0
 */
public class MSIMMessageManager {

    private static final Singleton<MSIMMessageManager> INSTANCE = new Singleton<MSIMMessageManager>() {
        @Override
        protected MSIMMessageManager create() {
            return new MSIMMessageManager();
        }
    };

    static MSIMMessageManager getInstance() {
        return INSTANCE.get();
    }

    private MSIMMessageManager() {
    }

    /**
     * 将消息入库，然后异步发送
     */
    public void sendMessage(@NonNull MSIMMessage message, long receiver) {
        this.sendMessage(message, receiver, null);
    }

    /**
     * 将消息入库，然后异步发送
     */
    public void sendMessage(@NonNull MSIMMessage message, long receiver, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueSendSessionMessage(
                message.getMessage(),
                receiver,
                callback
        );
    }

    /**
     * 异步重新发送一个已经发送失败的消息
     */
    public void resendMessage(@NonNull MSIMMessage message) {
        this.resendMessage(message, null);
    }

    /**
     * 异步重新发送一个已经发送失败的消息
     */
    public void resendMessage(@NonNull MSIMMessage message, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueResendSessionMessage(
                message.getMessage(),
                callback
        );
    }

    public void markAsRead(long targetUserId) {
        markAsRead(targetUserId, null);
    }

    public void markAsRead(long targetUserId, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueMarkAsReadActionMessage(targetUserId, callback);
    }

}
