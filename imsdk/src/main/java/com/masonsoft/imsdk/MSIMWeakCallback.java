package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakCallback<T> implements MSIMCallback<T> {

    @NonNull
    private final WeakReference<MSIMCallback<T>> mOutRef;

    public MSIMWeakCallback(@Nullable MSIMCallback<T> callback) {
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onCallback(@NonNull T payload) {
        final MSIMCallback<T> out = mOutRef.get();
        if (out != null) {
            out.onCallback(payload);
        }
    }

}
