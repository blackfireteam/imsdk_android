package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.WeakClockManager;
import com.masonsoft.imsdk.util.WeakObservable;

/**
 * @see WeakClockManager
 */
public class ClockObservable extends WeakObservable<ClockObservable.ClockObserver> {

    public static final ClockObservable DEFAULT = new ClockObservable();

    public interface ClockObserver {
        void onClock();
    }

    public void notifyOnClock() {
        try {
            final boolean[] isEmpty = new boolean[]{true};
            forEach(clockObserver -> {
                isEmpty[0] = false;
                clockObserver.onClock();
            });
            if (isEmpty[0]) {
                IMLog.v("clock observer may empty, invalidate clock state.");
                WeakClockManager.getInstance().invalidateClockState();
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
    }

    @Override
    public void registerObserver(ClockObserver observer) {
        super.registerObserver(observer);

        WeakClockManager.getInstance().invalidateClockState();
    }

}
