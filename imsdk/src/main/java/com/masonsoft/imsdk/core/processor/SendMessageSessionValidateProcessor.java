package com.masonsoft.imsdk.core.processor;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.lang.Processor;

public class SendMessageSessionValidateProcessor implements Processor<IMSessionMessage> {

    @Override
    public boolean doProcess(@Nullable IMSessionMessage target) {
        if (target == null) {
            // unexpected
            final Throwable e = new NullPointerException("SendMessageSessionValidateProcessor doProcess target is null");
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        if (target.getSessionUserId() <= 0) {
            target.getEnqueueCallback().onEnqueueFail(target, IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_SESSION_USER_ID, "未登录");
            return true;
        }
        return false;
    }

}
