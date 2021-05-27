package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public class MSIMMessageListenerAdapter implements MSIMMessageListener {

    @Override
    public void onMessageChanged(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        // ignore
    }

    @Override
    public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        // ignore
    }

    @Override
    public void onMultiMessageChanged(long sessionUserId) {
        // ignore
    }

}
