package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMUserInfoListenerProxy extends RunOnUiThread implements MSIMUserInfoListener {

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
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onUserInfoChanged(userId);
            }
        });
    }

}
