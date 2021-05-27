package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMConversationListener {

    void onConversationChanged(final long conversationId,
                               final long targetUserId);

    void onConversationCreated(final long conversationId,
                               final long targetUserId);

}
