package com.masonsoft.imsdk.db;

/**
 * 生成数据库序列值(在不同的表或者不同的会话中，序列值可能会重复).<br/>
 * 序列号最大 63 位(有符号长整型)<br/>
 * 其中：低 55 位为时间戳位，高 8 位为功能位<br/>
 * <table>
 *   <tr>
 *     <th>第几位</td>
 *     <th>含义</td>
 *   </tr>
 *   <tr>
 *     <td>第1-55位(低55位)</td>
 *     <td>时间戳(可表示至3021年的微秒)</td>
 *   </tr>
 *   <tr>
 *     <td>第56-61位</td>
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
    private static final long TOP_FLAG = 0x01L << 61;
    private static final long TIME_MAX = Long.MAX_VALUE >> 8;

    /**
     * 创建一个序列值
     *
     * @param top              是否置顶
     * @param timeMicroSeconds 时间戳微秒
     */
    public static long create(boolean top, long timeMicroSeconds) {
        long sequence = TIME_MAX & timeMicroSeconds;
        if (top) {
            sequence += TOP_FLAG;
        }
        return sequence;
    }

}
