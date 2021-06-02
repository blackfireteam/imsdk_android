package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.github.idonans.core.thread.TaskQueue;
import io.github.idonans.core.thread.Threads;

abstract class AutoRemoveDuplicateRunnable {

    private final TaskQueue mDispatchQueue = new TaskQueue(1);
    private final boolean mPostToUiThread;

    private final ReentrantLock mLock = new ReentrantLock();
    private List<Args> mList = new ArrayList<>();

    protected AutoRemoveDuplicateRunnable(boolean postToUiThread) {
        mPostToUiThread = postToUiThread;
    }

    protected void dispatch(@NonNull final Runnable runnable) {
        dispatch(null, runnable);
    }

    protected void dispatch(@Nullable final Object tag, @NonNull final Runnable runnable) {
        mLock.lock();
        try {
            final Args input = new Args(tag, runnable);
            for (Args args : mList) {
                if (args.merge(input)) {
                    return;
                }
            }
            mList.add(input);
        } finally {
            mLock.unlock();
        }
        mDispatchQueue.skipQueue();
        mDispatchQueue.enqueue(() -> {
            if (mPostToUiThread) {
                Threads.postUi(AutoRemoveDuplicateRunnable.this::onDispatch);
            } else {
                onDispatch();
            }
        });
    }

    private void onDispatch() {
        final List<Args> list;
        mLock.lock();
        try {
            list = mList;
            mList = new ArrayList<>();
        } finally {
            mLock.unlock();
        }
        for (Args args : list) {
            args.mRunnable.run();
        }
    }

    private static class Args {

        private final Object mKey;
        private Runnable mRunnable;

        public Args(Object key, Runnable runnable) {
            mKey = key;
            mRunnable = runnable;
        }

        private boolean merge(Args input) {
            if (this.mKey == null) {
                return false;
            }
            if (this.mKey.equals(input.mKey)) {
                this.mRunnable = input.mRunnable;
                return true;
            }

            return false;
        }

    }

}
