package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMSessionListenerProxy extends RunOnUiThread implements MSIMSessionListener {

    @Nullable
    private final MSIMSessionListener mOut;

    public MSIMSessionListenerProxy(@Nullable MSIMSessionListener listener) {
        this(listener, false);
    }

    public MSIMSessionListenerProxy(@Nullable MSIMSessionListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = listener;
    }

    @Override
    public void onSessionUserIdChanged() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onSessionUserIdChanged();
            }
        });
    }

    @Override
    public void onSessionChanged() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onSessionChanged();
            }
        });
    }

}
