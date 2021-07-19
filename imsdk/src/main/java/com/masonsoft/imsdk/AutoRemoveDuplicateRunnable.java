package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.idonans.core.thread.BatchQueue;

abstract class AutoRemoveDuplicateRunnable {

    private final BatchQueue<Args> mBatchQueue;

    protected AutoRemoveDuplicateRunnable(boolean postToUiThread) {
        mBatchQueue = new BatchQueue<>(postToUiThread);
        mBatchQueue.setMergeFunction((list, args) -> {
            if (args == null) {
                return list;
            }
            for (Args item : list) {
                if (item.merge(args)) {
                    return list;
                }
            }

            list.add(args);
            return list;
        });
        mBatchQueue.setConsumer(args -> {
            for (Args arg : args) {
                arg.mRunnable.run();
            }
        });
    }

    protected void dispatch(@NonNull final Runnable runnable) {
        dispatch(null, runnable);
    }

    protected void dispatch(@Nullable final Object tag, @NonNull final Runnable runnable) {
        mBatchQueue.add(new Args(tag, runnable));
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
