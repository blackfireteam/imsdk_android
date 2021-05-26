package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.idonans.core.thread.Threads;

/**
 * @since 1.0
 */
public class MSIMCallbackProxy<T> implements MSIMCallback<T> {

    @Nullable
    private final MSIMCallback<T> mOut;
    private final boolean mRunOnUiThread;

    public MSIMCallbackProxy(@Nullable MSIMCallback<T> callback) {
        this(callback, false);
    }

    public MSIMCallbackProxy(@Nullable MSIMCallback<T> callback, boolean runOnUiThread) {
        mOut = callback;
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
            if (mOut != null) {
                mOut.onCallback(payload);
            }
        });
    }

}
