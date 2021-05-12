package com.masonsoft.imsdk.core;

/**
 * IMSDK 相关常量
 *
 * @since 1.0
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

    public static int trueOfFalse(boolean input) {
        return input ? TRUE : FALSE;
    }

    /**
     * 指代任意 id
     */
    public static final int ID_ANY = -1;

    /**
     * 当两个 id 相等或者其中任意一个 id 为 ID_ANY 时，则匹配成功。
     */
    public static boolean isIdMatch(long id1, long id2) {
        return id1 == id2 || id1 == ID_ANY || id2 == ID_ANY;
    }

    /**
     * @see #TRUE
     * @see #FALSE
     */
    public static void checkLogicTrueOrFalse(int logicTrueOfFalse) {
        if (logicTrueOfFalse != TRUE && logicTrueOfFalse != FALSE) {
            throw new IllegalArgumentException("invalid logicTrueOfFalse:" + logicTrueOfFalse);
        }
    }

    public static class AbortId {
        /**
         * 重置
         */
        public static final long RESET = 0;
    }

    /**
     * 发送消息的配置选项
     */
    public static class SendMessageOption {
        /**
         * 发送文字消息的配置选项
         */
        public static class Text {
            /**
             * 发送文字时，是否 trim 前后的空白字符
             */
            public static final boolean TRIM_REQUIRED = true;
            /**
             * 是否允许发送空内容
             */
            public static final boolean ALLOW_EMPTY = false;
            /**
             * 最大允许发送的字符数量
             */
            public static final int MAX_LENGTH = 8000;
        }

        /**
         * 发送图片消息的配置选项
         */
        public static class Image {
            /**
             * 图片文件大小的限制
             */
            public static final int MAX_FILE_SIZE = 28 * 1024 * 1024;
        }

        /**
         * 发送语音消息的配置选项
         */
        public static class Audio {
            /**
             * 语音文件大小的限制
             */
            public static final int MAX_FILE_SIZE = 28 * 1024 * 1024;
        }

        /**
         * 发送视频消息的配置选项
         */
        public static class Video {
            /**
             * 视频文件大小的限制
             */
            public static final int MAX_FILE_SIZE = 100 * 1024 * 1024;
            /**
             * 视频封面图文件大小的限制
             */
            public static final int MAX_THUMB_FILE_SIZE = 28 * 1024 * 1024;
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

        public static String toHumanString(int conversationType) {
            if (conversationType == C2C) {
                return "C2C";
            }
            return String.valueOf(conversationType);
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

        public static void check(int sendStatus) {
            if (sendStatus != IDLE
                    && sendStatus != SENDING
                    && sendStatus != SUCCESS
                    && sendStatus != FAIL) {
                throw new IllegalArgumentException("invalid sendStatus:" + sendStatus);
            }
        }

        public static String toHumanString(int sendStatus) {
            switch (sendStatus) {
                case IDLE:
                    return "IDLE";
                case SENDING:
                    return "SENDING";
                case SUCCESS:
                    return "SUCCESS";
                case FAIL:
                    return "FAIL";
                default:
                    return String.valueOf(sendStatus);
            }
        }
    }

    /**
     * 消息的类型
     */
    public static class MessageType {
        /**
         * 文本
         */
        public static final int TEXT = 0;
        /**
         * 图片
         */
        public static final int IMAGE = 1;
        /**
         * 音频
         */
        public static final int AUDIO = 2;
        /**
         * 视频
         */
        public static final int VIDEO = 3;
        /**
         * 地理位置
         */
        public static final int LOCATION = 4;
        /**
         * 用户名片
         */
        public static final int USER_PROFILE = 5;
        /**
         * 自定义表情
         */
        public static final int CUSTOM_EMOJI = 6;

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        /**
         * 语音聊天结束
         */
        public static final int VOICE_END = 16;
        /**
         * 视频聊天结束
         */
        public static final int VIDEO_END = 17;

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        /**
         * 位置共享
         */
        public static final int REAL_TIME_LOCATION = 30;

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        /**
         * 已撤回的消息
         */
        public static final int REVOKED = 31;

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        /**
         * 媚眼
         */
        public static final int WINK = 32;
        /**
         * 匹配成功
         */
        public static final int MATCH = 33;
        /**
         * 请求上传相册
         */
        public static final int REQUEST_UPLOAD_PHOTO = 34;
        /**
         * 相册上传后回执
         */
        public static final int REQUEST_UPLOAD_PHOTO_RESULT = 35;
        /**
         * 请求私有相册访问权限
         */
        public static final int REQUEST_PRIVATE_PHOTO = 36;
        /**
         * 允许访问私有相册的回执
         */
        public static final int REQUEST_PRIVATE_PHOTO_RESULT = 37;
        /**
         * 请求用户认证资料
         */
        public static final int REQUEST_VERIFY_PROFILE = 38;
        /**
         * 分享相册
         */
        public static final int SHARE_PHOTO = 39;
        /**
         * 周报
         */
        public static final int WEEKLY = 40;

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////
        // 64 - 99 消息操作信息（此类型消息 不可见）

        /**
         * 消息撤回（该类型消息不可见）
         */
        public static final int REVOKE_MESSAGE = 64;

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////
        // 100-199 应用方自定义消息类型（可见）
        // 200-255 应用方自定义消息类型（不可见）

        /**
         * 判断指定类型的消息是否是指令消息，指令消息 UI 不可见
         */
        public static boolean isActionMessage(int type) {
            return (type >= 64 && type <= 99) || (type >= 200 && type <= 255);
        }
    }

}
