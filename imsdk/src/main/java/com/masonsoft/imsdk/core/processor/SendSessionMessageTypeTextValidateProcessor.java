package com.masonsoft.imsdk.core.processor;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMSessionMessage;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.lang.StateProp;

/**
 * 发送文字类型的消息合法性检查
 *
 * @since 1.0
 */
public class SendSessionMessageTypeTextValidateProcessor extends SendSessionMessageTypeValidateProcessor {

    public SendSessionMessageTypeTextValidateProcessor() {
        super(IMConstants.MessageType.TEXT);
    }

    @Override
    protected boolean doTypeProcess(@NonNull IMSessionMessage target, int type) {
        final StateProp<String> body = target.getMessage().body;
        if (body.isUnset()) {
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_TEXT_MESSAGE_TEXT_UNSET)
                            .withPayload(target)
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
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_TEXT_MESSAGE_TEXT_EMPTY)
                            .withPayload(target)
            );
            return true;
        }

        if (IMConstants.SendMessageOption.Text.MAX_LENGTH > 0 &&
                text.length() > IMConstants.SendMessageOption.Text.MAX_LENGTH) {
            // 文字长度超过了限制
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_TEXT_MESSAGE_TEXT_TOO_LARGE)
                            .withPayload(target)
            );
            return true;
        }

        return false;
    }

}
