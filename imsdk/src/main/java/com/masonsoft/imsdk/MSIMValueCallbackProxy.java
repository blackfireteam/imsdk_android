package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMValueCallbackProxy<T> implements MSIMValueCallback<T> {

    @Nullable
    private final MSIMValueCallback<T> mOut;

    public MSIMValueCallbackProxy(@Nullable MSIMValueCallback<T> callback) {
        mOut = callback;
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        if (mOut != null) {
            mOut.onError(errorCode, errorMessage);
        }
    }

    @Override
    public void onSuccess(T value) {
        if (mOut != null) {
            mOut.onSuccess(value);
        }
    }

}
