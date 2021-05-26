package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMActionMessage;
import com.masonsoft.imsdk.core.IMActionMessageManager;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * 撤回消息
 *
 * @since 1.0
 */
public class SendActionTypeRevokeValidateProcessor extends SendActionTypeValidateProcessor {

    public SendActionTypeRevokeValidateProcessor() {
        super(IMActionMessage.ACTION_TYPE_REVOKE);
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
            // 消息没有入库，不支持撤回
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_MESSAGE_ID)
            );
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
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_MESSAGE_ID)
            );
            return true;
        }

        if (dbMessage.fromUserId.get() != target.getSessionUserId()) {
            // 不是自己发送的消息，不能撤回
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_FROM_USER_ID)
            );
            return true;
        }

        if (dbMessage.remoteMessageId.getOrDefault(0L) <= 0) {
            // 消息没有发送成功，不能撤回
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_MESSAGE_ID)
            );
            return true;
        }

        if (dbMessage.messageType.get() == IMConstants.MessageType.REVOKED) {
            // 消息已撤回
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_MESSAGE_ALREADY_REVOKE)
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
