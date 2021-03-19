package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 收到一条新的聊天消息
 *
 * @since 1.0
 */
public class ReceivedMessageProtoTypeChatRProcessor extends ReceivedMessageProtoTypeProcessor<ProtoMessage.ChatR> {

    public ReceivedMessageProtoTypeChatRProcessor() {
        super(ProtoMessage.ChatR.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(@NonNull ProtoMessage.ChatR protoMessageObject) {
        return false;
    }

}
