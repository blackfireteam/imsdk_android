package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakCallback<T> extends AutoRemoveDuplicateRunnable implements MSIMCallback<T> {

    @NonNull
    private final WeakReference<MSIMCallback<T>> mOutRef;

    public MSIMWeakCallback(@Nullable MSIMCallback<T> callback) {
        this(callback, false);
    }

    public MSIMWeakCallback(@Nullable MSIMCallback<T> callback, boolean runOnUiThread) {
        super(runOnUiThread);
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onCallback(@NonNull T payload) {
        final Object tag = getOnCallbackTag(payload);
        dispatch(tag, () -> {
            final MSIMCallback<T> out = mOutRef.get();
            if (out != null) {
                out.onCallback(payload);
            }
        });
    }

    @Nullable
    protected Object getOnCallbackTag(@NonNull T payload) {
        return null;
    }

}
