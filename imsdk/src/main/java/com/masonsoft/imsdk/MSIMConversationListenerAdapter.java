package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public class MSIMConversationListenerAdapter implements MSIMConversationListener {

    @Override
    public void onConversationChanged(long conversationId, long targetUserId) {
        // ignore
    }

    @Override
    public void onConversationCreated(long conversationId, long targetUserId) {
        // ignore
    }

}
