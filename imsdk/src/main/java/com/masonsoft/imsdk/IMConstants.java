package com.masonsoft.imsdk;

/**
 * IMSDK 相关常量
 */
public final class IMConstants {

    /**
     * 全局命名空间。如用于区分数据库文件等。
     */
    public static final String GLOBAL_NAMESPACE = "_msimsdk_20210308_";

    public static class DELETE {
        /**
         * 已删除
         */
        public static final int YES = 1;
        /**
         * 未删除
         */
        public static final int NO = 0;

        public static void check(int delete) {
            if (delete != YES
                    && delete != NO) {
                throw new IllegalArgumentException("invalid delete:" + delete);
            }
        }
    }

    /**
     * 会话类型
     */
    public static class ConversationType {
        /**
         * 单聊
         */
        public static final int C2C = 0;

        public static void check(int conversationType) {
            if (conversationType != C2C) {
                throw new IllegalArgumentException("invalid conversationType:" + conversationType);
            }
        }
    }

}
