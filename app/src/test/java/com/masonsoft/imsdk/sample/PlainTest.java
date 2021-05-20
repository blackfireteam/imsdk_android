package com.masonsoft.imsdk.sample;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PlainTest {

    @Test
    public void doTest() throws ParseException {
        System.out.println("64位整数最大值：");
        println(Long.MAX_VALUE);
        System.out.println("==> 如果以毫秒值转换为时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date(Long.MAX_VALUE)));
        System.out.println("==> 如果以纳秒值转换为时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date(TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE))));

        System.out.println("以秒为单位，表示当前时间：");
        final long time1s = System.currentTimeMillis() / 1000;
        println(time1s);

        System.out.println("以秒为单位，表示 2300-01-01 时间：");
        final long time1sAt2300 = new SimpleDateFormat("yyyy-MM-dd").parse("2300-01-01").getTime() / 1000;
        println(time1sAt2300);

        System.out.println("以秒为单位，表示 3000-01-01 时间：");
        final long time1sAt3000 = new SimpleDateFormat("yyyy-MM-dd").parse("3000-01-01").getTime() / 1000;
        println(time1sAt3000);

        System.out.println("1970-01-02 时间：");
        println(new SimpleDateFormat("yyyy-MM-dd").parse("1970-01-02").getTime());
        System.out.println("1980-01-02 时间：");
        println(new SimpleDateFormat("yyyy-MM-dd").parse("1980-01-02").getTime());
        System.out.println("1990-01-02 时间：");
        println(new SimpleDateFormat("yyyy-MM-dd").parse("1990-01-02").getTime());
        System.out.println("2000-01-02 时间：");
        println(new SimpleDateFormat("yyyy-MM-dd").parse("2000-01-02").getTime());
        System.out.println("2050-01-02 时间：");
        println(new SimpleDateFormat("yyyy-MM-dd").parse("2050-01-02").getTime());
        System.out.println("2100-01-02 时间：");
        println(new SimpleDateFormat("yyyy-MM-dd").parse("2100-01-02").getTime());
        System.out.println("2500-01-02 时间：");
        println(new SimpleDateFormat("yyyy-MM-dd").parse("2500-01-02").getTime());
        System.out.println("3000-01-02 时间：");
        println(new SimpleDateFormat("yyyy-MM-dd").parse("3000-01-02").getTime());

        final long diffTimeMs = new SimpleDateFormat("yyyy-MM-dd").parse("2021-01-01").getTime();

        System.out.println("当前时间：");
        final long timeNowMs = System.currentTimeMillis();
        System.out.println("==>：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date(timeNowMs)));
        println(timeNowMs);
        System.out.println("当前时间(与 2021-01-01 作差之后):");
        System.out.println("==>：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date(timeNowMs - diffTimeMs)));
        println(timeNowMs - diffTimeMs);

        System.out.println("1000 年之后的时间：");
        final long timeMs1000YearsLater = timeNowMs + TimeUnit.DAYS.toMillis(365 * 1000);
        System.out.println("==>：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date(timeMs1000YearsLater)));
        println(timeMs1000YearsLater);
        System.out.println("1000 年之后的时间(与 2021-01-01 作差之后):");
        System.out.println("==>：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date(timeMs1000YearsLater - diffTimeMs)));
        println(timeMs1000YearsLater - diffTimeMs);

        System.out.println("1w 的二进制：");
        println(1000 * 10);

        System.out.println("10w 的二进制：");
        println(1000 * 10 * 10);

        System.out.println("100w 的二进制：");
        println(1000 * 10 * 100);

        System.out.println("1000w 的二进制：");
        println(1000 * 10 * 1000);

        System.out.println("毫秒的二进制：");
        println(System.currentTimeMillis() * 10);

        System.out.println("万分之一秒的二进制：");
        println(System.currentTimeMillis() * 10);

        System.out.println("十万分之一秒的二进制：");
        println(System.currentTimeMillis() * 100);

        System.out.println("百万分之一秒的二进制：");
        println(System.currentTimeMillis() * 1000);

        test2();
        test3();
    }

    private void test2() throws ParseException {
        System.out.println("test2 =========================");
        final long timeStart = new SimpleDateFormat("yyyy-MM-dd").parse("2021-05-01").getTime() * 1000;
        System.out.println("timeStart 百万分之一秒的二进制：");
        println(timeStart);
        final long timeMax = new SimpleDateFormat("yyyy-MM-dd").parse("2121-05-01").getTime() * 1000;
        System.out.println("timeMax 百万分之一秒的二进制：");
        println(timeMax);
        final long timeDiff = timeMax - timeStart;
        System.out.println("timeDiff 百万分之一秒的二进制：");
        println(timeDiff);
    }

    private void test3() throws ParseException {
        System.out.println("test3 =========================");
        final long timeStart = new SimpleDateFormat("yyyy-MM-dd").parse("2021-05-01").getTime();
        System.out.println("timeStart 毫秒的二进制：");
        println(timeStart);
        final long timeMax = new SimpleDateFormat("yyyy-MM-dd").parse("2121-05-01").getTime();
        System.out.println("timeMax 毫秒的二进制：");
        println(timeMax);
        final long timeDiff = timeMax - timeStart;
        System.out.println("timeDiff 毫秒的二进制：");
        println(timeDiff);
    }

    private void println(long input) {
        println(Long.toBinaryString(input));
    }

    private void println(String input) {
        final int length = input.length();
        int prefix = 64 - length;
        final StringBuilder builder = new StringBuilder();
        while (prefix > 0) {
            builder.append("0");
            prefix--;
        }
        builder.append(input);
        builder.append(" [").append(length).append("]");
        System.out.println(builder);
    }

}
