package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.IMMessageManager;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.core.db.TinyPage;
import com.masonsoft.imsdk.core.observable.MessageObservable;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.util.WeakObservable;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.util.Preconditions;

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

    @NonNull
    private final WeakObservable<MSIMMessageListener> mMessageListeners = new WeakObservable<>();
    @SuppressWarnings("FieldCanBeLocal")
    private final MessageObservable.MessageObserver mMessageObserver = new MessageObservable.MessageObserver() {
        @Override
        public void onMessageChanged(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            mMessageListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onMessageChanged(sessionUserId, conversationType, targetUserId, localMessageId);
                }
            });
        }

        @Override
        public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            mMessageListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onMessageCreated(sessionUserId, conversationType, targetUserId, localMessageId);
                }
            });
        }

        @Override
        public void onMessageBlockChanged(long sessionUserId, int conversationType, long targetUserId, long fromBlockId, long toBlockId) {
            // ignore
        }

        @Override
        public void onMultiMessageChanged(long sessionUserId) {
            mMessageListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onMultiMessageChanged(sessionUserId);
                }
            });
        }
    };

    private MSIMMessageManager() {
        MessageObservable.DEFAULT.registerObserver(mMessageObserver);
    }

    public void addMessageListener(@Nullable MSIMMessageListener listener) {
        if (listener != null) {
            mMessageListeners.registerObserver(listener);
        }
    }

    public void removeMessageListener(@Nullable MSIMMessageListener listener) {
        if (listener != null) {
            mMessageListeners.unregisterObserver(listener);
        }
    }

    /**
     * 将消息入库，然后异步发送
     */
    public void sendMessage(long sessionUserId, @NonNull MSIMMessage message, long receiver) {
        this.sendMessage(sessionUserId, message, receiver, null);
    }

    /**
     * 将消息入库，然后异步发送
     */
    public void sendMessage(long sessionUserId, @NonNull MSIMMessage message, long receiver, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueSendSessionMessage(
                sessionUserId,
                message.getMessage(),
                receiver,
                callback
        );
    }

    /**
     * 异步重新发送一个已经发送失败的消息
     */
    public void resendMessage(long sessionUserId, @NonNull MSIMMessage message) {
        this.resendMessage(sessionUserId, message, null);
    }

    /**
     * 异步重新发送一个已经发送失败的消息
     */
    public void resendMessage(long sessionUserId, @NonNull MSIMMessage message, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueResendSessionMessage(
                sessionUserId,
                message.getMessage(),
                callback
        );
    }

    public void markAsRead(long sessionUserId, long targetUserId) {
        markAsRead(sessionUserId, targetUserId, null);
    }

    public void markAsRead(long sessionUserId, long targetUserId, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueMarkAsReadActionMessage(sessionUserId, targetUserId, callback);
    }

    public void revoke(long sessionUserId, @NonNull MSIMMessage message) {
        revoke(sessionUserId, message, null);
    }

    public void revoke(long sessionUserId, @NonNull MSIMMessage message, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueRevokeActionMessage(
                sessionUserId,
                message.getMessage(),
                callback
        );
    }

    @WorkerThread
    @Nullable
    public MSIMMessage getMessage(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long messageId) {
        final IMMessage message = IMMessageManager.getInstance().getMessage(
                sessionUserId,
                conversationType,
                targetUserId,
                messageId
        );
        if (message == null) {
            return null;
        }
        return new MSIMMessage(message);
    }

    @WorkerThread
    @NonNull
    public TinyPage<MSIMMessage> pageQueryNewMessage(
            final long sessionUserId,
            final long seq,
            final int limit,
            final int conversationType,
            final long targetUserId) {
        final TinyPage<IMMessage> page = IMMessageManager.getInstance().pageQueryMessage(
                sessionUserId,
                seq,
                limit,
                conversationType,
                targetUserId,
                false
        );

        return page.transform(message -> {
            Preconditions.checkNotNull(message);
            return new MSIMMessage(message);
        });
    }

    @WorkerThread
    @NonNull
    public TinyPage<MSIMMessage> pageQueryHistoryMessage(
            final long sessionUserId,
            final long seq,
            final int limit,
            final int conversationType,
            final long targetUserId) {
        final TinyPage<IMMessage> page = IMMessageManager.getInstance().pageQueryMessage(
                sessionUserId,
                seq,
                limit,
                conversationType,
                targetUserId,
                true
        );

        return page.transform(message -> {
            Preconditions.checkNotNull(message);
            return new MSIMMessage(message);
        });
    }

}
