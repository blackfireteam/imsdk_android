package com.masonsoft.imsdk.core;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class SignGeneratorTest {

    @Test
    public void testGeneralSpeed() {
        // 期望每毫秒生成的 sign 数量
        final long speed = 100;
        final long count = 99999;

        final long timeStart = System.nanoTime();
        for (int i = 0; i < count; i++) {
            SignGenerator.next();
        }
        final long timeEnd = System.nanoTime();
        final long timeConsumed = timeEnd - timeStart;
        final double timeConsumedMs = TimeUnit.NANOSECONDS.toMillis(timeConsumed);

        // 实际测试结果每毫秒生成的 sign 数量
        final long speedTest = (long) (count * 1f / timeConsumedMs);

        Assert.assertTrue("Sign 生成速度(个/毫秒)太慢，期望值：" + speed + ", 实际值：" + speedTest, speedTest >= speed);
    }

}