package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 收到一条新的聊天消息
 *
 * @since 1.0
 */
public class ReceivedProtoMessageProtoTypeChatRProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.ChatR> {

    public ReceivedProtoMessageProtoTypeChatRProcessor() {
        super(ProtoMessage.ChatR.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.ChatR protoMessageObject) {
        final TinyChatRProcessor proxy = new TinyChatRProcessor();
        return proxy.doProcess(target);
    }

}
