package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakConversationListener extends RunOnUiThread implements MSIMConversationListener {

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
    public void onConversationCreated(long conversationId, long targetUserId) {
        runOrPost(() -> {
            final MSIMConversationListener out = mOutRef.get();
            if (out != null) {
                out.onConversationCreated(conversationId, targetUserId);
            }
        });
    }

    @Override
    public void onConversationChanged(long conversationId, long targetUserId) {
        runOrPost(() -> {
            final MSIMConversationListener out = mOutRef.get();
            if (out != null) {
                out.onConversationChanged(conversationId, targetUserId);
            }
        });
    }

}
