package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.IMMessageManager;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.core.IMSessionManager;
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

    @WorkerThread
    @Nullable
    public MSIMMessage getMessage(
            final long targetUserId,
            final long messageId) {
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        if (sessionUserId <= 0) {
            return null;
        }

        final IMMessage message = IMMessageManager.getInstance().getMessage(
                sessionUserId,
                IMConstants.ConversationType.C2C,
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
    public TinyPage<MSIMMessage> pageQueryNewMessage(final long seq,
                                                     final int limit,
                                                     final long targetUserId) {
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        if (sessionUserId <= 0) {
            return new TinyPage<>();
        }

        final TinyPage<IMMessage> page = IMMessageManager.getInstance().pageQueryMessage(
                sessionUserId,
                seq,
                limit,
                IMConstants.ConversationType.C2C,
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
    public TinyPage<MSIMMessage> pageQueryHistoryMessage(final long seq,
                                                         final int limit,
                                                         final long targetUserId) {
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        if (sessionUserId <= 0) {
            return new TinyPage<>();
        }

        final TinyPage<IMMessage> page = IMMessageManager.getInstance().pageQueryMessage(
                sessionUserId,
                seq,
                limit,
                IMConstants.ConversationType.C2C,
                targetUserId,
                true
        );

        return page.transform(message -> {
            Preconditions.checkNotNull(message);
            return new MSIMMessage(message);
        });
    }

}
