package com.masonsoft.imsdk;

/**
 * IMSDK 相关常量
 */
public final class IMConstants {

    /**
     * 全局命名空间。如用于区分数据库文件等。
     */
    public static final String GLOBAL_NAMESPACE = "_imsdk_20210308_K27rhzMw_";

    /**
     * 数据库中记录的逻辑 true
     */
    public static final int TRUE = 1;
    /**
     * 数据库中记录的逻辑 false
     */
    public static final int FALSE = 0;

    /**
     * @see #TRUE
     * @see #FALSE
     */
    public static void checkLogicTrueOrFalse(int logicTrueOfFalse) {
        if (logicTrueOfFalse != TRUE && logicTrueOfFalse != FALSE) {
            throw new IllegalArgumentException("invalid logicTrueOfFalse:" + logicTrueOfFalse);
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

    /**
     * 消息的发送状态
     */
    public static class SendStatus {

        /**
         * 待发送
         */
        public static final int IDLE = 0;

        /**
         * 发送中
         */
        public static final int SENDING = 1;

        /**
         * 发送成功
         */
        public static final int SUCCESS = 2;

        /**
         * 发送失败
         */
        public static final int FAIL = 3;

    }

}
