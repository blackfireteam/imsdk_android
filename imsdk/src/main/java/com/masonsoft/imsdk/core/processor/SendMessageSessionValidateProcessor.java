package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;

/**
 * 校验待发送消息的 session 合法性
 */
public class SendMessageSessionValidateProcessor extends SendMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull IMSessionMessage target) {
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
