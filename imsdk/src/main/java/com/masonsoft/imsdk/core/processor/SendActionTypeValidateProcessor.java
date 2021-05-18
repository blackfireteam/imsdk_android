package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMActionMessage;

/**
 * @since 1.0
 */
public abstract class SendActionTypeValidateProcessor extends SendActionNotNullValidateProcessor {

    @Nullable
    private final int[] mActionTypeArray;

    public SendActionTypeValidateProcessor(@Nullable int... actionType) {
        mActionTypeArray = actionType;
    }

    @Override
    protected final boolean doNotNullProcess(@NonNull IMActionMessage target) {
        final int actionType = target.getActionType();
        if (mActionTypeArray != null) {
            for (int item : mActionTypeArray) {
                if (item == actionType) {
                    return doActionTypeProcess(target, item);
                }
            }
        }

        return false;
    }

    protected abstract boolean doActionTypeProcess(@NonNull IMActionMessage target, int actionType);

}
