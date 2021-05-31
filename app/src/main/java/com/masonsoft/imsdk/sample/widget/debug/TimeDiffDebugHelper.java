package com.masonsoft.imsdk.sample.widget.debug;

import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.util.Objects;

public class TimeDiffDebugHelper {

    private final long mStart = System.nanoTime();
    private long mLast = System.nanoTime();
    private long mDiffWithStart;
    private long mDiffWithLast;

    public void mark() {
        final long now = System.nanoTime();
        mDiffWithStart = now - mStart;
        mDiffWithLast = now - mLast;
        mLast = now;
    }

    public void print() {
        SampleLog.i("%s diff[%s/%s]", Objects.defaultObjectTag(this), mDiffWithLast, mDiffWithStart);
    }

}
