package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMConversationListenerProxy extends RunOnUiThread implements MSIMConversationListener {

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
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onConversationChanged(sessionUserId, conversationId, conversationType, targetUserId);
            }
        });
    }

    @Override
    public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onConversationCreated(sessionUserId, conversationId, conversationType, targetUserId);
            }
        });
    }

}
