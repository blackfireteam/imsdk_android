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
 * @see InternalSendSessionMessageTypeValidateProcessor
 * @since 1.0
 */
public class SendSessionMessageRecoveryProcessor extends SendSessionMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull IMSessionMessage target) {
        target.getIMMessage().applyLogicField(
                target.getSessionUserId(),
                target.getConversationType(),
                target.getToUserId()
        );

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

        if (validateTimeMs(target)) {
            return true;
        }

        return validateOthers(target);
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
        } else {
            // 设置消息发送者
            target.getIMMessage().fromUserId.set(target.getSessionUserId());
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
        } else {
            // 设置消息接收者
            target.getIMMessage().toUserId.set(target.getToUserId());
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
     * 验证 timeMs
     */
    private boolean validateTimeMs(@NonNull IMSessionMessage target) {
        final IMMessage imMessage = target.getIMMessage();
        if (target.isResend()) {
            // 重新发送的消息需要有正确的时间
            final StateProp<Long> timeMs = imMessage.timeMs;
            if (timeMs.isUnset()
                    || timeMs.get() == null
                    || timeMs.get() <= 0) {
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_MESSAGE_TIME,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_message_time)
                );
                return true;
            }
        } else {
            // 新消息重置 timeMs
            imMessage.timeMs.clear();
        }

        return false;
    }

    /**
     * 验证其它字段
     */
    private boolean validateOthers(@NonNull IMSessionMessage target) {
        final IMMessage imMessage = target.getIMMessage();
        // 重置发送状态
        imMessage.sendState.clear();
        // 重置发送进度
        imMessage.sendProgress.clear();

        // 重置错误码 与 错误提示信息
        imMessage.errorCode.clear();
        imMessage.errorMessage.clear();

        return false;
    }

}
