package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public class MSIMConversationListenerAdapter implements MSIMConversationListener {

    @Override
    public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        // ignore
    }

    @Override
    public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        // ignore
    }

}
