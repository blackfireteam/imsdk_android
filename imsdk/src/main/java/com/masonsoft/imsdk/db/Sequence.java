package com.masonsoft.imsdk.db;

/**
 * 生成数据库序列值(在不同的表或者不同的会话中，序列值可能会重复).<br/>
 * 序列号最大 63 位(有符号长整型)<br/>
 * <table>
 *   <tr>
 *     <th>第几位(由低到高)</td>
 *     <th>含义</td>
 *   </tr>
 *   <tr>
 *     <td>第1-4位(低4位)</td>
 *     <td>循环序列号(0-0xF)</td>
 *   </tr>
 *   <tr>
 *     <td>第5位</td>
 *     <td>预留功能位</td>
 *   </tr>
 *   <tr>
 *     <td>第6-60位(共55位)</td>
 *     <td>时间戳(可表示至3021年的微秒)</td>
 *   </tr>
 *   <tr>
 *     <td>第61位</td>
 *     <td>预留功能位</td>
 *   </tr>
 *   <tr>
 *     <td>第62位</td>
 *     <td>置顶标志位</td>
 *   </tr>
 *   <tr>
 *     <td>第63位</td>
 *     <td>预留功能位</td>
 *   </tr>
 * </table>
 */
public class Sequence {

    /**
     * 置顶标记
     */
    private static final long TOP_FLAG = 0x1L << 61;
    @SuppressWarnings("NumericOverflow")
    private static final long TIME_MASK = (Long.MAX_VALUE >> 3) & (Long.MAX_VALUE << 5);
    private static final long INDEX_MASK = 0xFL;

    private static int sIndex;
    private static final Object INDEX_LOCK = new Object();

    /**
     * 创建一个序列值
     *
     * @param top              是否置顶
     * @param timeMicroSeconds 时间戳微秒(可表示至3021年的微秒)
     */
    public static long create(boolean top, long timeMicroSeconds) {
        long sequence = TIME_MASK & (timeMicroSeconds << 5);
        if (top) {
            sequence += TOP_FLAG;
        }
        sequence += nextIndex() & INDEX_MASK;
        return sequence;
    }

    private static int nextIndex() {
        synchronized (INDEX_LOCK) {
            sIndex = (sIndex + 1) % 0x10;
            return sIndex;
        }
    }

}
