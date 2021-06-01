package com.masonsoft.imsdk.util;

import com.masonsoft.imsdk.core.IMLog;

import java.util.concurrent.TimeUnit;

public class TimeDiffDebugHelper {

    private final long mStart = System.nanoTime();
    private final String mTagPrefix;
    private long mLast = System.nanoTime();
    private long mDiffWithStart;
    private long mDiffWithLast;
    private long mMarkCount;
    private long mPrintCount;

    public TimeDiffDebugHelper() {
        this("default");
    }

    public TimeDiffDebugHelper(String tagPrefix) {
        mTagPrefix = tagPrefix;
    }

    public void mark() {
        final long now = System.nanoTime();
        mDiffWithStart = now - mStart;
        mDiffWithLast = now - mLast;
        mLast = now;
        mMarkCount++;
    }

    public void print() {
        this.print("");
    }

    public void print(String extraMessage) {
        mPrintCount++;
        IMLog.v("%s [%s][%s/%s] diff[%s(%s)/%s(%s)] [%s]",
                Objects.defaultObjectTag(this),
                mTagPrefix,
                mPrintCount, mMarkCount,
                mDiffWithLast, TimeUnit.NANOSECONDS.toMillis(mDiffWithLast),
                mDiffWithStart, TimeUnit.NANOSECONDS.toMillis(mDiffWithStart),
                extraMessage);
    }

}
