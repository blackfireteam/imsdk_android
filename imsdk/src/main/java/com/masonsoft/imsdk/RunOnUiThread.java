package com.masonsoft.imsdk;

import io.github.idonans.core.thread.Threads;

abstract class RunOnUiThread {

    private final boolean mRunOnUiThread;

    protected RunOnUiThread(boolean runOnUiThread) {
        mRunOnUiThread = runOnUiThread;
    }

    protected void runOrPost(Runnable runnable) {
        if (mRunOnUiThread) {
            Threads.postUi(runnable);
        } else {
            runnable.run();
        }
    }

}
