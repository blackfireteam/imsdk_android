package com.masonsoft.imsdk;

/**
 * IMSDK 相关常量
 */
public final class IMConstants {

    /**
     * 全局命名空间。如用于区分数据库文件等。
     */
    public static final String GLOBAL_NAMESPACE = "_msimsdk_20210308_";

    public interface DELETE {
        /**
         * 已删除
         */
        int YES = 1;
        /**
         * 未删除
         */
        int NO = 0;
    }

}
