package com.masonsoft.imsdk;

import com.masonsoft.imsdk.core.IMConstants;

/**
 * @since 1.0
 */
public class MSIMConstants {

    /**
     * 指代任意 id
     */
    public static final int ID_ANY = IMConstants.ID_ANY;

    /**
     * 消息的发送状态
     */
    public static class SendStatus {

        /**
         * 待发送
         */
        public static final int IDLE = IMConstants.SendStatus.IDLE;

        /**
         * 发送中
         */
        public static final int SENDING = IMConstants.SendStatus.SENDING;

        /**
         * 发送成功
         */
        public static final int SUCCESS = IMConstants.SendStatus.SUCCESS;

        /**
         * 发送失败
         */
        public static final int FAIL = IMConstants.SendStatus.FAIL;

    }

    /**
     * 消息的类型
     */
    public static class MessageType {
        /**
         * 文本
         */
        public static final int TEXT = IMConstants.MessageType.TEXT;
        /**
         * 图片
         */
        public static final int IMAGE = IMConstants.MessageType.IMAGE;
        /**
         * 音频
         */
        public static final int AUDIO = IMConstants.MessageType.AUDIO;
        /**
         * 视频
         */
        public static final int VIDEO = IMConstants.MessageType.VIDEO;

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        /**
         * 已撤回的消息
         */
        public static final int REVOKED = IMConstants.MessageType.REVOKED;

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        public static final int FIRST_CUSTOM_MESSAGE = IMConstants.MessageType.FIRST_CUSTOM_MESSAGE;
        public static final int FIRST_CUSTOM_ACTION_MESSAGE = IMConstants.MessageType.FIRST_CUSTOM_ACTION_MESSAGE;

        /**
         * 判断指定类型的消息是否是指令消息，指令消息 UI 不可见
         */
        public static boolean isActionMessage(int type) {
            return IMConstants.MessageType.isActionMessage(type);
        }

        public static boolean isCustomMessage(int type) {
            return IMConstants.MessageType.isCustomMessage(type);
        }
    }

}
