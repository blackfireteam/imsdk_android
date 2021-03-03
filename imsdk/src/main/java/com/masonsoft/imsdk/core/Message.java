package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

/**
 * 传输于长连接上的原始消息
 *
 * @since 1.0
 */
public class Message {

    /**
     * proto buff 编码
     *
     * @see Type
     */
    private final int mType;

    /**
     * proto buff 的序列化数据
     */
    @NonNull
    private final byte[] mData;

    public Message(int type, @NonNull byte[] data) {
        this.mType = type;
        this.mData = data;
    }

    /**
     * @see Type
     */
    public int getType() {
        return mType;
    }

    @NonNull
    public byte[] getData() {
        return mData;
    }

    @NonNull
    public String toShortString() {
        return String.format("Message[type:%s, data length:%s]", mType, mData.length);
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

    /**
     * proto buff 编码
     *
     * @see #getType()
     */
    public static class Type {
        /**
         * 心跳
         */
        public static final int PING = 0;
        /**
         * 登录 IM
         */
        public static final int IM_LOGIN = 1;
        /**
         * 退出登录
         */
        public static final int IM_LOGOUT = 2;
        /**
         * 服务器回复的消息(对发送的非会话消息的回复)
         */
        public static final int RESULT = 3;
        /**
         * 发送会话消息
         */
        public static final int CHAT_S = 4;
        /**
         * 服务器回复的消息(对发送的会话消息的回复)
         */
        public static final int CHAT_S_R = 5;
        /**
         * 收到的服务器下发的会话消息(可能是别人给我发的，也可能是我给别人发的)
         */
        public static final int CHAT_R = 6;
        /**
         * 一次性收到多条会话消息(可能是别人给我发的，也可能是我给别人发的)
         */
        public static final int CHAT_R_BATCH = 7;
        /**
         * 请求获取历史消息
         */
        public static final int GET_HISTORY = 8;
        /**
         * 撤回消息
         */
        public static final int REVOKE = 9;
        /**
         * 消息已读
         */
        public static final int MSG_READ = 10;
        /**
         * 消息已读状态发生变更通知
         */
        public static final int LAST_READ_MSG = 11;
        /**
         * 删除会话
         */
        public static final int DEL_CHAT = 12;
        /**
         * 获取会话列表
         */
        public static final int GET_CHAT_LIST = 13;
        /**
         * 一个会话信息
         */
        public static final int CHAT_ITEM = 14;
        /**
         * 会话列表(并不一定包含全部会话)
         */
        public static final int CHAT_LIST = 15;
    }

}
