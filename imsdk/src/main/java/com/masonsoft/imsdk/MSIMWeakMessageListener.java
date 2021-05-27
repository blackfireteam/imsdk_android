package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakMessageListener extends RunOnUiThread implements MSIMMessageListener {

    @NonNull
    private final WeakReference<MSIMMessageListener> mOutRef;

    public MSIMWeakMessageListener(@Nullable MSIMMessageListener callback) {
        this(callback, false);
    }

    public MSIMWeakMessageListener(@Nullable MSIMMessageListener callback, boolean runOnUiThread) {
        super(runOnUiThread);
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onMultiMessageChanged(long sessionUserId) {
        runOrPost(() -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMultiMessageChanged(sessionUserId);
            }
        });
    }

    @Override
    public void onMessageCreated(long targetUserId, long messageId) {
        runOrPost(() -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMessageCreated(targetUserId, messageId);
            }
        });
    }

    @Override
    public void onMessageChanged(long targetUserId, long messageId) {
        runOrPost(() -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMessageChanged(targetUserId, messageId);
            }
        });
    }

}
