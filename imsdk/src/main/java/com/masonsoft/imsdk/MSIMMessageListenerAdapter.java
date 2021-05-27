package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public class MSIMMessageListenerAdapter implements MSIMMessageListener {

    @Override
    public void onMessageChanged(long targetUserId, long messageId) {
        // ignore
    }

    @Override
    public void onMessageCreated(long targetUserId, long messageId) {
        // ignore
    }

    @Override
    public void onMultiMessageChanged(long sessionUserId) {
        // ignore
    }

}
