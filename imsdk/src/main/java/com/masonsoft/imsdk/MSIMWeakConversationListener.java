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
        final String tag = getOnConversationCreatedTag(sessionUserId, conversationId, conversationType, targetUserId);
        dispatch(tag, () -> {
            final MSIMConversationListener out = mOutRef.get();
            if (out != null) {
                out.onConversationCreated(sessionUserId, conversationId, conversationType, targetUserId);
            }
        });
    }

    @Nullable
    protected String getOnConversationCreatedTag(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        return "onConversationCreated_" + sessionUserId + "_" + conversationId + "_" + conversationType + "_" + targetUserId;
    }

    @Override
    public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        final String tag = getOnConversationChangedTag(sessionUserId, conversationId, conversationType, targetUserId);
        dispatch(tag, () -> {
            final MSIMConversationListener out = mOutRef.get();
            if (out != null) {
                out.onConversationChanged(sessionUserId, conversationId, conversationType, targetUserId);
            }
        });
    }

    @Nullable
    protected String getOnConversationChangedTag(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
        return "onConversationChanged_" + sessionUserId + "_" + conversationId + "_" + conversationType + "_" + targetUserId;
    }

}
