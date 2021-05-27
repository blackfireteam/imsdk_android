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
    public void onMessageChanged(long targetUserId, long messageId) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onMessageChanged(targetUserId, messageId);
            }
        });
    }

    @Override
    public void onMessageCreated(long targetUserId, long messageId) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onMessageCreated(targetUserId, messageId);
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
