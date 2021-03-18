package com.masonsoft.imsdk.core.processor;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
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
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_SESSION_USER_ID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_session_user_id)
            );

            return true;
        }
        return false;
    }

}
