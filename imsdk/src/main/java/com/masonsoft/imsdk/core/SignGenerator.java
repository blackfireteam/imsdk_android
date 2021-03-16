package com.masonsoft.imsdk.core;

import android.os.SystemClock;

import com.masonsoft.imsdk.IMLog;

/**
 * 生成唯一的本地 Sign
 *
 * @since 1.0
 */
public class SignGenerator {

    private static final Object LOCK = new Object();
    // 微秒（千分之一毫秒）
    private static final long TIME_START_MICROSECONDS = System.currentTimeMillis() * 1000;
    private static final long TIME_DIFF_START_MICROSECONDS = SystemClock.elapsedRealtimeNanos() / 1000;
    private static long sLastTimeMicroSeconds;

    private SignGenerator() {
    }

    /**
     * 获取下一个唯一的本地 Sign
     */
    public static long next() {
        synchronized (LOCK) {
            while (true) {
                final long diffMicroseconds = SystemClock.elapsedRealtimeNanos() / 1000 - TIME_DIFF_START_MICROSECONDS;
                final long timeMicroSeconds = TIME_START_MICROSECONDS + diffMicroseconds;
                if (timeMicroSeconds > sLastTimeMicroSeconds) {
                    sLastTimeMicroSeconds = timeMicroSeconds;
                    return timeMicroSeconds;
                }
                if (timeMicroSeconds < sLastTimeMicroSeconds) {
                    IMLog.e(
                            new IllegalAccessError("SignGenerator#next timeMicroSeconds is less than sLastTimeMicroSeconds"),
                            "timeMicroSeconds:%s, sLastTimeMicroSeconds:%s",
                            timeMicroSeconds, sLastTimeMicroSeconds);
                } else {
                    IMLog.v("fetch SignGenerator#next too closely");
                }
            }
        }
    }

}
