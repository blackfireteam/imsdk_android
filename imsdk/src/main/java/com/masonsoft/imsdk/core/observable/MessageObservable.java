package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * @see com.masonsoft.imsdk.core.db.MessageDatabaseProvider
 */
public class MessageObservable extends WeakObservable<MessageObservable.MessageObserver> {

    public static final MessageObservable DEFAULT = new MessageObservable();

    public interface MessageObserver {
        void onMessageChanged(final long sessionUserId,
                              final int conversationType,
                              final long targetUserId,
                              final long localMessageId);

        void onMessageCreated(final long sessionUserId,
                              final int conversationType,
                              final long targetUserId,
                              final long localMessageId);

        void onMessageBlockChanged(final long sessionUserId,
                                   final int conversationType,
                                   final long targetUserId,
                                   final long fromBlockId,
                                   final long toBlockId);
    }

    public void notifyMessageChanged(final long sessionUserId,
                                     final int conversationType,
                                     final long targetUserId,
                                     final long localMessageId) {
        forEach(messageObserver -> messageObserver.onMessageChanged(
                sessionUserId,
                conversationType,
                targetUserId,
                localMessageId)
        );
    }

    public void notifyMessageCreated(final long sessionUserId,
                                     final int conversationType,
                                     final long targetUserId,
                                     final long localMessageId) {
        forEach(messageObserver -> messageObserver.onMessageCreated(
                sessionUserId,
                conversationType,
                targetUserId,
                localMessageId)
        );
    }

    public void notifyMessageBlockChanged(final long sessionUserId,
                                          final int conversationType,
                                          final long targetUserId,
                                          final long fromBlockId,
                                          final long toBlockId) {
        forEach(messageObserver -> messageObserver.onMessageBlockChanged(
                sessionUserId,
                conversationType,
                targetUserId,
                fromBlockId,
                toBlockId));
    }

}
