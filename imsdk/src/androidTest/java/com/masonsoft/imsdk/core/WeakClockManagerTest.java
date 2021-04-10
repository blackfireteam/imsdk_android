package com.masonsoft.imsdk.core;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.masonsoft.imsdk.core.observable.ClockObservable;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.idonans.core.thread.Threads;

@RunWith(AndroidJUnit4.class)
public class WeakClockManagerTest {

    private static void onClock1() {
        System.out.println("hello, clock observer 1 run " + System.currentTimeMillis());
        Threads.sleepQuietly(System.currentTimeMillis() % 1000L);
    }

    private static void onClock2() {
        System.out.println("hello, clock observer 2 run " + System.currentTimeMillis());
        Threads.sleepQuietly((long) (Math.random() * 2000L));
    }

    @Test
    public void testClock() {
        IMLog.setLogLevel(Log.VERBOSE);

        doTest();

        Threads.sleepQuietly(10 * 1000);
        System.out.println("set clock interval ms to 500");
        WeakClockManager.getInstance().setClockIntervalMs(500L);

        Threads.sleepQuietly(10 * 1000);
        System.out.println("clear weak clock manager all clock observable");
        ClockObservable.DEFAULT.unregisterAll();

        Threads.sleepQuietly(30 * 1000);
        System.out.println("test clock end");
    }

    private void doTest() {
        ClockObservable.ClockObserver clockObserver1 = WeakClockManagerTest::onClock1;
        ClockObservable.ClockObserver clockObserver2 = WeakClockManagerTest::onClock2;
        ClockObservable.DEFAULT.registerObserver(clockObserver1);
        ClockObservable.DEFAULT.registerObserver(clockObserver2);
        ClockObservable.DEFAULT.registerObserver(new ClockObservable.ClockObserver() {
            @Override
            public void onClock() {
                System.out.println("Anonymous clock observer 1 run " + System.currentTimeMillis());
            }
        });
        ClockObservable.DEFAULT.registerObserver(new ClockObservable.ClockObserver() {
            @Override
            public void onClock() {
                System.out.println("Anonymous clock observer 2 run " + System.currentTimeMillis());
            }
        });
    }

}