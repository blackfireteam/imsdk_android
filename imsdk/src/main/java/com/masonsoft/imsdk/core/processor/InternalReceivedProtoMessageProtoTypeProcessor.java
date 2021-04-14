package com.masonsoft.imsdk.core.processor;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.lang.MultiProcessor;

/**
 * 内置的接收消息处理器
 *
 * @see ReceivedProtoMessageProtoTypeProcessor
 * @since 1.0
 */
public class InternalReceivedProtoMessageProtoTypeProcessor extends MultiProcessor<SessionProtoByteMessageWrapper> {

    public InternalReceivedProtoMessageProtoTypeProcessor() {
        addLastProcessor(new ReceivedProtoMessageActionMessageResponseProcessor());
        addLastProcessor(new ReceivedProtoMessageFetchMessageHistoryResponseProcessor());
        addLastProcessor(new ReceivedProtoMessageProtoTypeProfileProcessor());
        addLastProcessor(new ReceivedProtoMessageProtoTypeProfileListProcessor());
        addLastProcessor(new ReceivedProtoMessageProtoTypeChatRProcessor());
    }

}
