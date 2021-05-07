package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.annotation.DemoOnly;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;

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
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" type:").append(this.mType);
        builder.append(" data length:").append(mData.length);
        return builder.toString();
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

        private interface Transform {
            @Nullable
            Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException;

            @Nullable
            ProtoByteMessage encode(@NonNull Object protoMessageObject);
        }

        private static final List<Transform> TRANSFORM_LIST = new ArrayList<>();

        /**
         * 心跳
         */
        public static final int PING = 0;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == PING) {
                        return ProtoMessage.Ping.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.Ping) {
                        return new ProtoByteMessage(PING, ((ProtoMessage.Ping) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 登录 IM
         */
        public static final int IM_LOGIN = 1;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == IM_LOGIN) {
                        return ProtoMessage.ImLogin.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ImLogin) {
                        return new ProtoByteMessage(IM_LOGIN, ((ProtoMessage.ImLogin) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 退出登录
         */
        public static final int IM_LOGOUT = 2;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == IM_LOGOUT) {
                        return ProtoMessage.ImLogout.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ImLogout) {
                        return new ProtoByteMessage(IM_LOGOUT, ((ProtoMessage.ImLogout) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 服务器回复的消息(对发送的非会话消息的回复)
         */
        public static final int RESULT = 3;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == RESULT) {
                        return ProtoMessage.Result.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.Result) {
                        return new ProtoByteMessage(RESULT, ((ProtoMessage.Result) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 发送会话消息
         */
        public static final int CHAT_S = 4;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == CHAT_S) {
                        return ProtoMessage.ChatS.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ChatS) {
                        return new ProtoByteMessage(CHAT_S, ((ProtoMessage.ChatS) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 服务器回复的消息(对发送的会话消息的回复)
         */
        public static final int CHAT_S_R = 5;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == CHAT_S_R) {
                        return ProtoMessage.ChatSR.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ChatSR) {
                        return new ProtoByteMessage(CHAT_S_R, ((ProtoMessage.ChatSR) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 收到的服务器下发的会话消息(可能是别人给我发的，也可能是我给别人发的)
         */
        public static final int CHAT_R = 6;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == CHAT_R) {
                        return ProtoMessage.ChatR.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ChatR) {
                        return new ProtoByteMessage(CHAT_R, ((ProtoMessage.ChatR) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 一次性收到多条会话消息(可能是别人给我发的，也可能是我给别人发的)
         */
        public static final int CHAT_R_BATCH = 7;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == CHAT_R_BATCH) {
                        return ProtoMessage.ChatRBatch.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ChatRBatch) {
                        return new ProtoByteMessage(CHAT_R_BATCH, ((ProtoMessage.ChatRBatch) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 请求获取历史消息
         */
        public static final int GET_HISTORY = 8;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == GET_HISTORY) {
                        return ProtoMessage.GetHistory.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.GetHistory) {
                        return new ProtoByteMessage(GET_HISTORY, ((ProtoMessage.GetHistory) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 撤回消息
         */
        public static final int REVOKE = 9;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == REVOKE) {
                        return ProtoMessage.Revoke.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.Revoke) {
                        return new ProtoByteMessage(REVOKE, ((ProtoMessage.Revoke) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 消息已读
         */
        public static final int MSG_READ = 10;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == MSG_READ) {
                        return ProtoMessage.MsgRead.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.MsgRead) {
                        return new ProtoByteMessage(MSG_READ, ((ProtoMessage.MsgRead) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 删除会话
         */
        public static final int DEL_CHAT = 11;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == DEL_CHAT) {
                        return ProtoMessage.DelChat.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.DelChat) {
                        return new ProtoByteMessage(DEL_CHAT, ((ProtoMessage.DelChat) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 获取会话列表
         */
        public static final int GET_CHAT_LIST = 12;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == GET_CHAT_LIST) {
                        return ProtoMessage.GetChatList.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.GetChatList) {
                        return new ProtoByteMessage(GET_CHAT_LIST, ((ProtoMessage.GetChatList) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 一个会话信息
         */
        public static final int CHAT_ITEM = 13;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == CHAT_ITEM) {
                        return ProtoMessage.ChatItem.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ChatItem) {
                        return new ProtoByteMessage(CHAT_ITEM, ((ProtoMessage.ChatItem) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 一个会话信息发生变更
         */
        public static final int CHAT_ITEM_UPDATE = 14;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == CHAT_ITEM_UPDATE) {
                        return ProtoMessage.ChatItemUpdate.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ChatItemUpdate) {
                        return new ProtoByteMessage(CHAT_ITEM_UPDATE, ((ProtoMessage.ChatItemUpdate) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 会话列表(并不一定包含全部会话)
         */
        public static final int CHAT_LIST = 15;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == CHAT_LIST) {
                        return ProtoMessage.ChatList.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ChatList) {
                        return new ProtoByteMessage(CHAT_LIST, ((ProtoMessage.ChatList) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 获取用户的 profile
         */
        public static final int GET_PROFILE = 16;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == GET_PROFILE) {
                        return ProtoMessage.GetProfile.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.GetProfile) {
                        return new ProtoByteMessage(GET_PROFILE, ((ProtoMessage.GetProfile) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 批量获取用户的 profile
         */
        public static final int GET_PROFILES = 17;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == GET_PROFILES) {
                        return ProtoMessage.GetProfiles.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.GetProfiles) {
                        return new ProtoByteMessage(GET_PROFILES, ((ProtoMessage.GetProfiles) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 用户信息
         */
        public static final int PROFILE = 18;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == PROFILE) {
                        return ProtoMessage.Profile.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.Profile) {
                        return new ProtoByteMessage(PROFILE, ((ProtoMessage.Profile) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 批量用户信息
         */
        public static final int PROFILE_LIST = 19;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == PROFILE_LIST) {
                        return ProtoMessage.ProfileList.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ProfileList) {
                        return new ProtoByteMessage(PROFILE_LIST, ((ProtoMessage.ProfileList) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 通知客户端用户上线事件
         */
        @DemoOnly
        public static final int PROFILE_ONLINE = 50;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == PROFILE_ONLINE) {
                        return ProtoMessage.ProfileOnline.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.ProfileOnline) {
                        return new ProtoByteMessage(PROFILE_ONLINE, ((ProtoMessage.ProfileOnline) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 通知客户端用户下线事件
         */
        @DemoOnly
        public static final int USR_OFFLINE = 52;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == USR_OFFLINE) {
                        return ProtoMessage.UsrOffline.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.UsrOffline) {
                        return new ProtoByteMessage(USR_OFFLINE, ((ProtoMessage.UsrOffline) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 注册新用户
         */
        @DemoOnly
        public static final int SIGN_UP = 53;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == SIGN_UP) {
                        return ProtoMessage.Signup.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.Signup) {
                        return new ProtoByteMessage(SIGN_UP, ((ProtoMessage.Signup) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 获取 spark
         */
        @DemoOnly
        public static final int FETCH_SPARK = 54;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == FETCH_SPARK) {
                        return ProtoMessage.FetchSpark.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.FetchSpark) {
                        return new ProtoByteMessage(FETCH_SPARK, ((ProtoMessage.FetchSpark) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * spark
         */
        @DemoOnly
        public static final int SPARK = 55;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == SPARK) {
                        return ProtoMessage.Spark.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.Spark) {
                        return new ProtoByteMessage(SPARK, ((ProtoMessage.Spark) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 多个 spark
         */
        @DemoOnly
        public static final int SPARKS = 56;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == SPARKS) {
                        return ProtoMessage.Sparks.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.Sparks) {
                        return new ProtoByteMessage(SPARKS, ((ProtoMessage.Sparks) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 获取用户 token
         */
        @DemoOnly
        public static final int GET_IM_TOKEN = 57;

        static {
            TRANSFORM_LIST.add(new Transform() {
                @Override
                public Object decode(@NonNull ProtoByteMessage protoByteMessage) throws com.google.protobuf.InvalidProtocolBufferException {
                    if (protoByteMessage.getType() == GET_IM_TOKEN) {
                        return ProtoMessage.GetImToken.parseFrom(protoByteMessage.getData());
                    }
                    return null;
                }

                @Override
                public ProtoByteMessage encode(@NonNull Object protoMessageObject) {
                    if (protoMessageObject instanceof ProtoMessage.GetImToken) {
                        return new ProtoByteMessage(GET_IM_TOKEN, ((ProtoMessage.GetImToken) protoMessageObject).toByteArray());
                    }
                    return null;
                }
            });
        }

        /**
         * 将 ProtoByteMessage 解码为 ProtoMessage 内定义的实体对象。如果解码失败，返回 null.
         */
        @Nullable
        public static Object decode(@NonNull ProtoByteMessage protoByteMessage) {
            try {
                for (Transform transform : TRANSFORM_LIST) {
                    Object result = transform.decode(protoByteMessage);
                    if (result != null) {
                        return result;
                    }
                }
            } catch (Throwable e) {
                IMLog.e(e);
            }
            IMLog.e(new IllegalAccessError(), "fail to decode protoByteMessage:%s", protoByteMessage.toString());
            return null;
        }

        /**
         * 将 ProtoMessage 内定义的实体对象编码为 ProtoByteMessage. 如果编码失败，将抛出异常.
         */
        @NonNull
        public static ProtoByteMessage encode(@NonNull Object protoMessageObject) {
            for (Transform transform : TRANSFORM_LIST) {
                ProtoByteMessage result = transform.encode(protoMessageObject);
                if (result != null) {
                    return result;
                }
            }

            final Throwable e = new IllegalArgumentException("unknown proto message object: " + protoMessageObject);
            throw new RuntimeException(e);
        }
    }

}
