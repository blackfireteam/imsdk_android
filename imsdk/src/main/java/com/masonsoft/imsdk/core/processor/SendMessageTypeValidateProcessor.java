package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.core.IMConstants.MessageType;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.lang.StateProp;

/**
 * 检查指定消息类型
 *
 * @see InternalSendMessageTypeValidateProcessor
 * @since 1.0
 */
public abstract class SendMessageTypeValidateProcessor extends SendMessageNotNullValidateProcessor {

    @Nullable
    private final int[] mTypeArray;

    /**
     * @see MessageType
     */
    public SendMessageTypeValidateProcessor(@Nullable int... type) {
        mTypeArray = type;
    }

    @Override
    protected boolean doNotNullProcess(@NonNull IMSessionMessage target) {
        final StateProp<Integer> type = target.getIMMessage().type;
        if (type.isUnset()) {
            // unexpected
            final Throwable e = new IllegalArgumentException("SendMessageTypeValidateProcessor doNotNullProcess target type is unset");
            IMLog.e(e, "target:%s", target);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        final Integer typeValue = type.get();
        if (mTypeArray != null) {
            for (int item : mTypeArray) {
                if (item == typeValue) {
                    return doTypeProcess(target, item);
                }
            }
        }

        return false;
    }

    /**
     * @see MessageType
     */
    protected abstract boolean doTypeProcess(@NonNull IMSessionMessage target, int type);

}
