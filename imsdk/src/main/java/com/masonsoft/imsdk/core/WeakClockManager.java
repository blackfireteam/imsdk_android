package com.masonsoft.imsdk.core;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.idonans.core.SimpleAbortSignal;
import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.util.WeakObservable;

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

    private final ClockObservable mClockObservable = new ClockObservable();
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

    // clock 任务之间执行的最大间隔. 默认 5 秒.
    private static final long CLOCK_INTERVAL_DEFAULT = 5000L;
    private long mClockInterval = CLOCK_INTERVAL_DEFAULT;
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////

    private WeakClockManager() {
    }

    @NonNull
    public ClockObservable getClockObservable() {
        return mClockObservable;
    }

    public interface ClockObserver {
        @WorkerThread
        void onClock();
    }

    public class ClockObservable extends WeakObservable<ClockObserver> {

        public void notifyOnClock() {
            final boolean[] isEmpty = new boolean[]{true};
            forEach(clockObserver -> {
                isEmpty[0] = false;
                clockObserver.onClock();
            });
            if (isEmpty[0]) {
                IMLog.v("clock observer may empty, invalidate clock state.");
                invalidateClockState();
            }
        }

        @Override
        public void registerObserver(ClockObserver observer) {
            super.registerObserver(observer);

            invalidateClockState();
        }

        private boolean isEmpty() {
            return size() == 0;
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
                final long timeMs = System.currentTimeMillis();
                IMLog.v("WeakClockManager clock task tick [%s] ...", timeMs);
                mClockObservable.notifyOnClock();
                IMLog.v("WeakClockManager clock task tick [%s] ... ok", timeMs);

                final long timeDiff = System.currentTimeMillis() - timeMs;
                long timeDelay = mClockInterval - timeDiff;
                if (timeDelay <= 0) {
                    timeDelay = CLOCK_INTERVAL_DEFAULT;
                }
                IMLog.v("WeakClockManager clock task tick [%s] ... ok post with delay:%s", timeMs, timeDelay);
                mClockQueue.postDelayed(this, timeDelay);
            } catch (Throwable e) {
                IMLog.e(e);
            }
        }
    }

    private void invalidateClockState() {
        if (mClockStateCheckQueue.getCurrentCount() <= 2) {
            mClockStateCheckQueue.enqueue(new ClockStateChecker());
        }
    }

    private class ClockStateChecker implements Runnable {
        @Override
        public void run() {
            try {
                synchronized (mClockStateCheckQueue) {
                    if (mClockObservable.isEmpty()) {
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
