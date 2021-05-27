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
    public void onConversationChanged(long conversationId, long targetUserId) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onConversationChanged(conversationId, targetUserId);
            }
        });
    }

    @Override
    public void onConversationCreated(long conversationId, long targetUserId) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onConversationCreated(conversationId, targetUserId);
            }
        });
    }

}
