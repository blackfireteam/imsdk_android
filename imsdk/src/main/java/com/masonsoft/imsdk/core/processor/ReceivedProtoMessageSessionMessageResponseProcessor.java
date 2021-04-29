package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMSessionMessageUploadManager;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.util.Objects;

/**
 * 处理发送会话消息的响应结果
 */
public class ReceivedProtoMessageSessionMessageResponseProcessor extends ReceivedProtoMessageNotNullProcessor {

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

        if (sign <= 0) {
            return false;
        }

        final boolean result = IMSessionMessageUploadManager.getInstance().dispatchTcpResponse(
                sign,
                target
        );
        IMLog.v(Objects.defaultObjectTag(this) + " dispatchTcpResponse return:%s, sessionUserId:%s, sign:%s",
                result, sessionUserId, sign);
        return result;
    }

}
