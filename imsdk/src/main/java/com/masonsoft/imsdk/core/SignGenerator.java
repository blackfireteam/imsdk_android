package com.masonsoft.imsdk.core;

import android.os.SystemClock;

import com.masonsoft.imsdk.IMLog;

/**
 * 生成唯一的本地时间戳
 */
public class SignGenerator {

    private static final Object LOCK = new Object();
    // 微秒（千分之一毫秒）
    private static final long TIME_START_MICROSECONDS = System.currentTimeMillis() * 1000;
    private static final long TIME_DIFF_START_MICROSECONDS = SystemClock.elapsedRealtimeNanos() / 1000;
    private static long sLastSign;

    private SignGenerator() {
    }

    /**
     * 获取下一个唯一的时间戳，精度：千分之一秒
     */
    public static long next() {
        synchronized (LOCK) {
            while (true) {
                final long diffMicroseconds = SystemClock.elapsedRealtimeNanos() / 1000 - TIME_DIFF_START_MICROSECONDS;
                final long sign = TIME_START_MICROSECONDS + diffMicroseconds;
                if (sign > sLastSign) {
                    sLastSign = sign;
                    return sign;
                }
                if (sign < sLastSign) {
                    IMLog.e(new IllegalAccessError("SignGenerator#next new sign is less than last sign"), "sign:%s, lastSign:%s", sign, sLastSign);
                }
                IMLog.v("fetch SignGenerator#next too closely");
            }
        }
    }

}
