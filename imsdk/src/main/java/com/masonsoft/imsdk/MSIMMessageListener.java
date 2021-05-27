package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMMessageListener {

    void onMessageChanged(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long localMessageId
    );

    void onMessageCreated(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long localMessageId
    );

    void onMultiMessageChanged(final long sessionUserId);

}
