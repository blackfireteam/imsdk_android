package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMCallbackProxy<T> extends AutoRemoveDuplicateRunnable implements MSIMCallback<T> {

    @Nullable
    private final MSIMCallback<T> mOut;

    public MSIMCallbackProxy(@Nullable MSIMCallback<T> callback) {
        this(callback, false);
    }

    public MSIMCallbackProxy(@Nullable MSIMCallback<T> callback, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = callback;
    }

    @Override
    public void onCallback(@NonNull T payload) {
        dispatch(new Object(), () -> {
            if (mOut != null) {
                mOut.onCallback(payload);
            }
        });
    }

}
