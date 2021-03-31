package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessageUploadManager;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;

/**
 * 处理发送消息的响应结果
 */
public class ReceivedMessageUploadResponseProcessor extends ReceivedMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
        final long sessionUserId = target.getSessionUserId();
        final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
        if (protoMessageObject != null) {
            if (IMMessageUploadManager.getInstance().dispatchTcpResponse(sessionUserId, protoMessageObject)) {
                return true;
            }
        }
        return false;
    }

}
