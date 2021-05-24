package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMCallbackProxy implements MSIMCallback {

    @Nullable
    private final MSIMCallback mOut;

    public MSIMCallbackProxy(@Nullable MSIMCallback callback) {
        mOut = callback;
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        if (mOut != null) {
            mOut.onError(errorCode, errorMessage);
        }
    }

    @Override
    public void onSuccess() {
        if (mOut != null) {
            mOut.onSuccess();
        }
    }

}
