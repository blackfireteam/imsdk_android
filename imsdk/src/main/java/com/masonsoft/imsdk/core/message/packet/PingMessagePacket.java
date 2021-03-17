package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.Message;
import com.masonsoft.imsdk.core.message.MessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 在长连接上的心跳消息包
 *
 * @since 1.0
 */
public class PingMessagePacket extends MessagePacket {

    private PingMessagePacket(Message message) {
        super(message);
    }

    @Override
    public boolean doProcess(@Nullable MessageWrapper target) {
        // 心跳消息包没有回执
        return false;
    }

    public static PingMessagePacket create() {
        return new PingMessagePacket(Message.Type.encode(ProtoMessage.Ping.newBuilder().build()));
    }

}
