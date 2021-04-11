package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.ResultIgnoreMessagePacket;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 过滤 FetchMessageHistory Result
 *
 * @since 1.0
 */
public class ReceivedMessageResultFetchMessageHistoryProcessor extends ReceivedMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
        final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();

        if (protoMessageObject instanceof ProtoMessage.Result) {
            return ((ProtoMessage.Result) protoMessageObject).getSign() == ResultIgnoreMessagePacket.SIGN_IGNORE;
        }

        return false;
    }

}
