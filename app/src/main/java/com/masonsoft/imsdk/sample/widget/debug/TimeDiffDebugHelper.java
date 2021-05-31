package com.masonsoft.imsdk.sample.widget.debug;

import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.util.Objects;

public class TimeDiffDebugHelper {

    private final long mStart = System.nanoTime();
    private final String mTagPrefix;
    private long mLast = System.nanoTime();
    private long mDiffWithStart;
    private long mDiffWithLast;

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
    }

    public void print() {
        SampleLog.i("%s [%s] diff[%s/%s]", Objects.defaultObjectTag(this), mTagPrefix, mDiffWithLast, mDiffWithStart);
    }

}
