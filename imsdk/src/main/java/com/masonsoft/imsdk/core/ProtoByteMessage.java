package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 传输于长连接上的原始消息
 *
 * @since 1.0
 */
public class ProtoByteMessage {

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

    public ProtoByteMessage(int type, @NonNull byte[] data) {
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
        return String.format("ProtoByteMessage[type:%s, data length:%s]", mType, mData.length);
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
         * 获取用户的 profile
         */
        public static final int GET_PROFILE = 16;
        /**
         * 批量获取用户的 profile
         */
        public static final int GET_PROFILES = 17;
        /**
         * 用户信息
         */
        public static final int PROFILE = 18;
        /**
         * 批量用户信息
         */
        public static final int PROFILE_LIST = 19;
        /**
         * block 指定用户
         */
        public static final int BLOCK_U = 20;
        /**
         * unblock 指定用户
         */
        public static final int UNBLOCK_U = 21;

        /**
         * 将 ProtoByteMessage 解码为 ProtoMessage 内定义的实体对象。如果解码失败，返回 null.
         */
        @Nullable
        public static Object decode(@NonNull ProtoByteMessage protoByteMessage) {
            try {
                final int type = protoByteMessage.getType();
                final byte[] data = protoByteMessage.getData();
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
                    case GET_PROFILE:
                        return ProtoMessage.GetProfile.parseFrom(data);
                    case GET_PROFILES:
                        return ProtoMessage.GetProfiles.parseFrom(data);
                    case PROFILE:
                        return ProtoMessage.Profile.parseFrom(data);
                    case PROFILE_LIST:
                        return ProtoMessage.ProfileList.parseFrom(data);
                    case BLOCK_U:
                        return ProtoMessage.BlockU.parseFrom(data);
                    case UNBLOCK_U:
                        return ProtoMessage.UnblockU.parseFrom(data);
                    default:
                        throw new IllegalAccessError("unknown type:" + type);
                }
            } catch (Throwable e) {
                IMLog.e(e);
            }
            IMLog.e(new IllegalAccessError(), "fail to decode protoByteMessage:%s", protoByteMessage.toString());
            return null;
        }

        /**
         * 将 ProtoMessage 内定义的实体对象编码为 ProtoByteMessage. 如果编码失败，返回 null.
         */
        @Nullable
        public static ProtoByteMessage encode(@NonNull Object protoMessageObject) {
            if (protoMessageObject instanceof ProtoMessage.Ping) {
                return new ProtoByteMessage(PING, ((ProtoMessage.Ping) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ImLogin) {
                return new ProtoByteMessage(IM_LOGIN, ((ProtoMessage.ImLogin) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ImLogout) {
                return new ProtoByteMessage(IM_LOGOUT, ((ProtoMessage.ImLogout) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.Result) {
                return new ProtoByteMessage(RESULT, ((ProtoMessage.Result) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatS) {
                return new ProtoByteMessage(CHAT_S, ((ProtoMessage.ChatS) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatSR) {
                return new ProtoByteMessage(CHAT_S_R, ((ProtoMessage.ChatSR) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatR) {
                return new ProtoByteMessage(CHAT_R, ((ProtoMessage.ChatR) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatRBatch) {
                return new ProtoByteMessage(CHAT_R_BATCH, ((ProtoMessage.ChatRBatch) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.GetHistory) {
                return new ProtoByteMessage(GET_HISTORY, ((ProtoMessage.GetHistory) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.Revoke) {
                return new ProtoByteMessage(REVOKE, ((ProtoMessage.Revoke) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.MsgRead) {
                return new ProtoByteMessage(MSG_READ, ((ProtoMessage.MsgRead) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.LastReadMsg) {
                return new ProtoByteMessage(LAST_READ_MSG, ((ProtoMessage.LastReadMsg) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.DelChat) {
                return new ProtoByteMessage(DEL_CHAT, ((ProtoMessage.DelChat) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.GetChatList) {
                return new ProtoByteMessage(GET_CHAT_LIST, ((ProtoMessage.GetChatList) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatItem) {
                return new ProtoByteMessage(CHAT_ITEM, ((ProtoMessage.ChatItem) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ChatList) {
                return new ProtoByteMessage(CHAT_LIST, ((ProtoMessage.ChatList) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.GetProfile) {
                return new ProtoByteMessage(GET_PROFILE, ((ProtoMessage.GetProfile) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.GetProfiles) {
                return new ProtoByteMessage(GET_PROFILES, ((ProtoMessage.GetProfiles) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.Profile) {
                return new ProtoByteMessage(PROFILE, ((ProtoMessage.Profile) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.ProfileList) {
                return new ProtoByteMessage(PROFILE_LIST, ((ProtoMessage.ProfileList) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.BlockU) {
                return new ProtoByteMessage(BLOCK_U, ((ProtoMessage.BlockU) protoMessageObject).toByteArray());
            }

            if (protoMessageObject instanceof ProtoMessage.UnblockU) {
                return new ProtoByteMessage(UNBLOCK_U, ((ProtoMessage.UnblockU) protoMessageObject).toByteArray());
            }

            IMLog.e(new IllegalAccessError(), "unknown proto message object: %s", protoMessageObject);
            return null;
        }
    }

}
