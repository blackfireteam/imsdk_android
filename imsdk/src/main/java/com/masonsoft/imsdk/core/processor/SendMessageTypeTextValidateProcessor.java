package com.masonsoft.imsdk.core.processor;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.lang.StateProp;

/**
 * 发送文字类型的消息合法性检查
 *
 * @since 1.0
 */
public class SendMessageTypeTextValidateProcessor extends SendMessageTypeValidateProcessor {

    public SendMessageTypeTextValidateProcessor() {
        super(IMConstants.MessageType.TEXT);
    }

    @Override
    protected boolean doTypeProcess(@NonNull IMSessionMessage target, int type) {
        final StateProp<String> body = target.getIMMessage().body;
        if (body.isUnset()) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_TEXT_MESSAGE_TEXT_UNSET,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_text_message_text_unset)
            );
            return true;
        }

        String text = body.get();
        if (text == null) {
            text = "";
            // 如果发送的是空对象，变更设置为空字符串
            body.set(text);
        }

        if (IMConstants.SendMessageOption.Text.TRIM_REQUIRED) {
            text = text.trim();
            // 去掉首尾空白字符后，应用变更
            body.set(text);
        }

        if (TextUtils.isEmpty(text)) {
            if (IMConstants.SendMessageOption.Text.ALLOW_EMPTY) {
                // 允许发送空字符串
                return false;
            }

            // 不允许发送空字符串
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_TEXT_MESSAGE_TEXT_EMPTY,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_text_message_text_empty)
            );
            return true;
        }

        if (IMConstants.SendMessageOption.Text.MAX_LENGTH > 0 &&
                text.length() > IMConstants.SendMessageOption.Text.MAX_LENGTH) {
            // 文字长度超过了限制
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_TEXT_MESSAGE_TEXT_TOO_LARGE,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_text_message_text_too_large)
            );
            return true;
        }

        return false;
    }

}
