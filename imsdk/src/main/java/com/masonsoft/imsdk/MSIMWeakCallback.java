package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.github.idonans.core.thread.Threads;

/**
 * @since 1.0
 */
public class MSIMWeakCallback<T> implements MSIMCallback<T> {

    @NonNull
    private final WeakReference<MSIMCallback<T>> mOutRef;
    private final boolean mRunOnUiThread;

    public MSIMWeakCallback(@Nullable MSIMCallback<T> callback) {
        this(callback, false);
    }

    public MSIMWeakCallback(@Nullable MSIMCallback<T> callback, boolean runOnUiThread) {
        mOutRef = new WeakReference<>(callback);
        mRunOnUiThread = runOnUiThread;
    }

    private void runOrPost(Runnable runnable) {
        if (mRunOnUiThread) {
            Threads.postUi(runnable);
        } else {
            runnable.run();
        }
    }

    @Override
    public void onCallback(@NonNull T payload) {
        runOrPost(() -> {
            final MSIMCallback<T> out = mOutRef.get();
            if (out != null) {
                out.onCallback(payload);
            }
        });
    }

}
