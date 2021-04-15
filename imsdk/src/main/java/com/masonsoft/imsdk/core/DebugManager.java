package com.masonsoft.imsdk.core;

import android.util.Log;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.observable.ClockObservable;
import com.masonsoft.imsdk.util.ReadWriteWeakSet;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;

/**
 * only for debug
 */
public class DebugManager {

    private static final Singleton<DebugManager> INSTANCE = new Singleton<DebugManager>() {
        @Override
        protected DebugManager create() {
            return new DebugManager();
        }
    };

    public static DebugManager getInstance() {
        return INSTANCE.get();
    }

    public interface DebugInfoProvider {
        void fetchDebugInfo(@NonNull StringBuilder builder);
    }

    private static final long DEFAULT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60);

    private final ReadWriteWeakSet<DebugInfoProvider> mDebugInfoProviderSet = new ReadWriteWeakSet<>();
    private final ClockObservable.ClockObserver mClockObserver = DebugManager.this::printDebugInfo;
    private final TaskQueue mActionQueue = new TaskQueue(1);

    private long mIntervalMs = DEFAULT_INTERVAL_MS;
    private long mLastPrintTimeMs;

    private DebugManager() {
    }

    public void addDebugInfoProvider(DebugInfoProvider debugInfoProvider) {
        if (IMLog.getLogLevel() <= Log.VERBOSE) {
            mDebugInfoProviderSet.put(debugInfoProvider);
        }
    }

    public void removeDebugInfoProvider(DebugInfoProvider debugInfoProvider) {
        mDebugInfoProviderSet.remove(debugInfoProvider);
    }

    public void start() {
        if (IMLog.getLogLevel() <= Log.VERBOSE) {
            ClockObservable.DEFAULT.registerObserver(mClockObserver);
        }
    }

    public void stop() {
        ClockObservable.DEFAULT.unregisterObserver(mClockObserver);
    }

    public void setIntervalMs(long intervalMs) {
        mIntervalMs = intervalMs;
    }

    public long getIntervalMs() {
        return mIntervalMs;
    }

    public void printDebugInfoNow() {
        printDebugInfo(true);
    }

    private void printDebugInfo() {
        printDebugInfo(false);
    }

    private void printDebugInfo(final boolean now) {
        mActionQueue.skipQueue();
        mActionQueue.enqueue(() -> {
            final long interval = System.currentTimeMillis() - mLastPrintTimeMs;
            if (!now && interval < mIntervalMs) {
                return;
            }
            mLastPrintTimeMs = System.currentTimeMillis();

            final StringBuilder builder = new StringBuilder();
            builder.append("=========== DEBUG INFO ===========:\n");
            final List<DebugInfoProvider> debugInfoProviderList = mDebugInfoProviderSet.getAll();
            final int size = debugInfoProviderList.size();
            int index = 0;
            for (DebugInfoProvider debugInfoProvider : debugInfoProviderList) {
                index++;
                final String indexInfo = "[" + index + "/" + size + "]";

                builder.append(indexInfo).append("-------------------------:\n");
                if (debugInfoProvider != null) {
                    debugInfoProvider.fetchDebugInfo(builder);
                }
                builder.append(indexInfo).append("------------------------- end\n");
            }
            builder.append("=========== DEBUG INFO =========== end\n");
            IMLog.v(builder.toString());
        });
    }

}
