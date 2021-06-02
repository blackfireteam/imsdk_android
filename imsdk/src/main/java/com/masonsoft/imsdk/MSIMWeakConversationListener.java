package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakConversationListener extends AutoRemoveDuplicateRunnable implements MSIMConversationListener {

    @NonNull
    private final WeakReference<MSIMConversationListener> mOutRef;

    public MSIMWeakConversationListener(@Nullable MSIMConversationListener callback) {
        this(callback, false);
    }

    public MSIMWeakConversationListener(@Nullable MSIMConversationListener callback, boolean runOnUiThread) {
        super(runOnUiThread);
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        final String tag = "onConversationCreated_" + sessionUserId + "_" + conversationId + "_" + conversationType + "_" + targetUserId;
        dispatch(tag, () -> {
            final MSIMConversationListener out = mOutRef.get();
            if (out != null) {
                out.onConversationCreated(sessionUserId, conversationId, conversationType, targetUserId);
            }
        });
    }

    @Override
    public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        final String tag = "onConversationChanged_" + sessionUserId + "_" + conversationId + "_" + conversationType + "_" + targetUserId;
        dispatch(tag, () -> {
            final MSIMConversationListener out = mOutRef.get();
            if (out != null) {
                out.onConversationChanged(sessionUserId, conversationId, conversationType, targetUserId);
            }
        });
    }

}
