package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * @see com.masonsoft.imsdk.core.db.ConversationDatabaseProvider
 */
public class ConversationObservable extends WeakObservable<ConversationObservable.ConversationObserver> {

    public static final ConversationObservable DEFAULT = new ConversationObservable();

    public interface ConversationObserver {
        void onConversationChanged(final long sessionUserId,
                                   final long conversationId,
                                   final int conversationType,
                                   final long targetUserId);

        void onConversationCreated(final long sessionUserId,
                                   final long conversationId,
                                   final int conversationType,
                                   final long targetUserId);
    }

    public void notifyConversationChanged(final long sessionUserId,
                                          final long conversationId,
                                          final int conversationType,
                                          final long targetUserId) {
        forEach(conversationObserver -> conversationObserver.onConversationChanged(
                sessionUserId,
                conversationId,
                conversationType,
                targetUserId)
        );
    }

    public void notifyConversationCreated(final long sessionUserId,
                                          final long conversationId,
                                          final int conversationType,
                                          final long targetUserId) {
        forEach(conversationObserver -> conversationObserver.onConversationCreated(
                sessionUserId,
                conversationId,
                conversationType,
                targetUserId)
        );
    }

}
