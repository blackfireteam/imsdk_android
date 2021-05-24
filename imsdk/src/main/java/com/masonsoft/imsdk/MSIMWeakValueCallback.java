package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakValueCallback<T> implements MSIMValueCallback<T> {

    @NonNull
    private final WeakReference<MSIMValueCallback<T>> mOutRef;

    public MSIMWeakValueCallback(@Nullable MSIMValueCallback<T> callback) {
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        final MSIMValueCallback<T> out = mOutRef.get();
        if (out != null) {
            out.onError(errorCode, errorMessage);
        }
    }

    @Override
    public void onSuccess(T value) {
        final MSIMValueCallback<T> out = mOutRef.get();
        if (out != null) {
            out.onSuccess(value);
        }
    }

}
