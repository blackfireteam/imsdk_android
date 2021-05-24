package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakCallback implements MSIMCallback {

    @NonNull
    private final WeakReference<MSIMCallback> mOutRef;

    public MSIMWeakCallback(@Nullable MSIMCallback callback) {
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        final MSIMCallback out = mOutRef.get();
        if (out != null) {
            out.onError(errorCode, errorMessage);
        }
    }

    @Override
    public void onSuccess() {
        final MSIMCallback out = mOutRef.get();
        if (out != null) {
            out.onSuccess();
        }
    }

}
