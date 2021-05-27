package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMMessageListener {

    void onMessageChanged(final long targetUserId, final long messageId);

    void onMessageCreated(final long targetUserId, final long messageId);

    void onMultiMessageChanged(final long sessionUserId);

}
