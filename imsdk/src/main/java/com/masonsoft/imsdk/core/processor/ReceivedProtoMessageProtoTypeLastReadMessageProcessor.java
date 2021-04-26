package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 收到消息已读状态变更
 *
 * @since 1.0
 */
public class ReceivedProtoMessageProtoTypeLastReadMessageProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.LastReadMsg> {

    public ReceivedProtoMessageProtoTypeLastReadMessageProcessor() {
        super(ProtoMessage.LastReadMsg.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.LastReadMsg protoMessageObject) {
        final long sessionUserId = target.getSessionUserId();
        final TinyLastReadMsgProcessor proxy = new TinyLastReadMsgProcessor(sessionUserId);
        return proxy.doProcess(protoMessageObject);
    }

}
