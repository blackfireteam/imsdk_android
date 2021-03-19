package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageFactory;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
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
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.ChatR protoMessageObject) {
        final Message message = MessageFactory.create(protoMessageObject);

        // TODO 撤回的消息类型需要交给消息发送队列处理
        return false;
    }

}
