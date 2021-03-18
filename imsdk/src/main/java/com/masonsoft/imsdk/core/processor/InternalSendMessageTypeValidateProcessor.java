package com.masonsoft.imsdk.core.processor;

import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.lang.MultiProcessor;

/**
 * 内置的发送消息类型检查处理器。检查所有内置类型的参数是否合法
 *
 * @see SendMessageTypeValidateProcessor
 */
public class InternalSendMessageTypeValidateProcessor extends MultiProcessor<IMSessionMessage> {

    public InternalSendMessageTypeValidateProcessor() {
        addLastProcessor(new SendMessageTypeTextValidateProcessor());
    }

}
