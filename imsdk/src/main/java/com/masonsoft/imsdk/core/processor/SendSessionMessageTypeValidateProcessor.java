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
 * @see InternalSendSessionMessageTypeValidateProcessor
 * @since 1.0
 */
public abstract class SendSessionMessageTypeValidateProcessor extends SendSessionMessageNotNullValidateProcessor {

    @Nullable
    private final int[] mTypeArray;

    /**
     * @see MessageType
     */
    public SendSessionMessageTypeValidateProcessor(@Nullable int... type) {
        mTypeArray = type;
    }

    @Override
    protected final boolean doNotNullProcess(@NonNull IMSessionMessage target) {
        final StateProp<Integer> type = target.getIMMessage().type;
        if (type.isUnset()) {
            // unexpected
            final Throwable e = new IllegalArgumentException("SendMessageTypeValidateProcessor doNotNullProcess target type is unset");
            IMLog.e(e, "target:%s", target);
            RuntimeMode.fixme(e);
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
