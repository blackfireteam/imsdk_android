package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 处理单条 ChatItemUpdate 消息
 *
 * @since 1.0
 */
public class ReceivedProtoMessageConversationUpdateProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.ChatItemUpdate> {

    public ReceivedProtoMessageConversationUpdateProcessor() {
        super(ProtoMessage.ChatItemUpdate.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(@NonNull SessionProtoByteMessageWrapper target, @NonNull ProtoMessage.ChatItemUpdate protoMessageObject) {
        final TinyConversationUpdateProcessor proxy = new TinyConversationUpdateProcessor();
        return proxy.doProcess(target);
    }

}
