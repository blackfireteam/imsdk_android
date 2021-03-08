package com.masonsoft.imsdk.sample;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class PlainTest {

    @Test
    public void doTest() {
        final long maxLong = Long.MAX_VALUE;
        final long timeNowMicro = System.currentTimeMillis() * 1000;
        System.out.println(maxLong);
        System.out.println(Long.MIN_VALUE);
        System.out.println(timeNowMicro);
        System.out.println(timeNowMicro + TimeUnit.DAYS.toMicros(365 * 1000));
        System.out.println(Long.toHexString(maxLong));
        System.out.println(Long.toHexString(timeNowMicro));
        System.out.println(Long.toBinaryString(maxLong));
        System.out.println(Long.toBinaryString(timeNowMicro));
    }

}
