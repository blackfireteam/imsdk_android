package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMActionMessageManager;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 过滤 IMActionMessage 响应结果
 *
 * @since 1.0
 */
public class ReceivedProtoMessageActionMessageResponseProcessor extends ReceivedProtoMessageNotNullProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
        final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
        final long sign;

        if (protoMessageObject instanceof ProtoMessage.Result) {
            sign = ((ProtoMessage.Result) protoMessageObject).getSign();
        } else if (protoMessageObject instanceof ProtoMessage.ChatR) {
            sign = ((ProtoMessage.ChatR) protoMessageObject).getSign();
        } else {
            return false;
        }

        if (sign <= 0) {
            return false;
        }

        return IMActionMessageManager.getInstance().dispatchTcpResponse(target.getSessionUserId(), sign, target.getProtoByteMessageWrapper());
    }

}
