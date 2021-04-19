package com.masonsoft.imsdk.core;

import android.os.Handler;
import android.os.HandlerThread;

import com.masonsoft.imsdk.core.observable.ClockObservable;

import io.github.idonans.core.SimpleAbortSignal;
import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;

/**
 * 以 weak 引用持有需要按照时间周期性处理的对象，当没有任何对象需要处理时，会挂起计时器。
 */
public class WeakClockManager {

    private static final Singleton<WeakClockManager> INSTANCE = new Singleton<WeakClockManager>() {
        @Override
        protected WeakClockManager create() {
            return new WeakClockManager();
        }
    };

    public static WeakClockManager getInstance() {
        return INSTANCE.get();
    }

    // 用来检查当前计时器状态的 queue (关闭或者开启计时器)
    private final TaskQueue mClockStateCheckQueue = new TaskQueue(1);

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // 计时器
    private final HandlerThread mClockQueueThread = new HandlerThread("WeakClockManager");

    {
        mClockQueueThread.start();
    }

    private final Handler mClockQueue = new Handler(mClockQueueThread.getLooper());
    private ClockTask mClockTask;

    // 开启或者关闭 clock 的锁
    private final Object START_OR_STOP_CLOCK_LOCK = new Object();

    // clock 任务之间执行的最大间隔. 默认 2 秒.
    private static final long CLOCK_INTERVAL_DEFAULT_MS = 2000L;
    private long mClockIntervalMs = CLOCK_INTERVAL_DEFAULT_MS;
    // 上一次开始执行 clock 任务的时间点
    private long mLastClockTimeMs;
    private final Object CLOCK_TASK_LOCK = new Object();
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////

    private WeakClockManager() {
    }

    public void setClockIntervalMs(long clockIntervalMs) {
        mClockIntervalMs = clockIntervalMs;
        synchronized (START_OR_STOP_CLOCK_LOCK) {
            stopClock();
            startClock();
        }
    }

    /**
     * 开启计时器，可能重复调用
     */
    private void startClock() {
        synchronized (mClockQueue) {
            if (mClockTask == null || mClockTask.isAbort()) {
                mClockTask = new ClockTask();
                mClockQueue.post(mClockTask);
            }
        }
    }

    /**
     * 关闭计时器，可能重复调用
     */
    private void stopClock() {
        synchronized (mClockQueue) {
            if (mClockTask != null) {
                mClockTask.setAbort();
                mClockTask = null;
            }
        }
    }

    private class ClockTask extends SimpleAbortSignal implements Runnable {

        @Override
        public boolean isAbort() {
            return super.isAbort() || mClockTask != this;
        }

        @Override
        public void run() {
            try {
                if (isAbort()) {
                    return;
                }
                synchronized (CLOCK_TASK_LOCK) {
                    final long timeStartMs = System.currentTimeMillis();
                    final long diffMs = timeStartMs - mLastClockTimeMs;

                    // 是否执行本轮的 tick 调用
                    boolean tick = false;
                    if (diffMs < 0) {
                        // unexpected logic, workaround with just tick.
                        IMLog.e("unexpected. ClockTask run invalid diffMs:%s", diffMs);
                        tick = true;
                    } else {
                        tick = diffMs >= mClockIntervalMs;
                    }

                    if (tick) {
                        // 执行本轮 tick, 记录下本轮 tick 的开始时间
                        mLastClockTimeMs = timeStartMs;

                        IMLog.v("WeakClockManager clock task tick [%s] ...", timeStartMs);
                        ClockObservable.DEFAULT.notifyOnClock();
                        IMLog.v("WeakClockManager clock task tick [%s] ... ok", timeStartMs);
                    }

                    long diffDelayMs = mClockIntervalMs - (System.currentTimeMillis() - mLastClockTimeMs);
                    if (diffDelayMs < 0) {
                        diffDelayMs = 0;
                    }

                    IMLog.v("WeakClockManager clock task tick [%s] ... ok post with delay:%s", timeStartMs, diffDelayMs);
                    mClockQueue.postDelayed(this, diffDelayMs);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.fixme(e);
            }
        }
    }

    public void invalidateClockState() {
        if (mClockStateCheckQueue.getCurrentCount() <= 2) {
            mClockStateCheckQueue.enqueue(new ClockStateChecker());
        }
    }

    private class ClockStateChecker implements Runnable {
        @Override
        public void run() {
            try {
                synchronized (START_OR_STOP_CLOCK_LOCK) {
                    if (ClockObservable.DEFAULT.isEmpty()) {
                        stopClock();
                    } else {
                        startClock();
                    }
                }
            } catch (Throwable e) {
                IMLog.e(e);
            }
        }
    }

}
