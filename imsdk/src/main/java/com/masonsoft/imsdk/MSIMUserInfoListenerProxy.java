package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMUserInfoListenerProxy extends AutoRemoveDuplicateRunnable implements MSIMUserInfoListener {

    @Nullable
    private final MSIMUserInfoListener mOut;

    public MSIMUserInfoListenerProxy(@Nullable MSIMUserInfoListener listener) {
        this(listener, false);
    }

    public MSIMUserInfoListenerProxy(@Nullable MSIMUserInfoListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = listener;
    }

    @Override
    public void onUserInfoChanged(long userId) {
        final String tag = getOnUserInfoChangedTag(userId);
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onUserInfoChanged(userId);
            }
        });
    }

    @Nullable
    protected String getOnUserInfoChangedTag(long userId) {
        return "onUserInfoChanged_" + userId;
    }

}
