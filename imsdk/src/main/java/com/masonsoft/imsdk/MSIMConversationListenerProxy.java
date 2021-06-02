package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMConversationListenerProxy extends AutoRemoveDuplicateRunnable implements MSIMConversationListener {

    @Nullable
    private final MSIMConversationListener mOut;

    public MSIMConversationListenerProxy(@Nullable MSIMConversationListener listener) {
        this(listener, false);
    }

    public MSIMConversationListenerProxy(@Nullable MSIMConversationListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = listener;
    }

    @Override
    public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        final String tag = "onConversationChanged_" + sessionUserId + "_" + conversationId + "_" + conversationType + "_" + targetUserId;
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onConversationChanged(sessionUserId, conversationId, conversationType, targetUserId);
            }
        });
    }

    @Override
    public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        final String tag = "onConversationCreated_" + sessionUserId + "_" + conversationId + "_" + conversationType + "_" + targetUserId;
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onConversationCreated(sessionUserId, conversationId, conversationType, targetUserId);
            }
        });
    }

}
