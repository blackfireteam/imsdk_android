package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakMessageListener extends AutoRemoveDuplicateRunnable implements MSIMMessageListener {

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
        final String tag = "onMultiMessageChanged_" + sessionUserId;
        dispatch(tag, () -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMultiMessageChanged(sessionUserId);
            }
        });
    }

    @Override
    public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        final String tag = "onMessageCreated_" + sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localMessageId;
        dispatch(tag, () -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMessageCreated(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    @Override
    public void onMessageChanged(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        final String tag = "onMessageChanged_" + sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localMessageId;
        dispatch(tag, () -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMessageChanged(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

}
