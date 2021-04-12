package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.FetchMessageHistoryManager;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 过滤 FetchMessageHistory 响应结果
 *
 * @since 1.0
 */
public class ReceivedMessageFetchMessageHistoryProcessor extends ReceivedMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
        final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
        final long sign;

        if (protoMessageObject instanceof ProtoMessage.Result) {
            sign = ((ProtoMessage.Result) protoMessageObject).getSign();
        } else if (protoMessageObject instanceof ProtoMessage.ChatRBatch) {
            sign = ((ProtoMessage.ChatRBatch) protoMessageObject).getSign();
        } else {
            return false;
        }

        return FetchMessageHistoryManager.getInstance().dispatchTcpResponse(target.getSessionUserId(), sign, target.getProtoByteMessageWrapper());
    }

}
