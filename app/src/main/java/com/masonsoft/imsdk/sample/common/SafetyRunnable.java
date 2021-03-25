package com.masonsoft.imsdk.sample.common;

import com.masonsoft.imsdk.sample.SampleLog;

public class SafetyRunnable implements Runnable {

    private final Runnable mTarget;

    public SafetyRunnable(Runnable target) {
        mTarget = target;
    }

    @Override
    public void run() {
        try {
            onSafetyRun();
        } catch (Throwable e) {
            SampleLog.e(e);
        }
    }

    protected void onSafetyRun() {
        if (mTarget != null) {
            mTarget.run();
        }
    }

}
