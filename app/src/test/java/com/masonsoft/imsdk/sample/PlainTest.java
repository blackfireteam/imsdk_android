package com.masonsoft.imsdk.sample;

import com.masonsoft.imsdk.core.db.Sequence;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class PlainTest {

    @Test
    public void doTest() {
        final long maxLong = Long.MAX_VALUE;
        final long timeNowMicro = System.currentTimeMillis() * 1000 + TimeUnit.DAYS.toMicros(365 * 1000);
        System.out.println(maxLong);
        System.out.println(timeNowMicro);
        System.out.println(Long.toHexString(maxLong));
        System.out.println(Long.toHexString(timeNowMicro));
        System.out.println(Long.toBinaryString(maxLong));
        System.out.println(Long.toBinaryString(timeNowMicro));

        for (int i = 0; i < 33; i++) {
            final long sequence = Sequence.create(timeNowMicro);
            System.out.println(Long.toBinaryString(sequence));
        }
    }

}
