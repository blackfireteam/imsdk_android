package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.proto.ProtoMessage;

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

        /**
         * 将 message 解码为 ProtoMessage 内定义的实体对象。如果解码失败，返回 null.
         */
        @Nullable
        public static Object decode(@NonNull Message message) {
            try {
                final int type = message.getType();
                final byte[] data = message.getData();
                switch (type) {
                    case PING:
                        return ProtoMessage.Ping.parseFrom(data);
                    case IM_LOGIN:
                        return ProtoMessage.ImLogin.parseFrom(data);
                    case IM_LOGOUT:
                        return ProtoMessage.ImLogout.parseFrom(data);
                    case RESULT:
                        return ProtoMessage.Result.parseFrom(data);
                    case CHAT_S:
                        return ProtoMessage.ChatS.parseFrom(data);
                    case CHAT_S_R:
                        return ProtoMessage.ChatSR.parseFrom(data);
                    case CHAT_R:
                        return ProtoMessage.ChatR.parseFrom(data);
                    case CHAT_R_BATCH:
                        return ProtoMessage.ChatRBatch.parseFrom(data);
                    case GET_HISTORY:
                        return ProtoMessage.GetHistory.parseFrom(data);
                    case REVOKE:
                        return ProtoMessage.Revoke.parseFrom(data);
                    case MSG_READ:
                        return ProtoMessage.MsgRead.parseFrom(data);
                    case LAST_READ_MSG:
                        return ProtoMessage.LastReadMsg.parseFrom(data);
                    case DEL_CHAT:
                        return ProtoMessage.DelChat.parseFrom(data);
                    case GET_CHAT_LIST:
                        return ProtoMessage.GetChatList.parseFrom(data);
                    case CHAT_ITEM:
                        return ProtoMessage.ChatItem.parseFrom(data);
                    case CHAT_LIST:
                        return ProtoMessage.ChatList.parseFrom(data);
                    default:
                        throw new IllegalAccessError("unknown type:" + type);
                }
            } catch (Throwable e) {
                IMLog.e(e);
            }
            IMLog.e(new IllegalAccessError(), "fail to decode message:%s", message.toString());
            return null;
        }

        /**
         * 将 ProtoMessage 内定义的实体对象编码为 message. 如果编码失败，返回 null.
         */
        @Nullable
        public static Message encode(@NonNull Object protoMessageObject) {
            if (protoMessageObject instanceof ProtoMessage.Ping) {
                return new Message(PING, ((ProtoMessage.Ping) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ImLogin) {
                return new Message(IM_LOGIN, ((ProtoMessage.ImLogin) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ImLogout) {
                return new Message(IM_LOGOUT, ((ProtoMessage.ImLogout) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.Result) {
                return new Message(RESULT, ((ProtoMessage.Result) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatS) {
                return new Message(CHAT_S, ((ProtoMessage.ChatS) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatSR) {
                return new Message(CHAT_S_R, ((ProtoMessage.ChatSR) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatR) {
                return new Message(CHAT_R, ((ProtoMessage.ChatR) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatRBatch) {
                return new Message(CHAT_R_BATCH, ((ProtoMessage.ChatRBatch) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.GetHistory) {
                return new Message(GET_HISTORY, ((ProtoMessage.GetHistory) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.Revoke) {
                return new Message(REVOKE, ((ProtoMessage.Revoke) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.MsgRead) {
                return new Message(MSG_READ, ((ProtoMessage.MsgRead) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.LastReadMsg) {
                return new Message(LAST_READ_MSG, ((ProtoMessage.LastReadMsg) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.DelChat) {
                return new Message(DEL_CHAT, ((ProtoMessage.DelChat) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.GetChatList) {
                return new Message(GET_CHAT_LIST, ((ProtoMessage.GetChatList) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatItem) {
                return new Message(CHAT_ITEM, ((ProtoMessage.ChatItem) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatList) {
                return new Message(CHAT_LIST, ((ProtoMessage.ChatList) protoMessageObject).toByteArray());
            }

            IMLog.e(new IllegalAccessError(), "unknown proto message object: %s", protoMessageObject);
            return null;
        }
    }

}
