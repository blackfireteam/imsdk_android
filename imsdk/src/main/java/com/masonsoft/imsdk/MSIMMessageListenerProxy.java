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
        final String tag = getOnMessageChangedTag(sessionUserId, conversationType, targetUserId, localMessageId);
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onMessageChanged(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    @Nullable
    protected String getOnMessageChangedTag(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        return "onMessageChanged_" + sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localMessageId;
    }

    @Override
    public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        final String tag = getOnMessageCreatedTag(sessionUserId, conversationType, targetUserId, localMessageId);
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onMessageCreated(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    @Nullable
    protected String getOnMessageCreatedTag(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        return "onMessageCreated_" + sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localMessageId;
    }

    @Override
    public void onMultiMessageChanged(long sessionUserId) {
        final String tag = getOnMultiMessageChangedTag(sessionUserId);
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onMultiMessageChanged(sessionUserId);
            }
        });
    }

    @Nullable
    protected String getOnMultiMessageChangedTag(long sessionUserId) {
        return "onMultiMessageChanged_" + sessionUserId;
    }

}
