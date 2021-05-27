package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakSessionListener extends RunOnUiThread implements MSIMSessionListener {

    @NonNull
    private final WeakReference<MSIMSessionListener> mOutRef;

    public MSIMWeakSessionListener(@Nullable MSIMSessionListener callback) {
        this(callback, false);
    }

    public MSIMWeakSessionListener(@Nullable MSIMSessionListener callback, boolean runOnUiThread) {
        super(runOnUiThread);
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onSessionChanged() {
        runOrPost(() -> {
            final MSIMSessionListener out = mOutRef.get();
            if (out != null) {
                out.onSessionChanged();
            }
        });
    }

    @Override
    public void onSessionUserIdChanged() {
        runOrPost(() -> {
            final MSIMSessionListener out = mOutRef.get();
            if (out != null) {
                out.onSessionUserIdChanged();
            }
        });
    }

}
