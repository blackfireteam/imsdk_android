package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * @see com.masonsoft.imsdk.core.db.ConversationDatabaseProvider
 */
public class ConversationObservable extends WeakObservable<ConversationObservable.ConversationObserver> {

    public static final ConversationObservable DEFAULT = new ConversationObservable();

    public interface ConversationObserver {
        void onConversationChanged(final long sessionUserId,
                                   final long conversationId);

        void onConversationCreated(final long sessionUserId,
                                   final long conversationId);
    }

    public void notifyConversationChanged(final long sessionUserId,
                                          final long conversationId) {
        forEach(conversationObserver -> conversationObserver.onConversationChanged(
                sessionUserId,
                conversationId)
        );
    }

    public void notifyConversationCreated(final long sessionUserId,
                                          final long conversationId) {
        forEach(conversationObserver -> conversationObserver.onConversationCreated(
                sessionUserId,
                conversationId)
        );
    }

}
