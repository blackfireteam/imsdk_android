package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 在长连接上的心跳消息包
 *
 * @since 1.0
 */
public class PingMessagePacket extends ResultIgnoreMessagePacket {

    private PingMessagePacket(@NonNull ProtoByteMessage protoByteMessage) {
        super(protoByteMessage);
    }

    @NonNull
    public static PingMessagePacket create() {
        return new PingMessagePacket(ProtoByteMessage.Type.encode(ProtoMessage.Ping.newBuilder().build()));
    }

}
