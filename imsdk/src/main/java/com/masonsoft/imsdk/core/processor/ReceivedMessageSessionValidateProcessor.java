package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;

/**
 * 校验收到消息的 session 合法性
 */
public class ReceivedMessageSessionValidateProcessor extends ReceivedMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
        if (target.getSessionUserId() <= 0) {
            // unexpected
            final Throwable e = new IllegalArgumentException("invalid SessionProtoByteMessageWrapper sessionUserId:" + target.getSessionUserId());
            IMLog.e(e);
            return true;
        }

        return false;
    }

}
