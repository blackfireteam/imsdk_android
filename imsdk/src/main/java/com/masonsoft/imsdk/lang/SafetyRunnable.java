package com.masonsoft.imsdk.lang;

import com.masonsoft.imsdk.core.IMLog;

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
            IMLog.e(e);
        }
    }

    protected void onSafetyRun() {
        if (mTarget != null) {
            mTarget.run();
        }
    }

}
