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
        final String tag = getOnMultiMessageChangedTag(sessionUserId);
        dispatch(tag, () -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMultiMessageChanged(sessionUserId);
            }
        });
    }

    @Nullable
    protected String getOnMultiMessageChangedTag(long sessionUserId) {
        return "onMultiMessageChanged_" + sessionUserId;
    }

    @Override
    public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        final String tag = getOnMessageCreatedTag(sessionUserId, conversationType, targetUserId, localMessageId);
        dispatch(tag, () -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMessageCreated(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    @Nullable
    protected String getOnMessageCreatedTag(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        return "onMessageCreated_" + sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localMessageId;
    }

    @Override
    public void onMessageChanged(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        final String tag = getOnMessageChangedTag(sessionUserId, conversationType, targetUserId, localMessageId);
        dispatch(tag, () -> {
            final MSIMMessageListener out = mOutRef.get();
            if (out != null) {
                out.onMessageChanged(sessionUserId, conversationType, targetUserId, localMessageId);
            }
        });
    }

    protected String getOnMessageChangedTag(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        return "onMessageChanged_" + sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localMessageId;
    }

}
