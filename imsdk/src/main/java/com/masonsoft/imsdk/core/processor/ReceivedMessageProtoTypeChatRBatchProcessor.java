package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 收到一组连续的历史聊天消息
 *
 * @since 1.0
 */
public class ReceivedMessageProtoTypeChatRBatchProcessor extends ReceivedMessageProtoTypeProcessor<ProtoMessage.ChatRBatch> {

    public ReceivedMessageProtoTypeChatRBatchProcessor() {
        super(ProtoMessage.ChatRBatch.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(@NonNull ProtoMessage.ChatRBatch protoMessageObject) {
        // TODO
        return false;
    }

}
