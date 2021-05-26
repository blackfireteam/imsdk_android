package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMActionMessage;
import com.masonsoft.imsdk.core.IMActionMessageManager;
import com.masonsoft.imsdk.lang.GeneralResult;

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
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_TO_USER_ID)
            );
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
