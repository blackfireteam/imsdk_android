package com.masonsoft.imsdk.core.processor;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.lang.MultiProcessor;

/**
 * 内置的接收消息处理器
 *
 * @see ReceivedMessageProtoTypeProcessor
 * @since 1.0
 */
public class InternalReceivedMessageProtoTypeProcessor extends MultiProcessor<SessionProtoByteMessageWrapper> {

    public InternalReceivedMessageProtoTypeProcessor() {
        addLastProcessor(new ReceivedMessageProtoTypeProfileProcessor());
        addLastProcessor(new ReceivedMessageProtoTypeChatRProcessor());
        addLastProcessor(new ReceivedMessageProtoTypeChatRBatchProcessor());
    }

}
