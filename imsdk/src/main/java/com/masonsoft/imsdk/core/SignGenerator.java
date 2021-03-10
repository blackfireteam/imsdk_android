package com.masonsoft.imsdk.core;

import android.os.SystemClock;

import com.masonsoft.imsdk.db.Sequence;

/**
 * 生成唯一的本地 Sign
 */
public class SignGenerator {

    // 微秒（千分之一毫秒）
    private static final long TIME_START_MICROSECONDS = System.currentTimeMillis() * 1000;
    private static final long TIME_DIFF_START_MICROSECONDS = SystemClock.elapsedRealtimeNanos() / 1000;

    private SignGenerator() {
    }

    /**
     * 获取下一个唯一的本地 Sign
     */
    public static long next() {
        final long diffMicroseconds = SystemClock.elapsedRealtimeNanos() / 1000 - TIME_DIFF_START_MICROSECONDS;
        final long timeMicroSeconds = TIME_START_MICROSECONDS + diffMicroseconds;
        return Sequence.create(false, timeMicroSeconds);
    }

}
