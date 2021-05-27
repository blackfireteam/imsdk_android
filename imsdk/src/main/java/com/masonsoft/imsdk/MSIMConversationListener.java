package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMConversationListener {

    void onConversationChanged(
            final long sessionUserId,
            final long conversationId,
            final int conversationType,
            final long targetUserId
    );

    void onConversationCreated(
            final long sessionUserId,
            final long conversationId,
            final int conversationType,
            final long targetUserId
    );

}
