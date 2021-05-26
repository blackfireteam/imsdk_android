package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMCallbackProxy<T> implements MSIMCallback<T> {

    @Nullable
    private final MSIMCallback<T> mOut;

    public MSIMCallbackProxy(@Nullable MSIMCallback<T> callback) {
        mOut = callback;
    }

    @Override
    public void onCallback(@NonNull T payload) {
        if (mOut != null) {
            mOut.onCallback(payload);
        }
    }

}
