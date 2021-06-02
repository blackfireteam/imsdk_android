package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMMessageListenerProxy extends AutoRemoveDuplicateRunnable implements MSIMMessageListener {

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
        final String tag = "onMessageChanged_" + sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localMessageId;
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onMessageChanged(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    @Override
    public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        final String tag = "onMessageCreated_" + sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localMessageId;
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onMessageCreated(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    @Override
    public void onMultiMessageChanged(long sessionUserId) {
        final String tag = "onMultiMessageChanged_" + sessionUserId;
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onMultiMessageChanged(sessionUserId);
            }
        });
    }

}
