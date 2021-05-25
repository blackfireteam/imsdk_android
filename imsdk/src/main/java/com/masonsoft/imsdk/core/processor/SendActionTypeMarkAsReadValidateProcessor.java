package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.EnqueueCallback;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMActionMessage;
import com.masonsoft.imsdk.core.IMActionMessageManager;

/**
 * 回执消息已读
 *
 * @since 1.0
 */
public class SendActionTypeMarkAsReadValidateProcessor extends SendActionTypeValidateProcessor {

    public SendActionTypeMarkAsReadValidateProcessor() {
        super(IMActionMessage.ACTION_TYPE_MARK_AS_READ);
    }

    @Override
    protected boolean doActionTypeProcess(@NonNull IMActionMessage target, int actionType) {
        final Object actionObject = target.getActionObject();
        if (!(actionObject instanceof Long)) {
            return false;
        }

        final long targetUserId = (long) actionObject;
        if (targetUserId <= 0) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_INVALID_TO_USER_ID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_to_user_id));
            return true;
        }

        // 提示成功入队
        target.getEnqueueCallback().onEnqueueSuccess(target);

        // 派发到指令发送队列
        IMActionMessageManager.getInstance().enqueueActionMessage(
                target.getSign(),
                target);
        return true;
    }

}
