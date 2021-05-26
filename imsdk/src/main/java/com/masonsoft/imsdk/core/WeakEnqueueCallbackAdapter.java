package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.idonans.core.WeakAbortSignal;
import io.github.idonans.core.thread.Threads;

@Deprecated
public class WeakEnqueueCallbackAdapter<T extends EnqueueMessage> extends WeakAbortSignal implements EnqueueCallback<T> {

    private final boolean mRunOnUiThread;

    public WeakEnqueueCallbackAdapter(@Nullable EnqueueCallback<T> callback, boolean runOnUiThread) {
        super(callback);
        mRunOnUiThread = runOnUiThread;
    }

    @Nullable
    private EnqueueCallback<T> getEnqueueCallback() {
        //noinspection unchecked
        return (EnqueueCallback<T>) getObject();
    }

    @Override
    public void onEnqueueSuccess(@NonNull T enqueueMessage) {
        final Runnable runnable = () -> {
            final EnqueueCallback<T> callback = getEnqueueCallback();
            if (callback != null) {
                callback.onEnqueueSuccess(enqueueMessage);
            }
        };
        if (mRunOnUiThread) {
            Threads.postUi(runnable);
        } else {
            runnable.run();
        }
    }

    @Override
    public void onEnqueueFail(@NonNull T enqueueMessage, int errorCode, String errorMessage) {
        final Runnable runnable = () -> {
            final EnqueueCallback<T> callback = getEnqueueCallback();
            if (callback != null) {
                callback.onEnqueueFail(enqueueMessage, errorCode, errorMessage);
            }
        };
        if (mRunOnUiThread) {
            Threads.postUi(runnable);
        } else {
            runnable.run();
        }
    }

}
