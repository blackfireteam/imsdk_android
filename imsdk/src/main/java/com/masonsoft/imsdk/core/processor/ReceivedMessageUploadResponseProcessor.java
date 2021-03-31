package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessageUploadManager;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 处理发送消息的响应结果
 */
public class ReceivedMessageUploadResponseProcessor extends ReceivedMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
        final long sessionUserId = target.getSessionUserId();
        final ProtoByteMessageWrapper protoByteMessageWrapper = target.getProtoByteMessageWrapper();
        final Object protoMessageObject = protoByteMessageWrapper.getProtoMessageObject();

        final long sign;
        if (protoMessageObject instanceof ProtoMessage.Result) {
            sign = ((ProtoMessage.Result) protoMessageObject).getSign();
        } else if (protoMessageObject instanceof ProtoMessage.ChatSR) {
            sign = ((ProtoMessage.ChatSR) protoMessageObject).getSign();
        } else {
            return false;
        }

        return IMMessageUploadManager.getInstance().dispatchTcpResponse(
                sessionUserId,
                sign,
                protoByteMessageWrapper
        );
    }

}
