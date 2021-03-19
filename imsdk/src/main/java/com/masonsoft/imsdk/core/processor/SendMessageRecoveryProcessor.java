package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.lang.StateProp;

/**
 * 矫正发送消息中的必要参数，如发送者，接受者，发送状态等。
 *
 * @see InternalSendMessageTypeValidateProcessor
 */
public class SendMessageRecoveryProcessor extends SendMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull IMSessionMessage target) {
        if (validateSendUser(target)) {
            return true;
        }
        if (validateToUser(target)) {
            return true;
        }

        if (validateId(target)) {
            return true;
        }

        if (validateSeq(target)) {
            return true;
        }

        return recoveryOthers(target);
    }

    /**
     * 校验发送者信息
     */
    private boolean validateSendUser(@NonNull IMSessionMessage target) {
        if (target.getSessionUserId() <= 0) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_SESSION_USER_ID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_session_user_id)
            );
            return true;
        }

        if (target.isResend()) {
            // 重新发送的消息，消息的发送者需要一致
            final IMMessage imMessage = target.getIMMessage();
            if (imMessage.fromUserId.isUnset()
                    || imMessage.fromUserId.get() != target.getSessionUserId()) {
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_FROM_USER_ID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_from_user_id)
                );
                return true;
            }
        }

        return false;
    }

    /**
     * 校验接收者信息
     */
    private boolean validateToUser(@NonNull IMSessionMessage target) {
        if (target.isResend()) {
            // 重新发送的消息，恢复消息接收者 id
            final IMMessage imMessage = target.getIMMessage();
            final StateProp<Long> toUserId = imMessage.toUserId;
            if (toUserId.isUnset()
                    || toUserId.get() == null
                    || toUserId.get() <= 0) {
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_TO_USER_ID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_to_user_id)
                );
                return true;
            }
            target.setToUserId(toUserId.get());
        }

        if (target.getToUserId() <= 0) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_TO_USER_ID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_to_user_id)
            );
            return true;
        }

        return false;
    }

    /**
     * 验证 id
     */
    private boolean validateId(@NonNull IMSessionMessage target) {
        final IMMessage imMessage = target.getIMMessage();
        if (target.isResend()) {
            // 重新发送的消息需要有正确的 id
            final StateProp<Long> id = imMessage.id;
            if (id.isUnset()
                    || id.get() == null
                    || id.get() <= 0) {
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_MESSAGE_ID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_message_id)
                );
                return true;
            }
        } else {
            // 新消息重置 id
            imMessage.id.clear();
        }

        return false;
    }

    /**
     * 验证 seq
     */
    private boolean validateSeq(@NonNull IMSessionMessage target) {
        final IMMessage imMessage = target.getIMMessage();
        if (target.isResend()) {
            // 重新发送的消息需要有正确的 seq
            final StateProp<Long> seq = imMessage.seq;
            if (seq.isUnset()
                    || seq.get() == null
                    || seq.get() <= 0) {
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_MESSAGE_SEQ,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_message_seq)
                );
                return true;
            }
        } else {
            // 新消息重置 seq
            imMessage.seq.clear();
        }

        return false;
    }

    /**
     * 重置其它字段
     */
    private boolean recoveryOthers(@NonNull IMSessionMessage target) {
        final IMMessage imMessage = target.getIMMessage();
        // 重置发送状态
        imMessage.sendState.clear();
        // 重置发送进度
        imMessage.sendProgress.clear();
        // 重置消息产生的时间
        imMessage.timeMs.clear();

        return false;
    }

}
