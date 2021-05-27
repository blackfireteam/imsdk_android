package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMMessageListenerProxy extends RunOnUiThread implements MSIMMessageListener {

    @Nullable
    private final MSIMMessageListener mOut;

    public MSIMMessageListenerProxy(@Nullable MSIMMessageListener listener) {
        this(listener, false);
    }

    public MSIMMessageListenerProxy(@Nullable MSIMMessageListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = listener;
    }

    @Override
    public void onMessageChanged(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onMessageChanged(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    @Override
    public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onMessageCreated(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    @Override
    public void onMultiMessageChanged(long sessionUserId) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onMultiMessageChanged(sessionUserId);
            }
        });
    }

}
