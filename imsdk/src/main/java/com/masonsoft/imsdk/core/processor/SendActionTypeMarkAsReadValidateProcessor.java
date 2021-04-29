package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.EnqueueCallback;
import com.masonsoft.imsdk.IMActionMessage;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMActionMessageManager;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;

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
        if (!(actionObject instanceof IMMessage)) {
            return false;
        }

        final IMMessage message = (IMMessage) actionObject;
        if (message.id.isUnset()
                || message.id.get() == null
                || message.id.get() <= 0) {
            // 消息没有入库，不支持回执消息已读
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_INVALID_MESSAGE_ID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_message_id));
            return true;
        }

        final Message dbMessage = MessageDatabaseProvider.getInstance().getMessage(
                message._sessionUserId.get(),
                message._conversationType.get(),
                message._targetUserId.get(),
                message.id.get());
        if (dbMessage == null) {
            // 消息没有找到
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_INVALID_MESSAGE_ID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_message_id));
            return true;
        }

        // 派发到指令发送队列
        IMActionMessageManager.getInstance().enqueueActionMessage(
                target.getSign(),
                target);
        return true;
    }

}
