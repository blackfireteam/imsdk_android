package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 在长连接上的心跳消息包
 *
 * @since 1.0
 */
public class PingMessagePacket extends MessagePacket {

    public PingMessagePacket(@NonNull ProtoByteMessage protoByteMessage) {
        super(protoByteMessage);
    }

    @Override
    public boolean doProcess(@Nullable ProtoByteMessageWrapper target) {
        // 心跳消息包没有回执
        return false;
    }

    @NonNull
    public static PingMessagePacket create() {
        return new PingMessagePacket(ProtoByteMessage.Type.encode(ProtoMessage.Ping.newBuilder().build()));
    }

}
